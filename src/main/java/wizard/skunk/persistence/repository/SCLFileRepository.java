package wizard.skunk.persistence.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.domain.SCLFile;

import java.util.List;

@Repository
public interface SCLFileRepository extends CrudRepository<SCLFile, Long> {
	SCLFile findTopByCategoryAndIdentifier(int category, String identifier);

	List<SCLFile> findByCategory(int category);

	@Transactional
	void deleteByCategoryAndIdentifier(int category, String identifier);

	@Transactional
	void deleteByIdentifier(String identifier);

	@Query("SELECT s.fileName FROM SCLFile s WHERE s.category = :category AND s.identifier = :identifier")
	String findFileNameByCategoryAndIdentifier(int category, String identifier);

	@Modifying
	@Transactional
	@Query("UPDATE SCLFile s SET s.identifier = :newIdentifier WHERE s.category = :category AND s.identifier = :originalIdentifier")
	void updateIdentifierByCategoryAndIdentifier(int category, String originalIdentifier, String newIdentifier);
}
