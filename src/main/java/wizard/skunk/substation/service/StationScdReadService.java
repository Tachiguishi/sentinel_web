package wizard.skunk.substation.service;

import jakarta.annotation.PreDestroy;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypesScanner;
import wizard.skunk.datasource.SqliteJdbcUrlBuilder;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.DeviceCategory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class StationScdReadService {
	private final StationScdImportService imports;
	private final SessionFactory sqliteSessions;

	public StationScdReadService(StationScdImportService imports, ResourceLoader resourceLoader) {
		this.imports = imports;
		MetadataSources mappings = new MetadataSources();
		new PersistenceManagedTypesScanner(resourceLoader).scan(Device.class.getPackageName())
				.getManagedClassNames().forEach(mappings::addAnnotatedClassName);
		Configuration configuration = new Configuration(mappings)
				.setProperty("hibernate.dialect", ScdSqliteConfiguration.DIALECT)
				.setProperty("hibernate.physical_naming_strategy", ScdSqliteConfiguration.NAMING_STRATEGY)
				.setProperty("hibernate.boot.allow_jdbc_metadata_access", "false");
		sqliteSessions = configuration.buildSessionFactory();
	}

	public List<DeviceSummary> devices(String station) {
		return read(station, session -> session.createSelectionQuery("""
				select d.name, d.description, d.category
				from Device d order by d.name
				""", DeviceSummary.class).getResultList());
	}

	public List<DeviceBayMapping> deviceBayMappings(String station) {
		return read(station, session -> {
			// Old snapshots have no directory code. Reading them must not change their schema.
			boolean hasCode = session.doReturningWork(connection -> {
				try (var statement = connection.createStatement(); var columns = statement.executeQuery("PRAGMA table_info(bay)")) {
					while (columns.next()) if ("code".equals(columns.getString("name"))) return true;
					return false;
				}
			});
			return session.createSelectionQuery("select d.name, d.description, " + (hasCode ? "b.code" : "cast(null as String)")
					+ " from Device d left join d.bay b order by d.name", DeviceBayMapping.class).getResultList();
		});
	}

	public List<DataSetSummary> dataSets(String station, String device) {
		// Earlier snapshots stored some generated IDs only as SQLite rowids.
		return read(station, session -> {
			List<DataSetRow> dataSets = session.createSelectionQuery("""
					select coalesce(ds.id, column(ds.rowid as Long)), ds.identity, ds.name, ds.description
					from DataSet ds join Device d on fk(ds.device) = coalesce(d.id, column(d.rowid as Long))
					where d.name = :device order by coalesce(ds.id, column(ds.rowid as Long))
					""", DataSetRow.class).setParameter("device", device).getResultList();
			if (dataSets.isEmpty()) {
				return List.of();
			}

			Map<Long, List<ControlBlock>> controlsByDataSet = new HashMap<>();
			for (LinkedControl control : controls(session, device)) {
				controlsByDataSet.computeIfAbsent(control.dataSetId(), ignored -> new ArrayList<>())
						.add(new ControlBlock(control.id(), control.type(), control.name(), control.reference()));
			}
			return dataSets.stream().map(dataSet -> new DataSetSummary(
					dataSet.identity(), dataSet.name(), dataSet.description(),
					controlsByDataSet.getOrDefault(dataSet.id(), List.of()))).toList();
		});
	}

	private List<LinkedControl> controls(Session session, String device) {
		return session.createSelectionQuery("""
				select coalesce(ds.id, column(ds.rowid as Long)), coalesce(r.id, column(r.rowid as Long)),
					case when r.buffered then 'BRCB' else 'URCB' end, r.name, r.reference
				from ReportControl r
				join DataSet ds on fk(r.dataSet) = coalesce(ds.id, column(ds.rowid as Long))
				join Device d on fk(ds.device) = coalesce(d.id, column(d.rowid as Long))
				where d.name = :device
				union all
				select coalesce(ds.id, column(ds.rowid as Long)), coalesce(g.id, column(g.rowid as Long)),
					case when g.goose then 'GOOSE' else 'SV' end, g.name, g.reference
				from GooseSvControl g
				join DataSet ds on fk(g.dataSet) = coalesce(ds.id, column(ds.rowid as Long))
				join Device d on fk(ds.device) = coalesce(d.id, column(d.rowid as Long))
				where d.name = :device
				""", LinkedControl.class).setParameter("device", device).getResultList().stream()
				.sorted(Comparator.comparing(LinkedControl::type)
						.thenComparing(LinkedControl::name, Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparingLong(LinkedControl::id))
				.toList();
	}

	public List<FcdaRow> fcda(String station, String device, String dataSet) {
		return read(station, session -> session.createSelectionQuery("""
				select coalesce(item.id, column(item.rowid as Long)), item.fcdaIndex,
					fcda.dotReference, fcda.description, fcda.fc, fcda.cdc
				from DataItem item
				join DataSet ds on fk(item.dataSet) = coalesce(ds.id, column(ds.rowid as Long))
				join Device d on fk(ds.device) = coalesce(d.id, column(d.rowid as Long))
				join FCDA fcda on fk(item.fcda) = coalesce(fcda.id, column(fcda.rowid as Long))
				where d.name = :device and ds.identity = :dataSet
				order by item.fcdaIndex, coalesce(item.id, column(item.rowid as Long))
				""", FcdaRow.class)
				.setParameter("device", device)
				.setParameter("dataSet", dataSet)
				.getResultList());
	}

	private <T> List<T> read(String station, Function<Session, List<T>> query) {
		Path database = imports.databasePath(station);
		if (Files.notExists(database)) {
			return List.of();
		}

		// Read related rows in one transaction so concurrent imports cannot mix versions.
		String url = SqliteJdbcUrlBuilder.fromPath(database.toString()) + "?mode=ro";
		try (var connection = DriverManager.getConnection(url);
			 Session session = sqliteSessions.withOptions().connection(connection).openSession()) {
			var transaction = session.beginTransaction();
			try {
				List<T> result = query.apply(session);
				transaction.commit();
				return result;
			} finally {
				if (transaction.isActive()) {
					transaction.rollback();
				}
			}
		} catch (SQLException exception) {
			throw new IllegalStateException("无法读取站点的 SQLite 数据库", exception);
		}
	}

	@PreDestroy
	void close() {
		sqliteSessions.close();
	}

	public record DeviceSummary(String name, String description, int category, String categoryLabel) {
		public DeviceSummary(String name, String description, int category) {
			this(name, description, category, DeviceCategory.labelOf(category));
		}
	}

	public record DeviceBayMapping(String name, String description, String bayCode) {}

	private record DataSetRow(long id, String identity, String name, String description) {}

	private record LinkedControl(long dataSetId, long id, String type, String name, String reference) {}

	public record DataSetSummary(String identity, String name, String description, List<ControlBlock> controls) {}

	public record ControlBlock(long id, String type, String name, String reference) {}

	public record FcdaRow(long id, int index, String reference, String description, String fc, String cdc) {}
}
