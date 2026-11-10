package wizard.skunk.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import wizard.skunk.persistence.station.StationEntity;
import wizard.skunk.persistence.station.StationRepository;
import wizard.skunk.persistence.station.SubstationEntity;
import wizard.skunk.persistence.station.SubstationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/stations")
public class StationController {
	private static final Sort STATION_ORDER = Sort.by(Sort.Order.desc("createAt"), Sort.Order.asc("name"));
	private static final String IPV4_OCTET = "(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])";
	private static final Pattern IPV4 = Pattern.compile(IPV4_OCTET + "(?:\\." + IPV4_OCTET + "){3}");

	private final StationRepository stations;
	private final SubstationRepository substations;

	public StationController(StationRepository stations, SubstationRepository substations) {
		this.stations = stations;
		this.substations = substations;
	}

	@GetMapping("/candidates")
	public List<StationCandidate> candidates() {
		return substations.findAllByOrderByCodeAsc().stream()
				.map(station -> new StationCandidate(station.getCode(), station.getDescription()))
				.toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public void add(@RequestBody AddStation request) {
		if (request.code() == null || request.code().isBlank() || request.code().length() > 48) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的站点代码");
		}

		SubstationEntity source = substations.findById(request.code())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "站点目录中没有该站点"));
		String name = source.getCode();
		if (stations.existsById(name)) {
			throw stationAlreadyExists();
		}

		try {
			stations.saveAndFlush(new StationEntity(name, source.getDescription()));
		} catch (DataIntegrityViolationException exception) {
			// Another request may have inserted this primary key after the existence check.
			if (stations.existsById(name)) {
				throw stationAlreadyExists();
			}
			throw exception;
		}
	}

	private ResponseStatusException stationAlreadyExists() {
		return new ResponseStatusException(HttpStatus.CONFLICT, "站点已经存在");
	}

	@PutMapping("/settings")
	@Transactional
	public StationSettings updateSettings(@RequestBody UpdateStationSettings request) {
		if (request.name() == null || request.name().isBlank() || request.name().length() > 64) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的站点名称");
		}

		String ipAddress = request.ipAddress() == null ? "" : request.ipAddress().strip();
		if (!ipAddress.isEmpty() && !IPV4.matcher(ipAddress).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入有效的 IPv4 地址");
		}

		JsonNode value = request.instantNo();
		Integer instantNo = null;
		if (value != null && !value.isNull()) {
			if (!value.isIntegralNumber() || !value.canConvertToInt()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "实例号必须是 32 位整数");
			}
			instantNo = value.intValue();
		}

		StationEntity station = stations.findById(request.name())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "站点不存在"));
		station.updateSettings(ipAddress.isEmpty() ? null : ipAddress, instantNo);
		return new StationSettings(station.getName(), station.getIpAddress(), station.getInstantNo());
	}

	@GetMapping
	@Transactional(readOnly = true)
	public StationPage list(@RequestParam(defaultValue = "1") int page,
							@RequestParam(defaultValue = "20") int size,
							@RequestParam(defaultValue = "") String keyword) {
		if (page < 1 || size < 1 || size > 100) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be positive and size must be between 1 and 100");
		}

		String term = keyword.strip();
		boolean filtered = !term.isEmpty();
		long total = filtered ? stations.countByNameContainingOrDescriptionContainingAllIgnoreCase(term, term) : stations.count();
		long totalPages = (total + size - 1) / size;
		int currentPage = (int) Math.min(page, Math.max(1, totalPages));
		PageRequest pageable = PageRequest.of(currentPage - 1, size, STATION_ORDER);
		List<StationEntity> matches = filtered
				? stations.findByNameContainingOrDescriptionContainingAllIgnoreCase(term, term, pageable)
				: stations.findAllBy(pageable);
		List<Station> rows = matches.stream()
				.map(station -> new Station(
						station.getName(),
						station.getDescription(),
						station.getIpAddress(),
						station.getInstantNo(),
						station.getCreateAt(),
						station.getScdUpdatedAt()))
				.toList();
		return new StationPage(rows, currentPage, size, total, totalPages);
	}

	public record StationPage(List<Station> rows, int page, int size, long total, long totalPages) {}

	public record StationCandidate(String code, String description) {}

	public record AddStation(String code) {}

	public record UpdateStationSettings(
			String name,
			@JsonProperty(required = true) String ipAddress,
			@JsonProperty(required = true) JsonNode instantNo) {}

	public record StationSettings(String name, String ipAddress, Integer instantNo) {}

	public record Station(String name, String description, String ipAddress, Integer instantNo,
						  LocalDateTime createAt, LocalDateTime scdUpdatedAt) {}
}
