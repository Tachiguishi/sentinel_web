package wizard.skunk.persistence.substation.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Substation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	public String name;
	public String description;

	private String identify;
	private String version;
	private String revision;
	private String who;
	private String what;
	private String why;
	private String modifyTime;

	private String scdCRC;
	private String terminalCRC;

	@Override
	public String toString(){
		return description + "(" + name + ")";
	}
}
