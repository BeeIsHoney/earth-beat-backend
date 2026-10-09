package org.example.earthjukebox.sound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import org.example.earthjukebox.temperature.TemperatureObservation;
import org.junit.jupiter.api.Test;

class SonificationServiceTest {
    private final SonificationService service = new SonificationService(
            new SoundProperties(-0.6, 1.6, 180, 880, 450));

    @Test
    void convertsAnomalyToTheSamePitchAsThePrototype() {
        var frame = service.frame(row("1.25"));
        assertThat(frame.frequencyHz()).isEqualTo(684);
        assertThat(frame.temperatureAnomalyC()).isEqualByComparingTo("1.25");
        assertThat(frame.clamped()).isFalse();
    }

    @Test
    void mapsTheReferenceRangeToItsBoundaryFrequencies() {
        assertThat(service.frame(row("-0.60")).frequencyHz()).isEqualTo(180);
        assertThat(service.frame(row("1.60")).frequencyHz()).isEqualTo(880);
    }

    @Test
    void flagsOutOfRangeValuesAndPreservesTheScientificMeasurement() {
        var low = service.frame(row("-1.00"));
        var high = service.frame(row("2.00"));
        assertThat(low.frequencyHz()).isEqualTo(180);
        assertThat(high.frequencyHz()).isEqualTo(880);
        assertThat(low.clamped()).isTrue();
        assertThat(high.clamped()).isTrue();
        assertThat(high.temperatureAnomalyC()).isEqualByComparingTo("2.00");
    }

    @Test
    void rejectsImpossibleConfiguration() {
        assertThatThrownBy(() -> new SoundProperties(-0.6, -0.6, 180, 880, 450))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SoundProperties(-0.6, 1.6, 0, 880, 450))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private TemperatureObservation row(String anomaly) {
        return new TemperatureObservation(2024, 1, new BigDecimal(anomaly));
    }
}

