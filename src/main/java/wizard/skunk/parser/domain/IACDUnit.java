package wizard.skunk.parser.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class IACDUnit {
	private String name;
	private String manufacturer;
	private String type;
	private String configVersion;
	private String className;
	private String physicalID;
	private String serialNumber;

	private final List<DeviceAlarm> deviceAlarmList = new ArrayList<>();

	public void addDeviceAlarm(List<DeviceAlarm> alarms){
		deviceAlarmList.addAll(alarms);
	}
}
