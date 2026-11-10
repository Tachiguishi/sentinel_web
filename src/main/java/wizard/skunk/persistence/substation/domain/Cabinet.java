package wizard.skunk.persistence.substation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Cabinet {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String description;
	private String serialNumber;

	@ManyToOne
	private District district;

	@Override
	public String toString(){
		return description + "(" + name + ")";
	}

	public String getReference(){
		return district.getName() + "." + name;
	}
}
