package org.example.earthjukebox.temperature;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.example.earthjukebox.sound.SonificationService.TemperatureFrame;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/earth")
public class TemperatureController {
    private final TemperatureService service;

    public TemperatureController(TemperatureService service) {
        this.service = service;
    }

    @GetMapping("/temperature")
    public Object temperature(@RequestParam("year") @Min(1880) @Max(9999) int year,
                              @RequestParam(name = "month", required = false) @Min(1) @Max(12) Integer month) {
        return month == null ? service.year(year) : service.month(year, month);
    }

    @GetMapping("/temperature/years")
    public List<Integer> years() {
        return service.years();
    }

    @GetMapping("/temperature/latest")
    public TemperatureFrame latest() {
        return service.latest();
    }

    @GetMapping("/dataset")
    public TemperatureService.DatasetMetadata dataset() {
        return service.metadata();
    }
}

