package wizard.skunk.parser.domain;

import lombok.Data;

@Data
public class BrokenAlarm {
	private String reference;
	private String sendDevice;
	private String sendControlBlock;
	private String appID;
}
