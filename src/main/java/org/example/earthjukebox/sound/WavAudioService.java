package org.example.earthjukebox.sound;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import org.example.earthjukebox.sound.EnvironmentalSoundService.SignalFrame;
import org.example.earthjukebox.sound.SonificationService.TemperatureFrame;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WavAudioService {
    public static final int SAMPLE_RATE = 22050;
    private static final int MAX_SAMPLES = SAMPLE_RATE * 60;

    public byte[] temperature(List<TemperatureFrame> frames) {
        return render(frames.stream().map(frame -> new SignalFrame("temperature",
                frame.temperatureAnomalyC().doubleValue(), "°C anomaly", frame.frequencyHz(),
                frame.durationMs(), "sine", 0, 0.30, 0, frame.clamped(), frame.mappingVersion())).toList());
    }

    public byte[] render(List<SignalFrame> frames) {
        if (frames == null || frames.isEmpty() || frames.size() > EnvironmentalSoundService.MAX_FRAMES) {
            throw badRequest("အသံအတွက် frame 1 မှ 120 ခု ထည့်ပါ။");
        }
        int totalSamples = 0;
        for (SignalFrame frame : frames) {
            if (frame.durationMs() <= 0 || frame.durationMs() > 60000
                    || frame.frequencyHz() <= 0 || frame.frequencyHz() >= SAMPLE_RATE / 2) {
                throw badRequest("Audio duration သို့မဟုတ် frequency setting မမှန်ပါ။");
            }
            long count = (long) SAMPLE_RATE * frame.durationMs() / 1000;
            if (count < 1 || count > MAX_SAMPLES - totalSamples) {
                throw badRequest("အသံတစ်ဖိုင်သည် 60 seconds ထက်မကျော်ရပါ။");
            }
            totalSamples += (int) count;
        }
        int dataSize = totalSamples * 2;
        ByteBuffer wav = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        ascii(wav, "RIFF"); wav.putInt(36 + dataSize); ascii(wav, "WAVE");
        ascii(wav, "fmt "); wav.putInt(16); wav.putShort((short) 1); wav.putShort((short) 1);
        wav.putInt(SAMPLE_RATE); wav.putInt(SAMPLE_RATE * 2);
        wav.putShort((short) 2); wav.putShort((short) 16);
        ascii(wav, "data"); wav.putInt(dataSize);
        for (SignalFrame frame : frames) writeFrame(wav, frame);
        return wav.array();
    }

    private void writeFrame(ByteBuffer wav, SignalFrame frame) {
        int count = (int) ((long) SAMPLE_RATE * frame.durationMs() / 1000);
        int fade = Math.max(1, Math.min(count / 2, SAMPLE_RATE / 50));
        Random noise = new Random(Double.doubleToLongBits(frame.value()));
        double filteredNoise = 0;
        double drop = 0;
        double filterAlpha = 1 - Math.exp(-2 * Math.PI * frame.frequencyHz() / SAMPLE_RATE);
        double pulsePhase = 0;
        for (int sample = 0; sample < count; sample++) {
            double time = sample / (double) SAMPLE_RATE;
            double phase = 2 * Math.PI * frame.frequencyHz() * time;
            double value;
            switch (frame.signal()) {
                case "rainfall" -> {
                    filteredNoise += filterAlpha * (noise.nextDouble() * 2 - 1 - filteredNoise);
                    if (sample == 0 && frame.pulseRateHz() > 0) drop = 1;
                    pulsePhase += frame.pulseRateHz() / SAMPLE_RATE;
                    if (pulsePhase >= 1) {
                        drop = 1;
                        pulsePhase -= 1;
                    }
                    drop *= Math.exp(-1.0 / (0.018 * SAMPLE_RATE));
                    value = frame.intensity() * (0.75 * filteredNoise
                            + 0.25 * drop * Math.sin(phase));
                }
                case "vegetation" -> {
                    double triangle = 2 / Math.PI * Math.asin(Math.sin(phase));
                    double chord = (Math.sin(phase * 1.25) + Math.sin(phase * 1.5)) / 2;
                    value = frame.intensity() * (0.75 * triangle
                            + 0.25 * frame.normalizedValue() * chord);
                }
                default -> value = frame.intensity() * Math.sin(phase);
            }
            double envelope = Math.min(1, Math.min(sample / (double) fade,
                    (count - 1 - sample) / (double) fade));
            short pcm = (short) Math.round(Math.max(-1, Math.min(1, value * envelope)) * 32767);
            wav.putShort(pcm);
        }
    }

    private void ascii(ByteBuffer buffer, String value) {
        buffer.put(value.getBytes(StandardCharsets.US_ASCII));
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
