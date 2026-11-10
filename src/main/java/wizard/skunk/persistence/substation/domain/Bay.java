package wizard.skunk.persistence.substation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Bay {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String description;

	@ManyToOne
	private VoltageLevel voltageLevel;

	@Override
	public String toString() {
		return description + "(" + name + ")";
	}
}
