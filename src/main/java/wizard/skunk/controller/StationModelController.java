package wizard.skunk.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import wizard.skunk.persistence.station.StationRepository;
import wizard.skunk.substation.service.StationModelExportService;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/stations")
public class StationModelController {
	private final StationRepository stations;
	private final StationModelExportService exports;

	public StationModelController(StationRepository stations, StationModelExportService exports) {
		this.stations = stations;
		this.exports = exports;
	}

	@GetMapping(value = "/model", produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<byte[]> exportModel(@RequestParam String name) {
		var station = stations.findById(name)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "站点不存在"));
		byte[] model = exports.exportModel(station);
		String filename = station.getName().replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]", "_") + ".xml";
		return ResponseEntity.ok()
				.contentType(new MediaType("application", "xml", StandardCharsets.UTF_8))
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(filename, StandardCharsets.UTF_8).build().toString())
				.cacheControl(CacheControl.noStore())
				.body(model);
	}
}
