package dev.cameron.nightwatch.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Server-thread helpers to place and remove the Watcher. Spawning goes through the integrated
 * server so the entity is real and persisted for the session; it is never sent as a command and
 * never touches terrain.
 */
public final class WatcherSpawner {
    private WatcherSpawner() {}

    /** Computes the ~14-blocks-ahead sighting spot and spawns the Watcher there if singleplayer.
     * Returns false when there is no integrated server (caller falls back to the particle fake). */
    public static boolean spawnSighting(Minecraft client) {
        if (client.player == null) return false;
        if (client.getSingleplayerServer() == null) return false;
        Vec3 eye = client.player.getEyePosition();
        Vec3 view = client.player.getViewVector(1.0F);
        Vec3 flat = new Vec3(view.x, 0.0, view.z);
        if (flat.lengthSqr() < 1.0E-6) flat = new Vec3(0.0, 0.0, 1.0);
        Vec3 ahead = eye.add(flat.normalize().scale(14.0));
        spawnAt(client, new Vec3(ahead.x, client.player.getY(), ahead.z));
        return true;
    }

    /** Spawns into the PLAYER'S CURRENT dimension (not always the overworld) and snaps to the
     * terrain surface at that column so the cue is never buried or in the wrong world. */
    public static void spawnAt(Minecraft client, Vec3 position) {
        IntegratedServer server = client.getSingleplayerServer();
        if (server == null || client.player == null) return;
        ResourceKey<Level> dimension = client.player.level().dimension();
        server.execute(() -> {
            ServerLevel level = server.getLevel(dimension);
            if (level == null) return;
            WatcherEntity watcher = NightwatchEntities.WATCHER.create(level, EntitySpawnReason.TRIGGERED);
            if (watcher == null) return;
            BlockPos column = BlockPos.containing(position.x, position.y, position.z);
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            watcher.snapTo(position.x, surfaceY, position.z, 0.0F, 0.0F);
            level.addFreshEntity(watcher);
        });
    }

    /** Discards every Watcher across every dimension. Used on the 6s lifetime and on world leave. */
    public static void vanishAll(Minecraft client) {
        IntegratedServer server = client.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            for (ServerLevel level : server.getAllLevels()) {
                for (WatcherEntity watcher : level.getEntitiesOfClass(WatcherEntity.class, EVERYWHERE)) {
                    watcher.discard();
                }
            }
        });
    }

    private static final AABB EVERYWHERE =
        new AABB(-3.0E7, -512.0, -3.0E7, 3.0E7, 512.0, 3.0E7);
}
