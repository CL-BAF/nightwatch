package dev.cameron.nightwatch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.cameron.nightwatch.engine.Provider;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Local Ollama provider. Connects to 127.0.0.1:11434 (fixed, no API key required).
 * Default provider for Nightwatch. No secrets needed.
 */
public final class OllamaProvider implements Provider {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final String model;
    private final String endpoint;

    public OllamaProvider(String model) {
        this(model, "http://127.0.0.1:11434");
    }

    public OllamaProvider(String model, String endpoint) {
        this.model = model;
        this.endpoint = endpoint;
    }

    @Override
    public CompletableFuture<String> complete(String prompt) {
        JsonObject request = new JsonObject();
        request.addProperty("model", model);
        request.addProperty("prompt", prompt);
        request.addProperty("stream", false);
        request.addProperty("format", "json");
        HttpRequest call = HttpRequest.newBuilder(URI.create(endpoint + "/api/generate"))
            .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(request.toString())).build();
        return http.sendAsync(call, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200 || response.body().length() > 8192) return "";
                JsonObject outer = JsonParser.parseString(response.body()).getAsJsonObject();
                return outer.has("response") ? outer.get("response").getAsString() : "";
            });
    }

    @Override
    public boolean isAvailable() {
        // Quick check: try to reach the endpoint
        try {
            HttpRequest call = HttpRequest.newBuilder(URI.create(endpoint + "/api/tags"))
                .timeout(Duration.ofSeconds(2)).GET().build();
            HttpResponse<String> response = http.send(call, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String name() {
        return "ollama";
    }
}
