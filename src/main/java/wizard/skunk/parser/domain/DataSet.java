package wizard.skunk.parser.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DataSet {
	private String name;
	private String description;

	private String accessPoint;
	private String ldName;
	private String ldDescription;

	private String lnPrefix = "";
	private String lnClass = "";
	private String instanceNO = "";

	private List<String> fcdaList = new ArrayList<>();

	public String getIdentity(){
		return accessPoint + "." + ldName + "." + name;
	}

	public String getLnName() {
		return lnPrefix + lnClass + instanceNO;
	}

	public String getLdNameAndLnName() {
		return ldName + Split.Slash + getLnName();
	}

	// LDName/LnName$DataSetName
	public String getReference() {
		return getLdNameAndLnName() + Split.Dollar + name;
	}

	public void addFCDA(FCDA fcda) {
		fcdaList.add(fcda.getDotReference());
	}

	@Override
	public String toString(){
		return description + "(" + getReference() + ")";
	}
}
