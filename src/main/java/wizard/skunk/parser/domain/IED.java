package wizard.skunk.parser.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class IED {
	private String name;
	private String type;
	private String manufacturer;
	private String version;
	private String description;

	private String crc;
	private String ipAddressA;
	private String ipAddressB;
	private String apTitle;
	private int aeQualifier;

	// <DataSetReference, DataSet>
	private List<DataSet> dataSets = new ArrayList<>();
	// <FCDAReference, FCDA>
	private Map<String, FCDA> fcdaMap = new HashMap<>();
	private List<ReportControl> reportControls = new ArrayList<>();
	private List<GooseSvControl> gooseSvControls = new ArrayList<>();
	private List<LogicalLink> logicalLinks = new ArrayList<>();
	private List<CDCMap> cdcMaps = new ArrayList<>();

	public void addDataSet(DataSet dataSet){
		dataSets.add(dataSet);
	}

	public boolean addFCDA(FCDA fcda, DataSet dataSet){
		boolean duplicated = true;
		if(!fcdaMap.containsKey(fcda.getDollarReference())){
			fcdaMap.put(fcda.getDollarReference(), fcda);
			duplicated = false;
		}

		FCDA fcdaCached = fcdaMap.get(fcda.getDollarReference());
		dataSet.addFCDA(fcdaCached);

		return duplicated;
	}

	public void addCDCMapList(List<CDCMap> cdcMapList) {
		cdcMaps.addAll(cdcMapList);
	}

	public void addReportControl(ReportControl reportControl){
		reportControls.add(reportControl);
	}

	public void addGooseSvControl(GooseSvControl gooseSvControl){
		gooseSvControls.add(gooseSvControl);
	}

	public void addLogicalLink(LogicalLink logicalLink){
		logicalLinks.add(logicalLink);
	}

	@Override
	public String toString() {
		return description + "(" + name + ")";
	}
}
