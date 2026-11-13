package wizard.skunk.substation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import wizard.skunk.datasource.SqliteJdbcUrlBuilder;
import wizard.skunk.persistence.station.StationEntity;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.repository.DeviceRepository;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class StationBayMappingTest {
	@TempDir Path directory;

	@Test
	void queriesOnlyTheRequestedStationAndTrimsCharPadding() throws Exception {
		try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
			JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
			jdbc.execute("CREATE TABLE TB1042_BAY (F1041_CODE CHAR(48), F1042_CODE CHAR(48), F1042_NAME CHAR(48), F1042_DESC CHAR(96), F1042_VOLTAGE SMALLINT)");
			jdbc.update("INSERT INTO TB1042_BAY VALUES (?, ?, ?, ?, ?)", "StationA", "Bay001  ", "Line1  ", "线路一  ", 220);
			jdbc.update("INSERT INTO TB1042_BAY VALUES (?, ?, ?, ?, ?)", "StationA", "Bay002", null, null, null);
			jdbc.update("INSERT INTO TB1042_BAY VALUES (?, ?, ?, ?, ?)", "StationB", "Bay003", "Other", "其他站", 500);
			StationBayCatalogService catalog = new StationBayCatalogService(jdbc);
			assertEquals(List.of(new StationBayCatalogService.BayOption("Bay001", "Line1", "线路一", 220),
					new StationBayCatalogService.BayOption("Bay002", null, null, null)), catalog.bays("StationA"));
			assertTrue(catalog.bays("StationA' OR 1=1 --").isEmpty());
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void savesReadsExportsAndRollsBackAssignmentsInExistingSnapshots(boolean legacy) throws Exception {
		StationScdImportService imports = new StationScdImportService(directory);
		Path database = imports.databasePath("StationA");
		Files.createFile(database);
		var source = new DriverManagerDataSource(SqliteJdbcUrlBuilder.fromPath(database.toString()));
		try (var context = new AnnotationConfigApplicationContext()) {
			context.registerBean(DataSource.class, () -> source);
			context.registerBean(ScdSqliteConfiguration.class, () -> new ScdSqliteConfiguration(true));
			context.refresh();
			new TransactionTemplate(context.getBean(JpaTransactionManager.class)).executeWithoutResult(status -> {
				Device first = new Device();
				first.setName("IED1");
				first.setDescription("保护装置");
				Device second = new Device();
				second.setName("IED2");
				second.setDescription("测控装置");
				context.getBean(DeviceRepository.class).saveAll(List.of(first, second));
			});
		}
		JdbcTemplate jdbc = new JdbcTemplate(source);
		if (legacy) {
			// Early snapshots had untyped generated IDs, backed only by SQLite rowid.
			jdbc.execute("DROP TABLE bay");
			jdbc.execute("CREATE TABLE bay (id, name TEXT, description TEXT, voltage_level_id BIGINT)");
			jdbc.execute("ALTER TABLE device DROP COLUMN category_overridden");
		}
		var catalog = List.of(new StationBayCatalogService.BayOption("Bay001", "Line1", "线路一", 220),
				new StationBayCatalogService.BayOption("Bay002", "Line2", "线路二", null));
		StationScdReadService reader = new StationScdReadService(imports, new DefaultResourceLoader());
		try {
			byte[] before = Files.readAllBytes(database);
			assertTrue(reader.deviceBayMappings("StationA").stream().allMatch(row -> row.bayCode() == null));
			assertArrayEquals(before, Files.readAllBytes(database), "Reading legacy snapshots must not migrate them");
			imports.updateDeviceBays("StationA", List.of(assignment("IED1", "Bay001"), assignment("IED2", "Bay001")), catalog);
			assertEquals(List.of("Bay001", "Bay001"), reader.deviceBayMappings("StationA").stream().map(StationScdReadService.DeviceBayMapping::bayCode).toList());
			assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM bay", Integer.class));
			assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM voltage_level", Integer.class));
			byte[] xml = new StationModelExportService(imports).exportModel(new StationEntity("StationA", "甲站"));
			var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xml));
			var ied = (org.w3c.dom.Element) document.getElementsByTagName("ied").item(0);
			assertEquals("Bay001", ied.getAttribute("bay"));
			assertEquals("Line1", ied.getAttribute("bayName"));
			assertEquals("220", ied.getAttribute("voltage"));
			assertThrows(NoSuchElementException.class, () -> imports.updateDeviceBays("StationA",
					List.of(assignment("IED1", "Bay002"), assignment("missing", "Bay002")), catalog));
			assertEquals("Bay001", reader.deviceBayMappings("StationA").getFirst().bayCode());
			assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM bay", Integer.class));
			imports.updateDeviceBays("StationA", List.of(assignment("IED1", null), assignment("IED2", "Bay002")), catalog);
			assertNull(reader.deviceBayMappings("StationA").getFirst().bayCode());
			assertEquals("Bay002", reader.deviceBayMappings("StationA").getLast().bayCode());
			assertTrue(reader.deviceBayMappings("StationB").isEmpty());
			String scd = "<SCL><Substation name=\"A\"/><IED name=\"IED1\" desc=\"保护装置\"/><IED name=\"IED2\" desc=\"测控装置更新\"/><IED name=\"IED3\" desc=\"新增装置\"/></SCL>";
			imports.importScd("StationA", new ByteArrayInputStream(scd.getBytes(StandardCharsets.UTF_8)));
			var afterImport = reader.deviceBayMappings("StationA");
			assertEquals("Bay002", afterImport.get(1).bayCode(), "Reimport must preserve the existing device assignment");
			assertNull(afterImport.getLast().bayCode(), "Newly imported devices start unassigned");
		} finally {
			reader.close();
		}
	}

	private static StationScdImportService.BayAssignment assignment(String device, String bay) {
		return new StationScdImportService.BayAssignment(device, bay);
	}
}
