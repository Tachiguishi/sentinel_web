package wizard.skunk.substation.service;

import org.springframework.stereotype.Component;
import wizard.skunk.persistence.substation.domain.DataItem;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.DeviceCategory;

import java.util.List;

@Component
public class CategoryIdentifier implements ICategoryIdentifier {
	private static final List<DescriptionRule> DESCRIPTION_RULES = List.of(
			new DescriptionRule(DeviceCategory.Supervisory, "管理单元"),
			new DescriptionRule(DeviceCategory.TerminalBox, "端子箱"),
			new DescriptionRule(DeviceCategory.OperationBox, "操作箱"),
			new DescriptionRule(DeviceCategory.TravelingWave, "行波测距"),
			new DescriptionRule(DeviceCategory.VoltageParallel, "电压并列"),
			new DescriptionRule(DeviceCategory.DCSystemMonitor, "直流系统"),
			new DescriptionRule(DeviceCategory.PowerQualityMonitor, "电能质量"),
			new DescriptionRule(DeviceCategory.HardPressurePlateMonitor, "硬压板"));

	@Override
	public void deduceDeviceCategory(Device device) {
		String type = device.getType();
		if (type != null && "AGENTCSG".equalsIgnoreCase(type.strip())) {
			device.setCategory(DeviceCategory.Supervisory.value);
			return;
		}

		String description = device.getDescription();
		if (description != null) {
			for (DescriptionRule rule : DESCRIPTION_RULES) {
				if (description.contains(rule.keyword())) {
					device.setCategory(rule.category().value);
					return;
				}
			}
		}
	}

	private record DescriptionRule(DeviceCategory category, String keyword) {}

	@Override
	public void deduceFCDACategory(List<DataItem> dataItems) {

	}
}
