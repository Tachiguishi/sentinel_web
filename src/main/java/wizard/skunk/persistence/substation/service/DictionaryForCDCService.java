package wizard.skunk.persistence.substation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wizard.skunk.persistence.substation.domain.DictionaryForCDC;
import wizard.skunk.persistence.substation.repository.DictionaryForCDCRepository;
import wizard.skunk.persistence.substation.domain.Device;

import java.util.List;

@Service
public class DictionaryForCDCService {
	private final DictionaryForCDCRepository dictionaryForCDCRepository;

	@Autowired
	public DictionaryForCDCService(DictionaryForCDCRepository dictionaryForCDCRepository) {
		this.dictionaryForCDCRepository = dictionaryForCDCRepository;
	}

	@Transactional
	public void deleteByDevice(Device device){
		if (device == null) {
			return;
		}
		dictionaryForCDCRepository.deleteByDevice(device);
	}

	public void saveAll(List<DictionaryForCDC> dictionaryForCDCs) {
		if (dictionaryForCDCs == null || dictionaryForCDCs.isEmpty()) {
			return;
		}
		dictionaryForCDCRepository.saveAll(dictionaryForCDCs);
	}

	public List<DictionaryForCDC> findAll() {
		return (List<DictionaryForCDC>) dictionaryForCDCRepository.findAll();
	}
}
