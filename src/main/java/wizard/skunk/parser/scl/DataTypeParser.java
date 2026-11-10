package wizard.skunk.parser.scl;

import org.w3c.dom.Element;
import wizard.skunk.parser.xml.XMLHelper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DataTypeParser {
	private final Element dataTypeTemplatesNode;

	// <LNodeType[@id], LNodeType>
	private final ConcurrentMap<String, LNodeType> lnTypeMap = new ConcurrentHashMap<>();
	// Map<DOType[@id], DoType>
	private final ConcurrentMap<String, DoType> doTypeMap = new ConcurrentHashMap<>();
	// Map<DAType[@id], DaType>
	private final ConcurrentMap<String, DaType> daTypeMap = new ConcurrentHashMap<>();
	// Map<EnumType[@id], EnumType>
	private final ConcurrentMap<String, EnumType> enumTypeMap = new ConcurrentHashMap<>();

	public DataTypeParser(Element dataTypeTemplatesNode) {
		this.dataTypeTemplatesNode = dataTypeTemplatesNode;
		if(dataTypeTemplatesNode != null){
			parseEnumType();
		}
	}

	public DoType getDoType(String lnTypeIndex, String doName){
		LNodeType lnType = getLNodeType(lnTypeIndex);
		if(lnType == null){
			return null;
		}

		String doIndex = lnType.getDOTypeIndex(doName);
		if(doIndex.isEmpty()){
			return null;
		}

		DoType doType = doTypeFactory(doIndex);
		String[] subDOs = doName.split("\\.");
		for(int i = 1; i < subDOs.length && doType != null; i++){
			doType = doType.getSubDoType(subDOs[i]);
		}

		return doType;
	}

	// Map<doName, DoType>
	public Map<String, DoType> getAllDoType(String lnTypeIndex) {
		LNodeType lnType = getLNodeType(lnTypeIndex);
		if (lnType == null) {
			return Map.of();
		}

		// Map<doName, doTypeIndex>
		Map<String, String> doTypeMap = lnType.getAllDOTypeIndex();
		Map<String, DoType> result = new ConcurrentHashMap<>();
		for (Map.Entry<String, String> entry : doTypeMap.entrySet()) {
			String doName = entry.getKey();
			String doTypeIndex = entry.getValue();

			DoType doType = doTypeFactory(doTypeIndex);
			result.put(doName, doType);
		}

		return result;
	}

	private LNodeType getLNodeType(String lnTypeIndex) {
		if(lnTypeMap.containsKey(lnTypeIndex)){
			return lnTypeMap.get(lnTypeIndex);
		}
		synchronized (this){
			if(!lnTypeMap.containsKey(lnTypeIndex)){
				parseLNodeType(lnTypeIndex);
			}
		}

		return lnTypeMap.get(lnTypeIndex);
	}

	public String parseEnum(String typeID, String ordinal){
		EnumType enumType = enumTypeMap.get(typeID);
		if(enumType == null){
			return ordinal;
		}

		return enumType.getEnumValue(ordinal);
	}

	public String getDoDescription(String lnTypeIndex, String doName) {
		LNodeType lnType = lnTypeMap.get(lnTypeIndex);
		if (lnType == null) {
			return null;
		}

		return lnType.getDODescription(doName);
	}

	private void parseLNodeType(String lnTypeIndex) {
		Element lnTypeNode = XMLHelper.getElementByTagNameAndAttribute(dataTypeTemplatesNode, "LNodeType", "id", lnTypeIndex);
		if(lnTypeNode == null){
			throw new IllegalArgumentException("LNodeType: " + lnTypeIndex + " does not exist.");
		}

		String id = lnTypeNode.getAttribute("id");
		String desc = lnTypeNode.getAttribute("desc");
		String lnClass = lnTypeNode.getAttribute("lnClass");

		LNodeType lnType = new LNodeType(id, lnClass, desc);

		List<Element> doNodes = XMLHelper.getElementsByTagName(lnTypeNode, "DO");
		for(Element doNode : doNodes) {
			String doName = doNode.getAttribute("name");
			String doTypeId = doNode.getAttribute("type");
			String doDesc = doNode.getAttribute("desc");

			lnType.addDoTypeIndex(doName, doTypeId, doDesc);
		}

		lnTypeMap.put(id, lnType);
	}

	private DoType doTypeFactory(String id){
		if(doTypeMap.containsKey(id)) {
			return doTypeMap.get(id);
		}

		synchronized (this){
			if(doTypeMap.containsKey(id)){
				return doTypeMap.get(id);
			}
			Element doTypeNode = XMLHelper.getElementByTagNameAndAttribute(dataTypeTemplatesNode, "DOType", "id", id);
			if(doTypeNode == null) {
				throw new IllegalArgumentException("DOType id is not exist : " + id);
			}

			String cdc = doTypeNode.getAttribute("cdc");
			DoType doType = new DoType(cdc);

			List<Element> sdoNodes = XMLHelper.getElementsByTagName(doTypeNode, "SDO");
			for(Element sdoNode : sdoNodes) {
				String sdoName = sdoNode.getAttribute("name");
				String type = sdoNode.getAttribute("type");

				DoType subDoType = doTypeFactory(type);

				doType.addChild(sdoName, "XX", subDoType);
			}

			List<Element> daNodes = XMLHelper.getElementsByTagName(doTypeNode, "DA");
			for(Element daNode : daNodes) {
				String daName = daNode.getAttribute("name");
				String bType = daNode.getAttribute("bType");
				String type = daNode.getAttribute("type");
				String fc = daNode.getAttribute("fc");

				DaType daType = daTypeFactory(bType, type);

				doType.addChild(daName, fc, daType);
			}

			doTypeMap.put(id, doType);
			return doType;
		}
	}

	private DaType daTypeFactory(String id){
		if(daTypeMap.containsKey(id)) {
			return daTypeMap.get(id);
		}

		synchronized (this){
			if(daTypeMap.containsKey(id)) {
				return daTypeMap.get(id);
			}

			Element daTypeNode = XMLHelper.getElementByTagNameAndAttribute(dataTypeTemplatesNode, "DAType", "id", id);
			if(daTypeNode == null) {
				throw new IllegalArgumentException("DAType id is not exist : " + id);
			}

			DaType daType = new DaType();
			List<Element> bdaNodes = XMLHelper.getElementsByTagName(daTypeNode, "BDA");
			for(Element bdaNode : bdaNodes) {
				String bdaName = bdaNode.getAttribute("name");
				String bType = bdaNode.getAttribute("bType");
				String type = bdaNode.getAttribute("type");

				DaType subDaType = daTypeFactory(bType, type);

				daType.addChild(bdaName, subDaType);
			}

			daTypeMap.put(id, daType);
			return daType;
		}
	}

	private DaType daTypeFactory(String bType, String type){
		DaType daType = new DaType();
		if(DaType.BasicType.Struct.name().equals(bType)) {
			daType = daTypeFactory(type);
		}
		else if(DaType.BasicType.Enum.name().equals(bType)) {
			daType.setBasicType(bType);
			daType.setEnumID(type);
		}
		else{
			daType.setBasicType(bType);
		}

		return daType;
	}

	private void parseEnumType(){
		List<Element> enumTypeNodes = XMLHelper.getElementsByTagName(dataTypeTemplatesNode, "EnumType");
		for(Element enumTypeNode : enumTypeNodes) {
			String id = enumTypeNode.getAttribute("id");

			EnumType enumType = new EnumType(id);

			List<Element> enumNodes = XMLHelper.getElementsByTagName(enumTypeNode, "EnumVal");
			for(Element enumNode : enumNodes) {
				String ordinal = enumNode.getAttribute("ord");
				String text = enumNode.getTextContent();

				enumType.addEnumValue(ordinal, text);
			}

			enumTypeMap.put(id, enumType);
		}
	}
}
