package wizard.skunk.persistence.substation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.persistence.substation.domain.Bay;
import wizard.skunk.persistence.substation.domain.VoltageLevel;
import wizard.skunk.persistence.substation.repository.BayRepository;
import wizard.skunk.persistence.substation.repository.VoltageLevelRepository;

import java.util.List;

@Service
public class BayService {
	private final VoltageLevelRepository voltageLevelRepository;
	private final BayRepository bayRepository;

	@Autowired
	public BayService(VoltageLevelRepository voltageLevelRepository, BayRepository bayRepository) {
		this.voltageLevelRepository = voltageLevelRepository;
		this.bayRepository = bayRepository;
	}

	public List<VoltageLevel> findVoltage(){
		return (List<VoltageLevel>) voltageLevelRepository.findAll();
	}

	public List<Bay> findByVoltage(VoltageLevel voltageLevel){
		return (List<Bay>) bayRepository.findByVoltageLevel(voltageLevel);
	}
}
