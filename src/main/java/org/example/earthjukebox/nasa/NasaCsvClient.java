package org.example.earthjukebox.nasa;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.Year;
import java.util.HexFormat;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NasaCsvClient {
    private final RestClient client;
    private final NasaProperties properties;
    private final GistempCsvParser parser;

    public NasaCsvClient(RestClient.Builder builder, NasaProperties properties, GistempCsvParser parser) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        this.client = builder.clone().requestFactory(factory).build();
        this.properties = properties;
        this.parser = parser;
    }

    public Download fetch() {
        // URI is passed directly so %2B is preserved instead of being encoded twice.
        String csv = client.get().uri(URI.create(properties.csvUrl())).retrieve().body(String.class);
        if (csv == null || csv.length() > 2_000_000) {
            throw new IllegalArgumentException("NASA CSV response ကို ဖတ်၍မရပါ။");
        }
        GistempCsvParser.ParsedDataset data = parser.parse(csv);
        if (data.firstYear() != 1880 || data.lastYear() < Year.now().getValue() - 1) {
            throw new IllegalArgumentException("NASA CSV ရဲ့ အချိန်အပိုင်းအခြား မပြည့်စုံပါ။");
        }
        return new Download(data, sha256(csv), Instant.now());
    }

    public static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public record Download(GistempCsvParser.ParsedDataset data, String sha256, Instant retrievedAt) {
    }
}

