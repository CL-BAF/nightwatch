package dev.cameron.nightwatch.engine;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** Offline fallback. A little atmosphere without a model or an internet connection. */
public final class RuleWriter implements Director.Writer {
    private final java.util.Random random = new java.util.Random();

    @Override
    public CompletableFuture<Action> decide(String situation, Scene scene, String memory) {
        String lower = situation.toLowerCase(Locale.ROOT);
        if (lower.contains("leave me alone") || lower.contains("stop")) {
            return CompletableFuture.completedFuture(Action.message("okay", 5));
        }
        if (scene.underground() && scene.light() < 6) {
            return CompletableFuture.completedFuture(random.nextBoolean()
                ? Action.sound("whisper", 3)
                : Action.message("still down there", 5));
        }
        if (!scene.moving()) {
            return CompletableFuture.completedFuture(random.nextInt(4) == 0
                ? Action.sound("footstep_distant", 4)
                : Action.message("you stopped", 5));
        }
        if (!scene.lookingAt().equals("nothing")) {
            return CompletableFuture.completedFuture(Action.message("keep looking at the " + scene.lookingAt(), 5));
        }
        return CompletableFuture.completedFuture(Action.message("hello", 5));
    }
}
