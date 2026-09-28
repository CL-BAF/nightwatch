package dev.cameron.nightwatch;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Typing illusion: shows "typing..." indicator in action bar, then sends final message to chat.
 * Uses 26.3 API: sendOverlayMessage for action bar, sendSystemMessage for chat.
 */
public final class TypingIndicator {
    private final Minecraft client;
    private final Random random = new Random();

    public TypingIndicator(Minecraft client) {
        this.client = client;
    }

    /**
     * Show typing indicator for 1-3 seconds, then send the final message.
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
        CompletableFuture.delayedExecutor(typingDuration, TimeUnit.MILLISECONDS)
            .execute(() -> client.execute(() -> {
                if (client.player == null) return;
                // Send final message to chat (26.3 API)
                client.player.sendSystemMessage(
                    Component.literal("<...> " + message)
                );
            }));
    }
}
