package wizard.skunk.substation.service;

import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.community.dialect.identity.SQLiteIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;

public class ScdSqliteDialect extends SQLiteDialect {
	private static final IdentityColumnSupport IDENTITY_COLUMNS = new SQLiteIdentityColumnSupport() {
		@Override
		public String getIdentityColumnString(int type) {
			// Hibernate can mistake an earlier "integer" column for the identity column's type.
			return "INTEGER";
		}
	};

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return IDENTITY_COLUMNS;
	}
}
