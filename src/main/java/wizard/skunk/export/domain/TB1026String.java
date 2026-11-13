package wizard.skunk.export.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TB1026String extends BaseEntity {
	public static final String Prefix = "String";

	// F1026_CODE,F1026_DESC,PARENT_CODE,F1011_NO,F1026_CALCFLAG
	private String code;
	private String desc;
	private String parent;
	private String substation;
	private int category;
	private int computed;
	private String reference;

	@Override
	public String getKey() {
		return code;
	}
}
