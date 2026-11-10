package wizard.skunk.parser.domain;

import lombok.Data;

@Data
public class PlateSwitcher {
	private String deviceName;
	private String cabinetID;
	private String rowNum;
	private String colNum;
	private String reference;
}
