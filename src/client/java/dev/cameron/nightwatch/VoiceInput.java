package dev.cameron.nightwatch;

import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.function.Consumer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

/** Captures speech in 3-second windows; WAV is sent only to a local STT process. */
public final class VoiceInput implements AutoCloseable {
    private volatile boolean running;
    private volatile boolean closed;
    private volatile TargetDataLine line;
    private Thread worker;
    private final Consumer<String> onWords;

    public VoiceInput(Consumer<String> onWords) { this.onWords = onWords; }

    public void start() {
        if (running || closed) return;
        running = true;
        worker = new Thread(this::capture, "nightwatch-microphone");
        worker.setDaemon(true);
        worker.start();
    }

    private void capture() {
        AudioFormat format = new AudioFormat(16_000, 16, 1, true, false);
        try {
            line = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
            line.open(format);
            line.start();
            byte[] pcm = new byte[16_000 * 2 * 3];
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
            while (running) {
                int offset = 0;
                while (running && offset < pcm.length) {
                    int count = line.read(pcm, offset, pcm.length - offset);
                    if (count > 0) offset += count;
                }
                if (!running || offset < pcm.length || !containsVoice(pcm)) continue;
                byte[] wav = wave(pcm);
                HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8765/transcribe"))
                    .timeout(Duration.ofSeconds(25)).header("Content-Type", "audio/wav")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(wav)).build();
                try {
                    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200 && response.body().length() < 2048) {
                        String words = JsonParser.parseString(response.body()).getAsJsonObject().get("text").getAsString();
                        if (!words.isBlank()) onWords.accept(words);
                    }
                } catch (Exception ignored) {
                    // Local transcription service is optional; keep playing without it.
                }
            }
        } catch (Exception error) {
            if (running) System.err.println("[Nightwatch] Microphone unavailable: " + error.getMessage());
        } finally {
            TargetDataLine lineRef = line;
            if (lineRef != null) {
                try { lineRef.stop(); } catch (Exception ignored) {}
                try { lineRef.close(); } catch (Exception ignored) {}
            }
            running = false;
        }
    }

    private static boolean containsVoice(byte[] pcm) {
        long energy = 0;
        for (int i = 0; i < pcm.length; i += 2) {
            short sample = (short) ((pcm[i] & 255) | pcm[i + 1] << 8);
            energy += Math.abs((int) sample);
        }
        return energy / (pcm.length / 2) > 350;
    }

    private static byte[] wave(byte[] pcm) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(pcm.length + 44);
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put(new byte[]{'R','I','F','F'}).putInt(36 + pcm.length);
        header.put(new byte[]{'W','A','V','E','f','m','t',' '}).putInt(16).putShort((short) 1);
        header.putShort((short) 1).putInt(16_000).putInt(32_000).putShort((short) 2).putShort((short) 16);
        header.put(new byte[]{'d','a','t','a'}).putInt(pcm.length);
        out.write(header.array());
        out.write(pcm);
        return out.toByteArray();
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        running = false;
        // Interrupt worker to break out of blocking STT POST (25s timeout)
        if (worker != null) worker.interrupt();
        // Release TargetDataLine — check for null to avoid NPE if close() called before line is assigned
        TargetDataLine lineRef = line;
        if (lineRef != null) {
            try {
                lineRef.stop();
            } catch (Exception ignored) {
                // Line may already be stopped or closed
            }
            try {
                lineRef.close();
            } catch (Exception ignored) {
                // Line may already be closed
            }
        }
    }
}
