package wizard.skunk.persistence.substation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wizard.skunk.persistence.substation.domain.NetworkCable;
import wizard.skunk.persistence.substation.repository.NetworkCableRepository;

import java.util.List;

@Service
public class NetworkCableService {
	private final NetworkCableRepository cableRepository;

	@Autowired
	public NetworkCableService(NetworkCableRepository cableRepository) {
		this.cableRepository = cableRepository;
	}

	public void save(List<NetworkCable> cables){
		cableRepository.saveAll(cables);
	}

	public List<NetworkCable> find() {
		return (List<NetworkCable>) cableRepository.findAll();
	}

	@Transactional
	public void deleteAll() {
		cableRepository.deleteAll();
	}
}
