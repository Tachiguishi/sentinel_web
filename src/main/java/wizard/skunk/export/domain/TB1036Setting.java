package wizard.skunk.export.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TB1036Setting extends BaseEntity {
	public static final String Prefix = "Set";

	private String code;
	private String reference;
	private int category;   // 1 参数, 2 定值
	private int dataType;
	private String desc;
	private String unit;
	private float stepSize;
	private float minValue;
	private float maxValue;

	private String parent;
	private String substation;

	@Override
	public String getKey() {
		return code;
	}
}
