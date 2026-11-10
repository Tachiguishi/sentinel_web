package wizard.skunk.persistence.substation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class GooseSvControl {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	// appID or smvID
	private String identify;
	private String reference;

	private boolean goose;  // true GOOSE, false: SampledValue

	private String macAddress;
	private String vlanId;
	private String vlanPriority;
	private String appId;

	@ManyToOne
	private DataSet dataSet;

	@ManyToOne
	private Device device;

	@Override
	public String toString(){
		return device.getName() + reference + "(" + appId + ")";
	}

	public String getDotReference(){
		String result = reference;
		if(result != null && !result.isEmpty()){
			result = result.replace("$", ".");
		}
		return result;
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		GooseSvControl gooseSvControl = (GooseSvControl) obj;
		return id.equals(gooseSvControl.id);
	}
}
