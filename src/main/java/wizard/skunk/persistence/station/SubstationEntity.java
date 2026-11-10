package wizard.skunk.persistence.station;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "\"TB1041_SUBSTATION\"")
public class SubstationEntity {
	@Id
	@Column(name = "F1041_CODE")
	private String code;

	@Column(name = "F1041_DESC")
	private String description;

	// Some JDBC drivers retain trailing padding from the directory's CHAR columns.
	public String getCode() {
		return code.stripTrailing();
	}

	public String getDescription() {
		return description == null ? null : description.stripTrailing();
	}
}
