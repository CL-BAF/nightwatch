package dev.cameron.nightwatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record Settings(boolean aiEnabled, boolean microphoneEnabled, String model) {
    public static Settings load(Path configDir) {
        Path file = configDir.resolve("nightwatch.properties");
        Properties props = new Properties();
        try {
            Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                props.setProperty("ai.enabled", "true");
                props.setProperty("ai.model", "qwen3:4b");
                props.setProperty("microphone.enabled", "false");
                props.setProperty("microphone.note", "Start voice/sidecar.py before enabling; audio stays on your machine.");
                try (var out = Files.newOutputStream(file)) { props.store(out, "Nightwatch settings; restart Minecraft after changing"); }
            }
            try (var in = Files.newInputStream(file)) { props.load(in); }
        } catch (IOException error) {
            System.err.println("[Nightwatch] Config unavailable: " + error.getMessage());
        }
        return new Settings(Boolean.parseBoolean(props.getProperty("ai.enabled", "true")),
            Boolean.parseBoolean(props.getProperty("microphone.enabled", "false")),
            props.getProperty("ai.model", "qwen3:4b"));
    }
}
