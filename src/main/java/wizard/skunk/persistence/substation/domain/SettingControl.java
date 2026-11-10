package wizard.skunk.persistence.substation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class SettingControl {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String accessPoint;
	private String ldName;

	private String name;
	private int numberOfSettingGroup;

	@ManyToOne
	private Device device;
}
