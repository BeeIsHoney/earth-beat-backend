package org.example.earthjukebox.nasa;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NasaImportService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(NasaImportService.class);
    private final NasaCsvClient client;
    private final TemperatureImportStore store;
    private final NasaProperties properties;

    public NasaImportService(NasaCsvClient client, TemperatureImportStore store, NasaProperties properties) {
        this.client = client;
        this.store = store;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.importOnStartup()) {
            refreshSafely();
        }
    }

    @Scheduled(cron = "${earth.nasa.refresh-cron:0 0 2 * * *}", zone = "UTC")
    public void scheduledRefresh() {
        refreshSafely();
    }

    public synchronized TemperatureImportStore.ImportResult refresh() {
        return store.persist(client.fetch());
    }

    private void refreshSafely() {
        try {
            var result = refresh();
            log.info("NASA GISTEMP import complete: {} observations, changed={}",
                    result.observationCount(), result.changed());
        } catch (RuntimeException e) {
            // Network/parse failures preserve the last successful database snapshot.
            log.warn("NASA GISTEMP refresh failed; serving the last successful snapshot: {}", e.toString());
        }
    }
}

