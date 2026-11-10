package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.GooseSvControl;
import wizard.skunk.persistence.substation.domain.LinkBreakAlarm;

public interface LinkBreakAlarmRepository extends CrudRepository<LinkBreakAlarm, Long> {
	Iterable<LinkBreakAlarm> findByReceiverAndSenderControl(Device receiver, GooseSvControl senderControl);
	Iterable<LinkBreakAlarm> findByReceiver(Device receiver);
}
