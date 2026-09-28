package dev.cameron.nightwatch.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

/** Finds the nearest vanilla light block (torch/lantern/campfire/candle) for the lantern-dip cue. */
final class LightSources {
    private LightSources() {}

    static BlockPos nearestLight(Minecraft client, int radius) {
        if (client.level == null || client.player == null) return null;
        BlockPos center = client.player.blockPosition();
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);
        BlockPos nearest = null;
        double best = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            int light = client.level.getBlockState(pos).getLightEmission();
            if (light <= 0) continue;
            if (!isLight(client.level.getBlockState(pos))) continue;
            double dx = pos.getX() - center.getX();
            double dy = pos.getY() - center.getY();
            double dz = pos.getZ() - center.getZ();
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < best) {
                best = distance;
                nearest = pos.immutable();
                if (distance == 0.0) break;
            }
        }
        return nearest;
    }

    private static boolean isLight(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.contains("torch") || path.contains("lantern")
            || path.contains("campfire") || path.contains("candle");
    }
}
