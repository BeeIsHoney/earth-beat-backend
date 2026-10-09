package org.example.earthjukebox.sound;

import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EnvironmentalSoundService {
    public static final int MAX_FRAMES = 120;
    private final SoundProperties properties;

    public EnvironmentalSoundService(SoundProperties properties) {
        this.properties = properties;
    }

    public SignalFrame frame(String signal, double value) {
        if (!Double.isFinite(value)) {
            throw badRequest("Measurement သည် finite number ဖြစ်ရပါမည်။");
        }
        String name = signal == null ? "" : signal.trim().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "temperature" -> {
                double raw = (value - properties.anomalyMinC())
                        / (properties.anomalyMaxC() - properties.anomalyMinC());
                double normalized = clamp(raw);
                int frequency = (int) Math.round(properties.frequencyMinHz()
                        * Math.pow(properties.frequencyMaxHz() / properties.frequencyMinHz(), normalized));
                yield new SignalFrame(name, value, "°C anomaly", frequency, properties.durationMs(),
                        "sine", normalized, 0.30, 0, raw != normalized,
                        SonificationService.MAPPING_VERSION);
            }
            case "rainfall" -> {
                if (value < 0) throw badRequest("rainfallMm သည် 0 နှင့်အထက် ဖြစ်ရပါမည်။");
                double raw = value / 100.0;
                double normalized = clamp(raw);
                yield new SignalFrame(name, value, "mm", (int) Math.round(220 + 1100 * normalized),
                        properties.durationMs(), "noise", normalized,
                        value == 0 ? 0 : 0.10 + 0.45 * normalized,
                        value == 0 ? 0 : 2 + 18 * normalized, raw > 1, "rainfall-density-v1");
            }
            case "vegetation" -> {
                if (value < -1 || value > 1) throw badRequest("ndvi သည် -1 မှ 1 အတွင်း ဖြစ်ရပါမည်။");
                double normalized = clamp(value);
                yield new SignalFrame(name, value, "NDVI", (int) Math.round(180 + 700 * normalized),
                        properties.durationMs(), "triangle", normalized,
                        0.16 + 0.20 * normalized, 0, value < 0, "vegetation-ndvi-v1");
            }
            default -> throw badRequest("signal သည် temperature၊ rainfall သို့မဟုတ် vegetation ဖြစ်ရပါမည်။");
        };
    }

    public List<SignalFrame> frames(String signal, List<Double> values) {
        if (values == null || values.isEmpty() || values.size() > MAX_FRAMES) {
            throw badRequest("values ထဲမှာ measurement 1 မှ 120 ခု ထည့်ပါ။");
        }
        return values.stream().map(value -> {
            if (value == null) throw badRequest("values ထဲမှာ null မထည့်ပါနှင့်။");
            return frame(signal, value);
        }).toList();
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    public record SignalFrame(String signal, double value, String unit, int frequencyHz,
                              int durationMs, String waveform, double normalizedValue,
                              double intensity, double pulseRateHz, boolean clamped,
                              String mappingVersion) {
    }
}
