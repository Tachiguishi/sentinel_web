package wizard.skunk.export.domain;

import wizard.skunk.persistence.substation.domain.FCDA;
import java.util.List;
import java.util.Map;

public class TB1016DAO extends IDataTransfer<TB1016Status> {
	public TB1016DAO(ModelData model, String stationCode) {
		super(model, stationCode);
	}

	@Override
	public void transferWithParent(Map<String, List<TB1016Status>> result, String prefix) {
		this.prefix = prefix;
		for (FCDA fcda : model.reportPoints(1)) {
			String parent = getCode(TB1046IED.Prefix, fcda.getDevice().getId());
			TB1016Status point = new TB1016Status();
			point.setCode(getCode(TB1016Status.Prefix, fcda.getId()));
			point.setDesc(fcda.getDescription());
			point.setReference(fcda.getDollarReference());
			point.setParent(parent);
			point.setSubstation(stationCode);
			addTarget2Parent(result, parent, point);
		}
	}
}
