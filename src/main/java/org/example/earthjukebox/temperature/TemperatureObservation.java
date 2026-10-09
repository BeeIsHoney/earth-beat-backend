package org.example.earthjukebox.temperature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(name = "earth_temperature_month",
        uniqueConstraints = @UniqueConstraint(name = "uk_earth_temperature_year_month",
                columnNames = {"observation_year", "observation_month"}))
public class TemperatureObservation {
    @Id
    private Integer id;

    @Column(name = "observation_year", nullable = false)
    private int year;

    @Column(name = "observation_month", nullable = false)
    private int month;

    @Column(name = "anomaly_c", nullable = false, precision = 6, scale = 2)
    private BigDecimal anomalyC;

    protected TemperatureObservation() {
    }

    public TemperatureObservation(int year, int month, BigDecimal anomalyC) {
        this.id = year * 100 + month;
        this.year = year;
        this.month = month;
        this.anomalyC = anomalyC;
    }

    public Integer getId() { return id; }
    public int getYear() { return year; }
    public int getMonth() { return month; }
    public BigDecimal getAnomalyC() { return anomalyC; }
    public void revise(BigDecimal value) { this.anomalyC = value; }
}

