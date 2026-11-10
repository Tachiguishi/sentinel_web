package wizard.skunk.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import wizard.skunk.persistence.station.StationEntity;
import wizard.skunk.persistence.station.StationRepository;
import wizard.skunk.persistence.substation.domain.DeviceCategory;
import wizard.skunk.substation.service.StationScdImportService;
import wizard.skunk.substation.service.StationScdReadService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/stations")
public class StationScdController {
	private final StationRepository stations;
	private final StationScdImportService imports;
	private final StationScdReadService scd;

	public StationScdController(StationRepository stations, StationScdImportService imports, StationScdReadService scd) {
		this.stations = stations;
		this.imports = imports;
		this.scd = scd;
	}

	@GetMapping("/detail")
	public StationDetail detail(@RequestParam String name) {
		StationEntity station = findStation(name);
		return new StationDetail(station.getName(), station.getDescription());
	}

	@GetMapping("/scd/devices")
	public List<StationScdReadService.DeviceSummary> devices(@RequestParam String name) {
		return scd.devices(findStation(name).getName());
	}

	@GetMapping("/scd/device-categories")
	public List<CategoryOption> deviceCategories() {
		return Arrays.stream(DeviceCategory.values()).map(CategoryOption::from).toList();
	}

	@PutMapping("/scd/device-category")
	public CategoryOption updateDeviceCategory(@RequestParam String name, @RequestParam String device,
											  @RequestParam int category) {
		return updateDeviceCategories(name, category, List.of(device));
	}

	@PutMapping(value = "/scd/device-categories", consumes = MediaType.APPLICATION_JSON_VALUE)
	public CategoryOption updateDeviceCategories(@RequestParam String name, @RequestParam int category,
												 @RequestBody List<String> devices) {
		if (devices.isEmpty() || devices.stream().anyMatch(device -> device == null || device.isBlank())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效的装置");
		}

		DeviceCategory selection = DeviceCategory.fromValue(category);
		if (selection == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的装置类型");
		}
		String stationName = findStation(name).getName();
		try {
			imports.updateDeviceCategories(stationName, devices, selection);
		} catch (NoSuchElementException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
		}
		return CategoryOption.from(selection);
	}

	@GetMapping("/scd/datasets")
	public List<StationScdReadService.DataSetSummary> dataSets(@RequestParam String name, @RequestParam String device) {
		return scd.dataSets(findStation(name).getName(), device);
	}

	@GetMapping("/scd/fcda")
	public List<StationScdReadService.FcdaRow> fcda(@RequestParam String name, @RequestParam String device,
												 @RequestParam String dataset) {
		return scd.fcda(findStation(name).getName(), device, dataset);
	}

	@PostMapping(value = "/scd", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ImportResult importScd(@RequestParam String name, @RequestParam MultipartFile file) throws IOException {
		StationEntity station = findStation(name);
		if (file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择非空的 SCD 文件");
		}

		try (InputStream upload = file.getInputStream()) {
			return new ImportResult(imports.importScd(station.getName(), upload));
		} catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
		}
	}

	private StationEntity findStation(String name) {
		return stations.findById(name)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "站点不存在"));
	}

	public record StationDetail(String name, String description) {}

	public record ImportResult(int deviceCount) {}

	public record CategoryOption(int value, String label) {
		static CategoryOption from(DeviceCategory category) {
			return new CategoryOption(category.value, category.description);
		}
	}
}
