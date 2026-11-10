package wizard.skunk.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import wizard.skunk.persistence.station.StationEntity;
import wizard.skunk.persistence.station.StationRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/stations")
public class StationController {
	private static final Sort STATION_ORDER = Sort.by(Sort.Order.desc("createAt"), Sort.Order.asc("name"));

	private final StationRepository stations;

	public StationController(StationRepository stations) {
		this.stations = stations;
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
						station.getCreateAt()))
				.toList();
		return new StationPage(rows, currentPage, size, total, totalPages);
	}

	public record StationPage(List<Station> rows, int page, int size, long total, long totalPages) {}

	public record Station(String name, String description, String ipAddress, Integer instantNo, LocalDateTime createAt) {}
}
