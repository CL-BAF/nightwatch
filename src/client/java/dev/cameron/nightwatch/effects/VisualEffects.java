package dev.cameron.nightwatch.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Maps the Director's closed effect allowlist to client-only vanilla particles.
 *
 * <p>Slice effects:
 * <ul>
 *   <li>{@code brief_sighting} - a fleeting pale figure ~14 blocks ahead; fake, no entity spawned.</li>
 *   <li>{@code shadow_flicker} - a few large smoke wisps at the edge of view.</li>
 *   <li>{@code particle_burst} - a sudden cloud around the player.</li>
 * </ul>
 *
 * <p>Loom ids ({@code pale_thread}, {@code island_morph}, {@code echo_spawn}, {@code echo_dissolve},
 * {@code door_appear}) are intentionally no-ops here; they arrive through the future LoomIntegration hook.
 * Nothing here changes blocks, entities or inventory.
 */
public final class VisualEffects {
    private VisualEffects() {}

    public static void trigger(Minecraft client, String effectType, String argument) {
        if (client.level == null || client.player == null) return;
        switch (effectType) {
            case "brief_sighting" -> briefSighting(client);
            case "shadow_flicker" -> shadowFlicker(client);
            case "particle_burst" -> particleBurst(client);
            default -> { }
        }
    }

    private static void briefSighting(Minecraft client) {
        Vec3 eye = client.player.getEyePosition();
        Vec3 view = client.player.getViewVector(1.0f);
        Vec3 flat = new Vec3(view.x, 0.0, view.z);
        if (flat.lengthSqr() < 1.0E-6) flat = new Vec3(0.0, 0.0, 1.0);
        Vec3 base = eye.add(flat.normalize().scale(14.0));
        RandomSource random = client.player.getRandom();
        for (int i = 0; i < 22; i++) {
            double y = base.y - 1.6 + random.nextDouble() * 1.85;
            spawn(client, ParticleTypes.WHITE_SMOKE,
                base.x + (random.nextDouble() - 0.5) * 0.55, y, base.z + (random.nextDouble() - 0.5) * 0.55);
        }
        for (int i = 0; i < 6; i++) {
            double y = base.y - 1.6 + random.nextDouble() * 1.85;
            spawn(client, ParticleTypes.SOUL,
                base.x + (random.nextDouble() - 0.5) * 0.5, y, base.z + (random.nextDouble() - 0.5) * 0.5);
        }
    }

    private static void shadowFlicker(Minecraft client) {
        Vec3 eye = client.player.getEyePosition();
        RandomSource random = client.player.getRandom();
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 5.0 + random.nextDouble() * 3.0;
            spawn(client, ParticleTypes.LARGE_SMOKE,
                eye.x + Math.cos(angle) * distance,
                eye.y + (random.nextDouble() - 0.5) * 1.5,
                eye.z + Math.sin(angle) * distance);
        }
    }

    private static void particleBurst(Minecraft client) {
        Vec3 eye = client.player.getEyePosition();
        RandomSource random = client.player.getRandom();
        for (int i = 0; i < 32; i++) {
            spawn(client, ParticleTypes.SMOKE,
                eye.x + (random.nextDouble() - 0.5) * 3.0,
                eye.y - 1.0 + random.nextDouble() * 2.0,
                eye.z + (random.nextDouble() - 0.5) * 3.0);
        }
    }

    static void smokeAt(Minecraft client, double x, double y, double z, int count) {
        for (int i = 0; i < count; i++) spawn(client, ParticleTypes.SMOKE, x, y, z);
    }

    /**
     * A free-standing pale outline facing the player (Loom's "visible door" / frame cue).
     * Client-side particles only: no block is placed, nothing interacts, nothing is revealed
     * beyond a doorway suggests. Caller rate-limits the emission.
     */
    static void paleDoor(Minecraft client, Vec3 base, double width, double height) {
        Vec3 view = client.player.getViewVector(1.0F);
        Vec3 flat = new Vec3(view.x, 0.0, view.z);
        if (flat.lengthSqr() < 1.0E-6) flat = new Vec3(0.0, 0.0, 1.0);
        flat = flat.normalize();
        Vec3 half = new Vec3(-flat.z, 0.0, flat.x).scale(width * 0.5);
        for (int i = 0; i <= 8; i++) {
            double f = i / 8.0;
            spawn(client, ParticleTypes.WHITE_SMOKE, base.x + half.x, base.y + f * height, base.z + half.z);
            spawn(client, ParticleTypes.WHITE_SMOKE, base.x - half.x, base.y + f * height, base.z - half.z);
        }
        for (int i = 0; i <= 6; i++) {
            double f = 1.0 - 2.0 * (i / 6.0);
            spawn(client, ParticleTypes.WHITE_SMOKE, base.x + half.x * f, base.y, base.z + half.z * f);
            spawn(client, ParticleTypes.WHITE_SMOKE, base.x + half.x * f, base.y + height, base.z + half.z * f);
        }
        RandomSource random = client.player.getRandom();
        for (int i = 0; i < 4; i++) {
            spawn(client, ParticleTypes.SOUL,
                base.x + half.x + (random.nextDouble() - 0.5) * 0.4,
                base.y + height + random.nextDouble() * 0.8,
                base.z + half.z + (random.nextDouble() - 0.5) * 0.4);
        }
    }

    private static void spawn(Minecraft client, ParticleOptions particle, double x, double y, double z) {
        client.level.addParticle(particle, x, y, z, 0.0, 0.0, 0.0);
    }
}
