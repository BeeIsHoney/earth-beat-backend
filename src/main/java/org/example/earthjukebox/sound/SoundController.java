package org.example.earthjukebox.sound;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.example.earthjukebox.sound.EnvironmentalSoundService.SignalFrame;
import org.example.earthjukebox.sound.SonificationService.TemperatureFrame;
import org.example.earthjukebox.temperature.TemperatureService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/earth")
public class SoundController {
    private final EnvironmentalSoundService signals;
    private final WavAudioService audio;
    private final TemperatureService temperature;

    public SoundController(EnvironmentalSoundService signals, WavAudioService audio,
                           TemperatureService temperature) {
        this.signals = signals;
        this.audio = audio;
        this.temperature = temperature;
    }

    @GetMapping("/sound/frame")
    public SignalFrame frame(@RequestParam("signal") String signal, @RequestParam("value") double value) {
        return signals.frame(signal, value);
    }

    @GetMapping(value = "/sound/audio", produces = "audio/wav")
    public ResponseEntity<byte[]> sound(@RequestParam("signal") String signal,
                                       @RequestParam("value") double value) {
        SignalFrame frame = signals.frame(signal, value);
        return wav(frame.signal(), audio.render(List.of(frame)));
    }

    @GetMapping("/rainfall")
    public SignalFrame rainfall(@RequestParam("rainfallMm") double rainfallMm) {
        return signals.frame("rainfall", rainfallMm);
    }

    @GetMapping(value = "/rainfall/audio", produces = "audio/wav")
    public ResponseEntity<byte[]> rainfallAudio(@RequestParam("rainfallMm") double rainfallMm) {
        return wav("rainfall", audio.render(List.of(signals.frame("rainfall", rainfallMm))));
    }

    @GetMapping("/vegetation")
    public SignalFrame vegetation(@RequestParam("ndvi") double ndvi) {
        return signals.frame("vegetation", ndvi);
    }

    @GetMapping(value = "/vegetation/audio", produces = "audio/wav")
    public ResponseEntity<byte[]> vegetationAudio(@RequestParam("ndvi") double ndvi) {
        return wav("vegetation", audio.render(List.of(signals.frame("vegetation", ndvi))));
    }

    @GetMapping(value = "/temperature/audio", produces = "audio/wav")
    public ResponseEntity<byte[]> temperatureAudio(@RequestParam("year") @Min(1880) @Max(9999) int year,
            @RequestParam(name = "month", required = false) @Min(1) @Max(12) Integer month) {
        List<TemperatureFrame> frames = month == null ? temperature.year(year).frames()
                : List.of(temperature.month(year, month));
        return wav("temperature", audio.temperature(frames));
    }

    @PostMapping("/sound/frames")
    public List<SignalFrame> frames(@RequestBody SoundRequest request) {
        return signals.frames(request.signal(), request.values());
    }

    @PostMapping(value = "/sound/audio", produces = "audio/wav")
    public ResponseEntity<byte[]> timeline(@RequestBody SoundRequest request) {
        List<SignalFrame> frames = signals.frames(request.signal(), request.values());
        return wav(frames.getFirst().signal(), audio.render(frames));
    }

    private ResponseEntity<byte[]> wav(String signal, byte[] bytes) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + signal + ".wav\"")
                .contentLength(bytes.length)
                .body(bytes);
    }

    public record SoundRequest(String signal, List<Double> values) {
    }
}
