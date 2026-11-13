package wizard.skunk.export.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class OptionTable {
	private String key;
	private String tableName;
	private boolean valid;

	private List<OptionColumn> columns = new ArrayList<>();

	public void addColumn(OptionColumn column){
		columns.add(column);
	}

	public String getKeyColumn(){
		for(OptionColumn column : columns){
			if(column.getKey().equals("code")){
				return column.getColumnName();
			}
		}
		return "";
	}
}
