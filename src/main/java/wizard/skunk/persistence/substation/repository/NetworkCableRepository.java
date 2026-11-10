package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.NetworkCable;

@Repository
public interface NetworkCableRepository extends CrudRepository<NetworkCable, Long> {
	void deleteByDeviceAEntityOrDeviceBEntity(Device deviceA, Device deviceB);
	// Additional query methods can be defined here if needed
}
