package wizard.skunk.persistence.substation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class ReportControl {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String rptID;
	private boolean buffered;
	private int rptEnabled;

	private String reference;

	@ManyToOne
	private DataSet dataSet;

	@ManyToOne
	private Device device;
}
