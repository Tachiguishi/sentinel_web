package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.Bay;
import wizard.skunk.persistence.substation.domain.VoltageLevel;

@Repository
public interface BayRepository extends CrudRepository<Bay, Long> {
	Iterable<Bay> findByVoltageLevel(VoltageLevel voltageLevel);
}
