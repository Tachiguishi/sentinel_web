package wizard.skunk.export.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TB1016Status extends BaseEntity {
	public static final String Prefix = "State";

	private String code;
	private String desc;
	private String reference;
	private String parent;
	private String substation;
	private int category;
	private int invalid;
	private int computed;

	@Override
	public String getKey() {
		return code;
	}
}
