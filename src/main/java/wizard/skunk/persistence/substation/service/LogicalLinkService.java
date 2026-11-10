package wizard.skunk.persistence.substation.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.GooseSvControl;
import wizard.skunk.persistence.substation.domain.LinkBreakAlarm;
import wizard.skunk.persistence.substation.domain.LogicalLink;
import wizard.skunk.persistence.substation.repository.LinkBreakAlarmRepository;
import wizard.skunk.persistence.substation.repository.LogicalLinkRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LogicalLinkService {
	private final LogicalLinkRepository logicalLinkRepository;
	private LinkBreakAlarmRepository linkBreakRepository;

	@Autowired
	public LogicalLinkService(LogicalLinkRepository logicalLinkRepository) {
		this.logicalLinkRepository = logicalLinkRepository;
	}

	@Autowired
	public void setLinkBreakRepository(LinkBreakAlarmRepository linkBreakRepository) {
		this.linkBreakRepository = linkBreakRepository;
	}

	public List<LogicalLink> findByReceiverDevice(Device device){
		return (List<LogicalLink>) logicalLinkRepository.findByReceiverDevice(device);
	}

	public List<LogicalLink> findBySenderDevice(Device device){
		return (List<LogicalLink>) logicalLinkRepository.findBySenderDevice(device);
	}

	public List<GooseSvControl> findSenderControlsByReceiverDevice(Device device) {
		List<GooseSvControl> result = new ArrayList<>();
		Set<String> controlSet = new HashSet<>();
		List<LogicalLink> logicalLinkList = findByReceiverDevice(device);
		for(LogicalLink logicalLink : logicalLinkList){
			GooseSvControl senderControl = logicalLink.getSenderControl();
			String key = senderControl + "-" + device.getName();
			if(!controlSet.contains(key)){
				controlSet.add(key);

				result.add(senderControl);
			}
		}

		return result;
	}

	@Transactional
	public void deleteLogicalLinks(List<LogicalLink> removedLogicalLinks) {
		logicalLinkRepository.deleteAll(removedLogicalLinks);
	}

	public void saveLogicalLinks(List<LogicalLink> logicalLinks) {
		logicalLinkRepository.saveAll(logicalLinks);
	}

	public List<LinkBreakAlarm> findLinkBreakAlarmsByLink(Device receiver, GooseSvControl senderControl) {
		return (List<LinkBreakAlarm>) linkBreakRepository.findByReceiverAndSenderControl(receiver, senderControl);
	}

	public List<LinkBreakAlarm> findLinkBreakAlarmsByReceiver(Device receiver) {
		return (List<LinkBreakAlarm>) linkBreakRepository.findByReceiver(receiver);
	}

	@Transactional
	public void deleteLinkBreakAlarm(LinkBreakAlarm removedLinkBreakAlarm) {
		linkBreakRepository.delete(removedLinkBreakAlarm);
	}

	public void saveLinkBreakAlarm(LinkBreakAlarm alarm) {
		if (alarm != null) {
			linkBreakRepository.save(alarm);
		}
	}
}
