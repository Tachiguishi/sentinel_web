package wizard.skunk.substation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.common.IProgressMonitor;
import wizard.skunk.parser.domain.IED;
import wizard.skunk.parser.domain.SCDVersion;
import wizard.skunk.persistence.substation.domain.*;
import wizard.skunk.persistence.substation.service.*;

import java.util.*;

@Service
@Slf4j
public class ScdService {
	private SubstationService substationService;
	private DeviceService deviceService;
	private DictionaryForCDCService cdcService;
	private DataSetService dataSetService;
	private FCDAService fcdaService;
	private LogicalLinkService logicalLinkService;
	private ICategoryIdentifier categoryIdentifier;

	private final Map<String, Map<String, FCDA>> fcdaMap = new HashMap<>();
	private final Map<String, Map<String, GooseSvControl>> fcdaControlMap = new HashMap<>();
	private final List<FCDA> receiverFCDAList = new ArrayList<>();

	@Autowired
	public void setSubstationService(SubstationService substationService){
		this.substationService = substationService;
	}

	@Autowired
	public void setDeviceService(DeviceService deviceService){
		this.deviceService = deviceService;
	}

	@Autowired
	public void setCdcService(DictionaryForCDCService cdcService){
		this.cdcService = cdcService;
	}

	@Autowired
	public void setDataSetService(DataSetService dataSetService){
		this.dataSetService = dataSetService;
	}

	@Autowired
	public void setFcdaService(FCDAService fcdaService){
		this.fcdaService = fcdaService;
	}

	@Autowired
	public void setLogicalLinkService(LogicalLinkService logicalLinkService){
		this.logicalLinkService = logicalLinkService;
	}

	@Autowired(required = false)
	public void setCategoryIdentifier(ICategoryIdentifier categoryIdentifier){
		this.categoryIdentifier = categoryIdentifier;
	}

	public void clear(){
		fcdaMap.clear();
		fcdaControlMap.clear();
		receiverFCDAList.clear();
	}

	public void persistSCD(SCDVersion scdVersion, List<IED> iedList, IProgressMonitor monitor){
		monitor.beginTask("Persist SCD Version", 1);
		persistVersionInfo(scdVersion);
		monitor.beginTask("Persist IED", iedList.size());

		clear();

		List<Device> devices = deviceService.find();
		List<Device> removedDevices = new ArrayList<>(devices);

		iedList.sort(Comparator.comparing(IED::getDescription));

		long stop1 = System.currentTimeMillis();
		for(IED ied : iedList){
			monitor.subTask(ied.getName());

			Device device = deviceFactory(devices, removedDevices, ied);

			persistDevice(device, ied);

			monitor.worked(1);
		}
		long stop2 = System.currentTimeMillis();
		log.info("Persist IED cost: " + (stop2 - stop1) + "ms");

		monitor.beginTask("Delete IED", removedDevices.size());
		for(Device device : removedDevices){
			monitor.subTask(device.getName());
			deviceService.delete(device);
			cdcService.deleteByDevice(device);
			monitor.worked(1);
		}

		monitor.beginTask("Persist LogicalLink", iedList.size());
		for(IED ied : iedList){
			monitor.subTask(ied.getName());

			Device device = findByName(devices, ied.getName());
			if(device == null){
				continue;
			}

			persistLogicalLink(device, ied, fcdaMap, fcdaControlMap);
			persistCDC(device, ied);

			monitor.worked(1);
		}
		fcdaService.saveAll(receiverFCDAList);
		stop1 = System.currentTimeMillis();
		log.info("Persist LogicalLink cost: " + (stop1 - stop2) + "ms");
	}

	private void persistCDC(Device device, IED ied){
		List<DictionaryForCDC> dictionaryForCDCs = new ArrayList<>();
		for(wizard.skunk.parser.domain.CDCMap scdCDC : ied.getCdcMaps()){
			DictionaryForCDC dictionaryForCDC = new DictionaryForCDC();
			dictionaryForCDC.setDevice(device);
			dictionaryForCDC.setCdc(scdCDC.getCdc());
			dictionaryForCDC.setReference(scdCDC.getReference());
			dictionaryForCDCs.add(dictionaryForCDC);
		}
		cdcService.saveAll(dictionaryForCDCs);
	}

	private void persistVersionInfo(SCDVersion scdVersion){
		Substation substation = substationService.find();
		substation.setIdentify(scdVersion.getIdentify());
		substation.setVersion(scdVersion.getVersion());
		substation.setRevision(scdVersion.getRevision());
		substation.setWho(scdVersion.getWho());
		substation.setModifyTime(scdVersion.getWhen());
		substation.setWhat(scdVersion.getWhat());
		substation.setWhy(scdVersion.getWhy());
		substation.setScdCRC(scdVersion.getScdCRC());
		substation.setTerminalCRC(scdVersion.getTerminalCRC());

		substationService.save(substation);
	}

	public void persistDevice(Device device, IED ied){
		// <dotReference, FCDA>
		Map<String, FCDA> existFcdaMap = new HashMap<>();
		persistFCDA(device, existFcdaMap, ied);
		fcdaMap.put(ied.getName(), existFcdaMap);

		List<DataSet> existDataSetList = dataSetService.findByDevice(device);
		// <dotReference, GooseSvControl>
		Map<String, GooseSvControl> controlMap = persistDataSet(device, existDataSetList, existFcdaMap, ied);
		fcdaControlMap.put(ied.getName(), controlMap);
	}

	private void persistLogicalLink(Device device, IED ied, Map<String, Map<String, FCDA>> fcdaMap, Map<String, Map<String, GooseSvControl>> fcdaControlMap){
		List<LogicalLink> existLogicalLinks = logicalLinkService.findByReceiverDevice(device);
		List<LogicalLink> removedLogicalLinks = new ArrayList<>(existLogicalLinks);
		for(wizard.skunk.parser.domain.LogicalLink scdLogicalLink : ied.getLogicalLinks()){
			FCDA receiverFcda = fcdaMap.get(ied.getName()).get(scdLogicalLink.getReceiverReference());
			String senderDevice = scdLogicalLink.getSenderIedName();
			if(!fcdaMap.containsKey(senderDevice)){
				log.error("Sender IED not found: " + senderDevice + " for logicalLink: " + scdLogicalLink);
				continue;
			}
			FCDA senderFcda = fcdaMap.get(senderDevice).get(scdLogicalLink.getSenderReference());
			if(senderFcda == null){
				log.error("Sender FCDA not found: " + scdLogicalLink.getSenderReference() + " for logicalLink: " + scdLogicalLink);
				continue;
			}

			LogicalLink logicalLink = findLogicalLinkByReceiver(existLogicalLinks, scdLogicalLink.getReceiverReference());
			if(logicalLink == null){
				logicalLink = new LogicalLink();
				logicalLink.setModifyStatus(ModifyStatus.CREATED);
				logicalLink.setReceiverDevice(device);
				logicalLink.setReceiverFcda(receiverFcda);
				logicalLink.setSenderFcda(senderFcda);
				logicalLink.setSenderDevice(senderFcda.getDevice());

				existLogicalLinks.add(logicalLink);
			}
			else{
				if(senderFcda.getId().equals(logicalLink.getSenderFcda().getId())){
					logicalLink.setModifyStatus(ModifyStatus.NO_CHANGES);
				}
				else{
					logicalLink.setModifyStatus(ModifyStatus.CREATED);
					logicalLink.setSenderFcda(senderFcda);
				}
				removedLogicalLinks.remove(logicalLink);
			}

			receiverFcda.setFc(senderFcda.getFc());
			receiverFCDAList.add(receiverFcda);

			logicalLink.setReceiverBoarPort(scdLogicalLink.getReceiverBoarPort());
			logicalLink.setSenderControl(fcdaControlMap.get(senderDevice).get(scdLogicalLink.getSenderReference()));
		}
		for(LogicalLink logicalLink : removedLogicalLinks){
			existLogicalLinks.remove(logicalLink);
		}
		logicalLinkService.deleteLogicalLinks(removedLogicalLinks);
		logicalLinkService.saveLogicalLinks(existLogicalLinks);
	}

	private Map<String, GooseSvControl> persistDataSet(Device device, List<DataSet> existDataSetList, Map<String, FCDA> existFcdaMap, IED ied){
		// <FcdaDotReference, GOOSE>
		Map<String, GooseSvControl> fcdaControlMap = new HashMap<>();

		dataSetService.deleteDataItemsByDataSet(existDataSetList);

		List<DataSet> removedDataSets = new ArrayList<>(existDataSetList);
		List<DataItem> dataItems = new ArrayList<>();
		Map<String, List<String>> dataSetFcdaMap = new HashMap<>();
		for(wizard.skunk.parser.domain.DataSet scdDataSet : ied.getDataSets()){
			DataSet dataSet = findByIdentity(existDataSetList, scdDataSet.getIdentity());
			if(dataSet == null){
				dataSet = new DataSet();
				dataSet.setName(scdDataSet.getName());
				dataSet.setIdentity(scdDataSet.getIdentity());
				dataSet.setAccessPoint(scdDataSet.getAccessPoint());
				dataSet.setLdName(scdDataSet.getLdName());
				dataSet.setLdDescription(scdDataSet.getLdDescription());

				dataSet.setDevice(device);
				existDataSetList.add(dataSet);
			}
			else{
				removedDataSets.remove(dataSet);
			}
			dataSet.setDescription(scdDataSet.getDescription());
			dataSet.setLnClass(scdDataSet.getLnClass());

			int index = 0;
			for(String fcdaReference : scdDataSet.getFcdaList()){
				FCDA fcda = existFcdaMap.get(fcdaReference);
				if(fcda == null){
					log.error("FCDA not found: " + fcdaReference + " in " + device.getName() + "." + dataSet.getIdentity());
					continue;
				}

				DataItem item = new DataItem();
				item.setDataSet(dataSet);
				item.setFcda(fcda);
				item.setFcdaIndex(index++);
				dataItems.add(item);

				dataSetFcdaMap.putIfAbsent(dataSet.getIdentity(), new ArrayList<>());
				dataSetFcdaMap.get(dataSet.getIdentity()).add(fcdaReference);
			}
		}

		for(DataSet dataSet : removedDataSets){
			existDataSetList.remove(dataSet);
			dataSetService.delete(dataSet);
		}



		List<ReportControl> existReportControls = dataSetService.findReportControlByDevice(device);
		List<ReportControl> removedReportControls = new ArrayList<>(existReportControls);
		for(wizard.skunk.parser.domain.ReportControl scdReportControl : ied.getReportControls()){
			DataSet dataSet = findByIdentity(existDataSetList, scdReportControl.getDataSet());
			if(dataSet == null){
				continue;
			}

			ReportControl reportControl = findReportByReference(existReportControls, scdReportControl.getReference());
			if(reportControl == null){
				reportControl = new ReportControl();
				reportControl.setName(scdReportControl.getName());
				reportControl.setReference(scdReportControl.getReference());
				reportControl.setDevice(device);
				existReportControls.add(reportControl);
			}
			else{
				removedReportControls.remove(reportControl);
			}

			reportControl.setRptID(scdReportControl.getRptID());
			reportControl.setBuffered(scdReportControl.isBuffered());
			reportControl.setRptEnabled(scdReportControl.getRptEnabled());
			reportControl.setDataSet(dataSet);

			dataSet.setCategory(DataSetCategory.ReportControl.value);
		}
		for(ReportControl reportControl : removedReportControls){
			existReportControls.remove(reportControl);
		}


		List<GooseSvControl> existGooseSvControls = dataSetService.findGooseSvControlByDevice(device);
		List<GooseSvControl> removedGooseSvControls = new ArrayList<>(existGooseSvControls);
		for(wizard.skunk.parser.domain.GooseSvControl scdGooseSvControl : ied.getGooseSvControls()){
			DataSet dataSet = findByIdentity(existDataSetList, scdGooseSvControl.getDataSet());
			if(dataSet == null){
				continue;
			}

			GooseSvControl gooseSvControl = findGooseSvControlByReference(existGooseSvControls, scdGooseSvControl.getReference());
			if(gooseSvControl == null){
				gooseSvControl = new GooseSvControl();
				gooseSvControl.setName(scdGooseSvControl.getName());
				gooseSvControl.setReference(scdGooseSvControl.getReference());
				gooseSvControl.setGoose(scdGooseSvControl.isGoose());
				gooseSvControl.setDevice(device);
				existGooseSvControls.add(gooseSvControl);
			}
			else{
				removedGooseSvControls.remove(gooseSvControl);
			}

			gooseSvControl.setIdentify(scdGooseSvControl.getIdentify());
			gooseSvControl.setMacAddress(scdGooseSvControl.getMacAddress());
			gooseSvControl.setVlanId(scdGooseSvControl.getVlanId());
			gooseSvControl.setVlanPriority(scdGooseSvControl.getVlanPriority());
			gooseSvControl.setAppId(scdGooseSvControl.getAppId());

			gooseSvControl.setDataSet(dataSet);
			dataSet.setCategory(scdGooseSvControl.isGoose() ? DataSetCategory.GOOSEControl.value : DataSetCategory.SVControl.value);

			List<String> fcdaList = dataSetFcdaMap.getOrDefault(dataSet.getIdentity(), new ArrayList<>());
			for(String fcdaReference : fcdaList){
				fcdaControlMap.put(fcdaReference, gooseSvControl);
			}
		}
		for(GooseSvControl gooseSvControl : removedGooseSvControls){
			existGooseSvControls.remove(gooseSvControl);
		}

		if (categoryIdentifier != null) {
			categoryIdentifier.deduceFCDACategory(dataItems);
		}

		dataSetService.saveAll(existDataSetList);
		dataSetService.save(dataItems);

		dataSetService.deleteReportControls(removedReportControls);
		dataSetService.saveReportControls(existReportControls);

		dataSetService.deleteGooseSvControls(removedGooseSvControls);
		dataSetService.saveGooseSvControls(existGooseSvControls);

		return fcdaControlMap;
	}

	private void persistFCDA(Device device, Map<String, FCDA> existFcdaMap, IED ied){
		List<FCDA> removedFCDAList = fcdaService.findByDevice(device);
		removedFCDAList.forEach(fcda -> existFcdaMap.put(fcda.getDotReference(), fcda));
		for(wizard.skunk.parser.domain.FCDA scdFCDA : ied.getFcdaMap().values()){
			FCDA fcda = existFcdaMap.get(scdFCDA.getDotReference());
			if(fcda == null){
				fcda = new FCDA();
				fcda.setLdInst(scdFCDA.getLdInst());
				fcda.setPrefix(scdFCDA.getPrefix());
				fcda.setLnClass(scdFCDA.getLnClass());
				fcda.setLnInst(scdFCDA.getLnInst());
				fcda.setDoName(scdFCDA.getDoName());
				fcda.setDaName(scdFCDA.getDaName());
				fcda.setFc(scdFCDA.getFc());
				fcda.setDevice(device);

				fcda.setDollarReference(scdFCDA.getDollarReference());
				fcda.setDotReference(scdFCDA.getDotReference());
				existFcdaMap.put(fcda.getDotReference(), fcda);
			}
			else{
				removedFCDAList.remove(fcda);
			}

			fcda.setDescription(scdFCDA.getDescription());
			fcda.setCdc(scdFCDA.getCdc());
			fcda.setUnitSerialization(scdFCDA.getUnitSerialization());
		}

		for(FCDA fcda : removedFCDAList){
			existFcdaMap.remove(fcda.getDotReference());
			fcdaService.delete(fcda);
		}

		fcdaService.saveAll(new ArrayList<>(existFcdaMap.values()));
	}

	private Device deviceFactory(List<Device> devices, List<Device> removedDevices, IED ied){
		Device device = findByName(devices, ied.getName());
		if(device == null){
			device = new Device();
			device.setName(ied.getName());
			device.setModify(ModifyStatus.CHANGED.value);
			devices.add(device);
		}
		else{
			removedDevices.remove(device);
		}

		device.setSource(DataSource.SCD.index);

		device.setDescription(ied.getDescription());
		device.setManufacturer(ied.getManufacturer());
		device.setVersion(ied.getVersion());
		device.setType(ied.getType());
		device.setCrc(ied.getCrc());

		device.setIpAddressA(ied.getIpAddressA());
		device.setIpAddressB(ied.getIpAddressB());
		device.setApTitle(ied.getApTitle());
		device.setAeQualifier(ied.getAeQualifier());

		if (categoryIdentifier != null) {
			categoryIdentifier.deduceDeviceCategory(device);
		}

		deviceService.save(device);

		return device;
	}

	private Device findByName(List<Device> devices, String name){
		for(Device device : devices){
			if(device.getName().equals(name)){
				return device;
			}
		}
		return null;
	}

	private DataSet findByIdentity(List<DataSet> dataSets, String identity){
		for(DataSet dataSet : dataSets){
			if(dataSet.getIdentity().equals(identity)){
				return dataSet;
			}
		}
		return null;
	}

	private ReportControl findReportByReference(List<ReportControl> reportControls, String reference){
		for(ReportControl reportControl : reportControls){
			if(reportControl.getReference().equals(reference)){
				return reportControl;
			}
		}
		return null;
	}

	private GooseSvControl findGooseSvControlByReference(List<GooseSvControl> gooseSvControls, String reference){
		for(GooseSvControl gooseSvControl : gooseSvControls){
			if(gooseSvControl.getReference().equals(reference)){
				return gooseSvControl;
			}
		}
		return null;
	}

	private LogicalLink findLogicalLinkByReceiver(List<LogicalLink> logicalLinks, String reference){
		for(LogicalLink logicalLink : logicalLinks){
			if(logicalLink.getReceiverFcda().getDotReference().equals(reference)){
				return logicalLink;
			}
		}
		return null;
	}
}
