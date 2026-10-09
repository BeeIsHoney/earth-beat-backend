package org.example.earthjukebox.sound;

import java.math.BigDecimal;
import java.time.YearMonth;
import org.example.earthjukebox.temperature.TemperatureObservation;
import org.springframework.stereotype.Service;

@Service
public class SonificationService {
    public static final String MAPPING_VERSION = "temperature-log-frequency-v1";
    private final SoundProperties properties;

    public SonificationService(SoundProperties properties) {
        this.properties = properties;
    }

    public TemperatureFrame frame(TemperatureObservation row) {
        double value = row.getAnomalyC().doubleValue();
        double normalized = (value - properties.anomalyMinC())
                / (properties.anomalyMaxC() - properties.anomalyMinC());
        boolean clamped = normalized < 0 || normalized > 1;
        normalized = Math.max(0, Math.min(1, normalized));
        int frequency = (int) Math.round(properties.frequencyMinHz()
                * Math.pow(properties.frequencyMaxHz() / properties.frequencyMinHz(), normalized));
        return new TemperatureFrame(row.getYear(), row.getMonth(),
                YearMonth.of(row.getYear(), row.getMonth()).toString(),
                row.getAnomalyC(), frequency, properties.durationMs(), "sine", clamped, MAPPING_VERSION);
    }

    public Mapping mapping() {
        return new Mapping(MAPPING_VERSION, properties.anomalyMinC(), properties.anomalyMaxC(),
                properties.frequencyMinHz(), properties.frequencyMaxHz(), properties.durationMs(),
                "sine", "minHz * (maxHz / minHz) ^ clamp((anomalyC - minC) / (maxC - minC), 0, 1)");
    }

    public record TemperatureFrame(int year, int month, String date, BigDecimal temperatureAnomalyC,
                                   int frequencyHz, int durationMs, String waveform,
                                   boolean clamped, String mappingVersion) {
    }

    public record Mapping(String version, double anomalyMinC, double anomalyMaxC,
                          double frequencyMinHz, double frequencyMaxHz, int durationMs,
                          String waveform, String formula) {
    }
}

