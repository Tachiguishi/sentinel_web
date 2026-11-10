package wizard.skunk.parser.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class XMLHelper {

	public static Element getRootElement(File xmlFile) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		return factory.newDocumentBuilder().parse(xmlFile).getDocumentElement();
	}

	public static Element getRootElement(byte[] bytes) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringElementContentWhitespace(true);
		return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes)).getDocumentElement();
	}

	public static Document generateNewDocument(Element rootElement) throws ParserConfigurationException {
		Document document = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument();
		if(rootElement != null){
			document.appendChild(document.importNode(rootElement, true));
		}
		return document;
	}

	public static void save2File(Document document, File outputFile) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer(new StreamSource(
				XMLHelper.class.getResourceAsStream("/xml/default.xslt")));

		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
//		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

		transformer.transform(new DOMSource(document), new StreamResult(outputFile));
	}

	public static ByteArrayInputStream convertDocumentToInputStream(Document document) throws Exception {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "no");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

		StringWriter writer = new StringWriter();

		transformer.transform(new DOMSource(document), new StreamResult(writer));
		String xmlString = writer.toString();

		return new ByteArrayInputStream(xmlString.getBytes());
	}

	public static Element getElementByTagName(Element parent, String tagName) {
		NodeList nodeList = parent.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i) instanceof Element element) {
				if (element.getTagName().equals(tagName)) {
					return element;
				}
			}
		}
		return null;
	}

	public static Element getElementByTagNameAndAttribute(Element parent, String tagName, String attribute, String value){
		NodeList nodeList = parent.getElementsByTagName(tagName);
		for(int i = 0; i < nodeList.getLength(); i++){
			Element element = (Element) nodeList.item(i);
			if(element.getAttribute(attribute).equals(value)){
				return element;
			}
		}
		return null;
	}

	public static List<Element> getElementsByTagName(Element parent, String tagName) {
		List<Element> result = new ArrayList<>();

		NodeList nodeList = parent.getElementsByTagName(tagName);
		for (int i = 0; i < nodeList.getLength(); i++) {
			result.add((Element) nodeList.item(i));
		}

		return result;
	}

	public static List<Element> getElementsByTagNameAndAttribute(Element parent, String tagName, String attribute, String value){
		List<Element> result = new ArrayList<>();
		NodeList nodeList = parent.getElementsByTagName(tagName);
		for(int i = 0; i < nodeList.getLength(); i++){
			Element element = (Element) nodeList.item(i);
			if(element.getAttribute(attribute).equals(value)){
				result.add(element);
			}
		}
		return result;
	}

	public static Element getLastElementByTagName(Element parent, String tagName) {
		NodeList nodeList = parent.getElementsByTagName(tagName);
		if (nodeList.getLength() > 0) {
			return (Element) nodeList.item(nodeList.getLength() - 1);
		}
		return null;
	}

	public static Element selectSingleNode(Element parent, String xpath) {
		try {
			XPath xPath = XPathFactory.newInstance().newXPath();
			return (Element) xPath.compile(xpath).evaluate(parent, javax.xml.xpath.XPathConstants.NODE);
		} catch (Exception e) {
			return null;
		}
	}

	public static List<Element> selectNodes(Element parent, String xpath) {
		List<Element> result = new ArrayList<>();
		try {
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodeList = (NodeList) xPath.compile(xpath).evaluate(parent, javax.xml.xpath.XPathConstants.NODESET);
			for (int i = 0; i < nodeList.getLength(); i++) {
				result.add((Element) nodeList.item(i));
			}
		} catch (Exception ignore) {
		}
		return result;
	}

	public static void insertAfter(Element existingElement, Element newElement) {
		if (existingElement == null || newElement == null) {
			return;
		}
		Document ownerDocument = existingElement.getOwnerDocument();
		if (ownerDocument != newElement.getOwnerDocument()) {
			newElement = (Element) ownerDocument.importNode(newElement, true);
		}
		Element parent = (Element) existingElement.getParentNode();
		if (parent != null) {
			parent.insertBefore(newElement, existingElement.getNextSibling());
		}
	}
}
