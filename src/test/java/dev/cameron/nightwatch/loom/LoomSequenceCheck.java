package dev.cameron.nightwatch.loom;

import java.util.List;
import java.util.Set;

/** Run with tools/check-core.sh without Minecraft or a model.
 * Proves the survivability guarantees: no soft-locks, way back from every stage,
 * echo dissolves are never damage, stand-still releases without a caught event. */
public final class LoomSequenceCheck {
    private static final List<LoomSequence.Input> ALL_INPUTS = List.of(LoomSequence.Input.values());

    public static void main(String[] args) {
        happyDrive();
        wayBackFromEveryStage();
        bruteForceNoLock(3);
        noDamageCuesDuringEchoes();
        standStillReleases();
        idleReleasesFromFacingEcho();
        idleRoutesBackFromThreadAndEchoStages();
        System.out.println("LoomSequenceCheck passed");
    }

    private static LoomSequence fresh() {
        LoomSequence loom = new LoomSequence();
        loom.openDoor();
        loom.observe(LoomSequence.Input.ENTERED_DOOR);
        return loom;
    }

    private static void happyDrive() {
        LoomSequence loom = fresh();
        for (LoomSequence.Input input : List.of(LoomSequence.Input.FACING_ECHO, LoomSequence.Input.MOVED,
            LoomSequence.Input.MOVED, LoomSequence.Input.CHOSE_LOOK_AWAY, LoomSequence.Input.ENTERED_DOOR)) {
            loom.observe(input);
        }
        if (loom.stage() != LoomSequence.Stage.DOOR || !"safe".equals(loom.status())) {
            throw new AssertionError("happy path did not exit safely: " + loom);
        }
        for (LoomSequence.Cue cue : loom.takeCues()) {
            if (cue.kind() == LoomSequence.CueKind.CAUGHT_RELOCATE) throw new AssertionError("caught on the happy path");
        }
    }

    private static void wayBackFromEveryStage() {
        List<List<LoomSequence.Input>> arrivals = List.of(
            List.of(),
            List.of(LoomSequence.Input.FACING_ECHO),
            List.of(LoomSequence.Input.FACING_ECHO, LoomSequence.Input.MOVED),
            List.of(LoomSequence.Input.FACING_ECHO, LoomSequence.Input.MOVED, LoomSequence.Input.MOVED),
            List.of(LoomSequence.Input.FACING_ECHO, LoomSequence.Input.MOVED, LoomSequence.Input.MOVED,
                LoomSequence.Input.CHOSE_LOOK_AWAY));
        for (List<LoomSequence.Input> arrival : arrivals) {
            LoomSequence loom = fresh();
            for (LoomSequence.Input input : arrival) loom.observe(input);
            loom.observe(LoomSequence.Input.WALKED_BACK);
            if (loom.stage() != LoomSequence.Stage.DOOR || !"retrace".equals(loom.status())) {
                throw new AssertionError("no way back from " + loom);
            }
        }
    }

    /** Any input sequence of length <= depth, followed by WALKED_BACK, must be able to leave. */
    private static void bruteForceNoLock(int depth) {
        walk(new LoomSequence.Input[0], depth, sequence -> {
            LoomSequence loom = fresh();
            for (LoomSequence.Input input : sequence) {
                loom.observe(input);
                if (loom.stage() == LoomSequence.Stage.DORMANT) return;
            }
            loom.observe(LoomSequence.Input.WALKED_BACK);
            if (loom.stage() != LoomSequence.Stage.DOOR && loom.stage() != LoomSequence.Stage.DORMANT) {
                throw new AssertionError("locked in after " + List.of((Object[]) sequence) + " -> " + loom);
            }
        });
    }

    interface Sink { void accept(LoomSequence.Input[] sequence); }

    private static void walk(LoomSequence.Input[] prefix, int depth, Sink sink) {
        sink.accept(prefix);
        if (prefix.length == depth) return;
        for (LoomSequence.Input input : ALL_INPUTS) {
            LoomSequence.Input[] next = new LoomSequence.Input[prefix.length + 1];
            System.arraycopy(prefix, 0, next, 0, prefix.length);
            next[prefix.length] = input;
            walk(next, depth, sink);
        }
    }

    private static void noDamageCuesDuringEchoes() {
        LoomSequence loom = fresh();
        loom.observe(LoomSequence.Input.FACING_ECHO);
        loom.observe(LoomSequence.Input.MOVED);
        List<LoomSequence.Cue> cues = loom.takeCues();
        if (cues.stream().noneMatch(c -> c.kind() == LoomSequence.CueKind.ECHO_DISSOLVE)) {
            throw new AssertionError("copycat dissolve missing");
        }
        for (LoomSequence.Cue cue : cues) {
            if (cue.kind() == LoomSequence.CueKind.CAUGHT_RELOCATE) throw new AssertionError("echo death caused a caught event");
            if (cue.kind() == LoomSequence.CueKind.ECHO_DISSOLVE && !Set.of("copycat").contains(cue.argument())) {
                throw new AssertionError("unexpected dissolve payload " + cue.argument());
            }
        }
    }

    /** Reviewer L6: refusing every prompt for a full cycle must still route toward the exit. */
    private static void idleReleasesFromFacingEcho() {
        LoomSequence loom = fresh();
        loom.observe(LoomSequence.Input.FACING_ECHO);
        loom.observe(LoomSequence.Input.MOVED);
        loom.observe(LoomSequence.Input.MOVED); // now at ECHO_FACING
        loom.observe(LoomSequence.Input.IDLE_CYCLE);
        if (loom.stage() != LoomSequence.Stage.ECHO_FACING) {
            throw new AssertionError("one idle cycle released early: " + loom);
        }
        loom.observe(LoomSequence.Input.IDLE_CYCLE);
        if (loom.stage() != LoomSequence.Stage.SURVIVAL) throw new AssertionError("idle cycles never release from ECHO_FACING: " + loom);
        for (LoomSequence.Cue cue : loom.takeCues()) {
            if (cue.kind() == LoomSequence.CueKind.CAUGHT_RELOCATE) throw new AssertionError("idle release became a caught event");
        }
        // then the player must still be able to walk back out
        loom.observe(LoomSequence.Input.WALKED_BACK);
        if (loom.stage() != LoomSequence.Stage.DOOR) throw new AssertionError("way back gone after idle release");
    }

    private static void idleRoutesBackFromThreadAndEchoStages() {
        List<List<LoomSequence.Input>> idleArrivals = List.of(
            List.of(),
            List.of(LoomSequence.Input.FACING_ECHO),
            List.of(LoomSequence.Input.FACING_ECHO, LoomSequence.Input.MOVED));
        for (List<LoomSequence.Input> arrival : idleArrivals) {
            LoomSequence loom = fresh();
            for (LoomSequence.Input input : arrival) loom.observe(input);
            for (int i = 0; i < 3; i++) loom.observe(LoomSequence.Input.IDLE_CYCLE);
            if (loom.stage() != LoomSequence.Stage.DOOR) {
                throw new AssertionError("idle cycles did not route back to the door from arrival " + loom);
            }
            if (!"retrace".equals(loom.status())) throw new AssertionError("idle retreat was not a retrace: " + loom);
            for (LoomSequence.Cue cue : loom.takeCues()) {
                if (cue.kind() == LoomSequence.CueKind.CAUGHT_RELOCATE) throw new AssertionError("idle routing became a caught event");
            }
        }
    }

    private static void standStillReleases() {
        LoomSequence loom = fresh();
        loom.observe(LoomSequence.Input.FACING_ECHO);
        loom.observe(LoomSequence.Input.MOVED);
        loom.observe(LoomSequence.Input.MOVED);
        loom.observe(LoomSequence.Input.CHOSE_STAND_STILL);
        for (int i = 0; i < 10; i++) loom.observe(LoomSequence.Input.STOPPED);
        if (loom.stage() != LoomSequence.Stage.SURVIVAL) throw new AssertionError("standing still never releases: " + loom);
        if ("caught".equals(loom.status())) throw new AssertionError("standing still became a caught event");
        for (LoomSequence.Cue cue : loom.takeCues()) {
            if (cue.kind() == LoomSequence.CueKind.CAUGHT_RELOCATE) throw new AssertionError("standing still caused a caught event");
        }
    }
}
