package org.example.earthjukebox.nasa;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "earth.nasa")
public record NasaProperties(String csvUrl, boolean importOnStartup, String refreshCron) {
}

