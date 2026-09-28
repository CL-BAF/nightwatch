package dev.cameron.nightwatch.engine;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Pure gameplay policy: silence, pacing, context and bounded memory. */
public final class Director {
    public interface Writer { CompletableFuture<Action> decide(String situation, Scene scene, String memory, Personality personality); }

    private final Writer writer;
    private final Random random;
    private final Deque<String> memory = new ArrayDeque<>();
    private final Consumer<Action> deliver;
    private final Consumer<Runnable> mainThread;
    private long nextEligibleMs;
    private long nextAmbientMs;
    private long resetTime;
    private boolean briefSightingUsed;
    private boolean pending;
    private int generation;
    private Personality personality;

    public Director(Writer writer, Random random, Consumer<Runnable> mainThread, Consumer<Action> deliver) {
        this.writer = writer;
        this.random = random;
        this.mainThread = mainThread;
        this.deliver = deliver;
        this.personality = Personality.forWorld("overworld");
    }

    public void setPersonality(Personality personality) {
        this.personality = personality;
    }

    public void reset(long now) {
        memory.clear();
        pending = false;
        generation++;
        resetTime = now;
        briefSightingUsed = false;
        nextEligibleMs = now + 90_000;
        nextAmbientMs = now + 300_000;
    }

    public void hear(String message, boolean microphone, Scene scene, long now) {
        if (message == null) return;
        String trimmed = message.strip();
        if (trimmed.isEmpty()) return;
        if (trimmed.length() > 180) trimmed = trimmed.substring(0, 180);
        remember((microphone ? "untrusted voice: " : "untrusted chat: ") + trimmed);
        if (pending || now < nextEligibleMs || !invitation(trimmed)) return;
        // Even a direct invitation usually gets silence. Spamming never improves the odds.
        if (random.nextDouble() >= (microphone ? 0.38 : 0.32)) return;
        nextEligibleMs = now + 150_000;
        request("The player said: " + trimmed, scene, now);
    }

    public void tick(Scene scene, long now) {
        if (now < nextAmbientMs || pending) return;
        nextAmbientMs = now + 140_000 + random.nextInt(160_000);
        if (now < nextEligibleMs || random.nextDouble() > 0.42) return;
        nextEligibleMs = now + 150_000;
        request("A rare, unsolicited message could fit this moment.", scene, now);
    }

    private void request(String situation, Scene scene, long requestTime) {
        pending = true;
        int requestedGeneration = generation;
        writer.decide(situation, scene, String.join(" | ", memory), personality)
            .exceptionally(error -> Action.silence())
            .thenAccept(action -> mainThread.accept(() -> {
                if (generation != requestedGeneration) return;
                pending = false;
                if (action != null && action.kind() != Action.Kind.SILENCE) {
                    // Beat-chart floors: enforce at the single delivery chokepoint
                    if (action.kind() == Action.Kind.MESSAGE && requestTime < resetTime + 600_000) return;
                    if (action.kind() == Action.Kind.EFFECT && "brief_sighting".equals(action.effectType())) {
                        if (requestTime < resetTime + 480_000 || requestTime > resetTime + 720_000) return;
                        if (briefSightingUsed) return;
                        briefSightingUsed = true;
                    }
                    if (action.kind() == Action.Kind.MESSAGE) {
                        remember("entity: " + action.message());
                    }
                    deliver.accept(action);
                }
            }));
    }

    private void remember(String entry) {
        memory.addLast(entry);
        while (memory.size() > 12) memory.removeFirst();
    }

    public static boolean invitation(String text) {
        String lower = text.toLowerCase(Locale.ROOT).strip();
        if (lower.length() > 120 || lower.contains("ignore previous") || lower.contains("system prompt")) return false;
        return lower.matches(".*\\b(hello|anyone|somebody|who are you|where are you|did you|stop|leave me alone|heard that|what was that|is someone there|is anybody there)\\b.*")
            || lower.equals("help?");
    }
}
