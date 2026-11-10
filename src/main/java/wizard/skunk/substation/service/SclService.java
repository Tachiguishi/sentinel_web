package wizard.skunk.substation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.common.IProgressMonitor;
import wizard.skunk.common.StandardMonitor;
import wizard.skunk.parser.domain.IED;
import wizard.skunk.parser.domain.SCDVersion;
import wizard.skunk.parser.scl.SclParser;
import wizard.skunk.persistence.substation.domain.Device;

import java.io.File;
import java.util.List;

@Service
@Slf4j
public class SclService {
	private final ScdService scdService;

	@Autowired
	public SclService(ScdService scdService) {
		this.scdService = scdService;
	}

	public void parseSCD(File file, IProgressMonitor monitor) throws Exception {
		if(monitor == null){
			monitor = new StandardMonitor();
		}

		ParsedScd parsed = readSCD(file, monitor);
		scdService.persistSCD(parsed.version(), parsed.devices(), monitor);
	}

	static ParsedScd readSCD(File file, IProgressMonitor monitor) throws Exception {
		SclParser parser = new SclParser();
		List<IED> devices = parser.parseSCD(file, monitor);
		return new ParsedScd(parser.getScdVersion(), devices);
	}

	record ParsedScd(SCDVersion version, List<IED> devices) {}

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
