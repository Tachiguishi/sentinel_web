package wizard.skunk.export.domain;

import wizard.skunk.persistence.substation.domain.FCDA;
import java.util.List;
import java.util.Map;

public class TB1006DAO extends IDataTransfer<TB1006Analog> {
	public TB1006DAO(ModelData model, String stationCode) {
		super(model, stationCode);
	}

	@Override
	public void transferWithParent(Map<String, List<TB1006Analog>> result, String prefix) {
		this.prefix = prefix;
		for (FCDA fcda : model.reportPoints(2)) {
			String parent = getCode(TB1046IED.Prefix, fcda.getDevice().getId());
			TB1006Analog point = new TB1006Analog();
			point.setCode(getCode(TB1006Analog.Prefix, fcda.getId()));
			point.setDesc(fcda.getDescription());
			point.setReference(fcda.getDollarReference());
			point.setParent(parent);
			point.setSubstation(stationCode);
			point.setCoefficient(1);
			point.setUnit(ModelData.unit(fcda));
			addTarget2Parent(result, parent, point);
		}
	}
}
