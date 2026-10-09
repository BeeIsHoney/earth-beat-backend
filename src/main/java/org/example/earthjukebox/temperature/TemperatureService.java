package org.example.earthjukebox.temperature;

import java.time.Instant;
import java.util.List;
import org.example.earthjukebox.sound.SonificationService;
import org.example.earthjukebox.sound.SonificationService.TemperatureFrame;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class TemperatureService {
    private final TemperatureRepository observations;
    private final DatasetRepository datasets;
    private final SonificationService sound;

    public TemperatureService(TemperatureRepository observations, DatasetRepository datasets,
                              SonificationService sound) {
        this.observations = observations;
        this.datasets = datasets;
        this.sound = sound;
    }

    public List<Integer> years() {
        requireDataset();
        return observations.findAvailableYears();
    }

    public TemperatureFrame month(int year, int month) {
        requireDataset();
        return sound.frame(observations.findByYearAndMonth(year, month)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "ဒီနှစ်နဲ့လအတွက် NASA temperature data မရှိသေးပါ။")));
    }

    public YearResponse year(int year) {
        DatasetMetadata metadata = metadata();
        List<TemperatureFrame> frames = observations.findByYearOrderByMonthAsc(year).stream()
                .map(sound::frame).toList();
        if (frames.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ဒီနှစ်အတွက် NASA data မရှိပါ။");
        }
        return new YearResponse(year, frames.size(), frames, metadata, sound.mapping());
    }

    public TemperatureFrame latest() {
        requireDataset();
        return sound.frame(observations.findFirstByOrderByYearDescMonthDesc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "NASA data import လုပ်ရန် လိုအပ်ပါသည်။")));
    }

    public DatasetMetadata metadata() {
        TemperatureDataset dataset = requireDataset();
        return new DatasetMetadata(TemperatureDataset.ID, "NASA GISTEMP v4",
                "global monthly temperature anomaly", "°C", "1951–1980",
                dataset.getSourceUrl(), "https://data.giss.nasa.gov/gistemp/",
                "https://svs.gsfc.nasa.gov/5190/",
                dataset.getFirstYear(), dataset.getLastYear(), dataset.getObservationCount(),
                dataset.getSha256(), dataset.getImportedAt(), dataset.getRetrievedAt());
    }

    private TemperatureDataset requireDataset() {
        return datasets.findById(TemperatureDataset.ID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "NASA data import မပြီးသေးပါ။ MySQL နဲ့ internet connection ကို စစ်ပါ။"));
    }

    public record YearResponse(int year, int availableMonths, List<TemperatureFrame> frames,
                               DatasetMetadata metadata, SonificationService.Mapping mapping) {
    }

    public record DatasetMetadata(String id, String name, String measurement, String unit,
                                  String baseline, String csvUrl, String documentationUrl,
                                  String visualizationPageUrl, int firstYear, int lastYear,
                                  int observationCount, String sha256, Instant importedAt,
                                  Instant retrievedAt) {
    }
}

