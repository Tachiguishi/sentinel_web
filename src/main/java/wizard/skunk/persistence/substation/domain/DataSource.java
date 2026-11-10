package wizard.skunk.persistence.substation.domain;

public enum DataSource {
	SCD(1, "SCD"),
	IMCD(2, "IMCD"),
	IVLD(3, "IVLD"),
	SPCD(4, "SPCD"),
	SSD(5, "SSD"),
	UI(6, "UI"),
	EXCEL(7, "Excel"),
	CID(8, "CID");

	public final int index;
	public final String value;

	DataSource(int index, String value) {
		this.index = index;
		this.value = value;
	}
}
