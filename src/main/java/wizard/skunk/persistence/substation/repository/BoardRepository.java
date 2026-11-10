package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.Board;
import wizard.skunk.persistence.substation.domain.Device;

@Repository
public interface BoardRepository extends CrudRepository<Board, Long> {
	Iterable<Board> findByDevice(Device device);
	void deleteByDevice(Device device);
}
