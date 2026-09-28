# Action Interface Design

## Overview

The `Action` record defines all actions the Director can emit. The AI receives limited truthful observations and can choose only these validated, predefined actions.

## Action Kinds

### SILENCE
No action. The Director often chooses this (speaks rarely, ignores chat bait).

### MESSAGE
Chat message with delay (2-30 seconds). Validated for:
- Max 90 characters
- No newlines, carriage returns, or formatting codes (§)
- Max 12 words from model output

### SOUND
Play a client-side sound effect. Allowlisted sounds:
- `footstep_distant` - Distant footstep sound
- `door_knock` - Rare door knock
- `whisper` - Whisper sound
- `lantern_dip` - Lantern extinguish cue (E3)
- `door_open` - Loom door open
- `door_close` - Loom door close

Delay: 0-30 seconds. Max 60 chars for sound ID.

### EFFECT
Trigger a visual or ambient effect. Allowlisted effects:
- `brief_sighting` - Brief creature sighting
- `shadow_flicker` - Shadow/lighting flicker
- `particle_burst` - Particle effect burst
- `pale_thread` - Loom thread reveal
- `island_morph` - Loom island morphing
- `echo_spawn` - Spawn parametric echo (argument: stage id like "copycat", "diverger", "facing")
- `echo_dissolve` - Dissolve parametric echo (argument: stage id)
- `door_appear` - Loom door appearance

Delay: 0-30 seconds. Max 40 chars for effect type. Optional argument string (max 80 chars) for parametric effects.

## Integration with Director

The `Director.Writer` interface now returns `CompletableFuture<Action>` instead of `CompletableFuture<Choice>`. The Director's pacing, cooldown, and memory logic applies uniformly across all action kinds.

### Callback Path
1. Writer returns `CompletableFuture<Action>`
2. Director awaits result asynchronously (non-blocking)
3. On completion, Director schedules delivery via main thread callback
4. Non-silence actions are delivered to the consumer (NightwatchClient)
5. MESSAGE actions are also added to memory

## Fabric Implementation Notes

Fabric (NightwatchClient) must:
1. Update the scheduled queue to hold `Action` instead of `Choice`
2. Handle each action kind:
   - MESSAGE: Display in chat with `<...>` prefix
   - SOUND: Play sound via Minecraft's sound system
   - EFFECT: Trigger visual effect via particle/lighting systems, passing `action.argument()` for parametric effects (echo_spawn/echo_dissolve stage ids)
3. Implement actual sound/effect playback in `.../effects/` classes

## Security

- All action IDs are validated against allowlists
- No arbitrary sound IDs or effect types accepted
- No world manipulation, no server commands, no arbitrary code execution
- Prompt injection resistance preserved in LocalWriter
- Memory lines prefixed with "untrusted" to prevent instruction injection
- Per-effect argument validation: echo_spawn/echo_dissolve accept only {copycat, diverger, facing}
- Model cannot select Loom-only IDs (pale_thread, island_morph, echo_spawn, echo_dissolve, door_appear, door_open, door_close) — these are engine-internal
- Lovecraft vocabulary bans enforced in LocalWriter (no Cthulhu, eldritch, etc)

## E3 Ruling (binding)

**No compound action kind.** Each effect ID is self-contained:
- `lantern_dip` = extinguish + smoke (single effect, single ID)
- `shadow_flicker` = lighting change (single effect, single ID)

**Scripted same-moment beats** (e.g., E3 lantern dip = shadow_flicker + lantern_dip together):
- Engine schedules two Actions with equal due-time
- Pairing is NEVER model-driven
- Model can only select one action at a time

**Volume caps:**
- Echo sounds (footstep_distant, whisper, lantern_dip): ≤0.3
- Door family (door_knock, door_open, door_close): 0.4-0.5 (pending human test)

## Final Allowlist Mapping

### Models Proposals → Action API
- `SIGHTING` → `EFFECT brief_sighting`
- E1 footstep → `SOUND footstep_distant`
- E2 breath-fog → `EFFECT particle_burst`
- E3 lantern dip → `EFFECT shadow_flicker` + `SOUND lantern_dip` (paired)
- E4 not-you → deferred to M2

### Loom LoomCues → Action API
- `THREAD_REVEAL` → `EFFECT pale_thread`
- `ISLAND_MORPH` → `EFFECT island_morph`
- `ECHO_SPAWN` → `EFFECT echo_spawn` (argument: stage id)
- `ECHO_DISSOLVE` → `EFFECT echo_dissolve` (argument: stage id)
- `DOOR_APPEAR` → `EFFECT door_appear` + `SOUND door_knock`
- `DOOR_OPEN` → `SOUND door_open`
- `DOOR_CLOSE` → `SOUND door_close`
- `CAUGHT_RELOCATE` → NOT an Action (Loom-internal hook, handled by Fabric)
- `STATUS` → `MESSAGE` (bounded status text)

### Model-Selectable IDs (LocalWriter prompt)
- SOUND: footstep_distant, door_knock, whisper, lantern_dip
- EFFECT: brief_sighting, shadow_flicker, particle_burst

### Engine-Internal IDs (Loom cues, never model-selected)
- SOUND: door_open, door_close
- EFFECT: pale_thread, island_morph, echo_spawn, echo_dissolve, door_appear

## Backward Compatibility

The old `Choice` class is replaced by `Action`. All code using `Choice` must update:
- `Director.Writer.decide()` returns `Action`
- Director's deliver callback accepts `Action`
- NightwatchClient's scheduled queue holds `Action`

Migration is straightforward: `Choice.message()` → `Action.message()`, `Choice.silence()` → `Action.silence()`.
