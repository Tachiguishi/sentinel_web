package wizard.skunk.parser.scl;

public class DataSetName {
	public static final String INPUTS = "Inputs";
	public static final String DS_SETTING = "dsSetting";
	public static final String DS_MATRIX = "dsMatrixSet";
	public static final String DS_PARAMETER = "dsParameter";

	public static boolean isReportDataSet(String name) {
		return !name.equals(INPUTS) && !isGeneralSettingDataSet(name);
	}

	public static boolean isGeneralSettingDataSet(String name){
		return name.startsWith(DS_PARAMETER) || isSettingDataSet(name);
	}

	public static boolean isSettingDataSet(String name){
		return name.startsWith(DS_SETTING) || name.startsWith(DS_MATRIX);
	}
}
