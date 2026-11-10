package wizard.skunk.parser.scl;

import lombok.Getter;
import lombok.Setter;
import wizard.skunk.common.StringUtil;
import wizard.skunk.parser.domain.Split;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DaType extends BaseDataType{
	private BasicType basicType = BasicType.Struct;
	@Getter
	@Setter
	private String enumID = "";

	Map<String, DaType> children = new HashMap<>();

	public void setBasicType(String basicType) {
		this.basicType = BasicType.valueOf(basicType);
	}

	public DaType getChild(String name){
		return children.get(name);
	}

	public void addChild(String name, DaType child) {
		children.put(name, child);
	}

	@Override
	public String getDaReference(String FC){
		for(Map.Entry<String, DaType> entry : children.entrySet()){
			if(entry.getValue().isStruct()){
				return entry.getKey() + "$" + entry.getValue().getDaReference(FC);
			}

			return entry.getKey();
		}

		return "";
	}


	@Override
	protected String typeString(){
		return "\"type\":\"" + basicType.toString() + "\"";
	}

	@Override
	protected boolean isStruct() {
		return basicType == BasicType.Struct;
	}

	@Override
	public String serialize(String targetFC, String reference){
		if(basicType == BasicType.Struct){
			List<String> childrenString = new ArrayList<>(children.size());
			String[] typeSplit = StringUtil.splitOnce(reference, Split.Dot.toString());

			for(Map.Entry<String, DaType> entry : children.entrySet()){
				if(!typeSplit[0].isEmpty() && !entry.getKey().equals(typeSplit[0])){
					continue;
				}
				String name = "\"name\":\"" + entry.getKey() + "\"";
				String type = entry.getValue().typeString();
				if(entry.getValue().basicType == BasicType.Struct){
					String children = "\"children\": " + entry.getValue().serialize(targetFC, typeSplit[1]);
					childrenString.add("{" + name + "," + type + "," + children + "}");
				}
				else{
					childrenString.add("{" + name + ", " + type + "}");
				}
			}

			return "[" + String.join(",", childrenString) + "]";
		}
		else{
			return basicType.toString();
		}
	}

	@Override
	public String toString(){
		return serialize("", "");
	}

	public enum BasicType{
		BOOLEAN,
		INT8,
		INT16,
		INT24,
		INT32,
		INT64,
		INT128,
		INT8U,
		INT16U,
		INT24U,
		INT32U,
		FLOAT32,
		FLOAT64,
		Enum,
		Dbpos,
		Tcmd,
		Quality,
		Timestamp,
		VisString32,
		VisString64,
		VisString65,
		VisString129,
		VisString255,
		Octet64,
		Struct,
		EntryTime,
		Unicode255,
		Check,
		ObjRef,
		Currency,
		PhyComAddr,
		TrgOps,
		OptFlds,
		SvOptFlds,
		LogOptFlds,
		EntryID,
		Octet6,
		Octet16
	}
}
