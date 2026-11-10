package wizard.skunk.parser.scl;

import lombok.Getter;
import wizard.skunk.common.StringUtil;
import wizard.skunk.parser.domain.Split;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoType extends BaseDataType{
	@Getter
	private final String cdc;

	Map<TypeIdentifier, BaseDataType> children = new HashMap<>();

	public DoType(String cdc) {
		this.cdc = cdc;
	}

	public void addChild(String name, String fc, BaseDataType type) {
		children.put(new TypeIdentifier(name, fc), type);
	}

	public DoType getSubDoType(String name) {
		for(Map.Entry<TypeIdentifier, BaseDataType> entry : children.entrySet()){
			if(entry.getKey().isTargetName(name)) {
				BaseDataType target = entry.getValue();
				if(target instanceof DoType) {
					return (DoType) target;
				}
			}
		}

		return null;
	}

	// Map<doName, DoType>
	public Map<String, DoType> getAllSubDoType(){
		Map<String, DoType> subDoTypes = new HashMap<>();
		for(Map.Entry<TypeIdentifier, BaseDataType> entry : children.entrySet()){
			if(entry.getValue() instanceof DoType subDoType){
				subDoTypes.put(entry.getKey().name, subDoType);
			}
		}
		return subDoTypes;
	}

	public boolean isDaNameExist(String name){
		for(Map.Entry<TypeIdentifier, BaseDataType> entry : children.entrySet()){
			if(entry.getKey().isTargetName(name)) {
				return true;
			}
		}

		return false;
	}

	public ControlModel getControlModel(){
		if(!CDC.SPC.name().equals(cdc) && !CDC.DPC.name().equals(cdc)) {
			return ControlModel.StatusOnly;
		}

		boolean operate = false;
		boolean select = false;
		for(Map.Entry<TypeIdentifier, BaseDataType> entry : children.entrySet()){
			if(entry.getKey().isTargetName("Oper")){
				operate = true;
			}
			else if(entry.getKey().isTargetName("SBOw")){
				select = true;
			}
		}

		if(!operate){
			return ControlModel.StatusOnly;
		}

		if(select){
			return ControlModel.SelectBeforeOperate;
		}

		return ControlModel.DirectWithNormalSecurity;
	}

	@Override
	public String getDaReference(String FC){
		for(Map.Entry<TypeIdentifier, BaseDataType> entry : children.entrySet()){
			if(entry.getKey().isTargetFC(FC)){
				if(entry.getKey().name.equals("q") || entry.getKey().name.equals("t")){
					continue;
				}
				BaseDataType dataType = entry.getValue();
				if(!dataType.isStruct()){
					return entry.getKey().name;
				}
				return entry.getKey().name + "$" + dataType.getDaReference(FC);
			}

		}
		return "";
	}

	public boolean existDaReference(String daName, String FC){
		for(Map.Entry<TypeIdentifier, BaseDataType> entry : children.entrySet()){
			if(entry.getKey().isTargetFC(FC) && entry.getKey().name.equals(daName)){
				return true;
			}
		}

		return false;
	}

	@Override
	protected String typeString() {
		return "\"cdc\":\"" + cdc + "\"";
	}

	@Override
	protected boolean isStruct() {
		return true;
	}

	@Override
	public String toString() {
		return "{\"cdc\":\"" + cdc + "\",\"children\":" + serialize("XX", "") + "}";
	}

	@Override
	public String serialize(String targetFC, String reference) {
		List<String> childrenString = new ArrayList<>(children.size());
		String[] typeSplit = StringUtil.splitOnce(reference, Split.Dot.toString());

		for(Map.Entry<TypeIdentifier, BaseDataType> entry : children.entrySet()) {
			if(!entry.getKey().isTargetFC(targetFC) || !entry.getKey().isTargetName(typeSplit[0])) {
				continue;
			}
			String name = "\"name\":\"" + entry.getKey().name + "\"";
			String fc = "\"fc\":\"" + entry.getKey().fc + "\"";
			String type = entry.getValue().typeString();
			if(entry.getValue().isStruct()) {
				String children = "\"children\":" + entry.getValue().serialize(targetFC, typeSplit[1]);
				childrenString.add("{" + name + "," + fc + "," + type + "," + children + "}");
			}
			else{
				childrenString.add("{" + name + "," + fc + "," + type + "}");
			}
		}
		return "[" + String.join(",", childrenString) + "]";
	}

	public String getUnitEnumID(String daName){
		for(Map.Entry<TypeIdentifier, BaseDataType> entry : children.entrySet()){
			if(!entry.getKey().isTargetFC("CF") || !entry.getKey().isTargetName("units")){
				continue;
			}

			BaseDataType dataType = entry.getValue();
			if(!dataType.isStruct()){
				continue;
			}

			DaType daType = (DaType)dataType;
			DaType enumType = daType.getChild(daName);
			if(enumType == null){
				continue;
			}

			return enumType.getEnumID();
		}

		return "";
	}

	public int getFCDAType(String fc){
		FC fcType = FC.valueOf(fc);

		DaType.BasicType basicType = CDC.getBasicType(cdc, fcType);
		if(basicType == null){
			return 0;
		}

		switch (basicType){
			case BOOLEAN, Enum, INT32, INT32U, INT64, INT8 -> {
//				return FCDAType.INT.value;
				return 1;
			}
			case FLOAT32 -> {
//				return FCDAType.FLOAT.value;
				return 2;
			}
			case VisString255, Unicode255 -> {
//				return FCDAType.STRING.value;
				return 3;
			}
			default ->{
				return 0;
			}
		}
	}

	private record TypeIdentifier(String name, String fc) {

		public boolean isTargetFC(String targetFC) {
			return fc.equals(targetFC) || targetFC.equals("XX") || fc.equals("XX");
		}

		public boolean isTargetName(String targetName) {
			return targetName == null || targetName.isEmpty() || name.equals(targetName);
		}
	}

	public enum CDC{
		// status information
		SPS("Single point status", Map.of(FC.ST, DaType.BasicType.BOOLEAN)),
		DPS("Double point status", Map.of(FC.ST, DaType.BasicType.Enum)),
		INS("Integer status", Map.of(FC.ST, DaType.BasicType.INT32)),
		ENS("Enumerated status", Map.of(FC.ST, DaType.BasicType.Enum)),
		ACT("Protection activation information", Map.of(FC.ST, DaType.BasicType.BOOLEAN)),
		ACD("Directional protection activation information", Map.of(FC.ST, DaType.BasicType.BOOLEAN)),
		SEC("Security violation counting", Map.of(FC.ST, DaType.BasicType.INT32)),
		BCR("Binary counter reading", Map.of(FC.ST, DaType.BasicType.INT64)),
		HST("Histogram", Map.of(FC.ST, DaType.BasicType.INT32)),
		VSS("Visible string status", Map.of(FC.ST, DaType.BasicType.VisString255)),
		// measured information
		MV("Measured value", Map.of(FC.MX, DaType.BasicType.FLOAT32)),
		CMV("Complex measured value", Map.of(FC.MX, DaType.BasicType.FLOAT32)),
		SAV("Sampled value", Map.of(FC.MX, DaType.BasicType.FLOAT32)),
		WYE("Phase to ground/neutral related measured values of a three-phase system", Map.of(FC.MX, DaType.BasicType.FLOAT32)),
		DEL("Phase to phase related measured values of a three-phase system", Map.of(FC.MX, DaType.BasicType.FLOAT32)),
		SEQ("Sequence", Map.of(FC.MX, DaType.BasicType.FLOAT32)),
		HWYE("Harmonic value for WYE", Map.of(FC.MX, DaType.BasicType.FLOAT32)),
		HDEL("Harmonic value for DEL", Map.of(FC.MX, DaType.BasicType.FLOAT32)),
		// controls
		SPC("Controllable single point", Map.of(FC.ST, DaType.BasicType.BOOLEAN, FC.CO, DaType.BasicType.BOOLEAN)),
		DPC("Controllable double point", Map.of(FC.ST, DaType.BasicType.Enum)),
		INC("Controllable integer status", Map.of(FC.ST, DaType.BasicType.INT32)),
		ENC("Controllable enumerated status", Map.of(FC.ST, DaType.BasicType.Enum)),
		BSC("Binary controlled step position information", Map.of(FC.ST, DaType.BasicType.BOOLEAN)),
		ISC("Integer controlled step position information", Map.of(FC.ST, DaType.BasicType.INT32)),
		APC("Controllable analogue process value", Map.of(FC.MX, DaType.BasicType.FLOAT32)),
		BAC("Binary controlled analog process value", Map.of(FC.MX, DaType.BasicType.FLOAT32)),
		// status settings
		SPG("Single point setting", Map.of(FC.SP, DaType.BasicType.BOOLEAN,
				FC.SG, DaType.BasicType.BOOLEAN,
				FC.SE, DaType.BasicType.BOOLEAN)),
		ING("Integer status setting", Map.of(FC.SP, DaType.BasicType.INT32, FC.SG, DaType.BasicType.INT32, FC.SE, DaType.BasicType.INT32)),
		ENG("Enumerated status setting", Map.of(FC.SP, DaType.BasicType.Enum, FC.SG, DaType.BasicType.Enum, FC.SE, DaType.BasicType.Enum)),
		ORG("Object reference setting", Map.of(FC.SP, DaType.BasicType.ObjRef, FC.SG, DaType.BasicType.ObjRef, FC.SE, DaType.BasicType.ObjRef)),
		TSG("Time setting group", Map.of(FC.SP, DaType.BasicType.Timestamp, FC.SG, DaType.BasicType.Timestamp, FC.SE, DaType.BasicType.Timestamp)),
		CUG("Currency setting group", Map.of(FC.SP, DaType.BasicType.Currency, FC.SG, DaType.BasicType.Currency, FC.SE, DaType.BasicType.Currency)),
		VSG("Visible string setting", Map.of(FC.SP, DaType.BasicType.VisString255, FC.SG, DaType.BasicType.VisString255, FC.SE, DaType.BasicType.VisString255)),
		// analogue settings
		ASG("Analogue setting", Map.of(FC.SP, DaType.BasicType.FLOAT32, FC.SG, DaType.BasicType.FLOAT32, FC.SE, DaType.BasicType.FLOAT32)),
		CURVE("Setting curve", Map.of(FC.SP, DaType.BasicType.FLOAT32, FC.SG, DaType.BasicType.FLOAT32, FC.SE, DaType.BasicType.FLOAT32)),
		// CSG("Curve shape setting", Map.of(FC.SP, DaType.BasicType.FLOAT32, FC.SG, DaType.BasicType.FLOAT32, FC.SE, DaType.BasicType.FLOAT32)),
		// description information
		DPL("Device name plate", Map.of(FC.DC, DaType.BasicType.VisString255)),
		LPL("Logical node name plate", Map.of(FC.DC, DaType.BasicType.VisString255)),
		CSD("Curve shape description", Map.of(FC.DC, DaType.BasicType.VisString255)),

		STG("String setting", Map.of(FC.SP, DaType.BasicType.Unicode255, FC.SG, DaType.BasicType.Unicode255, FC.SE, DaType.BasicType.Unicode255)),
		NST("", Map.of(FC.ST, DaType.BasicType.INT32U)),
		STS("String status", Map.of(FC.ST, DaType.BasicType.Unicode255));

		public final String description;
		public final Map<FC, DaType.BasicType> typeMap;

		CDC(String description, Map<FC, DaType.BasicType> typeMap) {
			this.description = description;
			this.typeMap = typeMap;
		}

		public static DaType.BasicType getBasicType(String cdc, FC fc) {
			CDC cdcEnum = fromString(cdc);
			if (cdcEnum == null) {
				return null;
			}
			return getBasicType(cdcEnum, fc);
		}

		public static DaType.BasicType getBasicType(CDC cdc, FC fc) {
			return cdc.typeMap.get(fc);
		}

		public static CDC fromString(String cdc){
			for(CDC c : CDC.values()){
				if(c.name().equals(cdc)){
					return c;
				}
			}

			return null;
		}

		public boolean isSame(String cdc){
			return name().equals(cdc);
		}

		public static boolean isDoublePoint(String cdc){
			return DPS.isSame(cdc) || DPC.isSame(cdc);
		}
	}

	public enum FC{
		XX,	// generic FC
		ST,
		MX,
		CO,
		SP,
		SG,
		SE,
		SV,
		CF,
		DC,
		EX
	}

	public enum ControlModel{
		StatusOnly,
		DirectWithNormalSecurity,
		SelectBeforeOperate,
		DirectWithEnhancedSecurity,
		SelectBeforeOperateWithEnhancedSecurity
	}
}
