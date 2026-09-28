package dev.cameron.nightwatch;

import dev.cameron.nightwatch.engine.Provider;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Factory for creating Provider instances from config/nightwatch.properties.
 *
 * <p>Config keys:
 * <ul>
 *   <li>ai.provider - "ollama" (default) or "openai-compatible"</li>
 *   <li>ai.model - Model name (e.g., "qwen3:4b" for Ollama, "gpt-4o-mini" for OpenAI)</li>
 *   <li>ai.endpoint - Endpoint URL (default: "http://127.0.0.1:11434" for Ollama)</li>
 *   <li>ai.api_key - API key for OpenAI-compatible providers (optional, never logged)</li>
 * </ul>
 *
 * <p>Secrets (api_key) are read from config only, never from environment variables,
 * never logged, never bundled in the jar.
 */
public final class ProviderFactory {
    private ProviderFactory() {}

    public static Provider create(Path configDir) {
        Properties props = new Properties();
        Path configFile = configDir.resolve("nightwatch.properties");
        if (configFile.toFile().exists()) {
            try (var reader = java.nio.file.Files.newBufferedReader(configFile)) {
                props.load(reader);
            } catch (Exception e) {
                // Fall through to defaults
            }
        }
        String providerName = props.getProperty("ai.provider", "ollama").strip().toLowerCase();
        String model = props.getProperty("ai.model", "qwen3:4b").strip();
        String endpoint = props.getProperty("ai.endpoint", "").strip();
        String apiKey = props.getProperty("ai.api_key", "").strip();

        return switch (providerName) {
            case "openai", "openai-compatible" -> {
                if (endpoint.isEmpty()) endpoint = "https://api.openai.com";
                yield new OpenAICompatibleProvider(apiKey, endpoint, model);
            }
            case "ollama" -> {
                if (endpoint.isEmpty()) endpoint = "http://127.0.0.1:11434";
                yield new OllamaProvider(model, endpoint);
            }
            default -> {
                // Unknown provider, fall back to Ollama
                if (endpoint.isEmpty()) endpoint = "http://127.0.0.1:11434";
                yield new OllamaProvider(model, endpoint);
            }
        };
    }
}
