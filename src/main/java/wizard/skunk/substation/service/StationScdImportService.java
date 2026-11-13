package wizard.skunk.substation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import wizard.skunk.common.StandardMonitor;
import wizard.skunk.datasource.SqliteJdbcUrlBuilder;
import wizard.skunk.parser.domain.IED;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.DeviceCategory;
import wizard.skunk.persistence.substation.domain.Bay;
import wizard.skunk.persistence.substation.domain.VoltageLevel;
import wizard.skunk.persistence.substation.repository.BayRepository;
import wizard.skunk.persistence.substation.repository.VoltageLevelRepository;
import wizard.skunk.persistence.substation.repository.DeviceRepository;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;

public class StationScdImportService {
	private static final int CATEGORY_UPDATE_BATCH_SIZE = 500;
	private final Path directory;

	public StationScdImportService(@Value("${sentinel.scd.directory:data/scd}") Path directory) {
		this.directory = directory.toAbsolutePath().normalize();
	}

	public synchronized void updateDeviceCategories(String stationName, List<String> deviceNames, DeviceCategory category) {
		Path database = databasePath(stationName);
		if (Files.notExists(database)) {
			throw new NoSuchElementException("装置不存在");
		}
		List<String> names = List.copyOf(new LinkedHashSet<>(deviceNames));
		write(database, false, context -> {
			DeviceRepository repository = context.getBean(DeviceRepository.class);
			for (int start = 0; start < names.size(); start += CATEGORY_UPDATE_BATCH_SIZE) {
				List<String> batch = names.subList(start, Math.min(names.size(), start + CATEGORY_UPDATE_BATCH_SIZE));
				List<Device> devices = repository.findByNameIn(batch);
				Set<String> found = new HashSet<>();
				for (Device device : devices) {
					found.add(device.getName());
				}
				if (devices.size() != batch.size() || !found.containsAll(batch)) {
					throw new NoSuchElementException("有装置不存在，请刷新列表");
				}
				devices.forEach(device -> device.overrideCategory(category));
			}
		});
	}

	public synchronized ImportResult importScd(String stationName, InputStream upload) throws IOException {
		Files.createDirectories(directory);
		Path scd = Files.createTempFile(directory, ".upload-", ".scd");
		try {
			Files.copy(upload, scd, StandardCopyOption.REPLACE_EXISTING);
			StandardMonitor monitor = new StandardMonitor();
			SclService.ParsedScd parsed = readScd(scd, monitor);

			Path database = databasePath(stationName);
			if (Files.notExists(database)) {
				Path staging = Files.createTempFile(directory, ".import-", ".sqlite");
				try {
					persist(staging, parsed, monitor, true);
					Files.move(staging, database, StandardCopyOption.ATOMIC_MOVE);
				} finally {
					Files.deleteIfExists(staging);
				}
			} else {
				persist(database, parsed, monitor, false);
			}

			return new ImportResult(parsed.devices().size(), LocalDateTime.now().withNano(0));
		} finally {
			Files.deleteIfExists(scd);
		}
	}

	public synchronized void updateDeviceBays(String stationName, List<BayAssignment> assignments,
			List<StationBayCatalogService.BayOption> catalog) {
		Path database = databasePath(stationName);
		if (Files.notExists(database)) throw new NoSuchElementException("请先导入 SCD 装置");
		Map<String, StationBayCatalogService.BayOption> options = new HashMap<>();
		catalog.forEach(bay -> options.put(bay.code(), bay));
		// Validate before opening a write transaction, including callers outside the controller.
		for (BayAssignment assignment : assignments) {
			if (assignment.bayCode() != null && !options.containsKey(assignment.bayCode())) {
				throw new IllegalArgumentException("无效的间隔代码");
			}
		}
		write(database, false, context -> {
			DeviceRepository devices = context.getBean(DeviceRepository.class);
			BayRepository bays = context.getBean(BayRepository.class);
			VoltageLevelRepository voltages = context.getBean(VoltageLevelRepository.class);
			Map<String, Bay> byCode = new HashMap<>();
			bays.findAll().forEach(bay -> { if (bay.getCode() != null) byCode.put(bay.getCode(), bay); });
			Map<Integer, VoltageLevel> byVoltage = new HashMap<>();
			voltages.findAll().forEach(voltage -> {
				if ("k".equals(voltage.getMultiplier()) && "V".equals(voltage.getUnit())) {
					byVoltage.put(voltage.getValue(), voltage);
				}
			});
			for (BayAssignment assignment : assignments) {
				Device device = devices.findByName(assignment.device());
				if (device == null) throw new NoSuchElementException("装置已不存在，请刷新列表");
				if (assignment.bayCode() == null) {
					device.setBay(null);
					continue;
				}
				var option = options.get(assignment.bayCode());
				Bay bay = byCode.get(option.code());
				if (bay == null) {
					bay = new Bay();
					bay.setCode(option.code());
				}
				bay.setName(option.name());
				bay.setDescription(option.description());
				VoltageLevel voltage = null;
				if (option.voltage() != null) {
					voltage = byVoltage.get(option.voltage());
					if (voltage == null) {
						voltage = new VoltageLevel();
						voltage.setValue(option.voltage());
						voltage.setMultiplier("k");
						voltage = voltages.save(voltage);
						byVoltage.put(option.voltage(), voltage);
					}
				}
				bay.setVoltageLevel(voltage);
				bay = bays.save(bay);
				byCode.put(option.code(), bay);
				device.setBay(bay);
			}
		});
	}

	private SclService.ParsedScd readScd(Path file, StandardMonitor monitor) {
		SclService.ParsedScd parsed;
		try {
			parsed = SclService.readSCD(file.toFile(), monitor);
		} catch (Exception exception) {
			throw new IllegalArgumentException("无法解析 SCD 文件，请检查文件格式。", exception);
		}

		Set<String> names = new HashSet<>();
		for (IED device : parsed.devices()) {
			if (device.getName().isBlank() || !names.add(device.getName())) {
				throw new IllegalArgumentException("SCD 文件包含空白或重复的装置名称。");
			}
		}
		if (names.isEmpty()) {
			throw new IllegalArgumentException("SCD 文件中没有装置。");
		}
		return parsed;
	}

	private void persist(Path database, SclService.ParsedScd parsed, StandardMonitor monitor, boolean createSchema) {
		write(database, createSchema, context ->
				context.getBean(ScdService.class).persistSCD(parsed.version(), parsed.devices(), monitor));
	}

	private void write(Path database, boolean createSchema, Consumer<AnnotationConfigApplicationContext> action) {
		// A separate context keeps the copied repositories and transaction manager away from MySQL.
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.registerBean(DataSource.class, () -> {
				DriverManagerDataSource dataSource = new DriverManagerDataSource(
						SqliteJdbcUrlBuilder.fromPath(database.toString()) + "?mode=rw");
				dataSource.setDriverClassName("org.sqlite.JDBC");
				return dataSource;
			});
			context.registerBean(ScdSqliteConfiguration.class, () -> new ScdSqliteConfiguration(createSchema));
			context.refresh();

			TransactionTemplate transaction = new TransactionTemplate(
					context.getBean(JpaTransactionManager.class));
			transaction.executeWithoutResult(status -> {
				if (!createSchema) {
					JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
					LegacyScdSqliteIds.repair(jdbc);
					boolean hasCategoryOverride = jdbc.query("PRAGMA table_info(device)",
							(rs, row) -> rs.getString("name")).contains("category_overridden");
					if (!hasCategoryOverride) {
						jdbc.execute("ALTER TABLE device ADD COLUMN category_overridden BOOLEAN");
					}
					if (!jdbc.query("PRAGMA table_info(bay)", (rs, row) -> rs.getString("name")).contains("code")) {
						jdbc.execute("ALTER TABLE bay ADD COLUMN code VARCHAR(48)");
					}
				}
				action.accept(context);
			});
		}
	}

	Path databasePath(String stationName) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(stationName.getBytes(StandardCharsets.UTF_8));
			return directory.resolve(HexFormat.of().formatHex(digest) + ".sqlite");
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}

	public record ImportResult(int deviceCount, LocalDateTime scdUpdatedAt) {}

	public record BayAssignment(String device, String bayCode) {}
}
