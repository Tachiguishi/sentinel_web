package wizard.skunk.persistence.station;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface StationRepository extends Repository<StationEntity, String> {
	boolean existsById(String name);

	Optional<StationEntity> findById(String name);

	StationEntity saveAndFlush(StationEntity station);

	long count();

	List<StationEntity> findAllBy(Pageable pageable);

	long countByNameContainingOrDescriptionContainingAllIgnoreCase(String name, String description);

	List<StationEntity> findByNameContainingOrDescriptionContainingAllIgnoreCase(
			String name, String description, Pageable pageable);
}
