package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.DataSet;
import wizard.skunk.persistence.substation.domain.Device;

@Repository
public interface DataSetRepository extends CrudRepository<DataSet, Long> {
	Iterable<DataSet> findByDevice(Device device);

	void deleteByDevice(Device device);
}
