package wizard.skunk.substation.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.web.server.ResponseStatusException;
import wizard.skunk.common.StandardMonitor;
import wizard.skunk.datasource.SqliteJdbcUrlBuilder;
import wizard.skunk.export.Exporter;
import wizard.skunk.export.domain.ModelData;
import wizard.skunk.persistence.station.StationEntity;
import wizard.skunk.persistence.substation.domain.*;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class StationModelExportService {
	private final StationScdImportService imports;

	public StationModelExportService(StationScdImportService imports) {
		this.imports = imports;
	}

	public byte[] exportModel(StationEntity station) {
		var database = imports.databasePath(station.getName());
		if (Files.notExists(database)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "请先更新站点的 SCD 文件");
		}
		ModelData model;
		String url = SqliteJdbcUrlBuilder.fromPath(database.toString()) + "?mode=ro";
		try (var connection = DriverManager.getConnection(url)) {
			// Keep every table in the same read transaction during concurrent SCD updates.
			connection.setAutoCommit(false);
			try {
				JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
				model = readModel(jdbc);
			} finally {
				connection.rollback();
			}
		} catch (SQLException exception) {
			throw new IllegalStateException("无法读取站点的 SQLite 数据库", exception);
		}

		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			new Exporter().exportModel(output, new StandardMonitor(), station, model);
			return output.toByteArray();
		} catch (Exception exception) {
			throw new IllegalStateException("站点模型导出失败", exception);
		}
	}

	private ModelData readModel(JdbcTemplate jdbc) {
		Map<Long, VoltageLevel> voltages = read(jdbc, "voltage_level", VoltageLevel.class);
		Map<Long, Bay> bays = read(jdbc, "bay", Bay.class);
		jdbc.query("select coalesce(id, rowid) as export_id, voltage_level_id from bay", rs -> {
			bays.get(rs.getLong("export_id")).setVoltageLevel(voltages.get(rs.getLong("voltage_level_id")));
		});
		Map<Long, Cabinet> cabinets = read(jdbc, "cabinet", Cabinet.class);
		Map<Long, Device> devices = read(jdbc, "device", Device.class);
		jdbc.query("select coalesce(id, rowid) as export_id, bay_id, cabinet_id from device", rs -> {
			Device device = devices.get(rs.getLong("export_id"));
			device.setBay(bays.get(rs.getLong("bay_id")));
			device.setCabinet(cabinets.get(rs.getLong("cabinet_id")));
		});
		Map<Long, DataSet> sets = read(jdbc, "data_set", DataSet.class);
		jdbc.query("select coalesce(id, rowid) as export_id, device_id from data_set", rs -> {
			sets.get(rs.getLong("export_id")).setDevice(devices.get(rs.getLong("device_id")));
		});
		Map<Long, FCDA> points = read(jdbc, "fcda", FCDA.class);
		jdbc.query("select coalesce(id, rowid) as export_id, device_id from fcda", rs -> {
			points.get(rs.getLong("export_id")).setDevice(devices.get(rs.getLong("device_id")));
		});
		Map<Long, DataItem> items = read(jdbc, "data_item", DataItem.class);
		jdbc.query("select coalesce(id, rowid) as export_id, data_set_id, fcda_id from data_item", rs -> {
			DataItem item = items.get(rs.getLong("export_id"));
			item.setDataSet(sets.get(rs.getLong("data_set_id")));
			item.setFcda(points.get(rs.getLong("fcda_id")));
		});
		Map<Long, ReportControl> reports = read(jdbc, "report_control", ReportControl.class);
		jdbc.query("select coalesce(id, rowid) as export_id, device_id, data_set_id from report_control", rs -> {
			ReportControl report = reports.get(rs.getLong("export_id"));
			report.setDevice(devices.get(rs.getLong("device_id")));
			report.setDataSet(sets.get(rs.getLong("data_set_id")));
		});
		return new ModelData(devices.values().stream().toList(), reports.values().stream().toList(),
				items.values().stream().sorted(java.util.Comparator.comparingInt(DataItem::getFcdaIndex)
						.thenComparing(DataItem::getId)).toList());
	}

	private <T> Map<Long, T> read(JdbcTemplate jdbc, String table, Class<T> type) {
		BeanPropertyRowMapper<T> mapper = BeanPropertyRowMapper.newInstance(type);
		Map<Long, T> rows = new LinkedHashMap<>();
		// Legacy snapshots stored generated identifiers in rowid and left id null.
		jdbc.query("select *, coalesce(id, rowid) as export_id from " + table + " order by export_id", rs -> {
			T row = mapper.mapRow(rs, rows.size());
			long id = rs.getLong("export_id");
			org.springframework.beans.PropertyAccessorFactory.forBeanPropertyAccess(row).setPropertyValue("id", id);
			rows.put(id, row);
		});
		return rows;
	}
}
