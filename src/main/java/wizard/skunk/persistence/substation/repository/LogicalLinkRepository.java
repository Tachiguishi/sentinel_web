package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.LogicalLink;

@Repository
public interface LogicalLinkRepository extends CrudRepository<LogicalLink, Long> {
	Iterable<LogicalLink> findByReceiverDevice(Device receiverDevice);
	Iterable<LogicalLink> findBySenderDevice(Device senderDevice);
}
