package wizard.skunk.persistence.station;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StationRepository extends Repository<StationEntity, String> {
	boolean existsById(String name);

	Optional<StationEntity> findById(String name);

	StationEntity saveAndFlush(StationEntity station);

	@Modifying
	@Transactional
	@Query("""
			update StationEntity s set s.scdUpdatedAt = :updatedAt
			where s.name = :name and (s.scdUpdatedAt is null or s.scdUpdatedAt < :updatedAt)
			""")
	int recordScdUpdatedAt(String name, LocalDateTime updatedAt);

	long count();

	List<StationEntity> findAllBy(Pageable pageable);

	long countByNameContainingOrDescriptionContainingAllIgnoreCase(String name, String description);

	List<StationEntity> findByNameContainingOrDescriptionContainingAllIgnoreCase(
			String name, String description, Pageable pageable);
}
