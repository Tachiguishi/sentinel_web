package wizard.skunk.export.domain;

import wizard.skunk.persistence.substation.domain.*;
import java.util.List;
import java.util.Map;

public class TB1058DAO extends IDataTransfer<TB1058MMS>{
	public TB1058DAO(ModelData model, String stationCode) {
		super(model, stationCode);
	}

	@Override
	public void transferWithParent(Map<String, List<TB1058MMS>> result, String prefix) {
		this.prefix = prefix;
		for (ReportControl report : model.reports) {
			for (DataItem item : model.items(report.getDataSet())) {
				FCDA fcda = item.getFcda();
				int type = ModelData.dataType(fcda);
				TB1058MMS mms = new TB1058MMS();
				// DataItem identifies membership; the same FCDA may occur in multiple reports.
				mms.setCode(getCode(TB1058MMS.Prefix, item.getId()) + "r" + report.getId());
				mms.setSubstationCode(stationCode);
				mms.setDevice(getCode(TB1046IED.Prefix, report.getDevice().getId()));
				mms.setRcb(getCode(TB1054RCB.Prefix, report.getId()));
				mms.setReference(fcda.getDollarReference());
				mms.setDaRef(fcda.getDollarReference());
				mms.setIndex(item.getFcdaIndex());
				mms.setDesc(fcda.getDescription());
				mms.setDataType(type);
				mms.setCdc(fcda.getCdc());
				mms.setValue(switch (type) {
					case 1 -> getCode(TB1016Status.Prefix, fcda.getId());
					case 2 -> getCode(TB1006Analog.Prefix, fcda.getId());
					case 3 -> getCode(TB1026String.Prefix, fcda.getId());
					default -> "";
				});
				addTarget2Parent(result, mms.getDevice(), mms);
			}
		}
	}
}
