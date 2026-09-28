# Loom wiring walkthrough — numbered verification script (Lead-approved)

Owner: Loom. Companion script for Reviewer's static verification of Fabric's wiring pass and for the operator's C2–C9 rows in `review/human-test-script.md`. Expectations come from `loom/dimension-sequence.md` (sequence + invariants + hook contract) and `loom/dimension-entry.md` (registration).

## Prefix assumptions (fail-closed guards, tested or statically verifiable — no MC needed)

| # | Check | Expected |
|---|-------|----------|
| A1 | `DimensionType/LevelStem` JSON codecs on 26.3 | Human-phase row C2 ground truth; code fails closed (`server.getLevel(LOOM)==null` → no teleport, no crash) |
| A2 | Client-env mod data pack applied to world | Same row; same fail-closed path |
| A3 | `ServerPlayNetworking` receiver registration | One receiver for `nightwatch:loom_move`; `PayloadTypeRegistry.serverboundPlay()` registered before it |
| A4 | Anchor map | Touched only inside `handle()` (server game thread via `ctx.server().executeIfPossible`) |
| A5 | Payload | int-only actions {0,1,2}, unknown → no teleport |
| A6 | Relocation source | Only `CAUGHT_RELOCATE`/`EXIT_REQUEST` from `LoomSequence` policy — never model output, never a free-form call |

## W2 entry walkthrough (map: C2, and C8/C9 halves)

1. Director emits `EFFECT door_appear` -> Fabric forwards `onDirectorAction("door_appear", …)`
2. Bridge (DORMANT, not already offered) -> `sequence.openDoor()` -> cue `DOOR_APPEAR` -> `hooks.effect("door_appear","overworld-night")` -> pale-door ghost drawn 5–8 ahead, refreshed every 500 ms; thread strands trail; NO occupant (rule 7)
3. Player right-clicks (or, per script C2 wording, faces the ghost ~3 s — hook contract addition) -> `hooks.doorTriggered()` true -> driver `doorEntered=true` -> next tick `ENTERED_DOOR`
4. Sequence ENTRY: `DOOR_OPEN` + `THREAD_REVEAL` cues -> `hooks.sound("door_open")`, `hooks.effect("pale_thread","long")`
5. `hooks.teleportEnter()` -> payload `{action:0}` -> server thread: anchor stored (player pos + current dimension), quartz pad+frame built idempotently at Loom(0,-61,0), `teleportTo(loom, 0.5,-61,0.5,…)`
6. Result: vast dark, wool floor underfoot, frame behind; **no damage, no hunger, no item loss** (C9 half)
7. Tab list stays empty of the echo (C8); `inLoom = LOOM.equals(dimension())` flips the observation feed

## W3 stage chain (map: C3, C4, C5, C6, C8, C9)

| # | Player act | Machine transition | Cue order (mapper output) |
|---|-----------|--------------------|---------------------------|
| 3.1 | walk thread; stop/turn back anytime | THREAD→(WALKED_BACK)→DOOR: `EXIT_REQUEST`+`DOOR_CLOSE` | teleportExit(false) + door_knock + door_close; anchor gone; overworld door re-offerable later (C3) |
| 3.2 | continue → echo appears ahead | THREAD→ECHO_COPYCAT (FACING_ECHO) | `echo_spawn:copycat` (8 ahead) |
| 3.3 | follow it (keep moving) | MOVED→ECHO_DIVERGER | `echo_dissolve:copycat` + status "the echo fell"; **no HP event** |
| 3.4 | continue | MOVED→ECHO_FACING | `echo_spawn:facing` (5 ahead) |
| 3.5 | walk into it (reach <2.5) | CHOSE_APPROACH→SURVIVAL | `echo_dissolve:facing` + `pale_thread:long-ahead`, status "scatter" |
| 3.6 | crosshair off it ≥15 ticks | CHOSE_LOOK_AWAY→SURVIVAL | same cues, status "let past" |
| 3.7 | hold still ≥10 STOPPED | →SURVIVAL | `pale_thread:long-ahead`, dissolve-only release; a `facing-closer` spawn (3.0 ahead) precedes the release when player chose STAND earlier |
| 3.8 | refuse everything, 2 idle cycles at facing | →SURVIVAL via through("released") | dissolve + thread reveal; never caught |
| 3.9 | refuse everything on thread/echo stages, 3 idle cycles | →DOOR: `EXIT_REQUEST` | teleportExit(false) + door_knock + door_close (C6: idle routing always exits; never trapped) |

## W-late exit (map: C7, C9)

1. SURVIVAL strand -> frame at `Vec3(0.5,-61,0.5)`, inside 1.2 debounced → `ENTERED_DOOR` → `EXIT_REQUEST` + `DOOR_CLOSE`
2. Driver → `teleportExit(false)` + `door_knock` anchor-side; server: anchor removed (reversible by design — the overworld door is re-offerable later), `teleportTo(anchorDimension, anchor pos)`
3. Status "safe"; DOOR stage → any further input → DORMANT (world watches; anchor gone until next offer)

## Way-back inversions (each is a C3/C6 proof)

- Every ECHO/THREAD/SURVIVAL state accepts `WALKED_BACK` → `EXIT_REQUEST` (verified: `LoomSequenceCheck.wayBackFromEveryStage`)
- Idle refuse routes out within 2–3 cycles (`idleReleasesFromFacingEcho`, `idleRoutesBackFromThreadAndEchoStages`)
- `CAUGHT_RELOCATE` (SURVIVAL idle) → door re-offered at entry; nothing taken (knock-back only)
- No reachable state has zero backward options (depth-3 brute force at `bruteForceNoLock`)

## Not in script (explicit non-goals)

- No full-body reveal (rule 7); no boss framing; no explanation dialogue
- Echo dissolves carry no damage/inventory cue — mapper rejects any such mapping at build time
- `LoomObservations` never teleports/damages/edits blocks/sends commands (Fabric's read-only guarantee)

## Reviewer linkage

- Map rows: C2←W2.3–7; C3←W2.7+3.1; C4←3.2–3.5; C5←3.6–3.7; C6←3.8–3.9; C7←W-late; C8←all; C9←all
- Source units verified: `LoomTeleportHandler.handle/enter/exit`, `LoomDirectorBridge.flush/tick/decideFacing`, `LoomCueMapper.mapOne` (closed ids), `LoomSequence` transitions
