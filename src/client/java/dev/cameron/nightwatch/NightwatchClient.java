package dev.cameron.nightwatch;

import dev.cameron.nightwatch.effects.SoundEffects;
import dev.cameron.nightwatch.effects.VisualEffects;
import dev.cameron.nightwatch.entity.NightwatchEntities;
import dev.cameron.nightwatch.entity.WatcherSpawner;
import dev.cameron.nightwatch.engine.Action;
import dev.cameron.nightwatch.engine.Director;
import dev.cameron.nightwatch.engine.Scene;
import dev.cameron.nightwatch.loom.driver.LoomDirectorBridge;
import dev.cameron.nightwatch.loom.net.LoomPayload;
import dev.cameron.nightwatch.loom.net.LoomTeleportHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** First playable slice. All effects are client-only and only run in singleplayer. */
public final class NightwatchClient implements ClientModInitializer {
    private final List<Scheduled> scheduled = new ArrayList<>();
    private Director director;
    private Settings settings;
    private VoiceInput voice;
    private Object currentWorld;
    private Vec3 previousPosition;
    private int ticks;
    private long watcherVanishAt;

    private record Scheduled(long due, Action action) {}

    @Override public void onInitializeClient() {
        NightwatchEntities.register();
        NightwatchEntities.registerClient();
        LoomTeleportHandler.register();
        LoomDirectorBridge.INSTANCE.register(new LoomHooks());
        settings = Settings.load(FabricLoader.getInstance().getConfigDir());
        director = new Director(new LocalWriter(settings.model(), settings.aiEnabled()),
            new Random(), runnable -> Minecraft.getInstance().execute(runnable),
            action -> scheduled.add(new Scheduled(System.currentTimeMillis() + action.delaySeconds() * 1000L, action)));
        ClientSendMessageEvents.CHAT.register(message -> {
            Minecraft client = Minecraft.getInstance();
            if (active(client)) director.hear(message, false, scene(client), System.currentTimeMillis());
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft client) {
        if (!active(client)) {
            if (currentWorld != null) {
                WatcherSpawner.vanishAll(client);
                currentWorld = null;
                scheduled.clear();
                if (voice != null) { voice.close(); voice = null; }
            }
            return;
        }
        if (currentWorld != client.level) {
            if (voice != null) { voice.close(); voice = null; }
            WatcherSpawner.vanishAll(client);
            currentWorld = client.level;
            scheduled.clear();
            watcherVanishAt = 0L;
            LoomDirectorBridge.INSTANCE.resetForWorldChange();
            previousPosition = client.player.position();
            ticks = 0;
            director.reset(System.currentTimeMillis());
            if (settings.microphoneEnabled()) {
                voice = new VoiceInput(words -> client.execute(() -> {
                    if (active(client)) director.hear(words, true, scene(client), System.currentTimeMillis());
                }));
                voice.start();
            }
        }
        long now = System.currentTimeMillis();
        scheduled.removeIf(item -> {
            if (now < item.due()) return false;
            Action action = item.action();
            switch (action.kind()) {
                case MESSAGE -> client.player.sendSystemMessage(Component.literal("<...> " + action.message()));
                case SOUND -> {
                    SoundEffects.play(client, action.soundId());
                    LoomDirectorBridge.INSTANCE.onDirectorAction(action.soundId(), "");
                }
                case EFFECT -> {
                    if ("brief_sighting".equals(action.effectType()) && WatcherSpawner.spawnSighting(client)) {
                        watcherVanishAt = now + 6_000L; // observe from distance, then guaranteed vanish
                    } else {
                        VisualEffects.trigger(client, action.effectType(), action.argument());
                    }
                    LoomDirectorBridge.INSTANCE.onDirectorAction(action.effectType(), action.argument());
                }
                case SILENCE -> {}
            }
            return true;
        });
        if (watcherVanishAt != 0L && now >= watcherVanishAt) {
            watcherVanishAt = 0L;
            WatcherSpawner.vanishAll(client);
        }
        boolean moving = previousPosition != null && previousPosition.distanceTo(client.player.position()) > 0.3;
        boolean inLoom = LoomTeleportHandler.LOOM.equals(client.level.dimension());
        LoomDirectorBridge.INSTANCE.tick(inLoom, client.player.position(), moving, now);
        if (++ticks % 20 == 0) {
            director.tick(scene(client), now);
            previousPosition = client.player.position();
        }
    }

    /** Bridges Loom cues to concrete client output. Loom only ever emits allowlisted, validated ids. */
    private static final class LoomHooks implements LoomDirectorBridge.Hooks {
        @Override public void effect(String allowlistedEffectId, String argument) {
            VisualEffects.trigger(Minecraft.getInstance(), allowlistedEffectId, argument);
        }
        @Override public void sound(String allowlistedSoundId) {
            SoundEffects.play(Minecraft.getInstance(), allowlistedSoundId);
        }
        @Override public void status(String boundedText) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) client.player.sendSystemMessage(Component.literal(boundedText));
        }
        @Override public void teleportEnter() {
            ClientPlayNetworking.send(LoomPayload.ENTER_ACTION);
        }
        @Override public void teleportExit(boolean caught) {
            ClientPlayNetworking.send(caught ? LoomPayload.EXIT_CAUGHT_ACTION : LoomPayload.EXIT_ACTION);
        }
    }

    private static boolean active(Minecraft client) {
        return client.level != null && client.player != null && client.getSingleplayerServer() != null;
    }

    private Scene scene(Minecraft client) {
        BlockPos pos = client.player.blockPosition();
        String lookedAt = "nothing";
        if (client.hitResult instanceof BlockHitResult hit) {
            lookedAt = BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(hit.getBlockPos()).getBlock()).getPath();
        }
        String biome = client.level.getBiome(pos).unwrapKey()
            .map(key -> key.identifier().getPath()).orElse("unknown");
        boolean moving = previousPosition != null && previousPosition.distanceTo(client.player.position()) > 0.3;
        return new Scene(client.player.getName().getString(), biome,
            client.level.dimension().identifier().getPath(), lookedAt,
            pos.getY() < client.level.getSeaLevel() - 12, moving,
            client.level.getMaxLocalRawBrightness(pos), "observing surroundings");
    }

}
