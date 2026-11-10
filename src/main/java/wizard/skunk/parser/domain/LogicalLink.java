package wizard.skunk.parser.domain;

import lombok.Data;

@Data
public class LogicalLink {
	private String senderIedName;
	private String senderReference;

	private String receiverIedName;
	private String receiverReference;

	private String receiverBoarPort;

	@Override
	public String toString(){
		return senderIedName + "." + senderReference + " -> " + receiverIedName + "." + receiverReference;
	}
}
