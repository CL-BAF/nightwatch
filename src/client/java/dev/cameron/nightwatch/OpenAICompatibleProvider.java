package dev.cameron.nightwatch;

import com.google.gson.JsonArray;
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
 * OpenAI-compatible provider. Supports any endpoint that implements the OpenAI
 * chat completions API (e.g., OpenAI, Azure OpenAI, local LM Studio, etc.).
 *
 * <p>API key is read from config/nightwatch.properties, never logged, never in the jar.
 * Endpoint URL and model are also configurable.
 */
public final class OpenAICompatibleProvider implements Provider {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final String apiKey;
    private final String endpoint;
    private final String model;

    public OpenAICompatibleProvider(String apiKey, String endpoint, String model) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = model;
    }

    @Override
    public CompletableFuture<String> complete(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return CompletableFuture.completedFuture("");
        }
        JsonObject request = new JsonObject();
        request.addProperty("model", model);
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        request.add("messages", messages);
        request.addProperty("temperature", 0.7);
        request.addProperty("max_tokens", 600);
        HttpRequest call = HttpRequest.newBuilder(URI.create(endpoint + "/v1/chat/completions"))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(request.toString())).build();
        return http.sendAsync(call, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200 || response.body().length() > 8192) return "";
                JsonObject outer = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray choices = outer.has("choices") ? outer.getAsJsonArray("choices") : null;
                if (choices == null || choices.size() == 0) return "";
                JsonObject first = choices.get(0).getAsJsonObject();
                JsonObject msg = first.has("message") ? first.getAsJsonObject("message") : null;
                if (msg == null || !msg.has("content")) return "";
                return msg.get("content").getAsString();
            });
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank() && endpoint != null && !endpoint.isBlank();
    }

    @Override
    public String name() {
        return "openai-compatible";
    }
}
