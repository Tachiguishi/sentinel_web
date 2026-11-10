package wizard.skunk.persistence.substation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class NetworkCable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String no;
	private String deviceA;
	private String boardA;
	private String portA;
	private String deviceB;
	private String boardB;
	private String portB;

	@OneToOne
	private Port portAEntity;
	@OneToOne
	private Port portBEntity;

	@ManyToOne
	private Device deviceAEntity;
	@ManyToOne
	private Device deviceBEntity;

	public String getPortAReference(){
		return String.join(".", deviceAEntity.getCabinetUnit(), boardA == null || boardA.isEmpty() ? "1" : boardA, portAEntity.getIdentity());
	}

	public String getPortBReference(){
		return String.join(".", deviceBEntity.getCabinetUnit(), boardB == null || boardB.isEmpty() ? "1" : boardB, portBEntity.getIdentity());
	}
}
