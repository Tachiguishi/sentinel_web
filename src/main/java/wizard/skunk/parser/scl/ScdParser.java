package wizard.skunk.parser.scl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import wizard.skunk.common.IProgressMonitor;
import wizard.skunk.common.StringUtil;
import wizard.skunk.parser.domain.*;
import wizard.skunk.parser.xml.XMLHelper;

import java.util.*;

@Slf4j
public class ScdParser {
	protected final Element communicationNode;
	protected final DataTypeParser dataTypeParser;
	protected final Element rootElement;
	protected final List<Element> iedNodes;

	protected final IProgressMonitor monitor;

	@Getter
	protected final SCDVersion scdVersion = new SCDVersion();
	@Getter
	protected final List<IED> iedList = new ArrayList<>();

	public ScdParser(Element rootElement, IProgressMonitor monitor){
		this.monitor = monitor;

		communicationNode = XMLHelper.getElementByTagName(rootElement, "Communication");

		Element dataTypeTemplatesNode = XMLHelper.getElementByTagName(rootElement, "DataTypeTemplates");
		dataTypeParser = new DataTypeParser(dataTypeTemplatesNode);

		this.rootElement = rootElement;
		iedNodes = XMLHelper.getElementsByTagName(rootElement, "IED");
	}

	public void parse(){
		parseVersionInfo();
		parseIED();
	}

	private void parseVersionInfo(){
		Element terminalCRCNode = XMLHelper.getElementByTagNameAndAttribute(rootElement, "Private", "type", "Substation virtual terminal conection CRC");
		if(terminalCRCNode != null){
			scdVersion.setTerminalCRC(terminalCRCNode.getTextContent());
		}

		Element substationNode = XMLHelper.getElementByTagName(rootElement, "Substation");
		if(substationNode != null){
			scdVersion.setName(substationNode.getAttribute("name"));
			scdVersion.setDescription(substationNode.getAttribute("desc"));
		}

		Element headerNode = XMLHelper.getElementByTagName(rootElement, "Header");
		if(headerNode == null){
			return;
		}
		String id = headerNode.getAttribute("id");
		String version = headerNode.getAttribute("version");
		String revision = headerNode.getAttribute("revision");

		scdVersion.setIdentify(id);
		scdVersion.setVersion(version);
		scdVersion.setRevision(revision);

		Element logNode = XMLHelper.selectSingleNode(rootElement, "History/Hitem[last()]");
		if(logNode == null){
			return;
		}
		scdVersion.setVersion(logNode.getAttribute("version"));
		scdVersion.setRevision(logNode.getAttribute("revision"));
		scdVersion.setWhat(logNode.getAttribute("what"));
		scdVersion.setWhen(logNode.getAttribute("when"));
		scdVersion.setWho(logNode.getAttribute("who"));
		scdVersion.setWhy(logNode.getAttribute("why"));
	}

	private void parseIED(){
		monitor.beginTask("Parsing IED", iedNodes.size());
		for(Element iedNode : iedNodes){
			IED ied = parseDeviceElement(iedNode);
			monitor.subTask(ied.getName());

			parseAccessPoint(ied, iedNode);

			iedList.add(ied);
			monitor.worked(1);
		}
	}

	private void parseAccessPoint(IED ied, Element iedNode){
		List<Element> accessPointNodes = XMLHelper.getElementsByTagName(iedNode, "AccessPoint");
		for(Element accessPointNode : accessPointNodes){
			String apName = accessPointNode.getAttribute("name");
			Element serverNode = XMLHelper.getElementByTagName(accessPointNode, "Server");
			if(serverNode == null){
				continue;
			}
			List<Element> lDeviceNodes = XMLHelper.getElementsByTagName(serverNode, "LDevice");
			for(Element lDeviceNode : lDeviceNodes){
				String ldName = lDeviceNode.getAttribute("inst");
				String ldDesc = lDeviceNode.getAttribute("desc");

				// <lnName, LN>
				Map<String, Element> lnNodesMap = parseLogicalNode(ied, apName, lDeviceNode);

				Element ln0Node = XMLHelper.getElementByTagName(lDeviceNode, "LN0");
				if(ln0Node != null){
					parseDataSet(ied, ln0Node, apName, ldName, ldDesc, lnNodesMap);
					parseInputs(ied, ln0Node, apName, ldName, lnNodesMap);
				}
			}
		}
	}

	protected Map<String, Element> parseLogicalNode(IED ied, String apName, Element lDeviceNode){
		Map<String, Element> lnNodesMap = new HashMap<>();

		String ldName = lDeviceNode.getAttribute("inst");

		NodeList nodeList = lDeviceNode.getChildNodes();
		for(int i = 0; i < nodeList.getLength(); i++){
			if(nodeList.item(i).getNodeType() != Element.ELEMENT_NODE){
				continue;
			}

			Element lnNode = (Element) nodeList.item(i);
			String prefix = lnNode.getAttribute("prefix");
			String lnClass = lnNode.getAttribute("lnClass");
			String inst = lnNode.getAttribute("inst");
			String lnName = prefix + lnClass + inst;

			lnNodesMap.put(lnName, lnNode);

			parseTraitLogicalNode(ied, ldName, prefix, lnClass, inst, lnNode);
		}

		return lnNodesMap;
	}

	protected void parseTraitLogicalNode(IED ied, String ldName, String prefix, String lnClass, String inst, Element lnNode){
	}

	private void parseInputs(IED ied, Element ln0Node, String apName, String ldName, Map<String, Element> lnNodesMap){
		Element inputNode = XMLHelper.getElementByTagName(ln0Node, "Inputs");
		if(inputNode == null){
			return;
		}

		List<Element> extRefNodes = XMLHelper.getElementsByTagName(inputNode, "ExtRef");
		if(extRefNodes.isEmpty()){
			return;
		}

		DataSet dataSet = new DataSet();
		dataSet.setName("Inputs");
		dataSet.setDescription("GOOSE/SV接收");
		dataSet.setAccessPoint(apName);
		dataSet.setLdName(ldName);
		dataSet.setLnClass(ln0Node.getAttribute("lnClass"));

		ied.addDataSet(dataSet);

		for(Element extRefNode : extRefNodes){
			// 2-B:PIGO/GOINGGIO1.SPCSO1.stVal
			String intAddr = extRefNode.getAttribute("intAddr");
			if(intAddr.isEmpty()){
				continue;
			}

			String[] intAddrArray = intAddr.split(":");
			String receiveBoardPort = "";
			if(intAddrArray.length > 1){
				receiveBoardPort = intAddrArray[0];
			}

			// PIGO/GOINGGIO1.SPCSO1.stVal
			String inputReference = intAddrArray[intAddrArray.length - 1];
			String[] inputReferenceArray = inputReference.split("/");
			int dotIndex = inputReferenceArray[1].indexOf(".");
			// GOINGGIO1
			String lnName = inputReferenceArray[1].substring(0, dotIndex);
			// SPCSO1.stVal
			String doNameAndDaName = inputReferenceArray[1].substring(dotIndex + 1);
			dotIndex = doNameAndDaName.indexOf(".");
			String doName;
			String daName = "";
			if(dotIndex == -1){
				doName = doNameAndDaName;
			}
			else{
				doName = doNameAndDaName.substring(0, dotIndex);
				daName = doNameAndDaName.substring(dotIndex + 1);
			}

			Element lnNode = lnNodesMap.get(lnName);
			if(lnNode == null){
				log.error("{}.intAddr: {} can not be found in {}", ldName, intAddr, ied.getName());
				continue;
			}

			FCDA inputFcda = new FCDA();
			inputFcda.setLdInst(ldName);
			inputFcda.setPrefix(lnNode.getAttribute("prefix"));
			inputFcda.setLnClass(lnNode.getAttribute("lnClass"));
			inputFcda.setLnInst(lnNode.getAttribute("inst"));
			inputFcda.setDoName(doName);
			inputFcda.setDaName(daName);

			inputFcda.generateReference();

			ied.addFCDA(inputFcda, dataSet);

			String lnTypeIndex = lnNode.getAttribute("lnType");
			DoType doType = dataTypeParser.getDoType(lnTypeIndex, inputFcda.getDoName());
			if(doType == null){
				log.error("Could not find DoType[{}] in lnType[{}]", inputFcda.getDoName(), lnTypeIndex);
				continue;
			}
			inputFcda.setCdc(doType.getCdc());

			Element doiNode = getDOINode(inputFcda.getDoName(), lnNode);
			inputFcda.setDescription(getFCDADescription(inputFcda.getDoName(), lnNode, doiNode));

			LogicalLink logicalLink = new LogicalLink();
			logicalLink.setReceiverBoarPort(receiveBoardPort);
			logicalLink.setReceiverIedName(ied.getName());
			logicalLink.setReceiverReference(inputFcda.getDotReference());

			String senderIedName = extRefNode.getAttribute("iedName");
			logicalLink.setSenderIedName(senderIedName);
			String ldInst = extRefNode.getAttribute("ldInst");
			String prefix = extRefNode.getAttribute("prefix");
			String lnClass = extRefNode.getAttribute("lnClass");
			String lnInst = extRefNode.getAttribute("lnInst");
			doName = extRefNode.getAttribute("doName");
			daName = extRefNode.getAttribute("daName");
			String senderReference = ldInst + Split.Slash + prefix + lnClass + lnInst +
					Split.Dot + doName;
			if(!daName.isEmpty()){
				senderReference += Split.Dot + daName;
			}
			logicalLink.setSenderReference(senderReference);
			ied.addLogicalLink(logicalLink);
		}
	}

	private void parseDataSet(IED ied, Element ln0Node, String apName, String ldName, String ldDesc, Map<String, Element> lnNodesMap){
		Set<String> dataSetNames = new HashSet<>();

		String lnClass = ln0Node.getAttribute("lnClass");

		String referencePrefix = ldName + "/" + lnClass;
		List<Element> reportControlNodes = XMLHelper.getElementsByTagName(ln0Node, "ReportControl");
		for(Element reportControlNode : reportControlNodes){
			ReportControl reportControl = new ReportControl();
			reportControl.setName(reportControlNode.getAttribute("name"));
			reportControl.setRptID(reportControlNode.getAttribute("rptID"));
			reportControl.setBuffered(reportControlNode.getAttribute("buffered").equals("true"));
			String dataSetName = reportControlNode.getAttribute("datSet");
			reportControl.setDataSet(apName + "." + ldName + "." + dataSetName);
			reportControl.setReference(referencePrefix + Split.Dollar + (reportControl.isBuffered() ? "BR" : "RP") + Split.Dollar + reportControl.getName());
			dataSetNames.add(dataSetName);

			Element rptEnabledNode = XMLHelper.getElementByTagName(reportControlNode, "RptEnabled");
			if(rptEnabledNode != null && !rptEnabledNode.getAttribute("max").isEmpty()) {
				reportControl.setRptEnabled(Integer.parseInt(rptEnabledNode.getAttribute("max")));
			}

			ied.addReportControl(reportControl);
		}

		List<Element> gooseControlNodes = XMLHelper.getElementsByTagName(ln0Node, "GSEControl");
		List<Element> sampledValueControlNodes = XMLHelper.getElementsByTagName(ln0Node, "SampledValueControl");
		gooseControlNodes.addAll(sampledValueControlNodes);
		for(Element gooseControlNode : gooseControlNodes){
			String nodeName = gooseControlNode.getNodeName();
			boolean isGOOSE = nodeName.equals("GSEControl");
			GooseSvControl gooseSvControl = new GooseSvControl();
			gooseSvControl.setGoose(isGOOSE);
			gooseSvControl.setName(gooseControlNode.getAttribute("name"));
			if(isGOOSE){
				gooseSvControl.setIdentify(gooseControlNode.getAttribute("appID"));
			}
			else{
				gooseSvControl.setIdentify(gooseControlNode.getAttribute("smvID"));
			}
			String dataSetName = gooseControlNode.getAttribute("datSet");
			gooseSvControl.setDataSet(apName + "." + ldName + "." + dataSetName);
			gooseSvControl.setReference(referencePrefix + Split.Dollar + gooseSvControl.getName());
			dataSetNames.add(dataSetName);

			parseGooseAddress(ied.getName(), apName, ldName, gooseSvControl);
			ied.addGooseSvControl(gooseSvControl);
		}

		List<Element> dataSetNodes = XMLHelper.getElementsByTagName(ln0Node, "DataSet");
		for(Element dataSetNode : dataSetNodes){
			String dataSetName = dataSetNode.getAttribute("name");
			if(!dataSetNames.contains(dataSetName) && !DataSetName.isGeneralSettingDataSet(dataSetName)){
				continue;
			}

			DataSet dataSet = new DataSet();
			dataSet.setName(dataSetNode.getAttribute("name"));
			dataSet.setDescription(dataSetNode.getAttribute("desc"));
			dataSet.setAccessPoint(apName);
			dataSet.setLdName(ldName);
			dataSet.setLdDescription(ldDesc);
			dataSet.setLnClass(lnClass);

			List<Element> fcdaNodes = XMLHelper.getElementsByTagName(dataSetNode, "FCDA");
			for(Element fcdaNode : fcdaNodes) {
				FCDA fcda = new FCDA();
				fcda.setLdInst(fcdaNode.getAttribute("ldInst"));
				fcda.setPrefix(fcdaNode.getAttribute("prefix"));
				fcda.setLnClass(fcdaNode.getAttribute("lnClass"));
				fcda.setLnInst(fcdaNode.getAttribute("lnInst"));
				fcda.setDoName(fcdaNode.getAttribute("doName"));
				fcda.setDaName(fcdaNode.getAttribute("daName"));
				fcda.setFc(fcdaNode.getAttribute("fc"));

				fcda.generateReference();

				if(ied.addFCDA(fcda, dataSet)){
					continue;
				}

				Element lnNode = lnNodesMap.get(fcda.getLnName());
				if(lnNode == null){
					log.error("Could not find LN node[{}] for FCDA[{}{}] in LDevice {}", fcda.getLnName(), ied.getName(), fcda.getDollarReference(), ldName);
					continue;
				}

				String lnTypeIndex = lnNode.getAttribute("lnType");
				DoType doType = dataTypeParser.getDoType(lnTypeIndex, fcda.getDoName());
				if(doType == null){
					log.error("Could not find DoType[{}] in lnType[{}] for FCDA[{}{}] in DataSet[{}]", fcda.getDoName(), lnTypeIndex, ied.getName(), fcda.getDollarReference(), dataSet);
					continue;
				}
				fcda.setCdc(doType.getCdc());

				Element doiNode = getDOINode(fcda.getDoName(), lnNode);
				fcda.setDescription(getFCDADescription(fcda.getDoName(), lnNode, doiNode));
				fcda.setUnitSerialization(getFCDAUnit(doiNode, doType));
			}

			ied.addDataSet(dataSet);
		}
	}

	private void parseGooseAddress(String iedName, String apName, String ldInst, GooseSvControl gooseSvControl){
		if(communicationNode == null){
			return;
		}

		String addressTag = gooseSvControl.isGoose() ? "GSE" : "SMV";

		Element addressNode = XMLHelper.selectSingleNode(communicationNode, "SubNetwork/ConnectedAP[@iedName='" + iedName +
				"' and @apName='" + apName + "']/" + addressTag +
				"[@cbName='" + gooseSvControl.getName() + "' and @ldInst='" + ldInst + "']/Address");

		if(addressNode == null){
			return;
		}

		List<Element> gooseAddressNodes = XMLHelper.getElementsByTagName(addressNode, "P");
		for(Element gooseAddressNode : gooseAddressNodes){
			String type = gooseAddressNode.getAttribute("type");
			switch (type) {
				case "MAC-Address" -> gooseSvControl.setMacAddress(gooseAddressNode.getTextContent());
				case "VLAN-ID" -> gooseSvControl.setVlanId(gooseAddressNode.getTextContent());
				case "VLAN-PRIORITY" -> gooseSvControl.setVlanPriority(gooseAddressNode.getTextContent());
				case "APPID" -> gooseSvControl.setAppId(gooseAddressNode.getTextContent());
			}
		}
	}

	private Element getDOINode(String doName, Element lnNode){
		String[] dos = doName.split("\\.");
		// "DOI[@name='" + dos[0] + "']"
		Element doiNode = XMLHelper.getElementByTagNameAndAttribute(lnNode, "DOI", "name", dos[0]);
		if(doiNode == null || dos.length == 1){
			return doiNode;
		}
		return XMLHelper.getElementByTagNameAndAttribute(doiNode, "SDI", "name", dos[1]);
	}

	private String getFCDADescription(String doName, Element lnNode, Element doiNode){
		if(doiNode == null){
			return dataTypeParser.getDoDescription(lnNode.getAttribute("lnType"), StringUtil.firstPhase(doName, Split.Dot.toString()));
		}

		String desc = "";
		Element duNode = getDaiValNode(doiNode, "dU");
		if(duNode != null){
			desc = duNode.getTextContent();
		}

		if(desc == null || desc.isEmpty()) {
			desc = doiNode.getAttribute("desc");
		}

		if(desc.isEmpty()) {
			desc = lnNode.getAttribute("desc");
		}

		return desc;
	}

	private Element getDaiValNode(Element parent, String value){
		Element targetNode = XMLHelper.getElementByTagNameAndAttribute(parent, "DAI", "name", value);
		if(targetNode == null){
			return null;
		}

		return XMLHelper.getElementByTagName(targetNode, "Val");
	}

	private String getFCDAUnit(Element doiNode, DoType doType){
		if(doiNode == null){
			return "";
		}
		/*
		<SDI name="units">
			<DAI name="SIUnit">
				<Val>m</Val>
			</DAI>
			<DAI name="multiplier">
				<Val>k</Val>
			</DAI>
		</SDI>
		 */
		Element unitNode = XMLHelper.getElementByTagNameAndAttribute(doiNode, "SDI", "name", "units");
		Element unitValNode = null;
		Element multiplierNode = null;
		if(unitNode != null) {
			unitValNode = getDaiValNode(unitNode, "SIUnit");
			multiplierNode = getDaiValNode(unitNode, "multiplier");
		}
		String unit = unitParser(unitValNode, "SIUnit", doType);
		String multiplier = unitParser(multiplierNode, "multiplier", doType);

		/*
		 * scene 1
		<SDI name="minVal">
			<DAI name="f">
				<Val>1.00</Val>
			</DAI>
		</SDI>
		 * scene 2
		<DAI name="minVal">
			<Val>0</Val>
		</DAI>
		 */
		Element minValNode = getRangeNode(doiNode, "minVal");
		Element maxValNode = getRangeNode(doiNode, "maxVal");
		Element stepSizeNode = getRangeNode(doiNode,"stepSize");

		return String.join(Split.Comma.toString(),
				unit,
				getNodeText(minValNode),
				getNodeText(maxValNode),
				getNodeText(stepSizeNode),
				multiplier);
	}

	private Element getRangeNode(Element doiNode, String category){
		Element rangeNode = XMLHelper.getElementByTagNameAndAttribute(doiNode, "SDI", "name", category);
		if(rangeNode == null){
			rangeNode = getDaiValNode(doiNode, category);
		}
		else{
			rangeNode = getDaiValNode(rangeNode, "f");
		}
		return rangeNode;
	}

	private String unitParser(Element node, String daName, DoType doType){
		String unit = getNodeText(node);
		if(unit == null || unit.isEmpty()){
			return "";
		}

		if(!unit.matches("^(-)?[0-9]+$")){
			return unit;
		}

		String enumIndex = doType.getUnitEnumID(daName);
		return dataTypeParser.parseEnum(enumIndex, unit);
	}

	private String getNodeText(Element node) {
		if(node == null) {
			return "";
		}
		return node.getTextContent();
	}

	private IED parseDeviceElement(Element iedNode){
		IED ied = new IED();
		ied.setName(iedNode.getAttribute("name"));
		ied.setType(iedNode.getAttribute("type"));
		ied.setManufacturer(iedNode.getAttribute("manufacturer"));
		ied.setVersion(iedNode.getAttribute("configVersion"));
		ied.setDescription(iedNode.getAttribute("desc"));

		Element crcNode = XMLHelper.getElementByTagNameAndAttribute(iedNode, "Private", "type", "IED virtual terminal conection CRC");
		if(crcNode != null){
			ied.setCrc(crcNode.getTextContent());
		}

		parseIPAddress(ied);

		return ied;
	}

	private void parseIPAddress(IED ied){
		if(communicationNode == null){
			return;
		}

		List<Element> addressNodes = XMLHelper.selectNodes(communicationNode,
									"SubNetwork/ConnectedAP[@iedName='" + ied.getName() + "' and @apName='S1']/Address");
		if(addressNodes.isEmpty()){
			addressNodes = XMLHelper.selectNodes(communicationNode,
							"SubNetwork/ConnectedAP[@iedName='" + ied.getName() + "']/Address");
		}

		boolean first = true;
		for(Element addressNode : addressNodes){
			Element ipNode = null;
			Element apTitle = null;
			Element aeQualifier = null;
			List<Element> pNode = XMLHelper.getElementsByTagName(addressNode, "P");
			for(Element p : pNode){
				switch (p.getAttribute("type")) {
					case "IP" -> ipNode = p;
					case "OSI-AP-Title" -> apTitle = p;
					case "OSI-AE-Qualifier" -> aeQualifier = p;
				}
			}

			if(ipNode == null){
				continue;
			}

			if(first){
				ied.setIpAddressA(ipNode.getTextContent());
				if(apTitle != null){
					ied.setApTitle(apTitle.getTextContent());
				}
				if(aeQualifier != null){
					try{
						ied.setAeQualifier(Integer.parseInt(aeQualifier.getTextContent()));
					}
					catch(NumberFormatException e) {
						ied.setAeQualifier(0);
					}
				}
				first = false;
			}
			else{
				ied.setIpAddressB(ipNode.getTextContent());
			}
		}
	}
}
