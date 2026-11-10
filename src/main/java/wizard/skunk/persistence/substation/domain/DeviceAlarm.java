package wizard.skunk.persistence.substation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
public class DeviceAlarm {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String reference;
	private String description;
	private String severity;
	private String effect;
	private String suggestion;

	private String boards;  // separated list of board slots by ,

	@ManyToOne
	private Device device;

	public void addBoardSlot(String boardSlot) {
		if (boards == null || boards.isEmpty()) {
			boards = "," + boardSlot + ",";
		}
		else {
			boards += boardSlot + ",";
		}
	}

	public List<String> getBoardSlots() {
		if (boards == null || boards.isEmpty()) {
			return List.of();
		}
		if (boards.startsWith(",")) {
			boards = boards.substring(1);  // remove leading comma
		}
		if (boards.endsWith(",")) {
			boards = boards.substring(0, boards.length() - 1);  // remove trailing comma
		}
		return List.of(boards.split(","));
	}
}
