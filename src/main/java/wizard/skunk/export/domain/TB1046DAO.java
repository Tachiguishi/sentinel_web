package wizard.skunk.export.domain;

import java.util.List;
import java.util.ArrayList;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.DeviceCategory;

public class TB1046DAO extends IDataTransfer<TB1046IED>{
	public TB1046DAO(ModelData model, String stationCode) {
		super(model, stationCode);
	}

	public List<TB1046IED> transfer(String prefix){
		this.prefix = prefix;
		String supervisory = model.devices.stream()
				.filter(device -> device.getCategory() == DeviceCategory.Supervisory.value)
				.findFirst().map(device -> getCode(TB1046IED.Prefix, device.getId())).orElse("");
		List<TB1046IED> result = new ArrayList<>();
		for (Device device : model.devices) {
			TB1046IED ied = new TB1046IED();
			ied.setCode(getCode(TB1046IED.Prefix, device.getId()));
			ied.setName(device.getName());
			ied.setDesc(device.getDescription());
			ied.setManufacturer(device.getManufacturer());
			ied.setModel(device.getType());
			ied.setVersion(device.getVersion());
			ied.setCategory(switch (DeviceCategory.fromValue(device.getCategory())) {
				case Supervisory -> 29;
				case TravelingWave -> 21;
				case TerminalBox -> 22;
				case OperationBox -> 23;
				case VoltageParallel -> 24;
				case DCSystemMonitor -> 25;
				case PowerQualityMonitor -> 26;
				case HardPressurePlateMonitor -> 30;
				case GeneralDevice -> 0;
				case null -> 0;
			});
			ied.setUuid(device.getPhysicalID());
			ied.setSerialNO(device.getSerialNumber());
			ied.setSubstation(stationCode);
			ied.setSupervisory(supervisory);
			if (device.getBay() != null) {
				ied.setBayName(device.getBay().getName());
				if (device.getBay().getVoltageLevel() != null) {
					ied.setVoltage(device.getBay().getVoltageLevel().getCompilableValue());
				}
			}
			if (device.getCabinet() != null) ied.setCabinet(getCode("Cubicle", device.getCabinet().getId()));
			result.add(ied);
		}
		return result;
	}
}
