package wizard.skunk.common;

public class StringUtil {
	public static String[] splitOnce(String str, String delimiter) {
		String[] result = new String[2];

		int index = str.indexOf(delimiter);
		if (index == -1) {
			result[0] = str;
			result[1] = "";
		} else {
			result[0] = str.substring(0, index);
			result[1] = str.substring(index + delimiter.length());
		}

		return result;
	}

	public static String firstPhase(String str, String delimiter) {
		int index = str.indexOf(delimiter);
		if (index == -1) {
			return str;
		}
		return str.substring(0, index);
	}
}
