package wizard.skunk.export.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TB1058MMS extends BaseEntity {
	public static final String Prefix = "FCDA";

	// F1058_CODE,F1046_CODE,F1054_CODE,F1058_REFADDR,F1058_DAREFADDR,F1058_INDEX," +
	//				"F1058_DESC,F1058_DATATYPE,DATA_CODE
	private String code;
	private String substationCode;
	private String device;
	private String rcb;
	private String reference;
	private String daRef;
	private int index;
	private String desc;
	private int dataType;
	private String cdc;
	private String value;

	@Override
	public String getKey() {
		return code;
	}
}
