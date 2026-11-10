package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.TerminalStrapRelation;

@Repository
public interface TerminalStrapRelationRepository extends CrudRepository<TerminalStrapRelation, Long> {
	Iterable<TerminalStrapRelation> findByDevice(Device device);
}
