package wizard.skunk.substation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.persistence.substation.domain.*;
import wizard.skunk.persistence.substation.service.DeviceService;
import wizard.skunk.persistence.substation.service.FCDAService;
import wizard.skunk.persistence.substation.service.LogicalLinkService;
import wizard.skunk.persistence.substation.service.TerminalStrapRelationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CSLKService {
	private LogicalLinkService logicalLinkService;
	private DeviceService deviceService;
	private FCDAService fcdaService;
	private TerminalStrapRelationService terminalStrapRelationService;

	@Autowired
	public void setLogicalLinkService(LogicalLinkService logicalLinkService) {
		this.logicalLinkService = logicalLinkService;
	}

	@Autowired
	public void setDeviceService(DeviceService deviceService) {
		this.deviceService = deviceService;
	}

	@Autowired
	public void setFcdaService(FCDAService fcdaService) {
		this.fcdaService = fcdaService;
	}

	@Autowired
	public void setTerminalStrapRelationService(TerminalStrapRelationService terminalStrapRelationService) {
		this.terminalStrapRelationService = terminalStrapRelationService;
	}

	public void parseBreakLinkAlarm(){
		List<TerminalStrapRelation> relations = new ArrayList<>();

		List<Device> devices = deviceService.find();
		for(Device device : devices){
			// P_K2201A
			List<FCDA> fcdaList = fcdaService.findByDotReferenceLike("Lnk1CBRef");
			fcdaList.addAll(fcdaService.findByDotReferenceLike("Lnk2CBRef"));
			if(fcdaList.isEmpty()){
				continue;
			}

			Map<String, GooseSvControl> controlMap = new HashMap<>();
			Map<String, List<LogicalLink>> logicalLinkMap = new HashMap<>();
			List<LogicalLink> logicalLinkList = logicalLinkService.findByReceiverDevice(device);
			for(LogicalLink logicalLink : logicalLinkList){
				GooseSvControl senderControl = logicalLink.getSenderControl();
				String key = senderControl.getDevice().getName() + senderControl.getDotReference();
				if(!logicalLinkMap.containsKey(key)){
					logicalLinkMap.put(key, new ArrayList<>());
				}

				logicalLinkMap.get(key).add(logicalLink);
				controlMap.put(key, senderControl);
			}

			for(FCDA fcda : fcdaList){
				String controlReference = fcda.getDescription();
				if(controlReference == null || controlReference.isEmpty()){
					continue;
				}

				GooseSvControl gooseSvControl = controlMap.get(controlReference);
				if(gooseSvControl == null){
					continue;
				}

				String prefix = fcda.getLdInst() + "/" + fcda.getPrefix() + fcda.getLnClass() + fcda.getLnInst();
				FCDA alarmFcda = fcdaService.findByDeviceAndDotReference(device, prefix + ".Lnk1Dwn");
				if(alarmFcda == null){
					alarmFcda = fcdaService.findByDeviceAndDotReference(device, prefix + ".Lnk1ADwn");
					if(alarmFcda == null){
						alarmFcda = fcdaService.findByDeviceAndDotReference(device, prefix + ".Lnk2Dwn");
						if(alarmFcda == null){
							alarmFcda = fcdaService.findByDeviceAndDotReference(device, prefix + ".Lnk2ADwn");
						}
					}
				}

				if(alarmFcda != null){
					LinkBreakAlarm alarm = new LinkBreakAlarm();
					alarm.setReceiver(device);
					alarm.setAlarmFCDA(alarmFcda);
					alarm.setSenderControl(gooseSvControl);

					logicalLinkService.saveLinkBreakAlarm(alarm);
				}


				List<LogicalLink> logicalLinks = logicalLinkMap.get(controlReference);
				if(logicalLinks == null || logicalLinks.isEmpty()){
					continue;
				}

				List<FCDA> straps = fcdaService.findAllByDeviceAndDotReference(device, prefix + ".LnkRxVirEna");
				if(straps.isEmpty()){
					continue;
				}
				FCDA strap = straps.get(0);
				for(LogicalLink logicalLink : logicalLinks){
					TerminalStrapRelation relation = new TerminalStrapRelation();
					relation.setDevice(device);
					relation.setTerminal(logicalLink.getReceiverFcda());
					relation.setStrap(strap);

					relations.add(relation);
				}
			}
		}

		if(!relations.isEmpty()){
			terminalStrapRelationService.saveAll(relations);
		}
	}
}
