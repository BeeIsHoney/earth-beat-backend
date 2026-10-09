package org.example.earthjukebox.temperature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "earth_temperature_dataset")
public class TemperatureDataset {
    public static final String ID = "gistemp-v4";

    @Id
    @Column(length = 40)
    private String id;

    @Column(nullable = false, length = 512)
    private String sourceUrl;

    @Column(nullable = false, length = 64)
    private String sha256;

    private int firstYear;
    private int lastYear;
    private int observationCount;

    @Column(nullable = false)
    private Instant importedAt;

    @Column(nullable = false)
    private Instant retrievedAt;

    protected TemperatureDataset() {
    }

    public TemperatureDataset(String sourceUrl) {
        this.id = ID;
        this.sourceUrl = sourceUrl;
    }

    public void imported(String sourceUrl, String hash, int firstYear, int lastYear, int count, Instant at) {
        this.sourceUrl = sourceUrl;
        this.sha256 = hash;
        this.firstYear = firstYear;
        this.lastYear = lastYear;
        this.observationCount = count;
        this.importedAt = at;
        this.retrievedAt = at;
    }

    public void checkedAt(Instant at) { this.retrievedAt = at; }
    public String getId() { return id; }
    public String getSourceUrl() { return sourceUrl; }
    public String getSha256() { return sha256; }
    public int getFirstYear() { return firstYear; }
    public int getLastYear() { return lastYear; }
    public int getObservationCount() { return observationCount; }
    public Instant getImportedAt() { return importedAt; }
    public Instant getRetrievedAt() { return retrievedAt; }
}

