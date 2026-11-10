package wizard.skunk.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import wizard.skunk.persistence.station.StationEntity;
import wizard.skunk.persistence.station.StationRepository;
import wizard.skunk.substation.service.StationScdImportService;
import wizard.skunk.substation.service.StationScdReadService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
}
