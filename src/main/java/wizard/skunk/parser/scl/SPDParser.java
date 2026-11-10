package wizard.skunk.parser.scl;

import lombok.Getter;
import org.w3c.dom.Element;
import wizard.skunk.common.IProgressMonitor;
import wizard.skunk.parser.domain.*;
import wizard.skunk.parser.xml.XMLHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SPDParser {
	private final List<Element> regionNodes;
	private final List<Element> bayNodes;
	private final Element deviceTemplates;
	private final IProgressMonitor monitor;

	// <Cabinet.Unit, Device Name>
	private final Map<String, String> deviceNameMap = new HashMap<>();
	private int unNamedDevice = 0;
	@Getter
	private final List<OpticalFiber> opticalFibers = new ArrayList<>();

	@Getter
	private List<Region> regions = new ArrayList<>();

	public SPDParser(Element rootElement, IProgressMonitor monitor) {
		this.monitor = monitor;

		Element substationNode = XMLHelper.getElementByTagName(rootElement, "Substation");
		if(substationNode == null){
			this.regionNodes = List.of();
			this.bayNodes = List.of();
		}
		else{
			this.regionNodes = XMLHelper.getElementsByTagName(substationNode, "Region");
			this.bayNodes = XMLHelper.getElementsByTagName(substationNode, "Bay");
		}

		this.deviceTemplates = XMLHelper.getElementByTagName(rootElement, "PartTemplates");
	}

	public void parse() {
		monitor.beginTask("Parsing SPD", regionNodes.size());
		for (Element regionNode : regionNodes) {
			parseRegion(regionNode);
			monitor.worked(1);
		}

		monitor.beginTask("Parsing Cables", bayNodes.size());
		for(Element bayNode : bayNodes){
			parseBay(bayNode);
			monitor.worked(1);
		}
	}

	private void parseBay(Element bayNode) {
		String bayName = bayNode.getAttribute("name");
		String bayDesc = bayNode.getAttribute("desc");

		monitor.subTask(bayDesc + " (" + bayName + ")");

		List<Element> cableNodes = XMLHelper.getElementsByTagName(bayNode, "Cable");
		for(Element cableNode : cableNodes){
			String cableName = cableNode.getAttribute("name");
			String cabinetA = cableNode.getAttribute("startCubicleName");
			String cabinetB = cableNode.getAttribute("endCubicleName");

			List<Element> coreNodes = XMLHelper.getElementsByTagName(cableNode, "Core");
			for(Element coreNode : coreNodes){
				String name = coreNode.getAttribute("name");
				String unitA = coreNode.getAttribute("startDeviceName");
				String iedA = deviceNameMap.get(cabinetA + "." + unitA);
				String boardA = coreNode.getAttribute("startSubDeviceName");
				String portA = coreNode.getAttribute("startPinName");
				String unitB = coreNode.getAttribute("endDeviceName");
				String iedB = deviceNameMap.get(cabinetB + "." + unitB);
				String boardB = coreNode.getAttribute("endSubDeviceName");
				String portB = coreNode.getAttribute("endPinName");

				if(iedA == null || iedB == null){
					continue; // Skip if either end is not found
				}

				OpticalFiber opticalFiber = new OpticalFiber();
				opticalFiber.setName(cableName);
				opticalFiber.setNo(name);
				opticalFiber.setIedA(iedA);
				opticalFiber.setBoardA(boardA);
				opticalFiber.setPortA(portA);
				opticalFiber.setIedB(iedB);
				opticalFiber.setBoardB(boardB);
				opticalFiber.setPortB(portB);

				opticalFibers.add(opticalFiber);
			}
		}
	}

	private void parseRegion(Element regionNode) {
		String regionName = regionNode.getAttribute("name");
		String regionDesc = regionNode.getAttribute("desc");

		monitor.subTask(regionName);

		Region region = new Region();
		region.setName(regionName);
		region.setDescription(regionDesc);

		List<Element> cubicleNodes = XMLHelper.getElementsByTagName(regionNode, "Cubicle");
		for(Element cubicleNode : cubicleNodes){
			region.addCubicle(parseCubicle(cubicleNode, regionName));
		}

		regions.add(region);
	}

	private Cubicle parseCubicle(Element cubicleNode, String regionName){
		String cubicleName = cubicleNode.getAttribute("name");
		String cubicleDesc = cubicleNode.getAttribute("desc");

		monitor.subTask(regionName + "." + cubicleName);

		Cubicle cubicle = new Cubicle();
		cubicle.setName(cubicleName);
		cubicle.setDescription(cubicleDesc);

		List<Element> unitNodes = XMLHelper.getElementsByTagName(cubicleNode, "Device");
		Map<String, String> deviceUnitMap = new HashMap<>();
		for(Element unitNode : unitNodes){
			String unitName = unitNode.getAttribute("name");
			String className = unitNode.getAttribute("class");

			String deviceName = unitNode.getAttribute("iedName");
			if(deviceName.isBlank()){
				unNamedDevice++;
				switch (className){
					// 交换机
					case "TSC" -> deviceName = "SW_" + unNamedDevice;
					case "ODF" -> deviceName = "ODF_" + unNamedDevice;
				}
			}

			deviceUnitMap.put(unitName, deviceName);
			deviceNameMap.put(cubicleName + "." + unitName, deviceName);

			CubicleUnit unit = new CubicleUnit();
			unit.setName(unitName);
			unit.setDevice(deviceName);
			unit.setDescription(unitNode.getAttribute("desc"));
			unit.setManufacturer(unitNode.getAttribute("manufacturer"));
			unit.setType(unitNode.getAttribute("type"));
			unit.setClassName(className);

			unit.addBoard(parseBoard(unitNode));

			cubicle.addUnit(unit);
		}

		parseInterCore(cubicleNode, deviceUnitMap);

		return cubicle;
	}

	private void parseInterCore(Element cubicleNode, Map<String, String> deviceUnitMap){
		List<Element> interCoreNodes = XMLHelper.getElementsByTagName(cubicleNode, "Core");
		String cabinetName = cubicleNode.getAttribute("name");
		String cableName = cabinetName + ".InterCore";
		for(Element interCoreNode : interCoreNodes){
			String name = interCoreNode.getAttribute("name");
			String unitA = interCoreNode.getAttribute("startDeviceName");
			String iedA = deviceUnitMap.get(unitA);
			if(iedA == null){
				continue;
			}
			String boardA = interCoreNode.getAttribute("startSubDeviceName");
			String portA = interCoreNode.getAttribute("startPinName");
			String unitB = interCoreNode.getAttribute("endDeviceName");
			String iedB = deviceUnitMap.get(unitB);
			if(iedB == null){
				continue;
			}
			String boardB = interCoreNode.getAttribute("endSubDeviceName");
			String portB = interCoreNode.getAttribute("endPinName");

			OpticalFiber opticalFiber = new OpticalFiber();
			opticalFiber.setName(cableName);
			opticalFiber.setNo(name);
			opticalFiber.setIedA(iedA);
			opticalFiber.setBoardA(boardA);
			opticalFiber.setPortA(portA);
			opticalFiber.setIedB(iedB);
			opticalFiber.setBoardB(boardB);
			opticalFiber.setPortB(portB);

			opticalFibers.add(opticalFiber);
		}
	}

	private List<Board> parseBoard(Element unitNode){
		String className = unitNode.getAttribute("class");
		List<Element> boardNodes = new ArrayList<>();
		if(!className.equals("TSC")){
			boardNodes = XMLHelper.getElementsByTagName(unitNode, "SubDevice");
		}
		else{
			boardNodes.add(unitNode);
		}
		List<Board> result = new ArrayList<>();

		for(Element boardNode : boardNodes){
			String templateName = boardNode.getAttribute("partName");

			Element boardTemplateNode = XMLHelper.getElementByTagNameAndAttribute(deviceTemplates, "PartTemplate", "name", templateName);
			if(boardTemplateNode == null){
				continue; // Skip if no template found
			}

			String slot = boardNode.getAttribute("name");
			String type = boardTemplateNode.getAttribute("type");
			String manufacturer = boardTemplateNode.getAttribute("manufacturer");
			String description = boardTemplateNode.getAttribute("desc");

			Board board = new Board();
			board.setSlot(slot);
			board.setType(type);
			board.setManufacturer(manufacturer);
			board.setDescription(description);
			board.setTemplateName(templateName);

			parsePort(boardTemplateNode, board);
			parsePortAttributes(boardNode, board);

			result.add(board);
		}

		if(className.equals("TSC")){
			int i = 1;
			for(Board board : result){
				board.setSlot("" + i);
				i++;
			}
		}

		return result;
	}

	private void parsePortAttributes(Element boardNode, Board board) {
		List<Element> componentNodes = XMLHelper.getElementsByTagName(boardNode, "Component");
		for(Element componentNode : componentNodes){
			String name = componentNode.getAttribute("name");
			String icdPort = componentNode.getAttribute("icdPort");
			board.setIcdPort(name, icdPort);
		}

		List<Element> pinNodes = XMLHelper.getElementsByTagName(boardNode, "Pin");
		for(Element pinNode : pinNodes){
			String name = pinNode.getAttribute("name");
			String reference = pinNode.getAttribute("icdRef");
			board.setPortLightReference(name, reference);
		}
	}

	private void parsePort(Element boardTemplateNode, Board board) {
		List<Element> portNodes = XMLHelper.getElementsByTagName(boardTemplateNode, "ComponentTemplate");
		for(Element portNode : portNodes){
			String portType = portNode.getAttribute("type");
			String portName = portNode.getAttribute("name");
			List<Element> portPins = XMLHelper.getElementsByTagName(portNode, "ComponentPinTemplate");
			List<Element> inCoreTemplateNodes = XMLHelper.getElementsByTagName(boardTemplateNode, "InCoreTemplate");
			switch (portType){
				case "Fiber_RX_1", "Fiber_TX_1", "Fiber_RXTX_2", "BNC_RXTX_2" -> {
					for(Element pinNode : portPins){
						String pinIndex = pinNode.getAttribute("pinIndex");
						String pinDesc = pinNode.getAttribute("desc");
						String formattedName = getPortFormatName(inCoreTemplateNodes, portName, pinIndex);

						Port port = new Port();
						port.setNo(portName);
						port.setDescription(pinDesc);
						port.setPlug("LC");
						if(portType.contains("RXTX")){
							port.setDirection("RT");
						}
						else if(portType.contains("RX")){
							port.setDirection("RX");
						}
						else {
							port.setDirection("TX");
						}
						port.setFormatedName(formattedName);

						board.addPort(port);
					}
				}
			}
		}
	}

	private String getPortFormatName(List<Element> inCoreTemplateNodes, String portName, String pinIndex){
		for(Element inCoreTemplateNode : inCoreTemplateNodes){
			String startComponent = inCoreTemplateNode.getAttribute("startComponent");
			String name = startComponent;
			String endComponent = inCoreTemplateNode.getAttribute("endComponent");
			String index = inCoreTemplateNode.getAttribute("startPinIndex");
			String formatName = inCoreTemplateNode.getAttribute("endPinIndex");
			if(startComponent.isEmpty()){
				name = endComponent;
				index = inCoreTemplateNode.getAttribute("endPinIndex");
				formatName = inCoreTemplateNode.getAttribute("startPinIndex");
			}

			if(name.equals(portName) && index.equals(pinIndex)){
				return formatName;
			}
		}

		return "";
	}
}
