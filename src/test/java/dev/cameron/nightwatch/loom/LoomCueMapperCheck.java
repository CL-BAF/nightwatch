package dev.cameron.nightwatch.loom;

import java.util.List;

/** Run with tools/check-core.sh without Minecraft. Verifies the cue→allowlist mapping
 * never drifts from Runtime's closed id/argument set and never produces effects from
 * the relocation path. */
public final class LoomCueMapperCheck {
    public static void main(String[] args) {
        LoomCueMapper mapper = new LoomCueMapper();

        LoomSequence loom = new LoomSequence();
        loom.openDoor();
        expect(mapper, loom.takeCues(), "door_appear");

        loom.observe(LoomSequence.Input.ENTERED_DOOR);
        expect(mapper, loom.takeCues(), "door_open", "pale_thread");

        loom.observe(LoomSequence.Input.FACING_ECHO);
        expect(mapper, loom.takeCues(), "echo_spawn:copycat");

        loom.observe(LoomSequence.Input.MOVED); // copycat dissolve + diverger spawn
        for (LoomSequence.Cue cue : loom.takeCues()) { checkId(mapper, cue); }

        loom.observe(LoomSequence.Input.MOVED); // facing echo spawn
        expect(mapper, loom.takeCues(), "echo_spawn:facing");

        loom.observe(LoomSequence.Input.CHOSE_APPROACH); // facing dissolve only in the closed whitelist
        for (LoomSequence.Cue cue : loom.takeCues()) checkId(mapper, cue);

        loom.observe(LoomSequence.Input.ENTERED_DOOR); // exit: close sound
        expect(mapper, loom.takeCues(), "door_close");

        // the caught path may never map to a client effect
        LoomSequence caught = new LoomSequence();
        caught.openDoor();
        caught.observe(LoomSequence.Input.ENTERED_DOOR);
        for (int i = 0; i < 3; i++) caught.observe(LoomSequence.Input.IDLE_CYCLE);
        caught.observe(LoomSequence.Input.FACING_ECHO);
        for (int i = 0; i < 3; i++) caught.observe(LoomSequence.Input.MOVED);
        caught.observe(LoomSequence.Input.CHOSE_LOOK_AWAY);
        for (LoomSequence.Input input : List.of(LoomSequence.Input.IDLE_CYCLE, LoomSequence.Input.IDLE_CYCLE,
            LoomSequence.Input.STOPPED, LoomSequence.Input.IDLE_CYCLE)) caught.observe(input);
        // SURVIVAL stall relocates; that cue is excluded from mapping output
        for (var cue : caught.takeCues()) {
            if (cue.kind() == LoomSequence.CueKind.CAUGHT_RELOCATE && mapper.mapOne(cue) != null) {
                throw new AssertionError("CAUGHT_RELOCATE became a client effect");
            }
        }

        // argument guard: mapper throws on unknown echo arguments
        try {
            mapper.mapOne(new LoomSequence.Cue(LoomSequence.CueKind.ECHO_SPAWN, "grey-smear"));
            throw new AssertionError("bad echo argument accepted");
        } catch (IllegalStateException expected) { }

        System.out.println("LoomCueMapperCheck passed");
    }

    private static void expect(LoomCueMapper mapper, List<LoomSequence.Cue> cues, String... ids) {
        List<String> mapped = mapper.map(cues).stream().map(LoomCueMapper.LoomCueSpec::id).toList();
        List<String> wantIds = java.util.Arrays.stream(ids).map(id -> id.split(":")[0]).toList();
        if (!(mapped.equals(wantIds))) throw new AssertionError("expected " + List.of(ids) + " got " + mapped);
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].contains(":")) {
                String wantArg = ids[i].split(":")[1];
                LoomCueMapper.LoomCueSpec spec = mapper.mapOne(cues.get(i));
                if (!spec.argument().equals(wantArg)) throw new AssertionError("arg expected " + wantArg + " got " + spec.argument());
            }
        }
    }

    private static void checkId(LoomCueMapper mapper, LoomSequence.Cue cue) {
        LoomCueMapper.LoomCueSpec spec = mapper.mapOne(cue);
        if (spec == null) {
            if (cue.kind() != LoomSequence.CueKind.CAUGHT_RELOCATE && cue.kind() != LoomSequence.CueKind.EXIT_REQUEST) {
                throw new AssertionError("unexpected unmapped cue " + cue);
            }
            return;
        }
        if (spec.effect() && !LoomCueMapper.EFFECT_IDS.contains(spec.id())) {
            throw new AssertionError("drifted effect id " + spec.id());
        }
        if (!spec.effect() && !spec.statusText() && !LoomCueMapper.SOUND_IDS.contains(spec.id())) {
            throw new AssertionError("drifted sound id " + spec.id());
        }
        if ((spec.id().startsWith("echo_") && !LoomCueMapper.ECHO_ARGUMENTS.contains(spec.argument()))) {
            throw new AssertionError("echo argument outside whitelist: " + spec.argument());
        }
    }
}
