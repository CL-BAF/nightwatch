# The Loom — dimension sequence design (vertical slice v1)

Owner: Loom. Status: draft v1 for Lead review.
Related: `claudeplan.md` M3, `design/first-15-minutes.md` (Design's entry conditions), Runtime's `engine/Action.java`, Fabric's `effects/` wiring.

## What the Loom is

A pocket dimension the Nightwatch entity remembers the player by. Visually: **vast dark space** — no sky, no fog wall, no ground plane; just depth. Through that dark run **pale thread-like geometry**: thin strands, grey-white, slightly luminous, strung between **fractured islands of familiar structures**. The islands are fragments of what the player already knows — half a village house, a mine support, a chunk of a stronghold corridor, their own spawn hut — repeated at intervals, wrong at the edges (mismatched walls, a door with no frame, a broken-open roof with second floor below it).

Familiarity is the horror. The player should keep thinking "I know this place" and be wrong.

## What "survivable" means (hard rules)

1. **No permadeath.** Nothing in the Loom damages or kills the real player. HP never changes; hunger never changes.
2. **No soft-locks.** From every stage, the way back **out** of the Loom is permanently available. If the player refuses every prompt, the sequence routes them out itself (Reviewer L6, implemented): three full idle cycles on the thread or in the echo-walk stages retrace them to the door; two full idle cycles at the facing junction release them onto the patient path to the exit (dissolve only, never a caught event).
3. **Clear exit conditions.** At every stage the player has an understandable forward option and an understandable backward option.
4. **Echo deaths are illusions.** Echo entities visibly "die" (falling apart into thread particles, sound cut mid-stride, hard light-out). The real player only ever receives a scare event, at most a very brief camera/overlay cue, never an actual death.
5. **One-way threat cap.** Any "caught" outcome relocates the player to the entry door and plays the knock-back. Nothing else is taken.
6. **Echoes are never mistaken for real players** (per Reviewer / Lead vision). All echoes — Overworld E4 and Loom alike (current ruling, pillars §3) — render with the **player's own skin, distorted by stage and location**, never a foreign skin, never a grey figure. They must have **no tab-list entry, no nametag, no name above head, and no join/leave chat**. They are not real entities with any server presence; client-side illusion rendering only, per Models' E4 parametric distortion spec with normative guards (multiplayer-metadata ban, never-speaks-as-player, first-glance distortion, always distorted; #dddde6 thread dissolve end-state). When a later multiplayer-safe engine lands, this rule stands unchanged: an echo looks like *you*, and never communicates. (Earlier "Overworld grey-only vs Loom own-skin" split retired 2026-09-29 after the own-skin-everywhere ruling — the unifying element across Overworld and Loom is now the same distorted own-skin asset at increasing distortion.)

(Plus review-later additions: 7. **No full-body reveal.** The being behind the Loom is never staged in full — no camera beat, cutscene or boss-fight framing ever shows all of it. See canon below.)

## Canon (Lead binding, 2026-09-29) — the Unraveller

- The Loom is where the Unraveller **works**: thread strands are its material, islands are pieces it has taken — the dimension must read as a **workspace mid-act**, not a lair with an occupant. Nothing is ever staged standing inside it.
- Echo deaths are the canon act itself: each dead echo is a version of the player it **tried to pull apart**. Echo I's mid-stride cut, Echo II bending into its own thread, Echo III's dissolve are all that verb shown at increasing closeness. The distortion parameter (per-stage) is the pull already in progress.
- Core feeling for every beat: *"it knows which version of you survives, and it's trying to make that stop being true."* The way out is narratively the surviving version walking free; the survivability invariants (rules 1–6, incl. the permanent way back) are therefore not just gates but the point of the story.
- The thread-mark is Unraveller material by construction (#dddde6 shared spec unchanged); Overworld thread-marks read as traces of work done; Loom strands as the work itself.
- HARD BANS (canon + project rules): no named Lovecraft entities in any doc, string, prompt or asset; no boss-fight framing; full-body reveal banned per rule 7.

## World entry — the Loom entry

The Design team defines when the mod first acknowledges the player in the Overworld. The Loom entry is its escalation:

- After a door knock back, on a later quiet night, a **pale door** appears as a sighting-type effect near the player (allowlisted effect, client-side): a free-standing door, thin thread-strands leading from its frame into the dark.
- Standing in front of it and facing it for 3 seconds, or (only after it has been ignored for 3 cycles) not — Design/Runtime decide the trigger — the door "opens" and the Loom sequence is entered. In-game this is an illusion overlay + teleport hook; the mechanics are Fabric/Runtime integration, not this doc's.

## Sequence stages

| # | Stage | Player perceives | Forward option | Backward option | Failure mode |
|---|-------|------------------|----------------|-----------------|--------------|
| 0 | **Entry** | Pale door, thin threads into dark | Step through | Walk away (door fades) | none |
| 1 | **The Thread** | Walk the strand; void below; an island each ~20 blocks with the same house fragment repeated, more worn each time | Keep walking | Stop anytime, threads behind retrace to the door | Losing the thread re-routes to Encounter 1 (harmless) |
| 2 | **Echo I — copycat** | Your own skin walks ~8 blocks ahead, mirroring your last movements on a one-frame delay, growing more distorted (gait stretch, head at wrong angle) | Follow it | retrace thread | None mandatory; the echo "dies" — falls off the strand without a fall, sound cut mid-stride, comes apart into thread particles. **Illusion. No player HP event.** |
| 3 | **Echo II — diverger** | A second echo splits onto a side-strand that bends where no strand should — takes a path the player cannot; ends running into a wall it made from its own thread and dies the same way | Follow threads | watch from the fork | None mandatory |
| 4 | **Echo III — facing** | The last echo stands still, facing you. This is the real choice junction | One of the three choices below | retrace thread to door | none |
| 5 | **Survival route** | After the choice, a longer strand to the far side: an open **frame** — a door like the entry's | Walk the strand | turn back at any time | Caught-event relocations to stage 0 door |
| 6 | **Exit** | Step through the frame; wake at the Overworld anchor with the knock sound as the door "closes" | — | — | none |

## Echo III choices (the decision the slice is about)

Three understandable options, all survivable, all with meaning:

- **Look away.** The echo walks past, brushing your shoulder — a trace of your own footsteps going the other way. The strand ahead opens. Least scary, most "honorable" — the Loom lets you by because you did not pretend it was you.
- **Approach.** The echo scatters into thread particles the moment you get within reach — it was never there to be caught. Strand ahead opens. Scariest, fastest, earns the "confrontational" memory note for the Director.
- **Stand still.** The echo waits. If the player holds still ~10 seconds, it steps close and stops one block away, and the strand ahead dims toward the exit regardless. If the player moves first, it copies — loop to Encounter 1's echo (harmless, repeats). Patient option.

The Director's memory applies these here for later pacing (Runtime coordinates: both choices write a bounded echo line to the Director's existing bounded remembrance).

## Implementation notes (minimal runtime hooks in this slice)

- `src/client/java/dev/cameron/nightwatch/loom/LoomSequence.java`: pure state machine, no Minecraft imports. Inputs are an observation enum (`moved`, `stopped`, `facingEntity`, `choseLookAway/Approach/StandStill`, `walkedBack`, `idleCycle`); outputs are allowlisted `LoomCue`s (`THREAD_REVEAL`, `ISLAND_MORPH`, `ECHO_SPAWN(n)`, `ECHO_DISSOLVE(n)`, `CAUGHT_RELOCATE`, `DOOR_APPEAR`, `DOOR_OPEN`, `EXIT_KNOCK`, and a bounded status text) that Fabric maps to `effects/` actions and Runtime maps to the `engine/Action` interface. Naming kept deliberately close to Runtime's kinds so the Director never needs changes.
- `src/test/java/dev/cameron/nightwatch/loom/LoomSequenceCheck.java`: survivability test — brute-force walks every input sequence up to a bounded length and asserts: every run terminates in EXIT within a bounded number of stages; no reachable state has zero backward options; echo dissolve never emits a damage cue.
- Not in this slice: actual dimension registration (needs a real Fabric/JDK 25 build), echo skin rendering (Models' echo design), sound set, and the entry trigger (Design/Fabric). LoomSequence first compiles against core with `tools/check-core.sh` exactly as the current gate does.

## Teleport / relocation invariants (Lead conditions, binding)

- All `CAUGHT_RELOCATE`, `DOOR_*` teleports fire **only** from LoomSequence/Director policy — never from model output. LocalWriter must never see or produce the argument channel for Loom effects (Runtime already enforces: argument field is engine-internal, model gets only Overworld, argument-less effects).
- Every relocation is **reversible**: the permanent way back per hard rule 2 applies after any teleport.
- **Zero item deletion, zero world damage, zero real damage** in either direction.
- All world mutations run on the integrated server's game thread, never from the netty/network thread — receivers queue a task via the server executor, and only that task calls `ServerPlayer.teleportTo`.
- **Suppression is additive only** (Lead, 2026-09-29): chat suppression inside the Loom may only silence Director chat — never bypass the MESSAGE chokepoint, validation, or pacing; and the way-back/exit guarantees (rules 1–6) hold regardless of suppression state. The Loom's personality inheritance may not raise message frequency or the word cap (CHATTY 10–12 stands everywhere).
- Effect ID / argument channel stays a closed, fail-closed allowlist (`echo_spawn`/`echo_dissolve` args ∈ {copycat, diverger, facing, facing-closer}); anything else → SILENCE/silently ignored.

## effects/LoomIntegration hook spec (for Fabric — design only, implement after Gate 0 + effects/ executors exist)

- One class `effects/LoomIntegration.java` owned by Fabric, consumed by the Loom.
- Listens for `Action` EFFECT ids originating from the Loom cue pipeline — not the model: `door_appear`, `pale_thread`, `island_morph`, `echo_spawn(argument)`, `echo_dissolve(argument)`.
- Sound ids mapping: `door_knock`, `door_open`, `door_close` (from the closed SOUND allowlist).
- Special-cases exactly one cue: `CAUGHT_RELOCATE(argument="entry-door")` → queues the relocate task on the integrated server's game thread to the stored per-player anchor + plays `door_knock`; nothing else may trigger server-side player relocation. Payload-coordinate validation (bounded range around the stored anchor, never trusting raw client values) applies before any `teleportTo`.
- Anchor store: `Map<UUID, BlockPos>` touched only from server-thread context (Reviewer's dim-entry review note 1).

## Allowlist IDs consumed by the Loom slice (closed, validated — for Runtime's Action.java allowlists)

- **EFFECT:** `pale_thread` (THREAD_REVEAL strands, incl. Overworld thread-mark shared render), `island_morph` (ISLAND_MORPH worn/repeat cycles), `echo_spawn` (argument: `copycat`/`facing`/`facing-closer`, one asset per Models E4), `echo_dissolve` (argument: stage id; always thread-particle dissolve, never damage), `door_appear` (free-standing pale door sighting)
- **SOUND:** `door_knock` (entry offer + caught knock-back), `door_open`, `door_close` (exit), `footstep_distant` (shared with Models E1, gravel-step per Reviewer — the Loom plays no mob-attributed sounds)
- **MESSAGE:** bounded status text via the existing `STATUS` cue, length-validated before Action conversion
- **NOT an Action:** `CAUGHT_RELOCATE` stays a Loom-only hook awaiting Fabric's LoomIntegration listener (decision logged by Runtime 2026-09-29: no RELOCATE Action kind)

Anything not on this list is rejected by the same closed-allowlist validation the engine already uses for text — no arbitrary IDs.

## Hook data-source contract (answers Fabric 2026-09-29; belongs alongside the LoomIntegration spec)

- `doorTriggered()` (overworld): the pale door is an effect-rendered ghost whose position VisualEffects fixes at spawn time — see it fixed **5–8 blocks in front of the player's facing** at `door_appear` time, and store that Vec3 in the hook. Trigger = player right-clicks while the ghost position is within 2.5 blocks of the crosshair hit/ray (or within 1.5 blocks of the position) — player-chosen, not time-forced.
- `frameTriggered()` (Loom side): the exit frame is at the server-constant door center `Vec3(0.5,-61,0.5)` in the Loom (same constants as LoomTeleportHandler). Trigger = player position within 1.2 blocks of the center, debounced client-side ≥1.5s (the bridge already debounces its side).
- `echoPos()` (Loom side): VisualEffects places the echo on `echo_spawn` deterministically: copycat ≈ 8 blocks ahead of the player's facing; diverger ≈ 12 ahead + 6 left (the forbidden side path); facing = exactly 5 ahead at eye height (reach check needs true distance to <= 2.5); **facing-closer = exactly 3.0 ahead at eye height** (the patience path's step-in; deliberately ≥ the 2.5 approach-reach threshold so a *standing* player never auto-triggers CHOSE_APPROACH, and moving ≥0.5 toward it is a real approach choice). Return Vec3, or null when no echo is live (drives FACING_ECHO discovery).
- `crosshairAtEcho()`: camera look vector vs direction to echo within ~5° and echo within ~20 blocks — "looking at it" for the look-away decision.
- `status()` — recommendation: action bar (26.3 call: `LocalPlayer.sendOverlayMessage(Component)` — Fabric's notes; same contract as the older displayClientMessage(component, true): overlay-only, ≤1 per stage transition), not a chat line: TEXT stays rare but chat silence purity is the mod's canon; the action bar keeps the one channel clean. Fabric decides as owner; if chat is used, keep it to the same ≤1-per-transition rate (no flooding).
- **Door visuals, slice** (Fabric f447533): content-complete gate ships the particle-outline door + frame stand-in (rate-limited 500ms refresh, sighting-fade language, no other side shown). The textured pale-door plane (quartz-white + #dddde6 strands; door texture by Models) is a tracked next-phase task pending a real render-pipeline check at content-complete — the door visual must exist in SOME read-clean form at all times before the door_appear cue expires so the right-click choice stays possible.

## Open items for coordination

- Runtime: confirm action kinds for relocating a player and overlays without a server mod (best effort is a client-side "wake" illusion if server teleport is out of scope for M3).
- Fabric: hook the Loom callbacks where world effects already land; the thread-reveal cue needs a shape render hook.
- Models: echo silhouettes must be reusable at stage 1/2/3 with one "distortion" parameter; dissolve needs particle + sound cutoff spec.
- Lead: FIRE gate — nothing here touches shared files except an additive line in `tools/check-core.sh` (adding loom paths to the same javac call).
