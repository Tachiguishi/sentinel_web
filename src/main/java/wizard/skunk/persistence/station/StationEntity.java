package wizard.skunk.persistence.station;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Table(name = "sentinel_station")
@Getter
public class StationEntity implements Persistable<String> {
	@Id
	@Column(name = "name", length = 64)
	private String name;

	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "ip_address", length = 15)
	private String ipAddress;

	@Column(name = "instant_no", insertable = false)
	private Integer instantNo;

	@Column(name = "create_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createAt;

	@Column(name = "scd_updated_at", insertable = false, updatable = false)
	private LocalDateTime scdUpdatedAt;

	@Transient
	private boolean newStation = true;

	protected StationEntity() {}

	public StationEntity(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public void updateSettings(String ipAddress, Integer instantNo) {
		this.ipAddress = ipAddress;
		this.instantNo = instantNo;
	}

	@Override
	public String getId() {
		return name;
	}

	@Override
	public boolean isNew() {
		return newStation;
	}

	@PostLoad
	@PostPersist
	void markPersisted() {
		newStation = false;
	}
}
