package dev.cameron.nightwatch.loom.driver;

import dev.cameron.nightwatch.loom.LoomCueMapper;
import dev.cameron.nightwatch.loom.LoomSequence;
import net.minecraft.world.phys.Vec3;

/** Client-tick driver the Loom hands to Fabric's NightwatchClient integration.
 * Fabric calls one method per tick and forwards Director actions; no engine/ or
 * NightwatchClient edits made by Loom. Sequence-driven only, never model-driven.
 * The choice junction is decided by real-time player behavior, not UI buttons:
 * approach it (within reach = approach), look away from it (crosshair off it = look-away),
 * or hold still with it on-screen (patience path). */
public final class LoomDirectorBridge {
    public interface Hooks {
        void effect(String allowlistedEffectId, String argument);
        void sound(String allowlistedSoundId);
        void status(String boundedText);
        default void teleportEnter() { }
        default void teleportExit(boolean caught) { }
        default boolean doorTriggered() { return false; }
        default boolean frameTriggered() { return false; }
        default Vec3 echoPos() { return null; }
        default boolean crosshairAtEcho() { return false; }
    }

    public static final LoomDirectorBridge INSTANCE = new LoomDirectorBridge();
    public final LoomSequence sequence = new LoomSequence();
    private final LoomCueMapper mapper = new LoomCueMapper();
    private Hooks hooks;
    private boolean doorOffered, doorEntered;
    private long lastFrameAt;
    private int lookAwayTicks;

    public void register(Hooks hooks) { this.hooks = hooks; }

    /** Fabric forwards every Director-issued allowlisted action here. Only the overworld
     * pale door signal is consumed; Loom stage arguments are engine-internal. */
    public void onDirectorAction(String effectId, String argument) {
        if ("door_appear".equals(effectId) && sequence.stage() == LoomSequence.Stage.DORMANT && !doorOffered) {
            doorOffered = true;
            sequence.openDoor();
            flush();
        }
    }

    /** One call per client tick from Fabric; moving/pos are observations, never commands. */
    public void tick(boolean inLoom, Vec3 pos, boolean moving, long nowMs) {
        if (hooks == null || sequence.stage() == LoomSequence.Stage.DORMANT) return;
        LoomSequence.Stage before = sequence.stage();
        if (inLoom) tickInLoom(pos, moving); else tickOverworld();
        if (before == LoomSequence.Stage.ENTRY && doorEntered) {
            sequence.observe(LoomSequence.Input.ENTERED_DOOR);
            if (sequence.stage() == LoomSequence.Stage.THREAD) { hooks.teleportEnter(); doorOffered = false; doorEntered = false; }
        }
        if (inLoom && sequence.stage() == LoomSequence.Stage.SURVIVAL && hooks.frameTriggered()
            && nowMs - lastFrameAt > 1500) {
            lastFrameAt = nowMs;
            sequence.observe(LoomSequence.Input.ENTERED_DOOR);
        }
        flush();
    }

    private void tickOverworld() {
        if (sequence.stage() == LoomSequence.Stage.ENTRY && hooks.doorTriggered()) doorEntered = true;
    }

    private void tickInLoom(Vec3 pos, boolean moving) {
        switch (sequence.stage()) {
            case THREAD -> {
                if (hooks.echoPos() != null) sequence.observe(LoomSequence.Input.FACING_ECHO);
                else sequence.observe(moving ? LoomSequence.Input.MOVED : LoomSequence.Input.STOPPED);
            }
            case ECHO_COPYCAT, ECHO_DIVERGER -> {
                sequence.observe(moving ? LoomSequence.Input.MOVED : LoomSequence.Input.STOPPED);
            }
            case ECHO_FACING -> decideFacing(pos);
            default -> { }
        }
    }

    /** Echo III choice, resolved from real player behavior only. */
    private void decideFacing(Vec3 pos) {
        Vec3 echo = hooks.echoPos();
        if (echo == null) { sequence.observe(LoomSequence.Input.STOPPED); return; }
        double reach = 2.5;
        if (pos.distanceTo(echo) < reach) {
            lookAwayTicks = 0;
            sequence.observe(LoomSequence.Input.CHOSE_APPROACH);
            return;
        }
        if (!hooks.crosshairAtEcho()) {
            if (++lookAwayTicks >= 15) { lookAwayTicks = 0; sequence.observe(LoomSequence.Input.CHOSE_LOOK_AWAY); return; }
        } else {
            lookAwayTicks = 0;
        }
        sequence.observe(LoomSequence.Input.STOPPED); // standing still is the patience path
    }

    /** Reset when leaving the Loom (Fabric calls after its teleport bookkeeping). */
    public void resetForWorldChange() {
        sequence.observe(LoomSequence.Input.WALKED_BACK);
        doorOffered = false; doorEntered = false; lookAwayTicks = 0;
    }

    private void flush() {
        for (LoomSequence.Cue cue : sequence.takeCues()) {
            switch (cue.kind()) {
                case CAUGHT_RELOCATE -> { hooks.teleportExit(true); hooks.sound("door_knock"); }
                case EXIT_REQUEST -> { hooks.teleportExit(false); hooks.sound("door_knock"); }
                default -> {
                    var spec = mapper.mapOne(cue);
                    if (spec == null) continue;
                    if (spec.effect()) hooks.effect(spec.id(), spec.argument());
                    else if (spec.statusText()) hooks.status(spec.argument());
                    else hooks.sound(spec.id());
                }
            }
        }
    }
}
