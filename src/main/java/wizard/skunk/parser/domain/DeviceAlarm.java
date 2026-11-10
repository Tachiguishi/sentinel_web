package wizard.skunk.parser.domain;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class DeviceAlarm {
	private String reference;
	private String description;
	private String level;
	private String effect;
	private String suggestion;

	private Set<String> boardList = new HashSet<>();

	public void addBoard(String board){
		boardList.add(board);
	}
}
