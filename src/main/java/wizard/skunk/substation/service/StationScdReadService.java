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

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
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
				select d.name, d.description
				from Device d order by d.name
				""", DeviceSummary.class).getResultList());
	}

	public List<DataSetSummary> dataSets(String station, String device) {
		// Earlier snapshots stored some generated IDs only as SQLite rowids.
		return read(station, session -> session.createSelectionQuery("""
				select ds.identity, ds.name, ds.description
				from DataSet ds join Device d on fk(ds.device) = coalesce(d.id, column(d.rowid as Long))
				where d.name = :device order by coalesce(ds.id, column(ds.rowid as Long))
				""", DataSetSummary.class)
				.setParameter("device", device)
				.getResultList());
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

		// Each query opens the published snapshot afresh, so re-imports are visible immediately.
		String url = SqliteJdbcUrlBuilder.fromPath(database.toString()) + "?mode=ro";
		try (var connection = DriverManager.getConnection(url);
			 Session session = sqliteSessions.withOptions().connection(connection).openSession()) {
			return query.apply(session);
		} catch (SQLException exception) {
			throw new IllegalStateException("无法读取站点的 SQLite 数据库", exception);
		}
	}

	@PreDestroy
	void close() {
		sqliteSessions.close();
	}

	public record DeviceSummary(String name, String description) {}

	public record DataSetSummary(String identity, String name, String description) {}

	public record FcdaRow(long id, int index, String reference, String description, String fc, String cdc) {}
}
