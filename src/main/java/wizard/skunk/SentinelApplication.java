package wizard.skunk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import wizard.skunk.controller.StationController;
import wizard.skunk.persistence.station.StationEntity;
import wizard.skunk.persistence.station.StationRepository;

@SpringBootApplication(scanBasePackageClasses = StationController.class)
@EntityScan(basePackageClasses = StationEntity.class)
@EnableJpaRepositories(basePackageClasses = StationRepository.class)
public class SentinelApplication {
	public static void main(String[] args) {
		SpringApplication.run(SentinelApplication.class, args);
	}
}
