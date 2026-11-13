package wizard.skunk.export.domain;

import wizard.skunk.persistence.substation.domain.DataItem;
import wizard.skunk.persistence.substation.domain.DataSetCategory;
import wizard.skunk.persistence.substation.domain.FCDA;
import java.util.*;

public class TB1036DAO extends IDataTransfer<TB1036Setting>{
	public TB1036DAO(ModelData model, String stationCode) {
		super(model, stationCode);
	}

	@Override
	public void transferWithParent(Map<String, List<TB1036Setting>> result, String prefix) {
		this.prefix = prefix;
		Set<Long> seen = new HashSet<>();
		for (DataItem item : model.items) {
			FCDA fcda = item.getFcda();
			int category = item.getDataSet().getCategory();
			// Some imported snapshots only identify settings by functional constraint.
			if ("SG".equals(fcda.getFc()) || "SE".equals(fcda.getFc())) category = DataSetCategory.Setting.value;
			else if ("SP".equals(fcda.getFc())) category = DataSetCategory.Parameter.value;
			if (category != DataSetCategory.Parameter.value && category != DataSetCategory.Setting.value) continue;
			if (!seen.add(fcda.getId())) continue;
			TB1036Setting setting = new TB1036Setting();
			setting.setCode(getCode(TB1036Setting.Prefix, fcda.getId()));
			setting.setParent(getCode(TB1046IED.Prefix, fcda.getDevice().getId()));
			setting.setSubstation(stationCode);
			setting.setCategory(category);
			setting.setReference(fcda.getDollarReference());
			setting.setDataType(ModelData.dataType(fcda));
			setting.setDesc(fcda.getDescription());
			setting.setUnit(ModelData.unit(fcda));
			setting.setStepSize(fcda.getStepSize());
			setting.setMinValue(fcda.getMinValue());
			setting.setMaxValue(fcda.getMaxValue());
			addTarget2Parent(result, setting.getParent(), setting);
		}
	}
}
