package wizard.skunk.parser.domain;

public enum Split {
	Slash("/"),
	Dot("."),
	Dollar("$"),
	Comma(",");

	private final String character;

	Split(String character) {
		this.character = character;
	}

	@Override
	public String toString(){
		return character;
	}
}
