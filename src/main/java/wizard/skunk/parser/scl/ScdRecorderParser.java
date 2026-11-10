package wizard.skunk.parser.scl;

import org.w3c.dom.Element;
import wizard.skunk.common.IProgressMonitor;
import wizard.skunk.parser.domain.CDCMap;
import wizard.skunk.parser.domain.DataSet;
import wizard.skunk.parser.domain.FCDA;
import wizard.skunk.parser.domain.IED;
import wizard.skunk.parser.xml.XMLHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScdRecorderParser extends ScdParser{
	private final List<FCDA> cslkFCDAList = new ArrayList<>();
	private final List<CDCMap> cdcMapList = new ArrayList<>();

	public ScdRecorderParser(Element rootElement, IProgressMonitor monitor) {
		super(rootElement, monitor);
	}

	@Override
	protected Map<String, Element> parseLogicalNode(IED ied, String apName, Element lDeviceNode){
		cslkFCDAList.clear();
		Map<String, Element> logicalNodeMap = super.parseLogicalNode(ied, apName, lDeviceNode);

		String ldName = lDeviceNode.getAttribute("inst");
		if(!cslkFCDAList.isEmpty()){
			DataSet cslkDataSet = new DataSet();
			cslkDataSet.setName("CSLK");
			cslkDataSet.setDescription("CSLK数据集");
			cslkDataSet.setAccessPoint(apName);
			cslkDataSet.setLdName(ldName);
			cslkDataSet.setLnClass("LLN0");

			for(FCDA fcda : cslkFCDAList){
				ied.addFCDA(fcda, cslkDataSet);
			}

			ied.addDataSet(cslkDataSet);
		}

		return logicalNodeMap;
	}

	@Override
	protected void parseTraitLogicalNode(IED ied, String ldName, String prefix, String lnClass, String inst, Element lnNode) {
		parseAllCDC(ied, ldName + "/" + prefix + lnClass + inst, lnNode);

		if(!lnClass.equals("CSLK")){
			return;
		}

		parseCSLKFCDA(lnNode, ldName, prefix, lnClass, inst, "Lnk1CBRef");
		parseCSLKFCDA(lnNode, ldName, prefix, lnClass, inst, "Lnk2CBRef");
	}

	private void parseAllCDC(IED ied, String lnReference, Element lnNode){
		String lnTypeIndex = lnNode.getAttribute("lnType");

		cdcMapList.clear();
		Map<String, DoType> doTypeMaps = dataTypeParser.getAllDoType(lnTypeIndex);
		parseCDCMap(lnReference, doTypeMaps);
		ied.addCDCMapList(cdcMapList);
	}

	private void parseCDCMap(String lnReference, Map<String, DoType> doTypeMaps) {
		for(Map.Entry<String, DoType> entry : doTypeMaps.entrySet()) {
			String doName = entry.getKey();
			DoType doType = entry.getValue();

			CDCMap cdcMap = new CDCMap();
			cdcMap.setReference(lnReference + "." + doName);
			cdcMap.setCdc(doType.getCdc());
			cdcMapList.add(cdcMap);

			parseCDC(lnReference + "." + doName, doType);
		}
	}

	private void parseCDC(String parent, DoType doType){
		Map<String, DoType> subDoTypeMap = doType.getAllSubDoType();
		parseCDCMap(parent, subDoTypeMap);
	}

	private void parseCSLKFCDA(Element lnNode, String ldName, String prefix, String lnClass, String inst, String doName){
		String lnTypeIndex = lnNode.getAttribute("lnType");

		Element doiNode = XMLHelper.getElementByTagNameAndAttribute(lnNode, "DOI", "name", doName);
		if(doiNode == null){
			return;
		}

		FCDA fcda = new FCDA();
		fcda.setLdInst(ldName);
		fcda.setPrefix(prefix);
		fcda.setLnClass(lnClass);
		fcda.setLnInst(inst);
		fcda.setDoName(doName);
		fcda.setDaName("setVal");
		fcda.setFc("SP");

		fcda.generateReference();

		String cdc = dataTypeParser.getDoType(lnTypeIndex, doName).getCdc();
		fcda.setCdc(cdc);

		Element daiNode = XMLHelper.getElementByTagNameAndAttribute(doiNode, "DAI", "name", "setVal");
		if(daiNode != null){
			Element valNode = XMLHelper.getElementByTagName(daiNode, "Val");
			if(valNode != null && !valNode.getTextContent().isEmpty()){
				fcda.setDescription(valNode.getTextContent());
			}
		}

		if(fcda.getDescription() == null || fcda.getDescription().isEmpty()){
			daiNode = XMLHelper.getElementByTagNameAndAttribute(doiNode, "DAI", "name", "dU");
			if(daiNode != null){
				Element valNode = XMLHelper.getElementByTagName(daiNode, "Val");
				if(valNode != null && !valNode.getTextContent().isEmpty()){
					fcda.setDescription(valNode.getTextContent());
				}
			}
		}

		cslkFCDAList.add(fcda);
	}
}
