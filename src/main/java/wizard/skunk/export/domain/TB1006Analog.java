package wizard.skunk.export.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TB1006Analog extends BaseEntity {
	public static final String Prefix = "Analog";

	private String code;
	private String desc;
	private String parent;
	private String substation;
	private String reference;
	private int category;
	private int invalid;
	private int computed;
	private double coefficient;
	private float baseline;
	private float upperThreshold;
	private float lowerThreshold;
	private int saveInterval;
	private int alarmLevel;
	private String unit;

	@Override
	public String getKey() {
		return code;
	}
}
