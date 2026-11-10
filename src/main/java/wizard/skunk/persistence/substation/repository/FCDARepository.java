package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.FCDA;

@Repository
public interface FCDARepository extends CrudRepository<FCDA, Long> {
	Iterable<FCDA> findByDevice(Device device);
	Iterable<FCDA> findByDotReferenceContains(String dotReferencePattern);
	Iterable<FCDA> findByDeviceAndDotReference(Device device, String dotReferencePrefix);
	FCDA findTopByDeviceAndDotReference(Device device, String dotReference);
}
