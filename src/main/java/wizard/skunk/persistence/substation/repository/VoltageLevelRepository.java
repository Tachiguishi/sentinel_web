package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.VoltageLevel;

@Repository
public interface VoltageLevelRepository extends CrudRepository<VoltageLevel, Long> {
}
