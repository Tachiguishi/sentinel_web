package wizard.skunk.persistence.substation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class LinkBreakAlarm {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	private Device receiver;
	@ManyToOne
	private FCDA alarmFCDA; // Receiver link break 102

	@ManyToOne
	private GooseSvControl senderControl; // Sender GOOSE/SV Control Block

	@Override
	public String toString() {
		return alarmFCDA.toString();
	}
}
