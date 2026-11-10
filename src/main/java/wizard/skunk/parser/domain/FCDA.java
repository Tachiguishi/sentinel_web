package wizard.skunk.parser.domain;

import lombok.Data;

import javax.swing.*;

@Data
public class FCDA {
	private int index;

	private String ldInst;
	private String prefix;
	private String lnClass;
	private String lnInst;
	private String doName;
	private String daName;
	private String fc;
	private String description;

	private String lnName;
	private String dollarReference;
	private String dotReference;

	private String cdc;
	// unit,minValue,maxValue,stepSize,multiplier
	private String unitSerialization = "";

	public void generateReference(){
		lnName = prefix + lnClass + lnInst;
		String first = ldInst + Split.Slash + lnName + Split.Dot;
		String second = doName;
		if(daName != null && !daName.isEmpty()){
			second += Split.Dot + daName;
		}
		dotReference = (first + second).replace(Split.Dollar.toString(), Split.Dot.toString());
		dollarReference = (first + fc + Split.Dollar + second).replace(Split.Dot.toString(), Split.Dollar.toString());
	}

	@Override
	public String toString(){
		return description + "(" + getDotReference() + ")" + "-" + cdc + "-" + unitSerialization;
	}
}
