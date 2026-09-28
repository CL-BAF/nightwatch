package dev.cameron.nightwatch.loom;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Pure Loom sequence state machine. No Minecraft imports; Fabric/Runtime map cues to effects.
 * Survivability rules: every path ends in exit, every state can fall back to the door,
 * echo dissolves are never damage. */
public final class LoomSequence {
    public enum Input { MOVED, STOPPED, IDLE_CYCLE, WALKED_BACK, FACING_ECHO,
        CHOSE_LOOK_AWAY, CHOSE_APPROACH, CHOSE_STAND_STILL, ENTERED_DOOR }
    public enum Stage { DORMANT, ENTRY, THREAD, ECHO_COPYCAT, ECHO_DIVERGER, ECHO_FACING,
        SURVIVAL, DOOR }

    public enum CueKind { THREAD_REVEAL, ECHO_SPAWN, ECHO_DISSOLVE, ISLAND_MORPH,
        DOOR_APPEAR, DOOR_OPEN, DOOR_CLOSE, CAUGHT_RELOCATE, EXIT_REQUEST, STATUS }

    public record Cue(CueKind kind, String argument) {}

    private Stage stage = Stage.DORMANT;
    private final Deque<Cue> cues = new ArrayDeque<>();
    private int standTicks;
    private int idleCycles;
    private String status = "";

    public Stage stage() { return stage; }
    public List<Cue> takeCues() { List<Cue> out = new ArrayList<>(cues); cues.clear(); return out; }
    public String status() { return status; }

    /** Ordered by the design doc when the entity offers itself. */
    public void openDoor() { stage = Stage.ENTRY; cues.add(new Cue(CueKind.DOOR_APPEAR, "overworld-night")); }

    public void observe(Input input) {
        switch (stage) {
            case DORMANT -> { }
            case ENTRY -> entered(input, () -> { stage = Stage.THREAD; emit(CueKind.DOOR_OPEN, null); emit(CueKind.THREAD_REVEAL, "long"); });
            case THREAD -> atThread(input);
            case ECHO_COPYCAT, ECHO_DIVERGER -> inEchoStages(input);
            case ECHO_FACING -> atFacingEcho(input);
            case SURVIVAL -> onSurvival(input);
            case DOOR -> stage = Stage.DORMANT;
        }
    }

    private void entered(Input input, Runnable go) {
        if (input == Input.ENTERED_DOOR) go.run();
    }

    private void atThread(Input input) {
        if (input == Input.WALKED_BACK) { backToDoor(); return; }
        if (input == Input.FACING_ECHO) { idleCycles = 0; stage = Stage.ECHO_COPYCAT; emit(CueKind.ECHO_SPAWN, "copycat"); }
        else if (input == Input.IDLE_CYCLE) { if (++idleCycles >= 3) backToDoor(); else emit(CueKind.ISLAND_MORPH, "worn"); }
        else idleCycles = 0;
    }

    private void inEchoStages(Input input) {
        if (input == Input.WALKED_BACK) { backToDoor(); return; }
        if (input == Input.MOVED && stage == Stage.ECHO_COPYCAT) {
            idleCycles = 0;
            stage = Stage.ECHO_DIVERGER;
            emit(CueKind.ECHO_DISSOLVE, "copycat"); // illusion; never a damage cue
            status = "the echo fell";
        } else if (input == Input.MOVED && stage == Stage.ECHO_DIVERGER) {
            idleCycles = 0;
            stage = Stage.ECHO_FACING;
            emit(CueKind.ECHO_SPAWN, "facing");
            status = "";
        } else if (input == Input.STOPPED) {
            emit(CueKind.ISLAND_MORPH, "worn");
        } else if (input == Input.IDLE_CYCLE && ++idleCycles >= 3) backToDoor();
    }

    private void atFacingEcho(Input input) {
        if (input == Input.WALKED_BACK) { backToDoor(); return; }
        if (input == Input.IDLE_CYCLE) { // the promised forced routing: refusing to choose still exits
            if (++idleCycles >= 2) { idleCycles = 0; standTicks = 0; through("released", "facing"); }
            return;
        }
        idleCycles = 0;
        switch (input) {
            case CHOSE_LOOK_AWAY -> through("let past", "facing");
            case CHOSE_APPROACH -> through("scatter", "facing");
            case CHOSE_STAND_STILL -> { stage = Stage.ECHO_FACING; emit(CueKind.ECHO_SPAWN, "facing-closer"); status = "standing"; }
            case MOVED -> { stage = Stage.ECHO_COPYCAT; emit(CueKind.ECHO_SPAWN, "copycat"); standTicks = 0; }
            case STOPPED -> {
                standTicks++;
                if (standTicks >= 10) { standTicks = 0; stage = Stage.SURVIVAL; emit(CueKind.THREAD_REVEAL, "long-ahead"); }
            }
            default -> { }
        }
    }

    private void through(String newStatus, String dissolve) {
        stage = Stage.SURVIVAL;
        emit(CueKind.ECHO_DISSOLVE, dissolve);
        emit(CueKind.THREAD_REVEAL, "long-ahead");
        status = newStatus;
    }

    private void onSurvival(Input input) {
        if (input == Input.WALKED_BACK) { backToDoor(); return; }
        if (input == Input.ENTERED_DOOR) {
            stage = Stage.DOOR;
            emit(CueKind.EXIT_REQUEST, null);
            emit(CueKind.DOOR_CLOSE, null);
            status = "safe";
        }
        else if (input == Input.IDLE_CYCLE) relocate();
    }

    private void backToDoor() { status = "retrace"; stage = Stage.DOOR; emit(CueKind.EXIT_REQUEST, null); emit(CueKind.DOOR_CLOSE, null); }

    private void relocate() {
        stage = Stage.ENTRY;
        status = "caught";
        emit(CueKind.CAUGHT_RELOCATE, "entry-door");
        emit(CueKind.DOOR_APPEAR, "overworld-night");
    }

    private void emit(CueKind kind, String arg) { cues.add(new Cue(kind, arg)); }

    @Override public String toString() { return stage + ":" + status + ":" + cues.size() + " cues"; }
}
