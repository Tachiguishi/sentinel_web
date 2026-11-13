package wizard.skunk.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import wizard.skunk.persistence.station.StationEntity;
import wizard.skunk.persistence.station.StationRepository;
import wizard.skunk.substation.service.StationModelExportService;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StationModelControllerTest {
	@Test
	void downloadsXmlWithAUnicodeAttachmentFilename() throws Exception {
		StationRepository stations = mock(StationRepository.class);
		StationModelExportService exports = mock(StationModelExportService.class);
		StationEntity station = new StationEntity("中文站 & A", null);
		byte[] xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><substation/>".getBytes(StandardCharsets.UTF_8);
		when(stations.findById(station.getName())).thenReturn(Optional.of(station));
		when(exports.exportModel(station)).thenReturn(xml);
		var mvc = MockMvcBuilders.standaloneSetup(new StationModelController(stations, exports)).build();
		var response = mvc.perform(get("/api/stations/model").param("name", station.getName()))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
				.andExpect(content().bytes(xml))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
				.andReturn().getResponse();
		var disposition = ContentDisposition.parse(response.getHeader(HttpHeaders.CONTENT_DISPOSITION));
		assertEquals("attachment", disposition.getType());
		assertEquals("中文站 & A.xml", disposition.getFilename());
		verify(exports).exportModel(station);
	}

	@Test
	void rejectsUnknownStationsBeforeReadingTheirModel() throws Exception {
		StationRepository stations = mock(StationRepository.class);
		StationModelExportService exports = mock(StationModelExportService.class);
		when(stations.findById("missing")).thenReturn(Optional.empty());
		var mvc = MockMvcBuilders.standaloneSetup(new StationModelController(stations, exports)).build();
		mvc.perform(get("/api/stations/model").param("name", "missing")).andExpect(status().isNotFound());
		verifyNoInteractions(exports);
	}
}
