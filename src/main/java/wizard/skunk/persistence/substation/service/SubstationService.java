package wizard.skunk.persistence.substation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.persistence.substation.domain.Substation;
import wizard.skunk.persistence.substation.repository.SubstationRepository;

@Service
public class SubstationService {
	private final SubstationRepository substationRepository;

	@Autowired
	public SubstationService(SubstationRepository substationRepository) {
		this.substationRepository = substationRepository;
	}

	public Substation find(){
		Substation substation = substationRepository.findTopByIdNotNull();
		if(substation == null){
			substation = new Substation();
			substation.setName("Substation");
			substation.setDescription("未命名变电站");
		}
		return substation;
	}

	public void save(Substation substation){
		substationRepository.save(substation);
	}
}
