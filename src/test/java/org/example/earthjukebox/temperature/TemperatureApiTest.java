package org.example.earthjukebox.temperature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.List;
import org.example.earthjukebox.nasa.GistempCsvParser;
import org.example.earthjukebox.nasa.NasaCsvClient;
import org.example.earthjukebox.nasa.TemperatureImportStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TemperatureApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private TemperatureImportStore store;
    @Autowired private GistempCsvParser parser;
    @Autowired private TemperatureRepository observations;
    @Autowired private DatasetRepository datasets;

    @BeforeEach
    void reset() {
        observations.deleteAll();
        datasets.deleteAll();
    }

    @Test
    void importsOnceAndRevisesDataWithoutCreatingDuplicates() {
        String original = csv("1.25", ".20");
        store.persist(download(original));
        long count = observations.count();
        var identical = store.persist(download(original));
        assertThat(identical.changed()).isFalse();
        assertThat(observations.count()).isEqualTo(count);

        store.persist(download(csv("1.30", ".20")));
        assertThat(observations.count()).isEqualTo(count);
        assertThat(observations.findByYearAndMonth(2024, 1).orElseThrow().getAnomalyC())
                .isEqualByComparingTo("1.30");
    }

    @Test
    void removesAnObservationThatNasaRetractsAsMissing() {
        store.persist(download(csv("1.25", ".20")));
        store.persist(download(csv("1.25", "***")));
        assertThat(observations.findByYearAndMonth(2024, 2)).isEmpty();
        assertThat(observations.findByYearAndMonth(2024, 1)).isPresent();
    }

    @Test
    void rejectsAShortSnapshotAndKeepsPreviousData() {
        String original = csv("1.25", ".20");
        store.persist(download(original));
        String shorter = original.substring(0, original.indexOf("2024,"));
        assertThatThrownBy(() -> store.persist(download(shorter))).isInstanceOf(IllegalArgumentException.class);
        assertThat(observations.findByYearAndMonth(2024, 1)).isPresent();
        assertThat(datasets.findById(TemperatureDataset.ID).orElseThrow().getSha256())
                .isEqualTo(NasaCsvClient.sha256(original));
    }

    @Test
    void returnsAFrequencyFrameAndYearTimeline() throws Exception {
        store.persist(download(csv("1.25", "***")));
        mvc.perform(get("/api/earth/temperature").param("year", "2024").param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.temperatureAnomalyC").value(1.25))
                .andExpect(jsonPath("$.frequencyHz").value(684))
                .andExpect(jsonPath("$.waveform").value("sine"));
        mvc.perform(get("/api/earth/temperature").param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableMonths").value(1))
                .andExpect(jsonPath("$.frames[0].date").value("2024-01"))
                .andExpect(jsonPath("$.metadata.baseline").value("1951–1980"));
    }

    @Test
    void returns404ForMissingMonthsAnd400ForInvalidInputs() throws Exception {
        store.persist(download(csv("1.25", "***")));
        mvc.perform(get("/api/earth/temperature").param("year", "2024").param("month", "2"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(get("/api/earth/temperature").param("year", "2024").param("month", "13"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/earth/temperature").param("year", "1879"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/earth/temperature").param("year", "hello"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/earth/temperature"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsAvailableYearsLatestMonthAndSourceMetadata() throws Exception {
        store.persist(download(csv("1.25", ".20")));
        mvc.perform(get("/api/earth/temperature/years"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value(1880));
        mvc.perform(get("/api/earth/temperature/latest"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.date").value("2024-02"));
        mvc.perform(get("/api/earth/dataset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("gistemp-v4"))
                .andExpect(jsonPath("$.csvUrl").value(
                        "https://data.giss.nasa.gov/gistemp/tabledata_v4/GLB.Ts%2BdSST.csv"))
                .andExpect(jsonPath("$.sha256").isNotEmpty())
                .andExpect(jsonPath("$.retrievedAt").isNotEmpty());
    }

    @Test
    void returns503BeforeTheFirstSuccessfulImport() throws Exception {
        mvc.perform(get("/api/earth/temperature").param("year", "2024"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void allowsTheConfiguredFrontendOrigin() throws Exception {
        mvc.perform(options("/api/earth/temperature")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private NasaCsvClient.Download download(String csv) {
        return new NasaCsvClient.Download(parser.parse(csv), NasaCsvClient.sha256(csv), Instant.now());
    }

    @Test
    void providesRealTemperatureAudioForAMonthAndAYear() throws Exception {
        store.persist(download(csv("1.25", ".20")));
        byte[] month = mvc.perform(get("/api/earth/temperature/audio")
                        .param("year", "2024").param("month", "1"))
                .andExpect(status().isOk()).andExpect(content().contentType("audio/wav"))
                .andReturn().getResponse().getContentAsByteArray();
        byte[] year = mvc.perform(get("/api/earth/temperature/audio").param("year", "2024"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(month, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("RIFF");
        assertThat(year.length - 44).isEqualTo(2 * (month.length - 44));
        mvc.perform(get("/api/earth/temperature/audio").param("year", "2024").param("month", "3"))
                .andExpect(status().isNotFound());
    }

    @Test
    void servesRainfallAndVegetationWithoutDependingOnTheTemperatureDataset() throws Exception {
        mvc.perform(get("/api/earth/rainfall").param("rainfallMm", "30"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.signal").value("rainfall"))
                .andExpect(jsonPath("$.value").value(30)).andExpect(jsonPath("$.unit").value("mm"));
        mvc.perform(get("/api/earth/vegetation").param("ndvi", ".7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.signal").value("vegetation"))
                .andExpect(jsonPath("$.value").value(.7)).andExpect(jsonPath("$.unit").value("NDVI"));
        for (String url : List.of("/api/earth/rainfall/audio?rainfallMm=30",
                "/api/earth/vegetation/audio?ndvi=.7",
                "/api/earth/sound/audio?signal=temperature&value=1.25")) {
            mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("audio/wav"));
        }
        mvc.perform(get("/api/earth/rainfall").param("rainfallMm", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/earth/vegetation").param("ndvi", "2")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/earth/rainfall").param("rainfallMm", "NaN")).andExpect(status().isBadRequest());
    }

    @Test
    void acceptsMeasurementTimelinesAndRejectsInvalidBodies() throws Exception {
        String request = "{\"signal\":\"rainfall\",\"values\":[0,10,30]}";
        mvc.perform(post("/api/earth/sound/frames").contentType("application/json").content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].intensity").value(0))
                .andExpect(jsonPath("$[2].value").value(30));
        mvc.perform(post("/api/earth/sound/audio").contentType("application/json").content(request))
                .andExpect(status().isOk()).andExpect(content().contentType("audio/wav"));
        for (String invalid : List.of("{}", "{\"signal\":\"rainfall\",\"values\":[null]}",
                "{\"signal\":\"vegetation\",\"values\":[2]}")) {
            mvc.perform(post("/api/earth/sound/audio").contentType("application/json").content(invalid))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(options("/api/earth/sound/audio").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private String csv(String january, String february) {
        StringBuilder text = new StringBuilder("Land-Ocean: Global Means\n");
        text.append("Year,Jan,Feb,Mar,Apr,May,Jun,Jul,Aug,Sep,Oct,Nov,Dec\n");
        for (int year = 1880; year < 2024; year++) {
            text.append(year).append(",.1,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1,.1\n");
        }
        return text.append("2024,").append(january).append(",").append(february)
                .append(",***,***,***,***,***,***,***,***,***,***\n").toString();
    }
}

