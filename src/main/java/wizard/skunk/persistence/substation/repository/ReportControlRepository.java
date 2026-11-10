package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.DataSet;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.ReportControl;

@Repository
public interface ReportControlRepository extends CrudRepository<ReportControl, Long> {
	Iterable<ReportControl> findByDevice(Device device);

	void deleteByDataSet(DataSet dataSet);

	ReportControl findByDataSet(DataSet dataSet);

	boolean existsByDevice(Device device);
}
