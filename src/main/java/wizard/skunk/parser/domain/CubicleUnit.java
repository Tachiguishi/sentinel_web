package wizard.skunk.parser.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CubicleUnit {
	private String name;
	private String device;
	private String description;
	private String manufacturer;
	private String type;
	private String className;

	private final List<Board> boardList = new ArrayList<>();

	public void addBoard(List<Board> boards){
		boardList.addAll(boards);
	}
}
