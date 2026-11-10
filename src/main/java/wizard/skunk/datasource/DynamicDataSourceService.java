package wizard.skunk.datasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DynamicDataSourceService {
	private final DynamicDataSource dynamicDataSource;

	@Autowired
	public DynamicDataSourceService(DynamicDataSource dynamicDataSource){
		this.dynamicDataSource = dynamicDataSource;
	}

	public void addDataSource(String key){
		dynamicDataSource.addDataSource(key, SqliteJdbcUrlBuilder.fromPath(key));
	}

	public void removeDataSource(String key){
		dynamicDataSource.removeDataSource(key);
	}

	public void switchDataSource(String key){
		DynamicDataSourceContextHolder.setDataSourceKey(key);
	}
}
