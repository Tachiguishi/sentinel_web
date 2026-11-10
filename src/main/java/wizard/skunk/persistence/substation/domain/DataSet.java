package wizard.skunk.persistence.substation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class DataSet {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String description;
	private String identity;

	private String accessPoint;
	private String ldName;
	private String ldDescription;

	private String lnPrefix = "";
	private String lnClass = "";
	private String instanceNO = "";

	private int category;

	public String getReference(){
		return ldName + "/" + lnPrefix + lnClass + instanceNO + "." + name;
	}

	@ManyToOne
	private Device device;

	@Override
	public String toString(){
		return description + "(" + name + ")";
	}
}
