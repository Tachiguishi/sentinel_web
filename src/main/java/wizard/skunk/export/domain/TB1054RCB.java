package wizard.skunk.export.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TB1054RCB extends BaseEntity {
	public static final String Prefix = "RCB";

	private String code;
	private String substationCode;
	private String device;
	private String deviceName;
	private String reference;
	private String dsName;
	private String dsDesc;
	private int buffered;
	private int type;

	@Override
	public String getKey() {
		return code;
	}
}
