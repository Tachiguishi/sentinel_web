package wizard.skunk.persistence.substation.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.persistence.substation.domain.Cabinet;
import wizard.skunk.persistence.substation.domain.District;
import wizard.skunk.persistence.substation.repository.CabinetRepository;
import wizard.skunk.persistence.substation.repository.DeviceRepository;
import wizard.skunk.persistence.substation.repository.DistrictRepository;

import java.util.List;

@Service
public class CabinetService {
	private final DistrictRepository districtRepository;
	private final CabinetRepository cabinetRepository;
	private DeviceRepository deviceRepository;

	@Autowired
	public CabinetService(DistrictRepository districtRepository, CabinetRepository cabinetRepository) {
		this.districtRepository = districtRepository;
		this.cabinetRepository = cabinetRepository;
	}

	@Autowired
	public void setDeviceRepository(DeviceRepository deviceRepository) {
		this.deviceRepository = deviceRepository;
	}

	public List<District> findAllDistrict() {
		return (List<District>) districtRepository.findAll();
	}

	public List<Cabinet> findByDistrict(District district) {
		return (List<Cabinet>) cabinetRepository.findByDistrict(district);
	}

	@Transactional
	public void deleteAllDistrict(){
		deviceRepository.clearCabinetForAllDevices();
		cabinetRepository.deleteAll();
		districtRepository.deleteAll();
	}

	public void saveDistrict(District district){
		districtRepository.save(district);
	}

	public void saveDistrict(List<District> districts){
		districtRepository.saveAll(districts);
	}

	public void saveCabinet(Cabinet cabinet){
		cabinetRepository.save(cabinet);
	}

	public void saveCabinets(List<Cabinet> cabinets){
		cabinetRepository.saveAll(cabinets);
	}
}
