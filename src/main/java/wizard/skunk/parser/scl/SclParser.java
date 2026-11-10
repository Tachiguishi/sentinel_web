package wizard.skunk.parser.scl;

import lombok.Getter;
import org.w3c.dom.Element;
import wizard.skunk.common.IProgressMonitor;
import wizard.skunk.common.StandardMonitor;
import wizard.skunk.parser.domain.*;
import wizard.skunk.parser.xml.XMLHelper;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.XMLConstants;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
public class SclParser {
	private SCDVersion scdVersion = new SCDVersion();
	private List<IED> iedList = new ArrayList<>();
	private List<IMCDUnit> imcdUnitList = new ArrayList<>();
	private List<IVLDUnit> ivldUnitList = new ArrayList<>();
	private List<Region> regionList = new ArrayList<>();
	private List<OpticalFiber> opticalFiberList = new ArrayList<>();

	public List<IED> parseSCD(File scdFile, IProgressMonitor monitor) throws Exception {
		if(monitor == null){
			monitor = new StandardMonitor();
		}

		Element rootElement = getRootElement(scdFile);
		if (!"SCL".equals(rootElement.getTagName())) {
			throw new IllegalArgumentException("SCL root element required");
		}

		ScdParser parser = new ScdParser(rootElement, monitor);
		parser.parse();
		scdVersion = parser.getScdVersion();

		return parser.getIedList();
	}

	public List<IED> parseRecorderSCD(File scdFile, IProgressMonitor monitor) throws Exception {
		if(monitor == null){
			monitor = new StandardMonitor();
		}

		Element rootElement = getRootElement(scdFile);

		ScdParser parser = new ScdRecorderParser(rootElement, monitor);
		parser.parse();
		scdVersion = parser.getScdVersion();

		return parser.getIedList();
	}

	public IED parseCID(File cidFile, IProgressMonitor monitor) throws Exception{
		if(monitor == null){
			monitor = new StandardMonitor();
		}

		Element rootElement = getRootElement(cidFile);

		ScdParser parser = new ScdParser(rootElement, monitor);
		parser.parse();

		List<IED> iedList = parser.getIedList();
		if(iedList.isEmpty()){
			return null;
		}

		return iedList.get(0);
	}

	public List<IACDUnit> parseIACD(List<File> iacdFiles, IProgressMonitor monitor) throws Exception {
		if(monitor == null){
			monitor = new StandardMonitor();
		}

		List<IACDUnit> iacdUnitList = new ArrayList<>();
		for(File iacdFile : iacdFiles){
			Element rootElement = getRootElement(iacdFile);
			IACDParser parser = new IACDParser(rootElement, monitor);
			parser.parse();
			iacdUnitList.addAll(parser.getIacdUnitList());
		}

		return iacdUnitList;
	}

	public void parseSPD(File spdFile, IProgressMonitor monitor) throws Exception {
		if(monitor == null){
			monitor = new StandardMonitor();
		}

		Element rootElement = getRootElement(spdFile);

		SPDParser parser = new SPDParser(rootElement, monitor);
		parser.parse();

		regionList = parser.getRegions();
		opticalFiberList = parser.getOpticalFibers();
	}

	public void parseSMCD(File smcdFile, IProgressMonitor monitor) throws Exception {
		if(monitor == null){
			monitor = new StandardMonitor();
		}

		Element rootElement = getRootElement(smcdFile);

		Element scdElement = XMLHelper.getElementByTagName(rootElement, "SCL");
		if(scdElement == null){
			throw new IllegalArgumentException("SCL node not found");
		}

		ScdParser scdParser = new ScdParser(scdElement, monitor);
		scdParser.parse();
		iedList = scdParser.getIedList();

		Element imcdElement = XMLHelper.getElementByTagName(rootElement, "IMCD");
		IMCDParser imcdParser = new IMCDParser(imcdElement, monitor);
		imcdParser.parse();
		imcdUnitList = imcdParser.getImcdUnitList();

		Element ivldElement = XMLHelper.getElementByTagName(rootElement, "IVLD");
		IVLDParser ivldParser = new IVLDParser(ivldElement, monitor);
		ivldParser.parse();
		ivldUnitList = ivldParser.getIvldUnitList();

		Element spcdElement = XMLHelper.getElementByTagName(rootElement, "Substation");
		SPCDParser spcdParser = new SPCDParser(spcdElement, monitor);
		spcdParser.parse();
		regionList = spcdParser.getRegionList();
		opticalFiberList = spcdParser.getOpticalFibers();
	}

	private Element getRootElement(File xmlFile) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

		return factory.newDocumentBuilder().parse(xmlFile).getDocumentElement();
	}
}
