package wizard.skunk.datasource;

import lombok.Setter;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DynamicDataSource extends AbstractRoutingDataSource {
	private final Map<Object, Object> dataSourceMap = new ConcurrentHashMap<>();
	@Setter
	private static String DefaultDataSource = ":memory:";
	@Setter
	private static String username = "sa";
	@Setter
	private static String password = "";

	public DynamicDataSource(){
		super.setDefaultTargetDataSource(createDefaultDataSource());
		super.setTargetDataSources(dataSourceMap);
		super.afterPropertiesSet();
	}

	private DataSource createDefaultDataSource(){
		DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
		dataSourceBuilder.driverClassName("org.sqlite.JDBC");
		dataSourceBuilder.url(SqliteJdbcUrlBuilder.fromPath(DefaultDataSource));
		dataSourceBuilder.username(username);
		dataSourceBuilder.password(password);
		return dataSourceBuilder.build();
	}

	@Override
	protected Object determineCurrentLookupKey() {
		return DynamicDataSourceContextHolder.getDataSourceKey();
	}

	private void refresh(){
		super.setTargetDataSources(dataSourceMap);
		super.afterPropertiesSet();
	}

	public void addDataSource(String key, String url){
		DataSource dataSource = DataSourceBuilder.create()
				.driverClassName("org.sqlite.JDBC")
				.url(url)
				.username(username)
				.password(password)
				.build();
		dataSourceMap.put(key, dataSource);
		refresh();
	}

	public void addDefaultDataSource(String key, DataSource dataSource){
		dataSourceMap.put(key, dataSource);
		super.setTargetDataSources(dataSourceMap);
		super.setDefaultTargetDataSource(dataSource);
	}

	public void removeDataSource(String key){
		dataSourceMap.remove(key);
		refresh();
	}
}
