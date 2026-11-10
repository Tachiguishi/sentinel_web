package wizard.skunk.parser.scl;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class LNodeType {
	private final String id;
	private final String description;
	private final String lnClass;

	@Setter(AccessLevel.NONE)
	private Map<String, Doi> doTypeIndexMap;

	public LNodeType(String id, String lnClass, String desc) {
		this.id = id;
		this.description = desc;
		this.lnClass = lnClass;
		this.doTypeIndexMap = new HashMap<>();
	}

	public void addDoTypeIndex(String doName, String doTypeIndex, String description) {
		doTypeIndexMap.put(doName, new Doi(doTypeIndex, description));
	}

	// Map<doName, doTypeIndex>
	public Map<String, String> getAllDOTypeIndex(){
		Map<String, String> result = new HashMap<>();
		for(Map.Entry<String, Doi> entry : doTypeIndexMap.entrySet()) {
			result.put(entry.getKey(), entry.getValue().doTypeIndex);
		}

		return result;
	}

	public String getDOTypeIndex(String name) {
		String doName = name.split("\\.")[0];
		if(doTypeIndexMap.containsKey(doName)){
			return doTypeIndexMap.get(doName).doTypeIndex;
		}
		return "";
	}

	public String getDODescription(String name) {
		Doi doi = doTypeIndexMap.get(name);
		if(doi == null) {
			return null;
		}

		return doi.description;
	}

	private record Doi(String doTypeIndex, String description) {
	}
}
