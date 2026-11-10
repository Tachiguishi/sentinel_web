package wizard.skunk.persistence.substation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class FCDA {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String ldInst;
	private String prefix = "";
	private String lnClass;
	private String lnInst = "";
	private String doName;
	private String daName;
	private String fc;
	private String dotReference;    // LD/LN.DO.DA
	private String dollarReference; // LD/LN$FC$DO$DA

	private String description;
	private String cdc;
	private String lnType;
	private String unitSerialization;

	@ManyToOne
	private Device device;

	@Override
	public String toString(){
		return description + "(" + dotReference + ")";
	}

	public String getFullUnit(){
		return getMultiplier() + getUnit();
	}

	public String getUnit(){
		String result = firstPhase(unitSerialization, Split.Comma.toString());
		if(result == null || result.isBlank()){
			if(doName == null){
				return "";
			}
			if(doName.contains("Tmh")){
				return "h";
			}
			else if(doName.contains("Tmms")){
				return "ms";
			}
			else if(doName.contains("Tmm")){
				return "min";
			}
			else if(doName.contains("Tms")){
				return "s";
			}
			else{
				return "";
			}
		}

		return result;
	}

	public String getMultiplier(){
		if(unitSerialization == null || unitSerialization.isEmpty()) return "";
		return unitSerialization.split(Split.Comma.toString(), -1)[4];
	}

	public float getMinValue(){
		if(unitSerialization == null || unitSerialization.isEmpty()) return 0;
		String minValue = unitSerialization.split(Split.Comma.toString(), -1)[1];
		try{
			return Float.parseFloat(minValue);
		}
		catch (Exception e){
			return 0;
		}
	}

	public float getMaxValue(){
		if(unitSerialization == null || unitSerialization.isEmpty()) return 0;
		String maxValue = unitSerialization.split(Split.Comma.toString(), -1)[2];
		try{
			return Float.parseFloat(maxValue);
		}
		catch (Exception e){
			return 0;
		}
	}

	public float getStepSize(){
		if(unitSerialization == null || unitSerialization.isEmpty()) return 0;
		String stepSize = unitSerialization.split(Split.Comma.toString(), -1)[3];
		try{
			return Float.parseFloat(stepSize);
		}
		catch (Exception e){
			return 0;
		}
	}

	public String firstPhase(String str, String delimiter) {
		int index = str.indexOf(delimiter);
		if (index == -1) {
			return str;
		}
		return str.substring(0, index);
	}

	private enum Split {
		Comma(",");

		private final String value;

		Split(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;

		FCDA fcda = (FCDA) obj;

		if(fcda.id == null || this.id == null) return false;

		return fcda.id.equals(this.id);
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}
}
