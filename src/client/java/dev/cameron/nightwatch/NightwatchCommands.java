package dev.cameron.nightwatch;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client-side commands for Nightwatch: /nightwatch memory delete/reset/status
 * Registered via ClientCommandRegistrationCallback (26.3 Fabric API).
 */
public final class NightwatchCommands {
    private static WorldMemory worldMemory;

    public static void setWorldMemory(WorldMemory memory) {
        worldMemory = memory;
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerCommands(dispatcher);
        });
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommands.literal("nightwatch")
                .then(ClientCommands.literal("memory")
                    .then(ClientCommands.literal("delete")
                        .executes(NightwatchCommands::deleteMemory))
                    .then(ClientCommands.literal("reset")
                        .executes(NightwatchCommands::resetMemory))
                    .then(ClientCommands.literal("status")
                        .executes(NightwatchCommands::showStatus))
                )
        );
    }

    private static int deleteMemory(CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        if (worldMemory == null) {
            client.player.sendSystemMessage(Component.literal("§cMemory not initialized"));
            return 0;
        }

        worldMemory.delete();
        client.player.sendSystemMessage(Component.literal("§aNightwatch memory deleted"));
        return 1;
    }

    private static int resetMemory(CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        if (worldMemory == null) {
            client.player.sendSystemMessage(Component.literal("§cMemory not initialized"));
            return 0;
        }

        worldMemory.reset();
        client.player.sendSystemMessage(Component.literal("§aNightwatch memory reset"));
        return 1;
    }

    private static int showStatus(CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        if (worldMemory == null) {
            client.player.sendSystemMessage(Component.literal("§cMemory not initialized"));
            return 0;
        }

        client.player.sendSystemMessage(Component.literal("§e" + worldMemory.getStatus()));
        return 1;
    }
}
