package wizard.skunk.export.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OptionColumn {
	private String key;
	private String columnName;
	private int columnType; // 0: String, 1: Numeric, 2: Boolean
	private boolean valid;

	public void setType(String type){
		if("String".equals(type)){
			columnType = 0;
		}
		else if("Numeric".equals(type)){
			columnType = 1;
		}
		else if("Boolean".equals(type)){
			columnType = 2;
		}
	}

	public ColumnType getType(){
		return ColumnType.values()[columnType];
	}

	public enum ColumnType {
		STRING,
		NUMERIC,
		BOOLEAN;
	}
}
