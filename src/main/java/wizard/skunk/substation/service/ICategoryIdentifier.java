package wizard.skunk.substation.service;

import wizard.skunk.persistence.substation.domain.DataItem;
import wizard.skunk.persistence.substation.domain.Device;

import java.util.List;

public interface ICategoryIdentifier {
	void deduceDeviceCategory(Device device);
	void deduceFCDACategory(List<DataItem> dataItems);
}
