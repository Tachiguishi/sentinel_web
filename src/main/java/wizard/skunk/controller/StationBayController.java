package wizard.skunk.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import wizard.skunk.persistence.station.StationRepository;
import wizard.skunk.substation.service.StationBayCatalogService;
import wizard.skunk.substation.service.StationScdImportService;
import wizard.skunk.substation.service.StationScdReadService;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@RestController
@RequestMapping("/api/stations")
public class StationBayController {
	private final StationRepository stations;
	private final StationBayCatalogService catalog;
	private final StationScdReadService scd;
	private final StationScdImportService imports;

	public StationBayController(StationRepository stations, StationBayCatalogService catalog,
			StationScdReadService scd, StationScdImportService imports) {
		this.stations = stations;
		this.catalog = catalog;
		this.scd = scd;
		this.imports = imports;
	}

	@GetMapping("/bays")
	public List<StationBayCatalogService.BayOption> bays(@RequestParam String name) {
		return catalog.bays(stationName(name));
	}

	@GetMapping("/scd/bay-mappings")
	public List<StationScdReadService.DeviceBayMapping> mappings(@RequestParam String name) {
		return scd.deviceBayMappings(stationName(name));
	}

	@PutMapping("/scd/bay-mappings")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void update(@RequestParam String name, @RequestBody List<StationScdImportService.BayAssignment> assignments) {
		String station = stationName(name);
		Set<String> devices = new HashSet<>();
		if (assignments == null || assignments.isEmpty() || assignments.stream().anyMatch(item ->
				item == null || item.device() == null || item.device().isBlank() || !devices.add(item.device())
						|| (item.bayCode() != null && item.bayCode().isBlank()))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效的装置，每个装置只能配置一个间隔");
		}
		var bays = catalog.bays(station);
		Set<String> codes = new HashSet<>();
		bays.forEach(bay -> codes.add(bay.code()));
		if (assignments.stream().anyMatch(item -> item.bayCode() != null && !codes.contains(item.bayCode()))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "间隔不属于当前厂站或已不存在，请刷新间隔目录");
		}
		try {
			imports.updateDeviceBays(station, assignments, bays);
		} catch (NoSuchElementException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
		}
	}

	private String stationName(String name) {
		return stations.findById(name).orElseThrow(() ->
				new ResponseStatusException(HttpStatus.NOT_FOUND, "站点不存在")).getName();
	}
}
