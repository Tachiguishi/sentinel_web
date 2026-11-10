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

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

public class StationScdImportService {
	private final Path directory;

	public StationScdImportService(@Value("${sentinel.scd.directory:data/scd}") Path directory) {
		this.directory = directory.toAbsolutePath().normalize();
	}

	public synchronized int importScd(String stationName, InputStream upload) throws IOException {
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

			return parsed.devices().size();
		} finally {
			Files.deleteIfExists(scd);
		}
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
					LegacyScdSqliteIds.repair(new JdbcTemplate(context.getBean(DataSource.class)));
				}
				context.getBean(ScdService.class).persistSCD(parsed.version(), parsed.devices(), monitor);
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
}
