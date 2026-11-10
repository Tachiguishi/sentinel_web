package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.DeviceAlarm;

@Repository
public interface DeviceAlarmRepository extends CrudRepository<DeviceAlarm, Long> {
	Iterable<DeviceAlarm> findByDevice(Device device);

	void deleteByDevice(Device device);
}
