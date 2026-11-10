package wizard.skunk.persistence.station;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface SubstationRepository extends Repository<SubstationEntity, String> {
	List<SubstationEntity> findAllByOrderByCodeAsc();

	Optional<SubstationEntity> findById(String code);
}
