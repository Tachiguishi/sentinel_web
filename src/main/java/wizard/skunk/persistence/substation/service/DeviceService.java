package wizard.skunk.persistence.substation.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import wizard.skunk.persistence.service.SCLFileService;
import wizard.skunk.persistence.substation.domain.Bay;
import wizard.skunk.persistence.substation.domain.Cabinet;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.repository.DeviceRepository;

@Service
public class DeviceService {
	private final DeviceRepository deviceRepository;
	private SCLFileService sclFileService;
	private DataSetService dataSetService;
	private FCDAService fcdaService;

	@Autowired
	public DeviceService(DeviceRepository repository){
		this.deviceRepository = repository;
	}

	@Autowired
	public void setSclFileService(SCLFileService sclFileService){
		this.sclFileService = sclFileService;
	}

	@Autowired
	public void setDataSetService(DataSetService dataSetService){
		this.dataSetService = dataSetService;
	}

	@Autowired
	public void setFcdaService(FCDAService fcdaService) {
		this.fcdaService = fcdaService;
	}

	public Device findByName(String name){
		return deviceRepository.findByName(name);
	}

	public List<Device> findByCategory(int category){
		return (List<Device>) deviceRepository.findByCategory(category);
	}

	public List<Device> findByCabinet(Cabinet cabinet){
		return (List<Device>) deviceRepository.findByCabinet(cabinet);
	}

	/*
	 * find valid device
	 */
	public List<Device> find(){
		return (List<Device>) deviceRepository.findByIgnored(false);
	}

	public List<Device> findAll(){
		return (List<Device>) deviceRepository.findAll();
	}

	public void save(Device device){
		validateUniqueConstraints(device);
		deviceRepository.save(device);
	}

	public void saveAll(List<Device> devices){
		validateUniqueConstraints(devices);
		deviceRepository.saveAll(devices);
	}

	public void validateUniqueConstraints(Device device) {
		if (device == null) {
			return;
		}
		validateUniqueConstraints(List.of(device));
	}

	public void validateUniqueConstraints(List<Device> devices) {
		if (devices == null || devices.isEmpty()) {
			return;
		}

		List<Device> existingDevices = findAll();
		Map<String, Device> seenNameKeys = new LinkedHashMap<>();
		for (Device device : devices) {
			if (device == null) {
				continue;
			}

			String normalizedName = normalize(device.getName());
			if (normalizedName == null) {
				continue;
			}

			for (Device existingDevice : existingDevices) {
				if (existingDevice == null || isSameDevice(device, existingDevice)) {
					continue;
				}
				if (normalizedName.equals(normalize(existingDevice.getName()))) {
					throw duplicateNameException(normalizedName, existingDevice);
				}
			}

			Device seenDevice = seenNameKeys.putIfAbsent(normalizedName, device);
			if (seenDevice != null && !isSameDevice(device, seenDevice)) {
				throw duplicateNameException(normalizedName, seenDevice);
			}
		}
	}

	@Transactional
	public void delete(Device device){
		sclFileService.deleteByIdentifier(device.getName());
		dataSetService.deleteByDevice(device);
		fcdaService.deleteByDevice(device);
		deviceRepository.delete(device);
	}

	public List<Device> findByBay(Bay bay) {
		return (List<Device>) deviceRepository.findByBay(bay);
	}

	private IllegalArgumentException duplicateNameException(String deviceName, Device existingDevice) {
		String existingText = describeDevice(existingDevice);
		if (existingText == null) {
			return new IllegalArgumentException("二次设备名称“" + deviceName + "”不能重复");
		}
		return new IllegalArgumentException("二次设备名称“" + deviceName + "”不能重复，已被设备“" + existingText + "”使用");
	}

	private boolean isSameDevice(Device left, Device right) {
		return left != null
				&& right != null
				&& left.getId() != null
				&& left.getId().equals(right.getId());
	}

	private String normalize(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}

	private String describeDevice(Device device) {
		if (device == null) {
			return null;
		}
		String description = normalize(device.getDescription());
		String name = normalize(device.getName());
		if (description != null && name != null) {
			return description + "(" + name + ")";
		}
		return description != null ? description : name;
	}
}
