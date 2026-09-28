# Nightwatch — Loom Entry (Design v1, 2026-09-29)

Owner: Models (absorbed from unfilled Design role per Lead). Aligned with: Loom `loom/dimension-entry.md` (entry research/proposal) + `loom/dimension-sequence.md` (sequence design), pillars in `claudeplan.md`. Review: Reviewer (design-level). Loom implementation stays gated on Stage 2 — this doc defines the acknowledgement moment and the in-slice signal only.

## 1. The first acknowledgement (Overworld, Stage 1 — THIS SLICE)

The "Loom entry" as the player experiences it is NOT a dimension in the slice. It is the moment the mod first acknowledges the player as someone it is watching — the ONE early chat line from `design/first-15-minutes.md` (10:00–15:00 beat). Everything in this section is Stage 1 and implementable now.

### Timing
- Earliest: minute 10 of the session (after the 5-minute sighting ban + 150s cooldown have done their work). Typical: minutes 10–15. It must feel late — the silence before it is the content.
- Never during combat, falling, or while the player is typing a second message. The Director waits for a still moment; a line that interrupts play reads as plugin, not presence.

### Trigger conditions (either, whichever first; still exactly ONE line)
1. **Invitation answer:** player types a qualifying invitation (`hello?`, `who are you`, `is someone there`, `stop`, `leave me alone`) → ~1-in-3 silence roll passes → line after 2–30s delay. `stop` / `leave me alone` are always answered with `okay` (RuleWriter contract) and then a longer hush.
2. **Ambient notice:** rare Director ambient tick fires while the player is still, underground-and-dark, or staring at a block → line referencing that exact state.

### Exact text candidates (all ≤12 words, lowercase, factual-only)
- `you stopped` — default; player idle. Highest weight.
- `still down there` — underground + light < 6.
- `keep looking at the oak_leaves` — crosshair block (plain-words block id).
- `hello` — ONLY as a direct greeting answer, never volunteered.
- `okay` — ONLY as the answer to `stop` / `leave me alone`, followed by extended silence.
- Model-written lines must match this shape (LocalWriter enforces: ≤12 words, real observed detail, no threats/secrets/coordinates/instructions). Anything else → SILENCE.

### What it must feel like
- Being noticed once by something patient, not greeted by something eager. No question is asked of the player, no conversation is opened, nothing demands a reply. If the player answers, silence is the likely response — the acknowledgement was the event.

## 2. The eventual Loom signal (in-slice foreshadow, Stage 2+ entry proper)

### In THIS slice: signal only, never entry
- On a later quiet night after first contact (a subsequent session or well past minute 20 — never in the first 15 minutes), a **pale door** may appear as a sighting-type effect near the player (`EFFECT door_appear`, client-side illusion): free-standing, thin thread-marks (#dddde6 shared spec) leading from its frame into the dark.
- Slice behavior is fixed: the door NEVER opens in Stage 1. Approached or faced, it fades like the Watcher (sighting rules: ≤6s, gone when looked at directly, max once per session). No teleport exists, no dimension is registered, no prompt is shown. The signal means "there is a door now" and nothing else.
- Fabric must NOT build teleport/registration for the slice: per `loom/dimension-entry.md`, the entry hook (`LoomDoorService` + `LoomTeleportHandler` + 2 JSONs + payload, `CAUGHT_RELOCATE` via `effects/LoomIntegration`) is Stage 2 work gated behind Gate 0 and Lead approval. The open items there (client-env data-pack application, `teleportTo` drift, anchor persistence) are answered by in-game tests, not by this doc.

### At Stage 2 (reference, owned by Loom — not this doc)
- The door opens after the player faces it ~3s (or after 3 ignored cycles, per Loom sequence §World entry); `LoomSequence` runs thread → copycat → diverger → facing → exit; every stage keeps a backward option; caught outcomes relocate to the entry door + knock-back only. Survivability rules (no permadeath, no soft-locks, clear exit, illusion-only echo deaths) are Loom's binding contract, unchanged by anything here.

## 3. Sync notes (kept, not edited — other roles' files)
- `loom/dimension-sequence.md` "explicit distinction" line still describes Overworld E4 as grey-only "not-you". That is STALE: Lead + Reviewer have since ruled E4 uses the player's own distorted skin everywhere (pillars §3, echo-design §E4 with normative guards). Loom: please update that line when convenient — Models does not touch Loom files.
- Thread-mark shared spec (#dddde6, emissive 0.3, 1px, opacity 0.7) is the visual bridge: Overworld thread-marks (decoration) → door-frame strands (signal) → Loom walkable geometry (Stage 2). One material, three meanings.
- Pillar-change proposal: NONE. Both docs are written inside the current pillars; no canon change requested.
