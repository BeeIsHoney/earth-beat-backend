package org.example.earthjukebox.nasa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.example.earthjukebox.temperature.DatasetRepository;
import org.example.earthjukebox.temperature.TemperatureDataset;
import org.example.earthjukebox.temperature.TemperatureObservation;
import org.example.earthjukebox.temperature.TemperatureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemperatureImportStore {
    private final TemperatureRepository observations;
    private final DatasetRepository datasets;
    private final NasaProperties properties;

    public TemperatureImportStore(TemperatureRepository observations, DatasetRepository datasets,
                                  NasaProperties properties) {
        this.observations = observations;
        this.datasets = datasets;
        this.properties = properties;
    }

    @Transactional
    public ImportResult persist(NasaCsvClient.Download download) {
        var data = download.data();
        TemperatureDataset dataset = datasets.findById(TemperatureDataset.ID).orElse(null);
        if (dataset != null && dataset.getSha256().equals(download.sha256())) {
            dataset.checkedAt(download.retrievedAt());
            return new ImportResult(dataset.getObservationCount(), false);
        }
        List<TemperatureObservation> existing = observations.findAll();
        // Reject unexpectedly shortened responses before changing cached data.
        if (dataset != null && (data.firstYear() != dataset.getFirstYear()
                || data.lastYear() < dataset.getLastYear()
                || data.values().size() < existing.size() * 0.95)) {
            throw new IllegalArgumentException("NASA CSV အပိုင်းတချို့ ပျောက်နေသောကြောင့် import မလုပ်ပါ။");
        }
        Map<Integer, TemperatureObservation> byId = new HashMap<>();
        existing.forEach(row -> byId.put(row.getId(), row));
        Set<Integer> incomingIds = new HashSet<>();
        List<TemperatureObservation> changed = new ArrayList<>();
        for (var value : data.values()) {
            incomingIds.add(value.id());
            TemperatureObservation row = byId.get(value.id());
            if (row == null) {
                changed.add(new TemperatureObservation(value.year(), value.month(), value.anomalyC()));
            } else if (row.getAnomalyC().compareTo(value.anomalyC()) != 0) {
                row.revise(value.anomalyC());
                changed.add(row);
            }
        }
        // An explicit missing value in a complete new snapshot must not retain an old observation.
        List<TemperatureObservation> removed = existing.stream()
                .filter(row -> !incomingIds.contains(row.getId())).toList();
        observations.deleteAll(removed);
        observations.saveAll(changed);
        if (dataset == null) {
            dataset = new TemperatureDataset(properties.csvUrl());
        }
        dataset.imported(properties.csvUrl(), download.sha256(),
                data.firstYear(), data.lastYear(), data.values().size(), download.retrievedAt());
        datasets.save(dataset);
        return new ImportResult(data.values().size(), true);
    }

    public record ImportResult(int observationCount, boolean changed) {
    }
}

