# Nightwatch — operator human-test script (Reviewer-maintained)

Status: DRAFT v1 (2026-09-29). This consolidates every DEFERRED in-game verification leg into one operator-runnable session. Nobody passes these by proxy; agents verify statically and via check-core/gradle only. When Lead calls content-complete, this script IS the test session. Report results with timestamps + `run/logs/latest.log` tail to Reviewer.

## Pre-flight (operator, once)

1. Set `JAVA_HOME=C:\Users\Cameron\AppData\Roaming\PrismLauncher\java\java-runtime-epsilon` for any gradle/sidecar shell (`java -version` → 25.0.1).
2. Build the CURRENT HEAD: `.\gradlew.bat build` → jar in `build\libs\nightwatch-0.1.0.jar`. Gate status: check-core has been green since 9b18a2c (engine glob + all three checks); verify it is still green on the tree you build from.
2b. **WORLD-CREATION SMOKE GATE (standing, added 2026-09-29 after session F1):** after ANY resource/registry-affecting change (data JSONs, registries, dimension/worldgen files), a quickPlay world-creation smoke must pass BEFORE the session starts: `.\gradlew.bat runClient --args="--quickPlaySingleplayer SmokeTest"` (or any fresh singleplayer world entry) must reach an in-world state with a registry-phase-clean log — no `Registry Loading` / `Failed to load registries` / `Stopping!` lines. Main-menu boot is INSUFFICIENT evidence: only world creation exercises RegistryDataLoader on mod data. Session F1 reference: a dimension_type JSON parse failure was invisible to main-menu boot and fatal at world creation (client exited at "preparing for world creation").
3. Verify `bash tools/check-core.sh` (JAVA_HOME set) prints all three checks passing on the tree you built from.
4. Optional legs need: Ollama running with `qwen3:4b` (or set `ai.model`) — OR set `ai.enabled=false` to force the offline RuleWriter for deterministic pacing tests. Mic legs need: `python voice\sidecar.py` (venv with `pip install -r voice\requirements.txt`) + `microphone.enabled=true` in the profile's `config\nightwatch.properties`.
5. Fresh singleplayer world, survival, normal difficulty, daylight start. Keep the world tab-list OPEN during Stage 2 checks.

## A. Gate 0 human legs (the deferred set)

| # | Exact action | Exact expected observation | PASS/FAIL |
|---|---|---|---|
| A1 | Play normally 0:00–5:00 after world join. No chat. | ZERO mod activity: no chat lines, no sounds, no particles, no sighting. (Ambient floor = 5 min.) | ☐ |
| A2 | Type `yo entity is it pink?` at any time. | NEVER any reply or reaction to it (not an invitation). Silence. | ☐ |
| A3 | Type a correctly-spelled `hello?` BEFORE 10:00. | Silence guaranteed (MESSAGE floor blocks delivery pre-10-min even when the speak roll passes). | ☐ |
| A4 | After 10:00, stand still, then type `hello?` once. Repeat across a few sessions if silent (≈1-in-3 roll). | If answered: a delayed (2–30 s) `<...>` chat line, lowercase, ≤12 words, referencing something you actually just did (e.g. `you stopped`). Repeated hellos in the next 150 s: silence (cooldown). | ☐ |
| A5 | From 5:00 onward, stand still in an open area, `ai.enabled=false` build preferred. Wait through several ambient windows (each 2.5–5 min). | E1 footstep: quiet triple gravel-step ~30 blocks DIRECTLY BEHIND you, 1.1 s apart, then silence; nothing visible, nothing approaches; NOT a zombie/mob sound; AUDIBLE on your speakers (this is the audibility verdict — record yes/no + volume impression). At most one ambient beat per window; never overlapping with a chat line. | ☐ |
| A6 | Mic leg: sidecar running, `microphone.enabled=true`, restart MC. Say a short sentence aloud near the mic. | Transcript reaches the director only; no raw audio files anywhere; no transcript in any log. | ☐ |
| A7 | Mic leg: with capture live, walk through a nether portal (dimension change), then return and disconnect to the title screen. | Capture STOPS at disconnect (mic LED/indicator off; no lingering `nightwatch-microphone` activity); no duplicate transcripts; no `LineUnavailableException`; re-entering a world re-starts capture exactly once. | ☐ |
| A8 | Watch cues 10–15 min total. | No more than ONE chat line in the whole window; cue frequency never stacks (sighting + echo + chat never simultaneous); no jumpscares, no volume spikes. | ☐ |

## B. Stage 1 acceptance (verify only after Runtime reports it landed)

| # | Exact action | Exact expected observation | PASS/FAIL |
|---|---|---|---|
| B1 | Trigger any chat line (A4 method). | Action bar shows a brief grey `<...> is typing...` (1–3 s) BEFORE the line, then exactly ONE chat message. No per-letter chat spam. | ☐ |
| B2 | `/nightwatch memory status`, then `/nightwatch memory reset`, then `/nightwatch memory delete`. | All three respond; the memory file is gone after delete; memory stays OFF by default in a fresh profile unless opted in. | ☐ |
| B3 | Set `ai.provider=openai-compatible` + endpoint + key in config; restart; trigger a line; then `findstr` the run/logs directory for the key string. | Responses still work (or fall back offline cleanly); the key appears in NO log line; jar contains no key (Reviewer verifies jar statically each build). | ☐ |
| B4 | Nether vs Overworld tone (if personality landed): trigger offline lines in both. | Per-dimension tone/word-count differences only — never more FREQUENT (150 s pacing unchanged). | ☐ |

## C. Stage 2 acceptance (verify only after Fabric/Loom report wiring landed)

| # | Exact action | Exact expected observation | PASS/FAIL |
|---|---|---|---|
| C1 | Play until a Watcher sighting (Overworld, dark, 20–40 blocks — default ~30). | Spawn lands in YOUR current dimension at terrain height, 20–40 blocks ahead; while you look directly at it it FREEZES (desync reads "slightly wrong", not janky — canon f); when the ~6 s sighting lifetime expires it sinks/fades out (never snaps); no nametag, no tab entry, never approaches; no combat, no damage. | ☐ |
| C2 | After first contact, on a later quiet night, find the pale door. Stand facing it 3 s. | Door "opens"; you are relocated into the Loom: vast dark, pale thread geometry, familiar-fragment islands, midnight-fixed. | ☐ |
| C3 | Walk the thread stage; stop and turn back at ANY point. | Retracing always works; the way back is available at every stage (no soft-lock). | ☐ |
| C4 | Echo stages: follow the copycat; watch the diverger; reach the facing echo. Choose APPROACH (walk into it). | Copycat = your own skin, distorted, mimicking you; its "death" is a dissolve into thread particles — HP/hunger NEVER change, inventory intact. Facing echo dissolves the moment you reach it. | ☐ |
| C5 | Repeat the sequence; at the facing echo choose LOOK AWAY (crosshair off it ~15 ticks) and in another run STAND STILL ~10 s. | Look-away: it walks past, strand opens. Stand-still: it steps close, stops 1 block away, release follows. All three choices work; none damage. | ☐ |
| C6 | Idle/refuse everything at any junction (stand still, no choices) for a few cycles. | The sequence itself routes you onward/back (idle routing); you are never trapped by inaction. | ☐ |
| C7 | Survive to the far frame; step through. | You wake at your Overworld anchor with the knock sound; door re-offerable later. | ☐ |
| C8 | During ALL of C2–C7 keep the tab list open. | No second player EVER appears (no tab entry, no nametag, no join/leave chat); echoes read as you-but-wrong (own skin + visible distortion — never a real multiplayer character). | ☐ |
| C9 | Whole Loom session: watch HP, hunger, inventory, and your Overworld builds. | Zero damage, zero hunger drain, zero item loss, zero Overworld block changes. Canon: no full-body reveal of the Unraveller, no boss fight, nothing explains itself. | ☐ |

## Evidence to return to Reviewer

- Per-row PASS/FAIL with timestamps.
- `run/logs/latest.log` (full) + `run/config/nightwatch.properties` (redact nothing — key must NOT be in there unless B3 was run; if run, delete the key after testing).
- For A5: your audibility verdict (clear / faint / inaudible @ volume 0.25, 30 blocks) — this decides the E1 gain/position reconciliation.
- Any `<...>` chat lines received, verbatim.

## Reviewer notes

- A1–A8 exercise exactly the behavior already verified statically/code-level (floors, cooldowns, invitation filter, fail-closed sounds); the human legs are audibility, timing-feel, and mic-hardware truth.
- The script grows with the sprint: Stage 1/2 rows activate only after the owning roles' completion reports verify; verify statically first, add rows here, then Lead calls the session.
- Nothing in this script may be marked PASS by an agent — operator eyes/ears only.
