package org.example.earthjukebox.temperature;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TemperatureRepository extends JpaRepository<TemperatureObservation, Integer> {
    List<TemperatureObservation> findByYearOrderByMonthAsc(int year);
    Optional<TemperatureObservation> findByYearAndMonth(int year, int month);
    Optional<TemperatureObservation> findFirstByOrderByYearDescMonthDesc();

    @Query("select distinct t.year from TemperatureObservation t order by t.year")
    List<Integer> findAvailableYears();
}

