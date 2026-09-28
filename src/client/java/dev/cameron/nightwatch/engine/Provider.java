package dev.cameron.nightwatch.engine;

import java.util.concurrent.CompletableFuture;

/**
 * Provider abstraction for AI inference. Supports OpenAI-compatible endpoints
 * and local Ollama. ONE API key, ONE provider/endpoint, ONE model selection.
 *
 * <p>Secrets live in local config only (config/nightwatch.properties), never in logs or the jar.
 * Inference stays off the game thread (CompletableFuture boundary).
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link OllamaProvider} - Local Ollama at 127.0.0.1:11434 (default, no API key required)</li>
 *   <li>{@link OpenAICompatibleProvider} - Any OpenAI-compatible endpoint (requires API key)</li>
 * </ul>
 */
public interface Provider {
    /**
     * Send a prompt to the AI model and return the response asynchronously.
     *
     * @param prompt The prompt to send (includes situation, scene, memory)
     * @return CompletableFuture containing the model's JSON response
     */
    CompletableFuture<String> complete(String prompt);

    /**
     * Check if the provider is available (e.g., Ollama is running, API key is set).
     *
     * @return true if the provider can accept requests
     */
    boolean isAvailable();

    /**
     * Get the provider name for logging/display (no secrets).
     *
     * @return Provider name (e.g., "ollama", "openai-compatible")
     */
    String name();
}
