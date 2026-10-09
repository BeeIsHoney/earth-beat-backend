package org.example.earthjukebox.sound;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "earth.sound")
public record SoundProperties(double anomalyMinC, double anomalyMaxC,
                              double frequencyMinHz, double frequencyMaxHz, int durationMs) {
    public SoundProperties {
        if (!Double.isFinite(anomalyMinC) || !Double.isFinite(anomalyMaxC)
                || !Double.isFinite(frequencyMinHz) || !Double.isFinite(frequencyMaxHz)
                || anomalyMaxC <= anomalyMinC || frequencyMinHz <= 0
                || frequencyMaxHz <= frequencyMinHz || durationMs <= 0) {
            throw new IllegalArgumentException("Sonification setting မမှန်ပါ။");
        }
    }
}

