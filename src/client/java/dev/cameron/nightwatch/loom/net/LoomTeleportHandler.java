package dev.cameron.nightwatch.loom.net;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Integrated-server relocation for the Loom sequence (singleplayer only: this mod is
 * environment=client, so on a dedicated server this class never loads).
 * Invariants (loom/dimension-sequence.md): teleports only from LoomSequence policy,
 * coordinates computed server-side only (payload carries an int, never positions),
 * relocation reversible, zero damage, everything on the server game thread
 * via executeIfPossible. */
public final class LoomTeleportHandler {
    private static boolean registered;
    /** Server-thread-only anchor store (Reviewer: never touch from netty context). */
    private static final Map<UUID, Anchor> ANCHORS = new HashMap<>();

    private record Anchor(Vec3 position, ResourceKey<Level> dimension) {}

    public static final ResourceKey<Level> LOOM =
        Registries.levelStemToLevel(ResourceKey.create(Registries.LEVEL_STEM,
            Identifier.fromNamespaceAndPath("nightwatch", "loom")));

    // fixed Loom-side constants; client payload data never reaches a coordinate
    private static final Vec3 LOOM_DOOR = new Vec3(0.5, -61.0, 0.5);
    private static final BlockPos LOOM_FRAME_CENTER = new BlockPos(0, -61, 0);

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        PayloadTypeRegistry.serverboundPlay().register(LoomPayload.TYPE, LoomPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(LoomPayload.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            ctx.server().executeIfPossible(() -> handle(payload.action(), player, ctx.server()));
        });
    }

    private static void handle(int action, ServerPlayer player, net.minecraft.server.MinecraftServer server) {
        switch (action) {
            case LoomPayload.ENTER -> enter(player, server);
            case LoomPayload.EXIT_NORMAL, LoomPayload.EXIT_CAUGHT -> exit(player, server);
            default -> { } // fail-closed: unknown action = no teleport
        }
    }

    private static void enter(ServerPlayer player, net.minecraft.server.MinecraftServer server) {
        if (player.level().dimension() == LOOM) return; // already inside
        if (ANCHORS.containsKey(player.getUUID())) return; // mid-journey: way back is the Loom's own door
        ServerLevel loom = server.getLevel(LOOM);
        if (loom == null) return; // datapack not applied: the door does not open (fail-closed, no damage)
        ANCHORS.put(player.getUUID(), new Anchor(player.position(), player.level().dimension()));
        buildDoorFrame(loom);
        player.teleportTo(loom, LOOM_DOOR.x, LOOM_DOOR.y, LOOM_DOOR.z, java.util.Set.of(),
            player.getYRot(), player.getXRot(), false);
    }

    private static void exit(ServerPlayer player, net.minecraft.server.MinecraftServer server) {
        Anchor anchor = ANCHORS.get(player.getUUID());
        if (player.level().dimension() != LOOM) { ANCHORS.remove(player.getUUID()); return; }
        if (anchor == null) return; // no anchor: never teleport blind
        ServerLevel target = server.getLevel(anchor.dimension());
        if (target == null) return;
        ANCHORS.remove(player.getUUID()); // reversible by design: the overworld door can always be re-offered
        player.teleportTo(target, anchor.position().x, anchor.position().y, anchor.position().z,
            java.util.Set.of(), player.getYRot(), player.getXRot(), false);
    }

    /** Idempotent pale pad + frame for the way back; the visual door itself is client-side (Fabric). */
    private static void buildDoorFrame(ServerLevel loom) {
        BlockPos center = LOOM_FRAME_CENTER;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos pos = new BlockPos(center.getX() + dx, center.getY() - 1, center.getZ() + dz);
                if (loom.getBlockState(pos).isAir()) loom.setBlock(pos, Blocks.QUARTZ_BLOCK.defaultBlockState(), 3, 0);
            }
        }
        pour(loom, center.west(), 1, Blocks.QUARTZ_BLOCK);       // left pillar (-61, -62)
        pour(loom, center.east(), 1, Blocks.QUARTZ_BLOCK);       // right pillar
        pour(loom, center.above(1), 0, Blocks.QUARTZ_BLOCK);     // lintel (-60)
    }

    private static void pour(ServerLevel loom, BlockPos base, int extra, net.minecraft.world.level.block.Block block) {
        for (int i = 0; i <= extra; i++) {
            BlockPos pos = base.above(i);
            if (loom.getBlockState(pos).isAir()) loom.setBlock(pos, block.defaultBlockState(), 3, 0);
        }
    }
}
