package wizard.skunk.persistence.substation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Port {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String no;
	private String alias;
	private String description;
	private String formatedName;
	private String icdPort;
	private int direction; // 1 = input, 2 = output, 3 = bidirectional

	private String lightReference;

	@ManyToOne
	private Board board;

	@ManyToOne
	private Device device;

	public void setBoard(Board board){
		this.board = board;
		if(board != null && board.getDevice() != null){
			this.device = board.getDevice();
		}
	}

	public String getIdentity(){
		return no + "-" + getDirectionString();
	}

	public String getDirectionString(){
		for(Direction pd : Direction.values()){
			if(pd.value == direction){
				return pd.name;
			}
		}
		return "";
	}

	public enum Direction {
		UNKNOWN(0, "UNKNOWN"),
		RECEIVE(1, "RX"),
		SEND(2, "TX"),
		BOTH(3, "RT");

		public final int value;
		public final String name;

		Direction(int value, String name) {
			this.value = value;
			this.name = name;
		}

		public static Direction fromValue(int value) {
			for (Direction direction : Direction.values()) {
				if (direction.value == value) {
					return direction;
				}
			}
			return UNKNOWN;
		}

		public static Direction fromName(String name) {
			for (Direction direction : Direction.values()) {
				if (direction.name.equalsIgnoreCase(name)) {
					return direction;
				}
			}
			return UNKNOWN;
		}
	}

	@Override
	public String toString() {
		return "Port{" +
				"id=" + id +
				", no='" + no + '\'' +
				", alias='" + alias + '\'' +
				", description='" + description + '\'' +
				", formatedName='" + formatedName + '\'' +
				", icdPort='" + icdPort + '\'' +
				", direction=" + direction +
				", lightReference='" + lightReference + '\'' +
				", board=" + (board != null ? board.getSlot() : "null") +
				", device=" + (device != null ? device.getName() : "null") +
				'}';
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Port other = (Port) obj;
		if(this.id == null || other.id == null) {
			return false;
		}
		return this.id.equals(other.id);
	}
}
