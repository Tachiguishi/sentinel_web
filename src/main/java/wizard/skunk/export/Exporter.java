package wizard.skunk.export;

import wizard.skunk.persistence.station.StationEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import wizard.skunk.common.IProgressMonitor;
import wizard.skunk.export.domain.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Exporter {
	protected Map<String, OptionTable> optionTableMap = new HashMap<>();

	public Exporter(){
		init("sentinel.xml");
	}

	protected void init(String fileName){
		if(fileName == null || fileName.isEmpty()){
			return;
		}
		try{
			File customFile = new File(System.getProperty("user.home") + "/.config/sentinel/" + fileName);
			InputStream fileStream;
			if(customFile.exists()){
				fileStream = new FileInputStream(customFile);
			}
			else{
				fileStream = getClass().getResourceAsStream("/persistence/" + fileName);
			}

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document document;
			try (InputStream input = fileStream) {
				document = dBuilder.parse(input);
			}
			document.getDocumentElement().normalize();

			initOptionTable(document);
		}
		catch (Exception e){
			optionTableMap = new HashMap<>();
			throw new RuntimeException("parsing config file failed: " + fileName + e.getMessage());
		}
	}

	protected void initOptionTable(Document document) throws Exception {
		NodeList tableList = document.getElementsByTagName("table");
		for(int i = 0; i < tableList.getLength(); i++){
			Element tableNode = (Element) tableList.item(i);
			String valid = tableNode.getAttribute("valid");
			if("false".equals(valid)){
				continue;
			}

			String id = tableNode.getAttribute("id");
			String name = tableNode.getAttribute("name");

			OptionTable optionTable = new OptionTable();
			optionTable.setKey(id);
			optionTable.setTableName(name);

			NodeList columnList = tableNode.getElementsByTagName("column");
			for(int j = 0; j < columnList.getLength(); j++){
				Element columnNode = (Element) columnList.item(j);
				valid = columnNode.getAttribute("valid");
				if("false".equals(valid)){
					continue;
				}

				String columnId = columnNode.getAttribute("id");
				String columnName = columnNode.getAttribute("name");
				String columnType = columnNode.getAttribute("type");

				OptionColumn optionColumn = new OptionColumn();
				optionColumn.setKey(columnId);
				optionColumn.setColumnName(columnName);
				optionColumn.setType(columnType);

				optionTable.addColumn(optionColumn);
			}

			optionTableMap.put(id, optionTable);
		}
	}

	public void exportModel(OutputStream outputStream, IProgressMonitor monitor, StationEntity substation,
							ModelData model) throws Exception {
		String code = substation.getName();
		List<IDataTransfer<?>> tables = List.of(
				new TB1046DAO(model, code), new TB1006DAO(model, code), new TB1016DAO(model, code),
				new TB1026DAO(model, code), new TB1054DAO(model, code), new TB1036DAO(model, code),
				new TB1058DAO(model, code));

		String prefix = "";

		monitor.beginTask("导出至模型", tables.size());
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.newDocument();

		Element rootNode = doc.createElement("substation");
		rootNode.setAttribute("code", substation.getName());
		rootNode.setAttribute("name", substation.getName());
		rootNode.setAttribute("desc", java.util.Objects.toString(substation.getDescription(), ""));
		doc.appendChild(rootNode);

		Element networkNode = doc.createElement("network");
		networkNode.setAttribute("code", substation.getName());
		networkNode.setAttribute("l1_ip_a", java.util.Objects.toString(substation.getIpAddress(), ""));
		networkNode.setAttribute("l2_ip_a", java.util.Objects.toString(substation.getIpAddress(), ""));
		networkNode.setAttribute("instance",  String.valueOf(substation.getInstantNo() == null ? 3 : substation.getInstantNo()));
		networkNode.setAttribute("protocol_type", "3");
		networkNode.setAttribute("substation_type", "2");
		rootNode.appendChild(networkNode);

		List<TB1046IED> deviceList = new ArrayList<>();
		Map<String, List<TB1006Analog>> analogMap = new HashMap<>();
		Map<String, List<TB1016Status>> statusMap = new HashMap<>();
		Map<String, List<TB1026String>> stringMap = new HashMap<>();
		Map<String, List<TB1054RCB>> rcbMap = new HashMap<>();
		Map<String, List<TB1036Setting>> settingMap = new HashMap<>();
		Map<String, List<TB1058MMS>> mmsFcdaMap = new HashMap<>();
		TB1046DAO iedDao = null;
		TB1006DAO analogDao = null;
		TB1016DAO statusDao = null;
		TB1026DAO stringDao = null;
		TB1054DAO rcbDao = null;
		TB1036DAO settingDao = null;
		TB1058DAO mmsFcdaDao = null;
		for(IDataTransfer<?> dao : tables){
			String targetName = dao.getClass().getSimpleName();
			monitor.subTask(targetName);
			OptionTable options = optionTableMap.get(targetName);
			if (options == null) {
				throw new IllegalStateException("缺少模型导出配置：" + targetName);
			}
			dao.setOptionTable(options);

			if(targetName.equals(TB1046DAO.class.getSimpleName())){
				iedDao = (TB1046DAO) dao;
				deviceList = iedDao.transfer(prefix);
			}
			else if(targetName.equals(TB1006DAO.class.getSimpleName())){
				analogDao = (TB1006DAO) dao;
				analogDao.transferWithParent(analogMap, prefix);
			}
			else if(targetName.equals(TB1016DAO.class.getSimpleName())){
				statusDao = (TB1016DAO) dao;
				statusDao.transferWithParent(statusMap, prefix);
			}
			else if(targetName.equals(TB1026DAO.class.getSimpleName())){
				stringDao = (TB1026DAO) dao;
				stringDao.transferWithParent(stringMap, prefix);
			}
			else if(targetName.equals(TB1054DAO.class.getSimpleName())){
				rcbDao = (TB1054DAO) dao;
				rcbDao.transferWithParent(rcbMap, prefix);
			}
			else if(targetName.equals(TB1036DAO.class.getSimpleName())){
				settingDao = (TB1036DAO) dao;
				settingDao.transferWithParent(settingMap, prefix);
			}
			else if(targetName.equals(TB1058DAO.class.getSimpleName())){
				mmsFcdaDao = (TB1058DAO) dao;
				mmsFcdaDao.transferWithParent(mmsFcdaMap, prefix);
			}
			monitor.worked(1);
		}

		for(TB1046IED device : deviceList) {
			Element iedNode = doc.createElement("ied");
			iedDao.setElementAttribute(device, iedNode);
			rootNode.appendChild(iedNode);

			if(analogMap.containsKey(device.getCode())){
				Element analogNode = doc.createElement("analog");
				iedNode.appendChild(analogNode);

				List<TB1006Analog> analogList = analogMap.get(device.getCode());
				for(TB1006Analog analog : analogList){
					Element data = doc.createElement("data");
					analogDao.setElementAttribute(analog, data);
					analogNode.appendChild(data);
				}
			}

			if(statusMap.containsKey(device.getCode())){
				Element statusNode = doc.createElement("status");
				iedNode.appendChild(statusNode);

				List<TB1016Status> statusList = statusMap.get(device.getCode());
				for(TB1016Status status : statusList){
					Element data = doc.createElement("data");
					statusDao.setElementAttribute(status, data);
					statusNode.appendChild(data);
				}
			}

			if(stringMap.containsKey(device.getCode())){
				Element stringNode = doc.createElement("string");
				iedNode.appendChild(stringNode);

				List<TB1026String> stringList = stringMap.get(device.getCode());
				for(TB1026String string : stringList){
					Element data = doc.createElement("data");
					stringDao.setElementAttribute(string, data);
					stringNode.appendChild(data);
				}
			}

			if(rcbMap.containsKey(device.getCode())){
				Element rcbNode = doc.createElement("rcb");
				iedNode.appendChild(rcbNode);

				List<TB1054RCB> rcbList = rcbMap.get(device.getCode());
				for(TB1054RCB rcb : rcbList){
					Element data = doc.createElement("data");
					rcbDao.setElementAttribute(rcb, data);
					rcbNode.appendChild(data);
				}
			}

			if(settingMap.containsKey(device.getCode())){
				Element settingNode = doc.createElement("setting");
				iedNode.appendChild(settingNode);

				List<TB1036Setting> settingList = settingMap.get(device.getCode());
				for(TB1036Setting setting : settingList){
					Element data = doc.createElement("data");
					settingDao.setElementAttribute(setting, data);
					settingNode.appendChild(data);
				}
			}

			if(mmsFcdaMap.containsKey(device.getCode())){
				Element mmsFcdaNode = doc.createElement("mmsFcda");
				iedNode.appendChild(mmsFcdaNode);

				List<TB1058MMS> mmsFcdaList = mmsFcdaMap.get(device.getCode());
				for(TB1058MMS mmsFcda : mmsFcdaList){
					Element data = doc.createElement("data");
					mmsFcdaDao.setElementAttribute(mmsFcda, data);
					mmsFcdaNode.appendChild(data);
				}
			}
		}

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

		DOMSource source = new DOMSource(doc);
		transformer.transform(source, new StreamResult(outputStream));
		monitor.done();
	}
}
