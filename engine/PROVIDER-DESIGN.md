# Provider Abstraction Design

## Overview

The Provider abstraction allows Nightwatch to support multiple AI inference backends:
- **Ollama** (default) - Local inference at 127.0.0.1:11434, no API key required
- **OpenAI-compatible** - Any endpoint implementing the OpenAI chat completions API

ONE API key, ONE provider/endpoint, ONE model selection per installation.

## Config (config/nightwatch.properties)

```properties
# Provider: "ollama" (default) or "openai-compatible"
ai.provider=ollama

# Model name
ai.model=qwen3:4b

# Endpoint URL (optional, defaults shown)
# Ollama: http://127.0.0.1:11434
# OpenAI: https://api.openai.com
ai.endpoint=

# API key (OpenAI-compatible only, optional)
# NEVER logged, NEVER bundled in jar, read from config only
ai.api_key=
```

## Security

- **API keys live in local config only** (config/nightwatch.properties, already .gitignore'd)
- **Never logged** - no System.out.println, no logger calls with API key content
- **Never in the jar** - config is read at runtime, not compiled in
- **Never in environment variables** - config file is the only source
- **Inference stays off the game thread** - all HTTP calls use CompletableFuture

## Interface

```java
public interface Provider {
    CompletableFuture<String> complete(String prompt);
    boolean isAvailable();
    String name();
}
```

### Implementations

1. **OllamaProvider** - Local Ollama at 127.0.0.1:11434
   - No API key required
   - Uses `/api/generate` endpoint
   - Model selection via config

2. **OpenAICompatibleProvider** - OpenAI-compatible endpoints
   - API key from config (never logged)
   - Uses `/v1/chat/completions` endpoint
   - Supports OpenAI, Azure OpenAI, LM Studio, etc.

### Factory

```java
Provider provider = ProviderFactory.create(configDir);
```

Reads config/nightwatch.properties and creates the appropriate Provider instance.

## Integration with Director

LocalWriter (now ProviderWriter) uses the Provider interface:

```java
public class ProviderWriter implements Director.Writer {
    private final Provider provider;
    private final RuleWriter fallback = new RuleWriter();

    @Override
    public CompletableFuture<Action> decide(String situation, Scene scene, String memory) {
        if (!provider.isAvailable()) return fallback.decide(situation, scene, memory);
        String prompt = buildPrompt(situation, scene, memory);
        return provider.complete(prompt)
            .thenApply(this::parseResponse)
            .exceptionally(error -> fallback.decide(situation, scene, memory).join());
    }
}
```

## Migration Path

Current LocalWriter.java uses Ollama directly. Migration:
1. Replace LocalWriter with ProviderWriter
2. Use ProviderFactory.create() in NightwatchClient.onInitializeClient()
3. Pass Provider to ProviderWriter constructor
4. Keep fallback to RuleWriter when provider is unavailable

## Testing

- Provider interface is testable without Minecraft
- OllamaProvider can be tested against a running Ollama instance
- OpenAICompatibleProvider can be tested with mock HTTP responses
- ProviderFactory can be tested with temp config files

## Future Providers

Adding a new provider (e.g., Anthropic, Google Gemini):
1. Implement Provider interface
2. Add case to ProviderFactory.create() switch
3. Document config keys in this file
4. No changes to Director or Action API needed
