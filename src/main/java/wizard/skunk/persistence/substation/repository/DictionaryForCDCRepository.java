package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.DictionaryForCDC;
import wizard.skunk.persistence.substation.domain.Device;

@Repository
public interface DictionaryForCDCRepository extends CrudRepository<DictionaryForCDC, Long> {
	void deleteByDevice(Device device);
}
