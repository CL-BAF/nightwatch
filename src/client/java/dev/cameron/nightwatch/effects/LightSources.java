package dev.cameron.nightwatch.effects;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

/**
 * Finds the nearest vanilla light block (torch/lantern/campfire/candle) for the lantern-dip cue.
 *
 * <p>The set of light blocks is computed once (lazily) from the block registry, so the scan does no
 * per-block registry lookups on the client thread; it only checks a {@link Set} membership and an
 * emission short-circuit.
 */
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
            if (client.level.getBlockState(pos).getLightEmission() <= 0) continue;
            if (!LightBlocks.INSTANCE.contains(client.level.getBlockState(pos).getBlock())) continue;
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

    /** Lazily built, registry-derived set of light-emitting decoration blocks we recognise. */
    private static final class LightBlocks {
        static final Set<Block> INSTANCE = build();

        private static Set<Block> build() {
            Set<Block> set = new HashSet<>();
            for (Block block : BuiltInRegistries.BLOCK) {
                String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
                if (path.contains("torch") || path.contains("lantern")
                    || path.contains("campfire") || path.contains("candle")) {
                    set.add(block);
                }
            }
            return set;
        }
    }
}
