package wizard.skunk.substation.service;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/** The MySQL bay directory is read-only; assignments live in each station's SCD database. */
public class StationBayCatalogService {
	private final JdbcTemplate jdbc;

	public StationBayCatalogService(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public List<BayOption> bays(String stationCode) {
		return jdbc.query("""
				SELECT F1042_CODE, F1042_NAME, F1042_DESC, F1042_VOLTAGE
				FROM TB1042_BAY WHERE F1041_CODE = ?
				ORDER BY F1042_VOLTAGE DESC, F1042_CODE
				""", (rs, row) -> {
			int value = rs.getInt("F1042_VOLTAGE");
			Integer voltage = rs.wasNull() ? null : value;
			return new BayOption(trim(rs.getString("F1042_CODE")), trim(rs.getString("F1042_NAME")),
					trim(rs.getString("F1042_DESC")), voltage);
		}, stationCode);
	}

	private static String trim(String value) {
		return value == null ? null : value.stripTrailing();
	}

	public record BayOption(String code, String name, String description, Integer voltage) {}
}
