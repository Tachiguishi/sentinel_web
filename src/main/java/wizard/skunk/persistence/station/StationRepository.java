package wizard.skunk.persistence.station;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface StationRepository extends Repository<StationEntity, String> {
	long count();

	List<StationEntity> findAllBy(Pageable pageable);

	long countByNameContainingOrDescriptionContainingAllIgnoreCase(String name, String description);

	List<StationEntity> findByNameContainingOrDescriptionContainingAllIgnoreCase(
			String name, String description, Pageable pageable);
}
