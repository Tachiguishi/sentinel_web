package wizard.skunk.parser.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Board {
	private String slot;
	private String alias;
	private String description;
	private String type;
	private String manufacturer;
	private String templateName;

	private final List<Port> portList = new ArrayList<>();

	public void addPort(Port port){
		portList.add(port);
	}

	public void setIcdPort(String portName, String icdPortName) {
		for (Port port : portList) {
			if (port.getNo().equals(portName)) {
				port.setIcdPort(icdPortName);
			}
		}
	}

	public void setPortLightReference(String formatedName, String lightReference) {
		for (Port port : portList) {
			if (port.getFormatedName().equals(formatedName)) {
				port.setLightReference(lightReference);
				return;
			}
		}
	}
}
