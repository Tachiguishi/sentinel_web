package wizard.skunk.parser.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class IMCDUnit extends IACDUnit{
	private final List<Board> boardList = new ArrayList<>();

	public void addBoard(List<Board> boards){
		boardList.addAll(boards);
	}
}
