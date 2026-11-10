package wizard.skunk.persistence.substation.domain;

public enum DataSetCategory {
	PlaceHolder(0),
	Parameter(1),
	Setting(2),
	ReportControl(3),
	SVControl(4),
	GOOSEControl(5);

	public final int value;

	DataSetCategory(int value){
		this.value = value;
	}
}
