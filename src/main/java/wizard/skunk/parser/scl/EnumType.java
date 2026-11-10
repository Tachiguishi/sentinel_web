package wizard.skunk.parser.scl;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class EnumType {
	private String id;
	private Map<String, String> enumValues;

	public EnumType(String id) {
		this.id = id;
		this.enumValues = new HashMap<>();
	}

	public void addEnumValue(String ordinal, String text) {
		enumValues.put(ordinal, text);
	}

	public String getEnumValue(String ordinal) {
		return enumValues.get(ordinal);
	}
}
