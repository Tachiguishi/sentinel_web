package wizard.skunk.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import wizard.skunk.persistence.station.StationEntity;
import wizard.skunk.persistence.station.StationRepository;
import wizard.skunk.substation.service.StationBayCatalogService;
import wizard.skunk.substation.service.StationScdImportService;
import wizard.skunk.substation.service.StationScdReadService;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StationBayControllerTest {
	@Test
	void rejectsForeignBaysDuplicateDevicesAndUnknownStationsBeforeWriting() throws Exception {
		var stations = mock(StationRepository.class);
		var catalog = mock(StationBayCatalogService.class);
		var imports = mock(StationScdImportService.class);
		var reader = mock(StationScdReadService.class);
		when(stations.findById("A")).thenReturn(Optional.of(new StationEntity("A", "甲站")));
		when(stations.findById("missing")).thenReturn(Optional.empty());
		when(catalog.bays("A")).thenReturn(List.of(new StationBayCatalogService.BayOption("Bay001", "Line1", "线路一", 220)));
		var mvc = MockMvcBuilders.standaloneSetup(new StationBayController(stations, catalog, reader, imports)).build();
		mvc.perform(get("/api/stations/bays").param("name", "A"))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].voltage").value(220));
		for (String invalid : List.of("[{\"device\":\"IED1\",\"bayCode\":\"ForeignBay\"}]",
				"[{\"device\":\"IED1\",\"bayCode\":null},{\"device\":\"IED1\",\"bayCode\":\"Bay001\"}]",
				"[]", "[null]", "[{\"device\":\"\",\"bayCode\":null}]", "[{\"device\":\"IED1\",\"bayCode\":\"\"}]")) {
			mvc.perform(put("/api/stations/scd/bay-mappings").param("name", "A")
					.contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
		}
		mvc.perform(get("/api/stations/bays").param("name", "missing")).andExpect(status().isNotFound());
		verifyNoInteractions(imports);
		mvc.perform(put("/api/stations/scd/bay-mappings").param("name", "A").contentType(MediaType.APPLICATION_JSON)
				.content("[{\"device\":\"IED1\",\"bayCode\":\"Bay001\"},{\"device\":\"IED2\",\"bayCode\":null}]"))
				.andExpect(status().isNoContent());
		verify(imports).updateDeviceBays(eq("A"), eq(List.of(new StationScdImportService.BayAssignment("IED1", "Bay001"),
				new StationScdImportService.BayAssignment("IED2", null))), anyList());
	}
}
