package wizard.skunk.substation.service;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

final class LegacyScdSqliteIds {
	private static final List<String> TABLES = List.of("device", "data_set", "data_item", "logical_link",
			"port", "report_control", "sclfile", "setting_control", "voltage_level");

	private LegacyScdSqliteIds() {}

	static void repair(JdbcTemplate jdbc) {
		for (String table : TABLES) {
			boolean legacyId = jdbc.query("PRAGMA table_info(" + table + ")", (rs, row) ->
					rs.getString("name").equals("id") && rs.getString("type").isEmpty()).contains(true);
			if (legacyId) {
				// Old SQLite snapshots allocated IDs as rowids but left the mapped ID column null.
				jdbc.update("UPDATE " + table + " SET id = rowid WHERE id IS NULL");
				jdbc.execute("""
						CREATE TRIGGER IF NOT EXISTS sentinel_%1$s_fill_id AFTER INSERT ON %1$s
						WHEN NEW.id IS NULL
						BEGIN
							UPDATE %1$s SET id = NEW.rowid WHERE rowid = NEW.rowid;
						END
						""".formatted(table));
			}
		}
	}
}
