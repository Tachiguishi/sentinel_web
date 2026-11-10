package wizard.skunk.parser.domain;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Cubicle {
	private String name;
	private String description;

	private final List<CubicleUnit> cubicleUnits = new ArrayList<>();

	public void addUnit(CubicleUnit unit){
		cubicleUnits.add(unit);
	}
}
