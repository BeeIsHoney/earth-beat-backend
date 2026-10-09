package org.example.earthjukebox.nasa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.example.earthjukebox.temperature.DatasetRepository;
import org.example.earthjukebox.temperature.TemperatureDataset;
import org.example.earthjukebox.temperature.TemperatureRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// Optional live NASA integration check: mvn -Dtest=NasaLiveImportIT test
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NasaLiveImportIT {
    @Autowired private NasaImportService importer;
    @Autowired private TemperatureRepository observations;
    @Autowired private DatasetRepository datasets;
    @Autowired private MockMvc mvc;

    @Test
    void importsTheActualNasaCsvAndServesItThroughTheApi() throws Exception {
        observations.deleteAll();
        datasets.deleteAll();
        var result = importer.refresh();
        assertThat(result.observationCount()).isGreaterThan(1700);
        assertThat(observations.count()).isEqualTo(result.observationCount());
        assertThat(datasets.findById(TemperatureDataset.ID).orElseThrow().getFirstYear()).isEqualTo(1880);
        mvc.perform(get("/api/earth/temperature").param("year", "2024").param("month", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.frequencyHz").isNumber());
        mvc.perform(get("/api/earth/temperature/latest"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.date").isNotEmpty());
        mvc.perform(get("/api/earth/dataset"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.baseline").value("1951–1980"));
        System.out.println("LIVE NASA IMPORT: observations=" + result.observationCount()
                + ", latest=" + observations.findFirstByOrderByYearDescMonthDesc().orElseThrow().getYear()
                + "-" + observations.findFirstByOrderByYearDescMonthDesc().orElseThrow().getMonth());
    }
}

