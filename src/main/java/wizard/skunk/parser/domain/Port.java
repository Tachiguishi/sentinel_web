package wizard.skunk.parser.domain;

import lombok.Data;

@Data
public class Port {
	private String no;
	private String description;
	private String direction;
	private String plug;
	private String usage;
	private String formatedName;
	private String icdPort;

	private String lightReference;
}
