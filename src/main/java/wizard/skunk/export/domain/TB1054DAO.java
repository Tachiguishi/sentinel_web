package wizard.skunk.export.domain;

import wizard.skunk.persistence.substation.domain.ReportControl;
import java.util.List;
import java.util.Map;

public class TB1054DAO extends IDataTransfer<TB1054RCB>{
	public TB1054DAO(ModelData model, String stationCode) {
		super(model, stationCode);
	}

	@Override
	public void transferWithParent(Map<String, List<TB1054RCB>> result, String prefix) {
		this.prefix = prefix;
		for (ReportControl report : model.reports) {
			TB1054RCB rcb = new TB1054RCB();
			rcb.setCode(getCode(TB1054RCB.Prefix, report.getId()));
			rcb.setSubstationCode(stationCode);
			rcb.setDevice(getCode(TB1046IED.Prefix, report.getDevice().getId()));
			rcb.setDeviceName(report.getDevice().getName());
			rcb.setReference(report.getReference());
			rcb.setDsName(report.getDataSet().getName());
			rcb.setDsDesc(report.getDataSet().getDescription());
			rcb.setBuffered(report.isBuffered() ? 1 : 0);
			addTarget2Parent(result, rcb.getDevice(), rcb);
		}
	}
}
