package wizard.skunk.parser.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class IVLDUnit {
	private String name;
	private String description;
	private String manufacturer;
	private String type;
	private String configVersion;
	private String crc;

	private final List<VirtualTerminal> virtualTerminalList = new ArrayList<>();
	private final List<BrokenAlarm> brokenAlarmList = new ArrayList<>();
	private final List<PlateSwitcher> plateSwitcherList = new ArrayList<>();

	public void addVirtualTerminal(List<VirtualTerminal> virtualTerminals){
		virtualTerminalList.addAll(virtualTerminals);
	}

	public void addBrokenAlarm(List<BrokenAlarm> brokenAlarms){
		brokenAlarmList.addAll(brokenAlarms);
	}

	public void addPlateSwitcher(List<PlateSwitcher> plateSwitchers){
		plateSwitcherList.addAll(plateSwitchers);
	}
}
