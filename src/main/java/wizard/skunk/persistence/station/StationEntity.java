package wizard.skunk.persistence.station;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sentinel_station")
@Getter
public class StationEntity {
	@Id
	@Column(name = "name", length = 64)
	private String name;

	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "ip_address", length = 15)
	private String ipAddress;

	@Column(name = "instant_no")
	private Integer instantNo;

	@Column(name = "create_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createAt;
}
