package wizard.skunk.export.domain;

import wizard.skunk.parser.scl.DoType;
import wizard.skunk.persistence.substation.domain.*;

import java.util.*;

/** A detached, consistent snapshot of one station's model. */
public class ModelData {
	public final List<Device> devices;
	public final List<ReportControl> reports;
	public final List<DataItem> items;
	private final Map<Long, List<DataItem>> itemsByDataSet = new HashMap<>();

	public ModelData(List<Device> devices, List<ReportControl> reports, List<DataItem> items) {
		this.devices = devices.stream().filter(device -> !device.isIgnored()).toList();
		Set<Long> deviceIds = new HashSet<>();
		this.devices.forEach(device -> deviceIds.add(device.getId()));
		this.reports = reports.stream().filter(report -> report.getDevice() != null
				&& deviceIds.contains(report.getDevice().getId()) && report.getDataSet() != null).toList();
		this.items = items.stream().filter(item -> item.getDataSet() != null && isValue(item.getFcda())
				&& item.getDataSet().getDevice() != null
				&& deviceIds.contains(item.getDataSet().getDevice().getId())
				&& item.getFcda().getDevice() != null
				&& deviceIds.contains(item.getFcda().getDevice().getId())).toList();
		this.items.forEach(item -> itemsByDataSet.computeIfAbsent(item.getDataSet().getId(),
				ignored -> new ArrayList<>()).add(item));
	}

	public List<DataItem> items(DataSet dataSet) {
		return itemsByDataSet.getOrDefault(dataSet.getId(), List.of());
	}

	public List<FCDA> reportPoints(int type) {
		Map<Long, FCDA> points = new LinkedHashMap<>();
		for (ReportControl report : reports) {
			for (DataItem item : items(report.getDataSet())) {
				FCDA point = item.getFcda();
				if (dataType(point) == type) points.putIfAbsent(point.getId(), point);
			}
		}
		return new ArrayList<>(points.values());
	}

	public static int dataType(FCDA point) {
		if (point.getCdc() == null || point.getFc() == null) return 0;
		try {
			return new DoType(point.getCdc()).getFCDAType(point.getFc());
		} catch (IllegalArgumentException exception) {
			return 0;
		}
	}

	public static boolean isValue(FCDA point) {
		if (point == null) return false;
		String da = point.getDaName();
		return da == null || !(da.equals("q") || da.equals("t") || da.endsWith(".q")
				|| da.endsWith(".t") || da.endsWith("$q") || da.endsWith("$t"));
	}

	public static String unit(FCDA point) {
		return point.getUnitSerialization() == null ? "" : point.getFullUnit();
	}
}
