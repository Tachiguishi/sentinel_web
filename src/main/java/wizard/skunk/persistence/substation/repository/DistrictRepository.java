package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.District;

@Repository
public interface DistrictRepository extends CrudRepository<District, Long> {
}
