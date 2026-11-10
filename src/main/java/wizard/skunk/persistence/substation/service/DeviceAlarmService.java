package wizard.skunk.persistence.substation.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.DeviceAlarm;
import wizard.skunk.persistence.substation.repository.DeviceAlarmRepository;

import java.util.List;

@Service
public class DeviceAlarmService {
	private final DeviceAlarmRepository deviceAlarmRepository;

	@Autowired
	public DeviceAlarmService(DeviceAlarmRepository deviceAlarmRepository) {
		this.deviceAlarmRepository = deviceAlarmRepository;
	}

	public List<DeviceAlarm> find(){
		return (List<DeviceAlarm>) deviceAlarmRepository.findAll();
	}

	public List<DeviceAlarm> findByDevice(Device device) {
		return (List<DeviceAlarm>) deviceAlarmRepository.findByDevice(device);
	}

	public void saveDeviceAlarm(DeviceAlarm deviceAlarm) {
		deviceAlarmRepository.save(deviceAlarm);
	}

	public void saveAllDeviceAlarms(Iterable<DeviceAlarm> deviceAlarms) {
		deviceAlarmRepository.saveAll(deviceAlarms);
	}

	@Transactional
	public void deleteDeviceAlarm(DeviceAlarm deviceAlarm) {
		deviceAlarmRepository.delete(deviceAlarm);
	}

	@Transactional
	public void deleteAllDeviceAlarms(Iterable<DeviceAlarm> deviceAlarms) {
		deviceAlarmRepository.deleteAll(deviceAlarms);
	}

	@Transactional
	public void deleteByDevice(Device device){
		deviceAlarmRepository.deleteByDevice(device);
	}
}
