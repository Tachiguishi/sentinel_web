package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.Substation;

@Repository
public interface SubstationRepository extends CrudRepository<Substation, Long> {
	Substation findTopByIdNotNull();
}
