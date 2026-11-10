package wizard.skunk.parser.scl;

import lombok.Getter;
import org.w3c.dom.Element;
import wizard.skunk.common.IProgressMonitor;
import wizard.skunk.parser.domain.DeviceAlarm;
import wizard.skunk.parser.domain.IACDUnit;
import wizard.skunk.parser.xml.XMLHelper;

import java.util.ArrayList;
import java.util.List;

public class IACDParser {
	private final List<Element> unitNodes;

	private final IProgressMonitor monitor;

	@Getter
	private final List<IACDUnit> iacdUnitList = new ArrayList<>();

	public IACDParser(Element rootElement, IProgressMonitor monitor) {
		this.monitor = monitor;
		this.unitNodes = XMLHelper.getElementsByTagName(rootElement, "Unit");
	}

	public void parse(){
		monitor.beginTask("Parsing IMCD", unitNodes.size());
		for(Element unitNode : unitNodes){
			parseUnitNode(unitNode);
			monitor.worked(1);
		}
	}

	private void parseUnitNode(Element unitNode){
		IACDUnit unit = parseUnit(unitNode);

		monitor.subTask("unit " + unit.getName());

		Element alarmParentNode = XMLHelper.getElementByTagName(unitNode, "AlmStaticInfo");
		List<DeviceAlarm> alarms = parseDeviceAlarm(alarmParentNode);
		unit.addDeviceAlarm(alarms);

		iacdUnitList.add(unit);
	}

	private List<DeviceAlarm> parseDeviceAlarm(Element parentNode){
		List<DeviceAlarm> result = new ArrayList<>();

		if(parentNode == null){
			return result;
		}

		List<Element> alarmNodes = XMLHelper.getElementsByTagName(parentNode, "FCDA");
		for(Element alarmNode : alarmNodes){
			String reference = alarmNode.getAttribute("intAddr");
			String description = alarmNode.getAttribute("desc");
			String level = alarmNode.getAttribute("level");
			String effect = "";
			Element effectNode = XMLHelper.getElementByTagName(alarmNode, "Effect");
			if(effectNode != null){
				effect = effectNode.getTextContent();
			}
			String suggestion = "";
			Element suggestionNode = XMLHelper.getElementByTagName(alarmNode, "Suggestion");
			if(suggestionNode != null){
				suggestion = suggestionNode.getTextContent();
			}

			DeviceAlarm alarm = new DeviceAlarm();
			alarm.setReference(reference);
			alarm.setDescription(description);
			alarm.setLevel(level);
			alarm.setEffect(effect);
			alarm.setSuggestion(suggestion);

			List<Element> boardNodes = XMLHelper.getElementsByTagName(alarmNode, "Board");
			for(Element boardNode : boardNodes){
				String board = boardNode.getAttribute("slot");
				alarm.addBoard(board);
			}

			result.add(alarm);
		}

		return result;
	}

	private IACDUnit parseUnit(Element unitNode) {
		String unitName = unitNode.getAttribute("iedName");
		String manufacturer = unitNode.getAttribute("manufacturer");
		String type = unitNode.getAttribute("type");
		String configVersion = unitNode.getAttribute("configVersion");
		String className = unitNode.getAttribute("className");
		String physicalID = unitNode.getAttribute("phyID");
		String serialNumber = unitNode.getAttribute("SN");

		IACDUnit unit = new IACDUnit();
		unit.setName(unitName);
		unit.setManufacturer(manufacturer);
		unit.setType(type);
		unit.setConfigVersion(configVersion);
		unit.setClassName(className);
		unit.setPhysicalID(physicalID);
		unit.setSerialNumber(serialNumber);
		return unit;
	}
}
