package dev.cameron.nightwatch;

import dev.cameron.nightwatch.effects.LoomObservations;
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
import net.minecraft.world.level.Level;
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
    private LoomHooks loomHooks;
    private Object currentServer;

    private record Scheduled(long due, Action action) {}

    @Override public void onInitializeClient() {
        NightwatchEntities.register();
        NightwatchEntities.registerClient();
        LoomTeleportHandler.register();
        loomHooks = new LoomHooks();
        LoomDirectorBridge.INSTANCE.register(loomHooks);
        settings = Settings.load(FabricLoader.getInstance().getConfigDir());
        Director.Writer writer;
        if (settings.aiEnabled()) {
            writer = new ProviderWriter(ProviderFactory.create(FabricLoader.getInstance().getConfigDir()));
        } else {
            writer = new dev.cameron.nightwatch.engine.RuleWriter();
        }
        director = new Director(writer,
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
                currentServer = null;
                scheduled.clear();
                if (voice != null) { voice.close(); voice = null; }
                director.reset(System.currentTimeMillis()); // save leave: per-save memory must not bleed
            }
            return;
        }
        if (currentWorld != client.level) {
            // Same integrated server = an in-save dimension change (Loom enter/exit, nether portal).
            // Lead ruling: pacing floors survive dimension hops; only a save entry/leave resets them.
            Object server = client.getSingleplayerServer();
            boolean sameSave = server != null && server == currentServer;
            // Entering the Loom is part of the sequence, not a world change: the bridge already
            // advanced to THREAD and the server has teleported us. Resetting here would collapse
            // the sequence and immediately eject the player (EXIT_REQUEST). Skip only the reset.
            boolean enteringLoom = LoomTeleportHandler.LOOM.equals(client.level.dimension())
                && !(currentWorld instanceof Level previous
                    && LoomTeleportHandler.LOOM.equals(previous.dimension()));
            if (voice != null) { voice.close(); voice = null; }
            WatcherSpawner.vanishAll(client);
            currentWorld = client.level;
            currentServer = server;
            scheduled.clear();
            watcherVanishAt = 0L;
            if (loomHooks != null) loomHooks.resetObservations();
            if (!enteringLoom) LoomDirectorBridge.INSTANCE.resetForWorldChange();
            previousPosition = client.player.position();
            ticks = 0;
            if (!sameSave) director.reset(System.currentTimeMillis()); // save entry only
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
        if (loomHooks != null) loomHooks.tick(client, now, inLoom);
        LoomDirectorBridge.INSTANCE.tick(inLoom, client.player.position(), moving, now);
        if (++ticks % 20 == 0) {
            director.tick(scene(client), now);
            previousPosition = client.player.position();
        }
    }

    /** Bridges Loom cues to concrete client output. Loom only ever emits allowlisted, validated ids. */
    private static final class LoomHooks implements LoomDirectorBridge.Hooks {
        private final LoomObservations observations = new LoomObservations();

        void resetObservations() {
            observations.reset();
        }

        void tick(Minecraft client, long nowMs, boolean inLoom) {
            observations.tick(client, nowMs, inLoom);
        }

        @Override public void effect(String allowlistedEffectId, String argument) {
            Minecraft client = Minecraft.getInstance();
            observations.onEffect(allowlistedEffectId, argument, client);
            VisualEffects.trigger(client, allowlistedEffectId, argument);
        }
        @Override public void sound(String allowlistedSoundId) {
            SoundEffects.play(Minecraft.getInstance(), allowlistedSoundId);
        }
        @Override public void status(String boundedText) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) client.player.sendOverlayMessage(Component.literal(boundedText));
        }
        @Override public void teleportEnter() {
            observations.reset();
            ClientPlayNetworking.send(LoomPayload.ENTER_ACTION);
        }
        @Override public void teleportExit(boolean caught) {
            observations.reset();
            ClientPlayNetworking.send(caught ? LoomPayload.EXIT_CAUGHT_ACTION : LoomPayload.EXIT_ACTION);
        }
        @Override public boolean doorTriggered() {
            return observations.doorTriggered(Minecraft.getInstance());
        }
        @Override public boolean frameTriggered() {
            return observations.frameTriggered(Minecraft.getInstance());
        }
        @Override public Vec3 echoPos() {
            return observations.echoPos();
        }
        @Override public boolean crosshairAtEcho() {
            return observations.crosshairAtEcho(Minecraft.getInstance());
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
