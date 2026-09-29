package dev.cameron.nightwatch;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Typing illusion: shows "typing..." indicator in action bar, then sends final message to chat.
 * Uses 26.3 API: sendOverlayMessage for action bar, sendSystemMessage for chat.
 * 
 * Lifecycle: cancelPending() must be called at save leave (disconnect or return to
 * title screen) to prevent stale messages from firing into the wrong world.
 * In-save dimension transitions (Loom, nether) do NOT call cancelPending — pending
 * messages may survive by design, guarded by liveness re-check + Loom suppression.
 */
public final class TypingIndicator {
    private final Minecraft client;
    private final Random random = new Random();
    private final List<CompletableFuture<Void>> pending = new ArrayList<>();

    public TypingIndicator(Minecraft client) {
        this.client = client;
    }

    /**
     * Cancel all pending typing indicators. Call at save leave only (not at in-save
     * dimension transitions — those are handled by liveness re-check + Loom suppression).
     */
    public void cancelPending() {
        synchronized (pending) {
            pending.forEach(f -> f.cancel(false));
            pending.clear();
        }
    }

    /**
     * Show typing indicator for 1-3 seconds, then send the final message.
     * Re-checks active() before delivery to prevent stale sends.
     * @param message The final message to send to chat
     */
    public void showThenSend(String message) {
        if (client.player == null) return;

        // Show typing indicator in action bar (26.3 API)
        client.player.sendOverlayMessage(
            Component.literal("<...> is typing...").withStyle(style -> style.withColor(TextColor.fromRgb(0xAAAAAA)))
        );

        // Wait 1-3 seconds (simulated typing)
        int typingDuration = 1000 + random.nextInt(2000);
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {},
            CompletableFuture.delayedExecutor(typingDuration, TimeUnit.MILLISECONDS))
            .thenRunAsync(() -> client.execute(() -> {
                // Re-check active state before delivering
                if (client.player == null) return;
                if (client.level == null) return;
                if (client.getSingleplayerServer() == null) return;
                // Silence reads heavier in the Loom — suppress delivery if player entered Loom during typing window
                if (dev.cameron.nightwatch.loom.net.LoomTeleportHandler.LOOM.equals(client.level.dimension())) return;
                
                // Send final message to chat (26.3 API)
                client.player.sendSystemMessage(
                    Component.literal("<...> " + message)
                );
            }), client);

        synchronized (pending) {
            pending.add(future);
            // Clean up completed futures
            pending.removeIf(CompletableFuture::isDone);
        }
    }
}
