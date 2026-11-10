package wizard.skunk.parser.domain;

import lombok.Data;

@Data
public class ReportControl {
	private String name;
	private String rptID;
	private boolean buffered;
	private int rptEnabled;
	private String reference;

	private String dataSet;
}
