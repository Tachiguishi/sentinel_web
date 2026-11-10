package wizard.skunk.persistence.sentinel.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.sentinel.domain.Device;

@Repository
public interface DeviceRepository extends CrudRepository<Device, Long> {
}
