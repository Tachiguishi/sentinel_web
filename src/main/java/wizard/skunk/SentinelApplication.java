package wizard.skunk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import wizard.skunk.controller.StationController;
import wizard.skunk.persistence.station.StationEntity;
import wizard.skunk.persistence.station.StationRepository;
import wizard.skunk.substation.service.StationScdImportService;
import wizard.skunk.substation.service.StationScdReadService;
import wizard.skunk.substation.service.StationModelExportService;

@SpringBootApplication(scanBasePackageClasses = StationController.class)
@EntityScan(basePackageClasses = StationEntity.class)
@EnableJpaRepositories(basePackageClasses = StationRepository.class)
@Import({StationScdImportService.class, StationScdReadService.class, StationModelExportService.class})
public class SentinelApplication {
	public static void main(String[] args) {
		SpringApplication.run(SentinelApplication.class, args);
	}
}
