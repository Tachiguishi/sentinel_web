package wizard.skunk.persistence.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class SCLFile {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String fileName;
	private String identifier;
	private int category;

	private byte[] content;

	public enum Category{
		Placeholder,

		SCD,
		CID,
		IMCD,
		IVLD
	}
}
