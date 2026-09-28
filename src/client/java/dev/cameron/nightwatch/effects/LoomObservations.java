package dev.cameron.nightwatch.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side data sources for {@code LoomDirectorBridge.Hooks}, per Loom's durable contract in
 * loom/dimension-sequence.md ("Hook data-source contract").
 *
 * <p>Read-only observation: every position is derived from the player's own pose at cue time.
 * This class never teleports, damages, edits blocks or sends commands; the Loom driver alone
 * decides, and server relocation stays in LoomTeleportHandler.
 *
 * <p>Ghost placements: door 6.5 blocks ahead at door_appear; echo copycat 8 ahead, diverger
 * 12 ahead + 6 left (the forbidden path), facing 5 ahead at eye height. Null echo = none live.
 */
public final class LoomObservations {
    private static final Vec3 LOOM_FRAME_CENTER = new Vec3(0.5, -61.0, 0.5);
    private static final double DOOR_REACH = 1.5;
    private static final double DOOR_RAY = 2.5;
    private static final double FRAME_REACH = 1.2;
    private static final double ECHO_RANGE_SQ = 400.0; // 20 blocks
    private static final double ECHO_DOT = Math.cos(Math.toRadians(5.0));

    private static final long GHOST_REFRESH_MS = 500L;

    private Vec3 doorGhost;
    private Vec3 echoPos;
    private long lastDoorGhostMs;
    private long lastFrameGhostMs;

    /** Draw the offered door (and, inside the Loom, the fixed frame) as a rate-limited pale outline. */
    public void tick(Minecraft client, long nowMs, boolean inLoom) {
        if (client.player == null) return;
        if (doorGhost != null && nowMs - lastDoorGhostMs >= GHOST_REFRESH_MS) {
            lastDoorGhostMs = nowMs;
            VisualEffects.paleDoor(client, doorGhost, 1.0, 2.0);
        }
        if (inLoom && nowMs - lastFrameGhostMs >= GHOST_REFRESH_MS) {
            lastFrameGhostMs = nowMs;
            VisualEffects.paleDoor(client, LOOM_FRAME_CENTER, 7.0, 3.0);
        }
    }

    /** Update ghost placement from an allowlisted effect id; never trusts the model. */
    public void onEffect(String effectId, String argument, Minecraft client) {
        if (client.player == null) return;
        switch (effectId) {
            case "door_appear" -> doorGhost = ahead(client, 6.5, 0.0, 0.0);
            case "echo_spawn" -> echoPos = echoFor(argument, client);
            case "echo_dissolve" -> echoPos = null;
            default -> { }
        }
    }

    public void reset() {
        doorGhost = null;
        echoPos = null;
    }

    /** Player-chosen door entry: a use-click while the ghost is at hand or under the crosshair. */
    public boolean doorTriggered(Minecraft client) {
        if (doorGhost == null || client.player == null) return false;
        if (!client.options.keyUse.consumeClick()) return false;
        return client.player.position().distanceTo(doorGhost) < DOOR_REACH
            || rayDistance(client, doorGhost) < DOOR_RAY;
    }

    /** The fixed Loom frame pad centre. */
    public boolean frameTriggered(Minecraft client) {
        return client.player != null
            && client.player.position().distanceTo(LOOM_FRAME_CENTER) < FRAME_REACH;
    }

    public Vec3 echoPos() {
        return echoPos;
    }

    /** True when the local view is within ~5 degrees of the echo and it is within 20 blocks. */
    public boolean crosshairAtEcho(Minecraft client) {
        if (echoPos == null || client.player == null) return false;
        Vec3 toEcho = echoPos.subtract(client.player.getEyePosition());
        if (toEcho.lengthSqr() < 1.0E-4 || toEcho.lengthSqr() > ECHO_RANGE_SQ) return false;
        return client.player.getViewVector(1.0F).dot(toEcho.normalize()) > ECHO_DOT;
    }

    private static Vec3 echoFor(String stage, Minecraft client) {
        if (stage == null) return null;
        return switch (stage) {
            case "copycat" -> ahead(client, 8.0, 0.0, 0.0);
            case "diverger" -> ahead(client, 12.0, 6.0, 0.0);
            case "facing" -> ahead(client, 5.0, 0.0, 0.0);
            default -> null;
        };
    }

    private static Vec3 ahead(Minecraft client, double forward, double left, double up) {
        Vec3 eye = client.player.getEyePosition();
        Vec3 view = client.player.getViewVector(1.0F);
        Vec3 flat = new Vec3(view.x, 0.0, view.z);
        if (flat.lengthSqr() < 1.0E-6) flat = new Vec3(0.0, 0.0, 1.0);
        flat = flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x);
        return eye.add(flat.scale(forward)).add(right.scale(-left)).add(0.0, up, 0.0);
    }

    /** Perpendicular distance from the crosshair ray to a point; MAX_VALUE if behind the player. */
    private static double rayDistance(Minecraft client, Vec3 target) {
        Vec3 eye = client.player.getEyePosition();
        Vec3 view = client.player.getViewVector(1.0F);
        Vec3 to = target.subtract(eye);
        double along = to.dot(view);
        if (along <= 0.0) return Double.MAX_VALUE;
        return eye.add(view.scale(along)).distanceTo(target);
    }
}
