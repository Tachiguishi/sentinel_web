package wizard.skunk.datasource;

import java.nio.file.Path;

public final class SqliteJdbcUrlBuilder {
	private SqliteJdbcUrlBuilder() {
	}

	public static String fromPath(String databasePath) {
		if (databasePath == null || databasePath.isBlank()) {
			throw new IllegalArgumentException("Database path is empty");
		}
		if (":memory:".equals(databasePath)) {
			return "jdbc:sqlite::memory:";
		}
		return "jdbc:sqlite:" + Path.of(databasePath).toAbsolutePath().normalize().toUri();
	}
}
