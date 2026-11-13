package wizard.skunk.substation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import wizard.skunk.datasource.SqliteJdbcUrlBuilder;
import wizard.skunk.persistence.station.StationEntity;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

class StationModelExportServiceTest {
	@TempDir Path directory;

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void exportsSelectedStationWithLinkedPointsAndReportsWithoutChangingDatabase(boolean legacy) throws Exception {
		StationScdImportService imports = new StationScdImportService(directory);
		StationEntity station = new StationEntity("站点 & A", "测试 <站点>");
		station.updateSettings("192.168.1.20", 7);
		Path database = imports.databasePath(station.getName());
		createDatabase(database, legacy, "设备 A");
		createDatabase(imports.databasePath("站点 B"), false, "设备 B");
		byte[] before = Files.readAllBytes(database);

		byte[] xml = new StationModelExportService(imports).exportModel(station);
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new ByteArrayInputStream(xml));
		Element root = document.getDocumentElement();
		assertEquals("substation", root.getTagName());
		assertEquals(station.getName(), root.getAttribute("code"));
		assertEquals(station.getDescription(), root.getAttribute("desc"));
		Element network = (Element) document.getElementsByTagName("network").item(0);
		assertEquals("192.168.1.20", network.getAttribute("l1_ip_a"));
		assertEquals("192.168.1.20", network.getAttribute("l2_ip_a"));
		assertEquals("7", network.getAttribute("instance"));
		assertEquals(1, document.getElementsByTagName("ied").getLength());
		Element ied = (Element) document.getElementsByTagName("ied").item(0);
		assertEquals("设备 A", ied.getAttribute("name"));
		assertEquals("21", ied.getAttribute("category"));
		assertEquals("IED0000000001", ied.getAttribute("code"));
		assertEquals(1, ied.getElementsByTagName("analog").getLength());
		assertEquals(1, ied.getElementsByTagName("status").getLength());
		assertEquals(1, ied.getElementsByTagName("string").getLength());
		Element analog = data(ied, "analog", 0);
		assertEquals("Analog0000000001", analog.getAttribute("code"));
		assertEquals("1.0", analog.getAttribute("coefficient"));
		assertEquals("mA", analog.getAttribute("unit"));
		assertEquals("IED0000000001", analog.getAttribute("parent"));
		assertEquals(station.getName(), analog.getAttribute("substation"));
		assertEquals("LD0/GGIO1$MX$Amp", analog.getAttribute("reference"));
		assertEquals(1, ied.getElementsByTagName("setting").getLength());
		assertEquals("2", data(ied, "setting", 0).getAttribute("category"));
		assertEquals("0.5", data(ied, "setting", 0).getAttribute("stepSize"));
		assertEquals(2, ((Element) ied.getElementsByTagName("rcb").item(0)).getElementsByTagName("data").getLength());
		// Both reports share a dataset: values are deduplicated while MMS membership is preserved.
		assertEquals(1, ((Element) ied.getElementsByTagName("analog").item(0)).getElementsByTagName("data").getLength());
		var mms = ((Element) ied.getElementsByTagName("mmsFcda").item(0)).getElementsByTagName("data");
		assertEquals(6, mms.getLength());
		assertNotEquals(((Element) mms.item(0)).getAttribute("code"), ((Element) mms.item(3)).getAttribute("code"));
		assertEquals("Analog0000000001", ((Element) mms.item(0)).getAttribute("value"));
		assertEquals("RCB0000000001", ((Element) mms.item(0)).getAttribute("rcb"));
		assertEquals("0", ((Element) mms.item(0)).getAttribute("index"));
		assertArrayEquals(before, Files.readAllBytes(database));
	}

	@Test
	void rejectsMissingScdWithoutCreatingAnEmptyDatabase() {
		StationScdImportService imports = new StationScdImportService(directory);
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> new StationModelExportService(imports).exportModel(new StationEntity("missing", null)));
		assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
		assertFalse(Files.exists(imports.databasePath("missing")));
	}

	private Element data(Element ied, String group, int index) {
		return (Element) ((Element) ied.getElementsByTagName(group).item(0)).getElementsByTagName("data").item(index);
	}

	private void createDatabase(Path path, boolean legacy, String deviceName) throws Exception {
		try (var connection = DriverManager.getConnection(SqliteJdbcUrlBuilder.fromPath(path.toString()));
			 var statement = connection.createStatement()) {
			String id = legacy ? "id" : "id integer primary key";
			for (String sql : new String[] {
					"create table voltage_level (" + id + ", value integer, multiplier text)",
					"create table bay (" + id + ", name text, voltage_level_id integer)",
					"create table cabinet (" + id + ", name text)",
					"create table device (" + id + ", name text, description text, category integer, ignored boolean, bay_id integer, cabinet_id integer)",
					"create table data_set (" + id + ", device_id integer, category integer, name text, description text)",
					"create table fcda (" + id + ", device_id integer, cdc text, fc text, da_name text, dollar_reference text, unit_serialization text)",
					"create table data_item (" + id + ", data_set_id integer, fcda_id integer, fcda_index integer)",
					"create table report_control (" + id + ", device_id integer, data_set_id integer, reference text, buffered boolean)",
					"insert into data_set values (1,1,3,'dsAin','遥测 & 遥信'),(2,1,2,'dsSetting','定值')",
					"insert into fcda values (1,1,'MV','MX','','LD0/GGIO1$MX$Amp','A,,,,m'),(2,1,'SPS','ST','','LD0/GGIO1$ST$Alm',null),(3,1,'STS','ST','','LD0/GGIO1$ST$Text',null),(4,1,'MV','MX','q','LD0/GGIO1$MX$Amp$q',null),(5,1,'ASG','SG','','LD0/GGIO1$SG$Set','A,0,10,0.5,')",
					"insert into data_item values (1,1,1,0),(2,1,2,1),(3,1,3,2),(4,1,4,3),(5,2,5,0)",
					"insert into report_control values (1,1,1,'LD0/LLN0$RP$urcbAin',false),(2,1,1,'LD0/LLN0$BR$brcbAin',true)"
			}) statement.execute(sql);
			try (var insert = connection.prepareStatement("insert into device values (1,?,?,65536,false,null,null),(2,'ignored',null,0,true,null,null)")) {
				insert.setString(1, deviceName);
				insert.setString(2, "装置 <描述>");
				insert.executeUpdate();
			}
			if (legacy) {
				for (String table : new String[] {"device", "data_set", "data_item", "report_control"}) {
					statement.executeUpdate("update " + table + " set id = null");
				}
			}
		}
	}
}
