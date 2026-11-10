package wizard.skunk.persistence.substation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Device {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String description;
	private String manufacturer;
	private String type;
	private String version;

	private int category;
	private Boolean categoryOverridden;

	@Column(length = 16)
	private String ipAddressA;
	@Column(length = 16)
	private String ipAddressB;
	private String apTitle;
	private int aeQualifier;
	private int optionField;
	private int triggerOption;
	private String waveDirectory;
	private String wavePrefix;

	private String crc;
	private String manualCRC;

	private String physicalID;
	private String serialNumber;

	private boolean ignored = false;
	private int source;
	private int modify;

	@ManyToOne
	private Bay bay;

	@ManyToOne
	private Cabinet cabinet;
	private String cabinetUnit;

	public void overrideCategory(DeviceCategory category) {
		this.category = category.value;
		this.categoryOverridden = true;
	}

	@Override
	public String toString() {
		return description + "(" + name + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Device device = (Device) obj;
		return id != null && id.equals(device.id);
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}

	public String getExportCRC(){
		if(manualCRC == null || manualCRC.isBlank()){
			return crc;
		}
		else {
			return manualCRC;
		}
	}
}
