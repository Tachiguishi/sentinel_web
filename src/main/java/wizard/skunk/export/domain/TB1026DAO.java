package wizard.skunk.export.domain;

import wizard.skunk.persistence.substation.domain.FCDA;
import java.util.List;
import java.util.Map;

public class TB1026DAO extends IDataTransfer<TB1026String> {
	public TB1026DAO(ModelData model, String stationCode) {
		super(model, stationCode);
	}

	@Override
	public void transferWithParent(Map<String, List<TB1026String>> result, String prefix) {
		this.prefix = prefix;
		for (FCDA fcda : model.reportPoints(3)) {
			String parent = getCode(TB1046IED.Prefix, fcda.getDevice().getId());
			TB1026String point = new TB1026String();
			point.setCode(getCode(TB1026String.Prefix, fcda.getId()));
			point.setDesc(fcda.getDescription());
			point.setReference(fcda.getDollarReference());
			point.setParent(parent);
			point.setSubstation(stationCode);
			addTarget2Parent(result, parent, point);
		}
	}
}
