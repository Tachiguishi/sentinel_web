package wizard.skunk.substation.service;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import wizard.skunk.persistence.domain.SCLFile;
import wizard.skunk.persistence.repository.SCLFileRepository;
import wizard.skunk.persistence.service.SCLFileService;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.repository.DeviceRepository;
import wizard.skunk.persistence.substation.service.DataSetService;
import wizard.skunk.persistence.substation.service.DeviceService;
import wizard.skunk.persistence.substation.service.DictionaryForCDCService;
import wizard.skunk.persistence.substation.service.FCDAService;
import wizard.skunk.persistence.substation.service.LogicalLinkService;
import wizard.skunk.persistence.substation.service.SubstationService;

import javax.sql.DataSource;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(basePackageClasses = {DeviceRepository.class, SCLFileRepository.class})
@EnableTransactionManagement
@Import({ScdService.class, CategoryIdentifier.class, DeviceService.class, DataSetService.class, DictionaryForCDCService.class,
		FCDAService.class, LogicalLinkService.class, SubstationService.class, SCLFileService.class})
class ScdSqliteConfiguration {
	static final String DIALECT = ScdSqliteDialect.class.getName();
	static final String NAMING_STRATEGY = "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl";
	private final boolean createSchema;

	ScdSqliteConfiguration(boolean createSchema) {
		this.createSchema = createSchema;
	}

	@Bean
	LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
		factory.setDataSource(dataSource);
		factory.setPackagesToScan(Device.class.getPackageName(), SCLFile.class.getPackageName());
		factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		factory.setJpaPropertyMap(Map.of(
				"hibernate.dialect", DIALECT,
				"hibernate.hbm2ddl.auto", createSchema ? "create-only" : "none",
				"hibernate.hbm2ddl.halt_on_error", true,
				"hibernate.physical_naming_strategy", NAMING_STRATEGY));
		return factory;
	}

	@Bean
	JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}
}
