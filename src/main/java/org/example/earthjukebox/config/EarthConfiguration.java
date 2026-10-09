package org.example.earthjukebox.config;

import org.example.earthjukebox.nasa.NasaProperties;
import org.example.earthjukebox.sound.SoundProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties({NasaProperties.class, SoundProperties.class})
public class EarthConfiguration implements WebMvcConfigurer {
    private final String[] allowedOrigins;

    public EarthConfiguration(@Value("${earth.frontend.allowed-origins:http://localhost:3000,http://localhost:5173}") String origins) {
        this.allowedOrigins = origins.split("\\s*,\\s*");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/earth/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .maxAge(3600);
    }
}

