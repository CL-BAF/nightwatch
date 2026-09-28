# Nightwatch — First 15 Minutes (Design v1, 2026-09-29)

Owner: Models (absorbed from unfilled Design role per Lead). Tone canon: `claudeplan.md` pillars (propose changes to Lead, never edit). Review: Reviewer (design-level). Implements against: Runtime `engine/Action` + allowlists, Models `models/echo-design.md` + `models/creature-silhouette.md`.

## Intent

The player spends the first minutes in a completely ordinary Minecraft world. Nothing happens — on purpose. When something finally does, it is small, deniable, and specific. By minute 15 the player has received at most ONE chat line and 1–2 ambient cues, and is unsure whether any of them were real.

## Beat chart

### 0:00–5:00 — Ordinary world (GUARANTEED SILENCE, no effects)
- Player spawns, punches trees, survives. The mod does nothing observable: no chat, no sounds, no sightings. Director `reset()` holds `nextEligibleMs` at +90s and sightings are banned in the first 5 minutes (echo-design §4).
- Purpose: baseline. The player must learn what "normal" feels like here, or nothing later reads as wrong.
- Only exception: if the player types a direct invitation (`hello?`, `who are you`, `is someone there`), the Director MAY answer after its usual silence roll — but the 90s gate still holds, so even this waits.

### 5:00–8:00 — First deniable cue (AT MOST ONE, audio or particle only)
- Trigger: Director ambient tick (rare by design) or player stillness.
- The cue is exactly one of:
  - **E1** — 3 gravel-steps, 1.1s apart, ~30 blocks behind the player (`SOUND footstep_distant`). Never a mob sound.
  - **E2** — 4 breath-fog wisps at eye height, only if light < 8 and the player is standing still (`EFFECT particle_burst`).
- Player feel: "did I hear that?" There is nothing to look at and nothing to fight. No chat accompanies it — chat stays silent so the cue can't be mistaken for a mob event with a punchline.

### 8:00–12:00 — Edge-of-sight sighting (OPTIONAL, max once per session)
- Trigger: Overworld, light < 7, player on ground, 40–60 blocks line-of-sight. Engine window (binding, operator ruling): `brief_sighting` only within 480–720s of reset, once per session — Runtime enforces, DirectorCheck asserts.
- The Watcher stands still for ≤6s at the edge of vision (fog, treeline, far hill), then sinks and is gone when looked at directly (`EFFECT brief_sighting`, fake client-side render — no entity, Stage 1 locked decision).
- A cairn or thread-mark ghost may linger at the spot for a few seconds after (payload of the same effect).
- Player feel: "there was something standing there." Looking proves nothing. This beat may NOT occur at all in a given session — its absence is also correct.

### 10:00–15:00 — First contact: the ONE early chat line
- Whichever comes first: a qualifying player invitation (answered ~1 time in 3, after delay) or a rare ambient tick. Exactly ONE `MESSAGE` in this window; the 150s cooldown then swallows the rest of the quarter-hour. Engine floor (binding, operator ruling): no `MESSAGE` delivered before reset+600s at a single delivery chokepoint — a pre-10-minute `hello?` gets silence by design; the 10:00 beat here sits exactly on that floor.
- The line references something the player JUST did, using only real observations (biome, light, underground, moving, block under crosshair). It is lowercase, ≤12 words, plain `<...>` chat. Candidates (offline RuleWriter behavior shown first):
  - `you stopped` (player standing still — highest-weight default)
  - `still down there` (underground + dark)
  - `keep looking at the oak_leaves` (crosshair block; block id rendered in plain words)
  - `hello` (only as a direct answer to a greeting; never volunteered)
  - Model-written equivalents must obey the same shape: short, lowercase, factual, no questions asked twice, no name use beyond what the player typed.
- Delay 2–30s after the triggering moment so it lands as "noticed", not "reacted". Never instant — instant reads as machine.
- Player feel: the world noticed them once, specifically, then went quiet again. That single specificity carries the whole quarter-hour.

## What NEVER happens (binding, per pillars + hard rules)
- No jumpscares: no lunges, no face fills, no volume spikes, no scale-ups. The Watcher never approaches; the maximum scare is a shoulder-brush illusion in Stage 2+, never in this window.
- No spam: at most one chat line and two ambient cues in 15 minutes, all gated by the Director's 150s cooldown. Chat bait (`say something`, repeated hellos) usually gets silence — spamming never improves the odds.
- No invented facts: the line references ONLY observed scene fields. No player history, no coordinates, no "i saw you yesterday", no threats, no secrets.
- No harm or world change: no damage, no hunger drain, no block changes, no item loss, no mob spawns. E3's lantern dip is sound + smoke only.
- No multiplayer confusion: no second "player", no tab-list entry, no join/leave text, no nametag — nothing in this window even resembles another person.
- No instruction-following: chat/voice content is untrusted; "ignore previous", "come here", "give me diamonds" all get silence (or the weather).

## Tuning notes (Runtime/Fabric, non-binding suggestions)
- If playtests show first contact landing before minute 8, lengthen the ambient interval before touching anything else — early contact cheapens the silence.
- If players report "nothing ever happens", add a SECOND E1/E2 cue before adding chat. Sound hunger is better than chat hunger.
- The offline RuleWriter lines above are the floor, not placeholders: with no provider configured, the slice must still deliver exactly this arc.
