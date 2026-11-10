package wizard.skunk.persistence.substation.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class VoltageLevel {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private int value;
	private String multiplier = "";
	private String unit = "V";

	public int getCompilableValue(){
		if(multiplier == null || multiplier.isBlank()){
			return -value;
		}
		else{
			return value;
		}
	}

	@Override
	public String toString() {
		return value + multiplier + unit;
	}
}
