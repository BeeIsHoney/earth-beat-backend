package org.example.earthjukebox.sound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class EnvironmentalSoundTest {
    private final EnvironmentalSoundService signals =
            new EnvironmentalSoundService(new SoundProperties(-0.6, 1.6, 180, 880, 450));
    private final WavAudioService audio = new WavAudioService();

    @Test
    void usesTheRequestedMeasurementAndPreservesValuesBeyondTheMappingRange() {
        assertThat(signals.frame("temperature", 1.25).frequencyHz()).isEqualTo(684);
        var highRain = signals.frame("rainfall", 150);
        assertThat(highRain.value()).isEqualTo(150);
        assertThat(highRain.clamped()).isTrue();
        assertThat(signals.frame("rainfall", 80).pulseRateHz())
                .isGreaterThan(signals.frame("rainfall", 10).pulseRateHz());
        assertThat(signals.frame("vegetation", 0.8).frequencyHz())
                .isGreaterThan(signals.frame("vegetation", 0.2).frequencyHz());
        assertThat(signals.frame("vegetation", -0.5).value()).isEqualTo(-0.5);
    }

    @Test
    void producesDecodablePcmWithSilenceForZeroRainAndDifferentSoundsForEachSignal() {
        byte[] dry = audio.render(List.of(signals.frame("rainfall", 0)));
        ByteBuffer wav = ByteBuffer.wrap(dry).order(ByteOrder.LITTLE_ENDIAN);
        assertThat(new String(dry, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("RIFF");
        assertThat(wav.getInt(4)).isEqualTo(dry.length - 8);
        assertThat(wav.getShort(20)).isEqualTo((short) 1);
        assertThat(wav.getShort(22)).isEqualTo((short) 1);
        assertThat(wav.getInt(24)).isEqualTo(22050);
        assertThat(wav.getShort(34)).isEqualTo((short) 16);
        assertThat(wav.getInt(40)).isEqualTo(dry.length - 44);
        for (int i = 44; i < dry.length; i++) assertThat(dry[i]).isZero();
        byte[] rain = audio.render(List.of(signals.frame("rainfall", 30)));
        byte[] plants = audio.render(List.of(signals.frame("vegetation", 0.7)));
        byte[] temperature = audio.render(List.of(signals.frame("temperature", 0.7)));
        assertThat(rain).isNotEqualTo(dry).isNotEqualTo(plants);
        assertThat(plants).isNotEqualTo(temperature);
        byte[] sequence = audio.render(signals.frames("rainfall", List.of(0.0, 30.0)));
        assertThat(sequence.length - 44).isEqualTo(2 * (rain.length - 44));
    }

    @Test
    void rejectsInvalidMeasurementsAndUnboundedRequests() {
        assertThatThrownBy(() -> signals.frame("rainfall", -1)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> signals.frame("vegetation", 1.1)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> signals.frame("temperature", Double.NaN)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> signals.frame("rainfall", Double.POSITIVE_INFINITY)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> signals.frame("unknown", 0)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> signals.frames("rainfall", List.of())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> signals.frames("rainfall", java.util.Collections.nCopies(121, 1.0)))
                .isInstanceOf(ResponseStatusException.class);
        var longSignals = new EnvironmentalSoundService(new SoundProperties(-0.6, 1.6, 180, 880, 10000));
        assertThatThrownBy(() -> audio.render(longSignals.frames("rainfall", java.util.Collections.nCopies(7, 1.0))))
                .isInstanceOf(ResponseStatusException.class);
    }
}
