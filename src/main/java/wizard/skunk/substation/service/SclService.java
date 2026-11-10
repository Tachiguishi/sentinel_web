package wizard.skunk.substation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.common.IProgressMonitor;
import wizard.skunk.common.StandardMonitor;
import wizard.skunk.parser.domain.IACDUnit;
import wizard.skunk.parser.domain.IED;
import wizard.skunk.parser.domain.OpticalFiber;
import wizard.skunk.parser.domain.Region;
import wizard.skunk.parser.scl.SclParser;
import wizard.skunk.persistence.substation.domain.Device;

import java.io.File;
import java.util.List;

@Service
@Slf4j
public class SclService {
	private final ScdService scdService;
	private IacdService iacdService;
	private CSLKService cslkService;
	private SPDService spdService;

	@Autowired
	public SclService(ScdService scdService) {
		this.scdService = scdService;
	}

	@Autowired
	public void setIacdService(IacdService iacdService) {
		this.iacdService = iacdService;
	}

	@Autowired
	public void setCslkService(CSLKService cslkService) {
		this.cslkService = cslkService;
	}

	@Autowired
	public void setSpdService(SPDService spdService) {
		this.spdService = spdService;
	}

	public void parseSCD(File file, IProgressMonitor monitor) throws Exception {
		if(monitor == null){
			monitor = new StandardMonitor();
		}

		long stop1 = System.currentTimeMillis();
		SclParser parser = new SclParser();
		List<IED> iedList = parser.parseSCD(file, monitor);
		long stop2 = System.currentTimeMillis();
		log.info("Parse SCD cost: " + (stop2 - stop1) + "ms");

		scdService.persistSCD(parser.getScdVersion(), iedList, monitor);
		stop1 = System.currentTimeMillis();
		log.info("Persist SCD cost: " + (stop1 - stop2) + "ms");
	}

	public void parseRecordSCD(File file, IProgressMonitor monitor) throws Exception {
		if(monitor == null){
			monitor = new StandardMonitor();
		}

		long stop1 = System.currentTimeMillis();
		SclParser parser = new SclParser();
		List<IED> iedList = parser.parseRecorderSCD(file, monitor);
		long stop2 = System.currentTimeMillis();
		log.info("Parse Record SCD cost: {}ms", stop2 - stop1);

		scdService.persistSCD(parser.getScdVersion(), iedList, monitor);
		stop1 = System.currentTimeMillis();
		log.info("Persist SCD cost: " + (stop1 - stop2) + "ms");

		cslkService.parseBreakLinkAlarm();
	}

	public void parseCID(Device targetDevice, File file, IProgressMonitor monitor) throws Exception{
		if(monitor == null){
			monitor = new StandardMonitor();
		}

		long stop1 = System.currentTimeMillis();
		SclParser parser = new SclParser();
		IED ied =  parser.parseCID(file, monitor);
		long stop2 = System.currentTimeMillis();
		log.info("Parse CID cost: " + (stop2 - stop1) + "ms");

		scdService.persistDevice(targetDevice, ied);
		stop1 = System.currentTimeMillis();
		log.info("Persist CID cost: " + (stop1 - stop2) + "ms");
	}

	public void parseIACD(List<File> files, IProgressMonitor monitor) throws Exception{
		if(monitor == null){
			monitor = new StandardMonitor();
		}

		monitor.beginTask("parse IACD", files.size());

		long stop1 = System.currentTimeMillis();
		SclParser sclParser = new SclParser();
		List<IACDUnit> units = sclParser.parseIACD(files, monitor);
		long stop2 = System.currentTimeMillis();
		log.info("Parse IACD cost: " + (stop2 - stop1) + "ms");

		iacdService.persistIACDUnits(units, monitor);
	}

	public void parseSPD(File file, IProgressMonitor monitor) throws Exception{
		if(monitor == null){
			monitor = new StandardMonitor();
		}

		long stop1 = System.currentTimeMillis();
		SclParser parser = new SclParser();
		parser.parseSPD(file, monitor);
		long stop2 = System.currentTimeMillis();
		log.info("Parse SPD cost: " + (stop2 - stop1) + "ms");

		List<Region> regions = parser.getRegionList();
		spdService.persistRegion(regions);
		List<OpticalFiber> opticalFibers = parser.getOpticalFiberList();
		spdService.persistOpticalFiber(opticalFibers);
	}

	public void parseSMCD(File file, IProgressMonitor monitor) throws Exception{
		if(monitor == null){
			monitor = new StandardMonitor();
		}

		long stop1 = System.currentTimeMillis();
		SclParser parser = new SclParser();
		parser.parseSMCD(file, monitor);
		long stop2 = System.currentTimeMillis();
		log.info("Parse SMCD cost: " + (stop2 - stop1) + "ms");
	}
}
