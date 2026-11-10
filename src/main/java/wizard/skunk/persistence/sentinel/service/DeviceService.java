package wizard.skunk.persistence.sentinel.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.persistence.sentinel.domain.Device;
import wizard.skunk.persistence.sentinel.repository.DeviceRepository;

import java.util.List;

@Service
public class DeviceService {
	private final DeviceRepository deviceRepository;

	@Autowired
	public DeviceService(DeviceRepository deviceRepository) {
		this.deviceRepository = deviceRepository;
	}

	public List<Device> findAll() {
		return (List<Device>) deviceRepository.findAll();
	}
}
