package wizard.skunk.export.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TB1046IED extends BaseEntity{
	public static final String Prefix = "IED";

	private String code;
	private String name;
	private String desc;
	private String manufacturer;
	private String model;
	private String version;

	private int aorB;
	private int virtual;
	private int category;
	private String uuid = "";
	private String serialNO = "";
	private String omsID = "";
	private String dcID = "";
	private String primaryEquip = "";

	private String substation;
	private String bay;
	private String bayName;
	private int voltage;
	private String cabinet;
	// Managed by which supervisory, only applied for Branch and Sentinel
	private String supervisory;

	@Override
	public String getKey() {
		return code;
	}
}
