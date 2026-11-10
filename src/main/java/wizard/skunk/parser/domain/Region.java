package wizard.skunk.parser.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Region {
	private String name;
	private String description;

	private final List<Cubicle> cubicleList = new ArrayList<>();

	public void addCubicle(Cubicle cubicle){
		cubicleList.add(cubicle);
	}
}
