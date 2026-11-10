package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.Board;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.Port;

@Repository
public interface PortRepository extends CrudRepository<Port, Long> {
	Iterable<Port> findByBoard(Board board);
	void deleteByDevice(Device device);
}
