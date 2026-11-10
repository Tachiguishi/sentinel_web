package wizard.skunk.parser.domain;

import lombok.Data;

@Data
public class OpticalFiber {
	private String name;
	private String no = "";
	private String iedA;
	private String boardA;
	private String portA;
	private String iedB;
	private String boardB;
	private String portB;
	private String usage;

	@Override
	public String toString() {
		return "OpticalFiber{" +
				"name='" + name + '\'' +
				", no='" + no + '\'' +
				", iedA='" + iedA + '\'' +
				", boardA='" + boardA + '\'' +
				", portA='" + portA + '\'' +
				", iedB='" + iedB + '\'' +
				", boardB='" + boardB + '\'' +
				", portB='" + portB + '\'' +
				", usage='" + usage + '\'' +
				'}';
	}
}
