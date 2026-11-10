package wizard.skunk.parser.scl;

import lombok.Getter;
import org.w3c.dom.Element;
import wizard.skunk.common.IProgressMonitor;
import wizard.skunk.parser.domain.Cubicle;
import wizard.skunk.parser.domain.CubicleUnit;
import wizard.skunk.parser.domain.OpticalFiber;
import wizard.skunk.parser.domain.Region;
import wizard.skunk.parser.xml.XMLHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SPCDParser {
	private final List<Element> regionNodes;
	private final List<Element> cableNodes;
	private final IProgressMonitor monitor;

	@Getter
	private final List<OpticalFiber> opticalFibers = new ArrayList<>();
	@Getter
	private final List<Region> regionList = new ArrayList<>();

	// <Region.Cubicle.Unit, DeviceName>
	private final Map<String, String> deviceMap = new HashMap<>();

	public SPCDParser(Element rootElement, IProgressMonitor monitor){
		this.monitor = monitor;

		regionNodes = XMLHelper.getElementsByTagName(rootElement, "Region");
		cableNodes = XMLHelper.getElementsByTagName(rootElement, "Cable");
	}

	public void parse(){
		monitor.beginTask("Parsing Region", regionNodes.size());
		for(Element regionNode : regionNodes){
			parseRegion(regionNode);
			monitor.worked(1);
		}

		monitor.beginTask("Parsing Cable", cableNodes.size());
		for(Element cableNode : cableNodes){
			parseCable(cableNode);
			monitor.worked(1);
		}
	}

	private void parseCable(Element cableNode){
		String name = cableNode.getAttribute("name");

		monitor.subTask(name);

		String cubicleA = cableNode.getAttribute("cubicleA");
		String cubicleB = cableNode.getAttribute("cubicleB");
		List<Element> coreNodes = XMLHelper.getElementsByTagName(cableNode, "Core");
		for(Element coreNode : coreNodes){
			String reserve= coreNode.getAttribute("reserve");
			if(reserve.equalsIgnoreCase("true")){
				continue;
			}

			String no = coreNode.getAttribute("no");
			String portA = coreNode.getAttribute("portA");
			String portB = coreNode.getAttribute("portB");

			addOpticalFibers(name, no, cubicleA, cubicleB, portA, portB);
		}
	}

	private String parseDevice(String cubicle, String port){
		String[] portArray = port.split("\\.");

		String deviceA = deviceMap.get(cubicle + "." + portArray[0]);
		if(deviceA == null){
			deviceA = cubicle + "." + portArray[0];
		}

		return deviceA;
	}

	private String parsePort(String port){
		String[] portArray = port.split("\\.");
		if(portArray.length == 3){
			return portArray[1] + "-" + portArray[2].replaceAll("-", "");
		}

		return port;
	}

	private void parseRegion(Element regionNode){
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
	}

	private Cubicle parseCubicle(Element cubicleNode, String regionName){
		String cubicleName = cubicleNode.getAttribute("name");
		String cubicleDesc = cubicleNode.getAttribute("desc");

		monitor.subTask(regionName + "." + cubicleName);

		Cubicle cubicle = new Cubicle();
		cubicle.setName(cubicleName);
		cubicle.setDescription(cubicleDesc);

		List<Element> unitNodes = XMLHelper.getElementsByTagName(cubicleNode, "Unit");
		for(Element unitNode : unitNodes){
			String unitName = unitNode.getAttribute("name");

			CubicleUnit unit = new CubicleUnit();
			unit.setName(unitName);
			String deviceName = unitNode.getAttribute("iedName");
			if(deviceName.isBlank()){
				deviceName = String.join(".", regionName, cubicleName, unitName);
			}
			unit.setDevice(deviceName);
			unit.setDescription(unitNode.getAttribute("desc"));
			unit.setManufacturer(unitNode.getAttribute("manufacturer"));
			unit.setType(unitNode.getAttribute("type"));
			unit.setClassName(unitNode.getAttribute("class"));

			if(!unit.getClassName().equals("IED")){
				unit.addBoard(BoardParser.parse(unitNode));
			}

			cubicle.addUnit(unit);

			deviceMap.put(String.join(".", regionName, cubicleName, unitName), deviceName);
		}

		List<Element> interCoreNodes = XMLHelper.getElementsByTagName(cubicleNode, "IntCore");
		for(Element interCodeNode : interCoreNodes){
			String name = interCodeNode.getAttribute("name");
			String no = "1";
			String portA = interCodeNode.getAttribute("portA");
			String portB = interCodeNode.getAttribute("portB");

			addOpticalFibers(name, no,
					regionName + "." + cubicleName,
					regionName + "." + cubicleName,
					portA, portB);
		}

		return cubicle;
	}

	private void addOpticalFibers(String name, String no, String cubicleA, String cubicleB, String portA, String portB){
		if(portA.isBlank() || portB.isBlank()){
			return;
		}

		OpticalFiber opticalFiber = new OpticalFiber();
		opticalFiber.setName(name);
		opticalFiber.setNo(no);
		opticalFiber.setIedA(parseDevice(cubicleA, portA));
		opticalFiber.setIedB(parseDevice(cubicleB, portB));
		opticalFiber.setPortA(parsePort(portA));
		opticalFiber.setPortB(parsePort(portB));
		opticalFiber.setUsage("GOOSE/SV");
		opticalFibers.add(opticalFiber);
	}
}
