package wizard.skunk.persistence.substation.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.Bay;
import wizard.skunk.persistence.substation.domain.Cabinet;
import wizard.skunk.persistence.substation.domain.Device;

import java.util.Collection;
import java.util.List;

@Repository
public interface DeviceRepository extends CrudRepository<Device, Long> {
	Device findByName(String name);
	List<Device> findByNameIn(Collection<String> names);
	Iterable<Device> findByCategory(int category);
	Iterable<Device> findByIgnored(boolean ignored);
	Iterable<Device> findByBay(Bay bay);
	Iterable<Device> findByCabinet(Cabinet cabinet);

	@Modifying
	@Query("UPDATE Device d SET d.cabinet = null, d.cabinetUnit = ''")
	void clearCabinetForAllDevices();
}
