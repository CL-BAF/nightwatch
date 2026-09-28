package dev.cameron.nightwatch.loom;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Pure mapping from LoomSequence cues to the closed Action allowlist id strings.
 * The strings here MUST stay identical to Runtime's Action.java SOUND/EFFECT allowlists;
 * LoomCueMapperCheck rejects drift at test time. Never model-driven. */
public final class LoomCueMapper {
    public record LoomCueSpec(boolean effect, String id, String argument, boolean statusText) {}

    public static final Set<String> EFFECT_IDS = Set.of(
        "pale_thread", "island_morph", "echo_spawn", "echo_dissolve", "door_appear");
    public static final Set<String> SOUND_IDS = Set.of(
        "door_knock", "door_open", "door_close", "footstep_distant");
    public static final Set<String> ECHO_ARGUMENTS = Set.of("copycat", "diverger", "facing", "facing-closer");

    public List<LoomCueSpec> map(List<LoomSequence.Cue> cues) {
        List<LoomCueSpec> out = new ArrayList<>();
        for (LoomSequence.Cue cue : cues) {
            LoomCueSpec spec = mapOne(cue);
            if (spec != null) out.add(spec);
        }
        return out;
    }

    public LoomCueSpec mapOne(LoomSequence.Cue cue) {
        return switch (cue.kind()) {
            case THREAD_REVEAL -> new LoomCueSpec(true, "pale_thread", cue.argument(), false);
            case ISLAND_MORPH -> new LoomCueSpec(true, "island_morph", cue.argument(), false);
            case ECHO_SPAWN -> new LoomCueSpec(true, "echo_spawn", requireEchoArg(cue), false);
            case ECHO_DISSOLVE -> new LoomCueSpec(true, "echo_dissolve", requireEchoArg(cue), false);
            case DOOR_APPEAR -> new LoomCueSpec(true, "door_appear", cue.argument(), false);
            case DOOR_OPEN -> new LoomCueSpec(false, "door_open", "", false);
            case DOOR_CLOSE -> new LoomCueSpec(false, "door_close", "", false);
            case STATUS -> new LoomCueSpec(false, "", cue.argument(), true);
            case CAUGHT_RELOCATE -> null; // never a client effect; driver routes it to the teleport payload
        };
    }

    private static String requireEchoArg(LoomSequence.Cue cue) {
        if (cue.argument() == null || !ECHO_ARGUMENTS.contains(cue.argument())) {
            throw new IllegalStateException("bad echo argument: " + cue.argument());
        }
        return cue.argument();
    }
}
