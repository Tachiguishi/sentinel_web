package wizard.skunk.parser.scl;

import lombok.Getter;
import org.w3c.dom.Element;
import wizard.skunk.common.IProgressMonitor;
import wizard.skunk.parser.domain.BrokenAlarm;
import wizard.skunk.parser.domain.IVLDUnit;
import wizard.skunk.parser.domain.PlateSwitcher;
import wizard.skunk.parser.domain.VirtualTerminal;
import wizard.skunk.parser.xml.XMLHelper;

import java.util.ArrayList;
import java.util.List;

public class IVLDParser {
	private final List<Element> unitNodes;
	private final IProgressMonitor monitor;

	@Getter
	private final List<IVLDUnit> ivldUnitList = new ArrayList<>();

	public IVLDParser(Element rootElement, IProgressMonitor monitor){
		this.monitor = monitor;
		this.unitNodes = XMLHelper.getElementsByTagName(rootElement, "Unit");
	}

	public void parse(){
		monitor.beginTask("Parsing IVLD", unitNodes.size());
		for(Element unitNode : unitNodes){
			parseUnitNode(unitNode);
			monitor.worked(1);
		}
	}

	private void parseUnitNode(Element unitNode){
		IVLDUnit unit = parseUnit(unitNode);
		monitor.subTask("unit " + unit.getName());
		unit.addVirtualTerminal(parseTerminal(unitNode));
		unit.addBrokenAlarm(parseBrokenAlarm(unitNode));
		unit.addPlateSwitcher(parsePlateSwitcher(unitNode));
		ivldUnitList.add(unit);
	}

	private List<PlateSwitcher> parsePlateSwitcher(Element unitNode){
		List<PlateSwitcher> result = new ArrayList<>();
		List<Element> privateNode = XMLHelper.getElementsByTagNameAndAttribute(unitNode, "Private", "type", "strapRef");
		privateNode.addAll(XMLHelper.getElementsByTagNameAndAttribute(unitNode, "private", "type", "strapRef"));
		for(Element node : privateNode){
			PlateSwitcher plateSwitcher = new PlateSwitcher();
			plateSwitcher.setReference(node.getTextContent().trim());
			plateSwitcher.setDeviceName(node.getAttribute("IEDName"));
			plateSwitcher.setCabinetID(node.getAttribute("LpPhyID"));
			plateSwitcher.setRowNum(node.getAttribute("PositionH"));
			plateSwitcher.setColNum(node.getAttribute("PositionV"));
			result.add(plateSwitcher);
		}

		return result;
	}

	private List<BrokenAlarm> parseBrokenAlarm(Element unitNode){
		List<BrokenAlarm> result = new ArrayList<>();
		Element parentNode = XMLHelper.getElementByTagName(unitNode, "InputLinks");
		if(parentNode == null){
			return result;
		}

		List<Element> linkNode = XMLHelper.getElementsByTagName(parentNode, "VirLink");
		for(Element link : linkNode){
			BrokenAlarm alarm = new BrokenAlarm();
			alarm.setReference(link.getAttribute("alarmRef"));
			alarm.setSendDevice(link.getAttribute("outputIED"));
			alarm.setSendControlBlock(link.getAttribute("CBRef"));
			alarm.setAppID(link.getAttribute("APPID"));
			result.add(alarm);
		}

		return result;
	}

	private List<VirtualTerminal> parseTerminal(Element unitNode){
		List<VirtualTerminal> result = new ArrayList<>();

		Element outputsNode = XMLHelper.getElementByTagName(unitNode, "Outputs");
		if(outputsNode != null){
			List<Element> outputTerminalNodes = XMLHelper.getElementsByTagName(outputsNode, "OutputTerminal");
			result.addAll(parseTerminal(outputTerminalNodes));
		}

		Element inputsNode = XMLHelper.getElementByTagName(unitNode, "Inputs");
		if(inputsNode != null){
			List<Element> inputTerminalNodes = XMLHelper.getElementsByTagName(inputsNode, "InputTerminal");
			result.addAll(parseTerminal(inputTerminalNodes));
		}

		return result;
	}

	private List<VirtualTerminal> parseTerminal(List<Element> terminalNodes){
		List<VirtualTerminal> result = new ArrayList<>();
		for(Element terminalNode : terminalNodes){
			VirtualTerminal terminal = new VirtualTerminal();
			terminal.setReference(terminalNode.getAttribute("intAddr"));
			terminal.setStrapReference(terminalNode.getAttribute("softStrap"));

			result.add(terminal);
		}

		return result;
	}

	private IVLDUnit parseUnit(Element unitNode){
		IVLDUnit unit = new IVLDUnit();
		unit.setName(unitNode.getAttribute("iedName"));
		unit.setManufacturer(unitNode.getAttribute("manufacturer"));
		unit.setType(unitNode.getAttribute("type"));
		unit.setConfigVersion(unitNode.getAttribute("configVersion"));
		unit.setCrc(unitNode.getAttribute("IEDVirtualCRC"));
		return unit;
	}
}
