package wizard.skunk.persistence.substation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Board {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String slot;
	private String alias;
	private String description;
	private String manufacturer;
	private String type;

	@ManyToOne
	private Device device;
}
