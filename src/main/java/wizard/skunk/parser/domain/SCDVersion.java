package wizard.skunk.parser.domain;

import lombok.Data;

@Data
public class SCDVersion {
	private String name;
	private String description;

	private String identify;
	private String version;
	private String revision;
	private String who;
	private String what;
	private String why;
	private String when;

	private String scdCRC = "";
	private String terminalCRC;
}
