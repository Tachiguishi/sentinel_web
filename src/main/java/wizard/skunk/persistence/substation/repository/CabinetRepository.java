package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.Cabinet;
import wizard.skunk.persistence.substation.domain.District;

@Repository
public interface CabinetRepository extends CrudRepository<Cabinet, Long> {
	Iterable<Cabinet> findByDistrict(District district);
}
