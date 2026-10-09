package org.example.earthjukebox.temperature;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRepository extends JpaRepository<TemperatureDataset, String> {
}

