package dev.cameron.nightwatch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Bounded, opt-in world memory with privacy sanitization.
 * Stores up to 50 entries per save, sanitized of coordinates, names, items, and API keys.
 * Memory is opt-in via memory.enabled config (default: false).
 * 
 * Scoping: Per-save (not per-dimension) to prevent cross-save bleed.
 * The saveId is derived from the integrated server's storage folder name.
 */
public final class WorldMemory {
    private static final int MAX_ENTRIES = 50;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Sanitization patterns
    private static final Pattern COORDINATE_PATTERN = Pattern.compile(
        "at\\s+[-\\d]+,\\s*[-\\d]+,\\s*[-\\d]+|\\b[-\\d]+,\\s*[-\\d]+,\\s*[-\\d]+\\b"
    );
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile(
        "\\b[A-Z][a-z]+\\s+(said|asked|typed)"
    );
    private static final Pattern INVENTORY_PATTERN = Pattern.compile(
        "(holding|wearing|carrying)\\s+\\w+\\s+\\w+"
    );
    private static final Pattern API_KEY_PATTERN = Pattern.compile(
        "sk-[a-zA-Z0-9]{20,}"
    );

    private final Path memoryFile;
    private final List<MemoryEntry> entries;
    private final boolean enabled;

    public record MemoryEntry(Instant timestamp, String type, String text) {}

    /**
     * Create WorldMemory scoped by save ID.
     * @param configDir Minecraft config directory
     * @param saveId Stable per-save identifier (e.g., integrated server storage folder name)
     * @param enabled Whether memory is enabled via config
     */
    public WorldMemory(Path configDir, String saveId, boolean enabled) {
        this.enabled = enabled;
        // Scope by save ID to prevent cross-save bleed
        String safeSaveId = saveId.replaceAll("[^a-zA-Z0-9_-]", "_");
        this.memoryFile = configDir.resolve("nightwatch").resolve("saves").resolve(safeSaveId + ".json");
        this.entries = load();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int size() {
        return entries.size();
    }

    /**
     * Add a memory entry after sanitization. No-op if memory is disabled.
     */
    public void remember(String type, String text) {
        if (!enabled) return;

        String sanitized = sanitize(text);
        if (sanitized.isBlank()) return;

        entries.add(new MemoryEntry(Instant.now(), type, sanitized));
        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
        save();
    }

    /**
     * Delete the memory file and clear entries.
     */
    public void delete() {
        try {
            Files.deleteIfExists(memoryFile);
        } catch (IOException e) {
            System.err.println("[Nightwatch] Failed to delete memory file: " + e.getMessage());
        }
        entries.clear();
    }

    /**
     * Clear entries but keep the file (reset to empty).
     */
    public void reset() {
        entries.clear();
        save();
    }

    /**
     * Get all entries as a formatted string for status display.
     */
    public String getStatus() {
        return "Memory: " + entries.size() + "/" + MAX_ENTRIES + " entries" +
               (enabled ? " (enabled)" : " (disabled)");
    }

    /**
     * Get all entries for prompt construction.
     */
    public String getMemoryForPrompt() {
        if (!enabled || entries.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (MemoryEntry entry : entries) {
            sb.append("[").append(entry.type()).append("] ")
              .append(entry.text()).append("\n");
        }
        return sb.toString();
    }

    private List<MemoryEntry> load() {
        if (!Files.exists(memoryFile)) {
            return new ArrayList<>();
        }

        try {
            String json = Files.readString(memoryFile);
            Type listType = new TypeToken<List<MemoryEntry>>() {}.getType();
            List<MemoryEntry> loaded = GSON.fromJson(json, listType);
            return loaded != null ? new ArrayList<>(loaded) : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("[Nightwatch] Failed to load memory file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void save() {
        try {
            Files.createDirectories(memoryFile.getParent());
            Files.writeString(memoryFile, GSON.toJson(entries));
        } catch (IOException e) {
            System.err.println("[Nightwatch] Failed to save memory file: " + e.getMessage());
        }
    }

    /**
     * Sanitize text to remove private information.
     */
    static String sanitize(String text) {
        if (text == null) return "";

        // Remove coordinates
        text = COORDINATE_PATTERN.matcher(text).replaceAll("[location]");

        // Remove player names (capitalized words followed by said/asked/typed)
        text = PLAYER_NAME_PATTERN.matcher(text).replaceAll("[player] $1");

        // Remove inventory items
        text = INVENTORY_PATTERN.matcher(text).replaceAll("$1 [item]");

        // Remove API keys
        text = API_KEY_PATTERN.matcher(text).replaceAll("[API_KEY]");

        return text.strip();
    }
}
