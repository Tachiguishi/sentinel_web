package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.DataSet;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.GooseSvControl;

@Repository
public interface GooseSvControlRepository extends CrudRepository<GooseSvControl, Long> {
	Iterable<GooseSvControl> findByDevice(Device device);

	GooseSvControl findByDataSet(DataSet dataSet);

	void deleteByDataSet(DataSet dataSet);

	boolean existsByDevice(Device device);
}
