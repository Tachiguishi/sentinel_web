package wizard.skunk.parser.scl;

public abstract class BaseDataType {
	protected abstract String typeString();
	protected abstract boolean isStruct();

	public abstract String getDaReference(String FC);

	public abstract String serialize(String targetFC, String reference);
}
