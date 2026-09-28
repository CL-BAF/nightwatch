package dev.cameron.nightwatch.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Maps the Director's closed sound allowlist to vanilla sound events. Client-side, singleplayer only.
 *
 * <p>Mapping (kept deliberately un-attributable to any real mob):
 * <ul>
 *   <li>{@code footstep_distant} - gravel step, low pitch, quiet, offset from the player.</li>
 *   <li>{@code door_knock} - wooden button click, quiet, offset from the player.</li>
 *   <li>{@code whisper} - player breath, quiet, at the player.</li>
 *   <li>{@code lantern_dip} - candle extinguish at the nearest torch/lantern + brief smoke.</li>
 *   <li>{@code door_open}/{@code door_close} - wooden door open/close, offset from the player (Loom hook backup).</li>
 * </ul>
 * No world blocks are changed; sounds are local playback only.
 */
public final class SoundEffects {
    private SoundEffects() {}

    public static void play(Minecraft client, String soundId) {
        if (client.level == null || client.player == null) return;
        Level level = client.level;
        Vec3 eye = client.player.getEyePosition();
        switch (soundId) {
            case "footstep_distant" -> {
                double[] at = behind(client, 30.0);
                level.playLocalSound(at[0], at[1], at[2], SoundEvents.GRAVEL_STEP, SoundSource.BLOCKS, 0.25f, 0.6f, false);
            }
            case "door_knock" -> {
                double[] at = distant(client, 6.0);
                level.playLocalSound(at[0], at[1], at[2], SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5f, 0.85f, false);
            }
            case "whisper" ->
                level.playLocalSound(eye.x, eye.y, eye.z, SoundEvents.PLAYER_BREATH, SoundSource.AMBIENT, 0.25f, 0.6f, false);
            case "lantern_dip" -> lanternDip(client);
            case "door_open" -> {
                double[] at = distant(client, 5.0);
                level.playLocalSound(at[0], at[1], at[2], SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 0.4f, 0.9f, false);
            }
            case "door_close" -> {
                double[] at = distant(client, 5.0);
                level.playLocalSound(at[0], at[1], at[2], SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 0.4f, 0.9f, false);
            }
            default -> { }
        }
    }

    private static void lanternDip(Minecraft client) {
        BlockPos light = LightSources.nearestLight(client, 12);
        double x;
        double y;
        double z;
        if (light != null) {
            x = light.getX() + 0.5;
            y = light.getY() + 0.5;
            z = light.getZ() + 0.5;
        } else {
            Vec3 eye = client.player.getEyePosition();
            x = eye.x;
            y = eye.y;
            z = eye.z;
        }
        client.level.playLocalSound(x, y, z, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 0.15f, 1.0f, false);
        VisualEffects.smokeAt(client, x, y, z, 2);
    }

    private static double[] distant(Minecraft client, double distance) {
        Vec3 eye = client.player.getEyePosition();
        RandomSource random = client.player.getRandom();
        double angle = random.nextDouble() * Math.PI * 2.0;
        return new double[] { eye.x + Math.cos(angle) * distance, eye.y, eye.z + Math.sin(angle) * distance };
    }

    /** ~30 blocks directly behind the player, per design docs: footsteps where nothing can be seen. */
    private static double[] behind(Minecraft client, double distance) {
        Vec3 eye = client.player.getEyePosition();
        Vec3 view = client.player.getViewVector(1.0f);
        Vec3 flat = new Vec3(view.x, 0.0, view.z);
        if (flat.lengthSqr() < 1.0E-6) flat = new Vec3(0.0, 0.0, 1.0);
        Vec3 dir = flat.normalize().scale(-distance);
        return new double[] { eye.x + dir.x, eye.y, eye.z + dir.z };
    }
}
