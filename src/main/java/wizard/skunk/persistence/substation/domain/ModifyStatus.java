package wizard.skunk.persistence.substation.domain;

public enum ModifyStatus {
	NO_CHANGES(0),
	CREATED(1),
	CHANGED(2),
	DELETED(3);

	public final int value;

	ModifyStatus(int value){
		this.value = value;
	}

	public static ModifyStatus getModifyByValue(int value) {
		for (ModifyStatus status : ModifyStatus.values()) {
			if (status.value == value) {
				return status;
			}
		}
		return NO_CHANGES;
	}
}
