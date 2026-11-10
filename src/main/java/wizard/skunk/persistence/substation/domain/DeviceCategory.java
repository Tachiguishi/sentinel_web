package wizard.skunk.persistence.substation.domain;

public enum DeviceCategory {
	GeneralDevice(0, "未知"),
	Supervisory(0x40, "管理单元"), // 管理单元

	TravelingWave(0x10000, "行波测距"),
	TerminalBox(0x20000, "端子箱"),
	OperationBox(0x30000, "操作箱"),
	VoltageParallel(0x40000, "电压并列"),
	DCSystemMonitor(0x50000, "直流系统监测"),
	PowerQualityMonitor(0x60000, "电能质量监测"),
	HardPressurePlateMonitor(0x70000, "硬压板监测");

	public final int value;
	public final String description;

	DeviceCategory(int value, String description) {
		this.value = value;
		this.description = description;
	}

	public static DeviceCategory fromValue(int value) {
		for (DeviceCategory category : values()) {
			if (category.value == value) {
				return category;
			}
		}
		return null;
	}

	public static String labelOf(int value) {
		DeviceCategory category = fromValue(value);
		return category == null ? GeneralDevice.description : category.description;
	}
}
