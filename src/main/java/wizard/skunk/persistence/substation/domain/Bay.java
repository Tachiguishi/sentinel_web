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

	// MySQL TB1042_BAY.F1042_CODE; name remains the human-readable bay name.
	private String code;
	private String name;
	private String description;

	@ManyToOne
	private VoltageLevel voltageLevel;

	@Override
	public String toString() {
		return description + "(" + name + ")";
	}
}
