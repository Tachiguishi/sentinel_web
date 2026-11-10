package wizard.skunk.substation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.common.IProgressMonitor;
import wizard.skunk.parser.domain.IACDUnit;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.DeviceAlarm;
import wizard.skunk.persistence.substation.service.DeviceAlarmService;
import wizard.skunk.persistence.substation.service.DeviceService;

import java.util.ArrayList;
import java.util.List;

@Service
public class IacdService {
	private DeviceService deviceService;
	private DeviceAlarmService deviceAlarmService;

	@Autowired
	public IacdService(DeviceService deviceService, DeviceAlarmService deviceAlarmService) {
		this.deviceService = deviceService;
		this.deviceAlarmService = deviceAlarmService;
	}

	public void persistIACDUnits(List<IACDUnit> iacdUnits, IProgressMonitor monitor) {
		if (iacdUnits == null || iacdUnits.isEmpty()) {
			return;
		}

		monitor.beginTask("Persisting IACD Units", iacdUnits.size());

		List<Device> devices = deviceService.find();
		for(IACDUnit iacdUnit : iacdUnits){
			monitor.subTask( iacdUnit.getManufacturer() + " " + iacdUnit.getType() + " " + iacdUnit.getConfigVersion());
			for(Device device : devices){
				if(device.getManufacturer().equals(iacdUnit.getManufacturer())
						&& device.getType().equals(iacdUnit.getType())
						&& device.getVersion().equals(iacdUnit.getConfigVersion())) {
					deviceAlarmService.deleteByDevice(device);

					List<DeviceAlarm> deviceAlarms = new ArrayList<>();
					for (wizard.skunk.parser.domain.DeviceAlarm alarm : iacdUnit.getDeviceAlarmList()) {
						DeviceAlarm deviceAlarm = new DeviceAlarm();
						deviceAlarm.setDevice(device);
						deviceAlarm.setReference(alarm.getReference());
						deviceAlarm.setDescription(alarm.getDescription());
						deviceAlarm.setSeverity(alarm.getLevel());
						deviceAlarm.setEffect(alarm.getEffect());
						deviceAlarm.setSuggestion(alarm.getSuggestion());

						for (String board : alarm.getBoardList()) {
							deviceAlarm.addBoardSlot(board);
						}

						deviceAlarms.add(deviceAlarm);
					}

					deviceAlarmService.saveAllDeviceAlarms(deviceAlarms);
				}
			}

			monitor.worked(1);
		}
	}
}
