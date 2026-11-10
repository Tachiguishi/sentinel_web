package wizard.skunk.persistence.substation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class LogicalLink {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private int status; // 0: no change, 1: created, 2: changed, 3: deleted

	@ManyToOne
	private FCDA senderFcda;
	@ManyToOne
	private FCDA receiverFcda;

	@ManyToOne
	private Device receiverDevice;
	@ManyToOne
	private Device senderDevice;

	@ManyToOne
	private GooseSvControl senderControl;

	private String receiverBoarPort;

	public void setModifyStatus(ModifyStatus status){
		this.status = status.value;
	}
}
