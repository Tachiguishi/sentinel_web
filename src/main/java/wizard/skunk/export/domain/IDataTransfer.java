package wizard.skunk.export.domain;

import lombok.Setter;
import org.w3c.dom.Element;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class IDataTransfer<Target> {
	protected final ModelData model;
	protected final String stationCode;
	protected String prefix = "";

	protected IDataTransfer(ModelData model, String stationCode) {
		this.model = model;
		this.stationCode = stationCode;
	}

	protected String getCode(String table, Long id) {
		return prefix + table + String.format(java.util.Locale.ROOT, "%010d", id);
	}

	protected void addTarget2Parent(Map<String, List<Target>> result, String parent, Target target) {
		result.computeIfAbsent(parent, ignored -> new ArrayList<>()).add(target);
	}
	@Setter
	protected OptionTable optionTable;

	public List<Target> transfer(String prefix){ return new ArrayList<>();}
	public void transferWithParent(Map<String, List<Target>> result, String prefix){}

	public void setElementAttribute(Target target, Element row){
		try{
			for(OptionColumn column : optionTable.getColumns()){
				String fieldName = column.getKey();
				Field field = target.getClass().getDeclaredField(fieldName);
				field.setAccessible(true);
				Object value = field.get(target);
				if(value != null){
					row.setAttribute(fieldName, value.toString());
				}
				else{
					row.setAttribute(fieldName, "");
				}
			}
		}
		catch (Exception e){
			throw new RuntimeException(e.getMessage());
		}
	}
}
