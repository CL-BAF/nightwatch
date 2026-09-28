# Stage 1 Chat Depth Design

## Overview

Stage 1 extends the Director's chat capabilities with per-world personality, typing illusion, and bounded world memory. All features remain client-side, singleplayer, privacy-preserving.

## 1. Per-World Personality

### Design
Each world/dimension gets a consistent personality profile that influences the Director's tone and response patterns. Personality is deterministic (no model needed) but can be overridden by the model when available.

### Personality Dimensions
- **Tone**: eerie, neutral, warm (default: eerie)
- **Verbosity**: sparse, moderate, chatty (default: sparse)
- **Observation focus**: environment, player, both (default: both)

### Implementation
```java
public record Personality(String tone, Verbosity verbosity, ObservationFocus focus) {
    public enum Verbosity { SPARSE, MODERATE, CHATTY }
    public enum ObservationFocus { ENVIRONMENT, PLAYER, BOTH }
    
    public static Personality forWorld(String dimension) {
        return switch (dimension) {
            case "overworld" -> new Personality("eerie", Verbosity.SPARSE, ObservationFocus.BOTH);
            case "the_nether" -> new Personality("oppressive", Verbosity.SPARSE, ObservationFocus.ENVIRONMENT);
            case "the_end" -> new Personality("distant", Verbosity.SPARSE, ObservationFocus.PLAYER);
            default -> new Personality("eerie", Verbosity.SPARSE, ObservationFocus.BOTH);
        };
    }
}
```

### Integration
- Director receives Personality in `tick()` and `hear()`
- RuleWriter uses Personality to select offline responses
- LocalWriter includes Personality in prompt (model can adapt tone)
- Personality persists for the world session (no save/load yet)

### Pacing Constraint (from Models review)
**CRITICAL:** Verbosity MODERATE/CHATTY must NOT override Director's 150s pacing. The Director's cooldown is the hard frequency cap. Verbosity affects word count per message, not message frequency:
- SPARSE: 1-6 words per message
- MODERATE: 7-9 words per message
- CHATTY: 10-12 words per message (capped at 12 to stay inside Action.message 90-char limit and LocalWriter's 12-word model cap)

This ensures the first-15-minutes one-line budget and ≤12-word line shape are preserved regardless of personality setting.

## 2. Typing Illusion

### Design
Before sending a chat message, the Director shows a typing indicator in the action bar (e.g., "<...> is typing...") for 1-3 seconds, then sends the final message. Never per-letter animation (no chat spam).

### Implementation
```java
public class TypingIndicator {
    private final Minecraft client;
    private final Random random = new Random();
    
    public void showThenSend(String message, int delaySeconds) {
        // Show typing indicator
        client.player.displayClientMessage(
            Component.literal("<...> is typing...").withStyle(ChatFormatting.GRAY),
            true  // action bar
        );
        
        // Wait 1-3 seconds (simulated typing)
        int typingDuration = 1000 + random.nextInt(2000);
        CompletableFuture.delayedExecutor(typingDuration, TimeUnit.MILLISECONDS)
            .execute(() -> client.execute(() -> {
                // Send final message
                client.player.displayClientMessage(
                    Component.literal("<...> " + message),
                    false  // chat
                );
            }));
    }
}
```

### Integration
- NightwatchClient's scheduled queue holds Action with delay
- When Action is due, if MESSAGE: show typing indicator, then send message after delay
- Typing indicator uses action bar (not chat) to avoid spam
- Delay is randomized (1-3s) to feel natural

## 3. Bounded World Memory

### Design
Each world gets a bounded, opt-in memory that persists across sessions. Memory is stored in `config/nightwatch/worlds/<world-id>.json`. Player can delete/reset memory via chat commands.

### Memory Schema
```json
{
  "world_id": "abc123",
  "created": "2026-09-29T12:00:00Z",
  "entries": [
    {"timestamp": "...", "type": "chat", "text": "player said hello"},
    {"timestamp": "...", "type": "entity", "text": "entity responded"}
  ],
  "max_entries": 50
}
```

### Privacy Rules
- No coordinates, no player names, no inventory items
- No raw chat transcripts (only sanitized summaries)
- Memory is opt-in: `memory.enabled=true` in config (default: false)
- Player can delete memory: `/nightwatch memory delete`
- Player can reset memory: `/nightwatch memory reset`

### Implementation
```java
public class WorldMemory {
    private final Path memoryFile;
    private final List<MemoryEntry> entries = new ArrayList<>();
    private final int maxEntries = 50;
    
    public void remember(String type, String text) {
        // Sanitize: remove coordinates, names, sensitive info
        String sanitized = sanitize(text);
        if (sanitized.isBlank()) return;
        
        entries.add(new MemoryEntry(Instant.now(), type, sanitized));
        while (entries.size() > maxEntries) entries.removeFirst();
        save();
    }
    
    public void delete() {
        Files.deleteIfExists(memoryFile);
        entries.clear();
    }
    
    public void reset() {
        entries.clear();
        save();
    }
    
    private String sanitize(String text) {
        // Remove coordinates (e.g., "at 100, 64, -200")
        text = text.replaceAll("at\\s+[-\\d]+,\\s*[-\\d]+,\\s*[-\\d]+", "[location]");
        // Remove player names (e.g., "Steve said")
        text = text.replaceAll("\\b[A-Z][a-z]+\\s+(said|asked)", "[player] $1");
        // Remove inventory items (e.g., "holding diamond sword")
        text = text.replaceAll("holding\\s+\\w+\\s+\\w+", "holding [item]");
        return text;
    }
}
```

### Integration
- Director receives WorldMemory in `tick()` and `hear()`
- Director calls `memory.remember()` for significant events
- WorldMemory is loaded on world join, saved on world leave
- Chat commands `/nightwatch memory delete` and `/nightwatch memory reset` trigger delete/reset

## 4. Transcript Privacy

### Design
All transcripts (memory, logs, prompts) are sanitized to remove private info:
- No coordinates
- No player names (use "[player]" placeholder)
- No inventory items (use "[item]" placeholder)
- No API keys, no secrets

### Implementation
```java
public class TranscriptSanitizer {
    public static String sanitize(String text) {
        // Remove coordinates
        text = text.replaceAll("[-\\d]+,\\s*[-\\d]+,\\s*[-\\d]+", "[location]");
        // Remove player names (capitalized words followed by "said/asked")
        text = text.replaceAll("\\b[A-Z][a-z]+\\s+(said|asked|typed)", "[player] $1");
        // Remove inventory items
        text = text.replaceAll("(holding|wearing|carrying)\\s+\\w+\\s+\\w+", "$1 [item]");
        // Remove API keys (if any leak)
        text = text.replaceAll("sk-[a-zA-Z0-9]{20,}", "[API_KEY]");
        return text;
    }
}
```

### Integration
- All memory entries are sanitized before storage
- All prompts sent to the model are sanitized
- All logs are sanitized (if logging is enabled)

## 5. Delete/Reset Controls

### Chat Commands
- `/nightwatch memory delete` — Delete world memory file
- `/nightwatch memory reset` — Clear memory entries (keep file)
- `/nightwatch memory status` — Show memory entry count

### Implementation
```java
public class NightwatchCommands {
    public static void register(CommandDispatcher<SharedSuggestionProvider> dispatcher) {
        dispatcher.register(
            literal("nightwatch")
                .then(literal("memory")
                    .then(literal("delete").executes(ctx -> {
                        WorldMemory memory = getMemory(ctx);
                        memory.delete();
                        ctx.getSource().sendSuccess("Nightwatch memory deleted", false);
                        return 1;
                    }))
                    .then(literal("reset").executes(ctx -> {
                        WorldMemory memory = getMemory(ctx);
                        memory.reset();
                        ctx.getSource().sendSuccess("Nightwatch memory reset", false);
                        return 1;
                    }))
                    .then(literal("status").executes(ctx -> {
                        WorldMemory memory = getMemory(ctx);
                        int count = memory.size();
                        ctx.getSource().sendSuccess("Nightwatch memory: " + count + " entries", false);
                        return 1;
                    }))
                )
        );
    }
}
```

### Integration
- Commands are client-side only (singleplayer)
- Commands require op level 0 (any player can use)
- Commands are registered in NightwatchClient.onInitializeClient()

## 6. Implementation Order

1. **Personality** — Add Personality record, integrate into Director/RuleWriter/LocalWriter
2. **Typing Indicator** — Add TypingIndicator class, integrate into NightwatchClient
3. **World Memory** — Add WorldMemory class, integrate into Director
4. **Transcript Sanitizer** — Add TranscriptSanitizer, integrate into WorldMemory/LocalWriter
5. **Commands** — Add NightwatchCommands, register in NightwatchClient

## 7. Testing

- Personality: verify per-world tone in offline responses (RuleWriter)
- Typing Indicator: verify action bar shows "<...> is typing..." before chat message
- World Memory: verify memory persists across world join/leave, delete/reset work
- Transcript Sanitizer: verify no coordinates/names/items in memory or prompts
- Commands: verify `/nightwatch memory delete/reset/status` work

## 8. Security

- Memory is opt-in (default: false)
- Memory is sanitized (no private info)
- Memory is local (no cloud sync)
- Commands are client-side only (no server authority needed)
- No API keys in memory or logs
