package wizard.skunk.parser.domain;

import lombok.Data;

@Data
public class GooseSvControl {
	private String name;
	// appID or smvID;
	private String identify;
	private String reference;

	private boolean goose;  // true: GOOSE, false: SampledValue

	private String macAddress;
	private String vlanId;
	private String vlanPriority;
	private String appId;

	private String dataSet;
}
