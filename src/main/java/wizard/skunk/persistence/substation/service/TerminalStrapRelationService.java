package wizard.skunk.persistence.substation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.TerminalStrapRelation;
import wizard.skunk.persistence.substation.repository.TerminalStrapRelationRepository;

import java.util.List;

@Service
public class TerminalStrapRelationService {
	private final TerminalStrapRelationRepository repository;

	@Autowired
	public TerminalStrapRelationService(TerminalStrapRelationRepository repository) {
		this.repository = repository;
	}

	public void saveAll(List<TerminalStrapRelation> relations) {
		repository.saveAll(relations);
	}

	public List<TerminalStrapRelation> findByDevice(Device device) {
		return (List<TerminalStrapRelation>) repository.findByDevice(device);
	}
}
