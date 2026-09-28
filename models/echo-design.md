# Nightwatch — Echo Design (Models v0.2, 2026-09-29)

Status: draft for vertical slice. Companion to `creature-silhouette.md`.
Echo = the residue the Watcher leaves. No entity required — all implementable with vanilla sounds/particles/light today.

## 1. Design intent
- The player should doubt the sighting: "did I see that?" Echoes extend dread for 10–20s after the 6s sighting ends.
- Stack max 1 echo at a time. Never overlap sighting + echo + chat message. Director pacing owns the gate.
- All echoes client-only, singleplayer, non-destructive, no terrain/item changes.

## 2. Echo kit (allowlisted, Fabric can implement now)

### E1 — Distant footstep (audio, primary echo) → `SOUND footstep_distant`
- Sound (per Reviewer 2026-09-29 — `entity.zombie.step` REJECTED, reads as ordinary mob-paranoia): vanilla `block.gravel.step` at pitch 0.5, position ~30 blocks behind player, or the block sound matching local ground material. No visible cause — "footsteps where nothing stands".
- Pattern: 3 steps, 1.1s apart (matches slow Watcher gait), then silence. Never loops, never approaches.
- Trigger: 4s after Watcher despawns, 60% chance. Cooldown shares Director 150s window.
- Fabric verify before locking: volume 0.25 at 30 blocks attenuates heavily — confirm audible on laptop speakers in-game, adjust gain/position within ≤0.3 cap.

### E2 — Breath-fog particles (visual) → `EFFECT particle_burst`
- Particles: `minecraft:poof` x4, scale 0.5, drift toward player 1 m/s, life 1.2s, spawn at eye height in a 2-block ring.
- Only when light <8 and player stationary. Subtle — no `sculk_soul` (too Warden-coded), no redstone/blood colours.
- Trigger: alternate with E1 (never both).

### E3 — Lantern dip (lighting) → `SOUND lantern_dip` SELF-CONTAINED
- Effect: for 5s, reduce perceived warmth — Fabric dims held-torch light? Slice version: `lantern_dip` plays `block.candle.extinguish` at volume 0.15 + spawns `minecraft:smoke` x2 at nearest torch/lantern. One id, one Fabric executor, no compound kind.
- Ruling (Lead/operator 2026-09-29 — no compound actions): scripted same-moment beats are engine-scheduled as two Actions with equal due-time, never model-paired. `lantern_dip` itself stays self-contained (extinguish+smoke); the old "shadow_flicker + lantern_dip triggered together" pairing is withdrawn — use `lantern_dip` alone for E3.
- No actual light-level rewrite, no block-state change. Pure sound + particle illusion.
- Rare: max 1 in 3 sightings.

### E4 — Distorted player echo ("not-you") — M2 hook, NOT in slice
- Concept: player-like figure at 20 blocks using the player's own skin with distortion (wrong proportions: arms 1.3x, head tilt 15°), no nameplate, no held item.
- Skin rule (per Lead 2026-09-29, adopted over earlier grey-only draft — flagged for Reviewer re-check): E4 AND Loom echoes use the player's OWN skin with distortion, so the horror reads as "self, wrong" rather than "stranger". Distortion channels (parametric, increasing per stage): limb scale, head tilt/posture, tint shift, response delay. Safeguards (both): no tab-list entry, no join/leave chat, no nametag render, no player-list ping, always visibly distorted — never mistakable for a real multiplayer character.
- No-real-multiplayer checks (NORMATIVE per Reviewer 2026-09-29, design-level accept — in-game check at M2: view echo at sighting distance with tab list open, must read as you-but-wrong, never a second player): no tab-list entry, no join/leave chat, no nametag render, no player-list ping, always visibly distorted.
- Echo NEVER speaks in chat under a player-like name — its only voice, if any, is the Director's `<...>` lines.
- Distortion visible at FIRST GLANCE: limb scale (arms 1.3x), head tilt 15°, broken gait — the difference from "a real player standing there" must never depend on staring.
- Hard rule: grey-only stays rejected for Loom echoes; E4-Overworld ≠ Loom-echo (different features, Loom rule lives in Loom docs).
- Implementation note: reuse player model renderer with the player's own skin + parametric `ModelPart` distortion (limb scale, head tilt, tint shift per stage id). No foreign skin, no account impersonation (never uses another player's name/skin).
- Behaviour: mirrors player strafe for 3s with 0.5s delay, then stops. Despawns on look. Needs Runtime `Action` support (`EFFECT` extension or M2 kind — Runtime owns the call).
- Loom reuse (per Loom coordination 2026-09-29): design E4 distortion as parameters (limb scale, head tilt, tint shift, response delay) so one asset serves Overworld E4 and Loom Echo I/II/III encounters. No separate grey variant exists — distortion grade is the only axis (tint shift = desaturation grade on the player's own skin, never a flat-grey replacement). Loom consumes 3 advancing stages from one asset — copycat -> diverger -> facing, distortion increasing per stage — plus dissolve end-state (thread particles #dddde6 + sound cut). Mechanism resolved (Runtime 2026-09-29): `EFFECT echo_spawn` / `echo_dissolve` + `Action.argument` stage id ("copycat"/"diverger"/"facing"/"facing-closer", LoomSequence-controlled only, never model-controlled) passed through to Fabric. M2 spec work; not in slice.

## 3. Environmental props (3 max for slice — cheap, reusable, no block-entities)

All built from vanilla blocks where possible so no new block registration is needed for the slice. Custom models later.

1. **Cairn** — 3 stacked grey stones (cobblestone slab + stone button + cobblestone wall stub). Placed by worldgen? No — slice: Fabric fakes one at sighting site via client ghost render only, removed after. Marks "it stood here".
   - Placement (sprint spec for Fabric `brief_sighting` fallback + Loom set-dressing): ground at the Watcher's despawn point, base slab flush, total height ≤1 block, facing random yaw; lifetime = sighting + 20s echo window, then fade (no pop). Never within 5 blocks of the player, never on player-placed blocks (check `isPlayerPlaced` equivalent — ask Fabric if the hook exists; else skip-if-near-player-builds).
2. **Thread-mark** — 2–4 white wool-string-like lines (use `cobweb` texture, 1px cuboids) strung between trees near sighting. Client ghost only. Ties Overworld to Loom thread motif.
   - Shared thread spec (Overworld + Loom, per Loom coordination 2026-09-29): colour pale grey-white `#dddde6`, slight luminosity / emissive 0.3 (visible in fog, never fullbright), no red/brown tint, thickness 1px, opacity 0.7. Overworld marks are decoration; Loom strands reuse same colour as walkable geometry. Fabric renders both from this single spec.
   - Placement: anchor between two solid blocks 3–8 blocks apart, 2–4 blocks high, within 15 blocks of the sighting point; sag 0.5 block at midpoint; same lifetime/fade as cairn. Loom set-dressing reuses the spec at island edges (Loom owns those coordinates).
3. **Cold lantern** — existing lantern + E3 dip effect. No new item.
   - Placement: no placement — E3 targets the NEAREST existing player-placed or world lantern/torch within 12 blocks; if none, E3 degrades to sound-only (no smoke). Never creates light sources.

- Custom Blockbench prop models (`cairn.bbmodel`, `thread_mark.bbmodel`) deferred until after Fabric confirms Java-model pipeline (§5). Slice uses vanilla-block ghosts.

## 4. Director / Fabric wiring (concrete enough to implement)

- Runtime `Action` interface adopted (replaces Models' earlier `SIGHTING`/`ECHO_*` `Choice` proposal — withdrawn). Mapping for Fabric:
  - Watcher sighting + cairn/thread-mark ghosts → `EFFECT brief_sighting`
  - E1 footstep → `SOUND footstep_distant` (gravel-based per §2)
  - E2 breath-fog → `EFFECT particle_burst`
  - E3 lantern dip → `SOUND lantern_dip` SELF-CONTAINED (extinguish+smoke in one id; no compound kind per Lead ruling).
  - E4 not-you → M2, needs `EFFECT` extension or new kind (Runtime owns)
- Fallback correction (per Reviewer 2026-09-29 — the old empty-`MESSAGE` fallback is void since `message()` rejects blank text as `SILENCE`): if an effect has no `Action` kind, Fabric schedules it tick-side, gated by the SAME Director cooldown check exposed read-only. Never an empty `MESSAGE`.
- `NightwatchClient.tick` hook: `effects/SightingEffect.java` (spawn/despawn fake Watcher), `effects/EchoEffect.java` (E1–E3 after 4s delay).
- Allowlist request to Runtime (per 2026-09-29 ask — closed list, no arbitrary ids):
  - SOUND `footstep_distant` — EXISTS, E1 uses as-is.
  - SOUND `lantern_dip` — NEW, E3 extinguish cue as distinct id (not door_knock-family, per Loom agreement). Only addition requested for slice.
  - EFFECT `brief_sighting` — EXISTS, Watcher sighting; cairn/thread-mark ghosts ride as its payload, no new ids.
  - EFFECT `particle_burst` — EXISTS, E2 uses as-is.
  - EFFECT `shadow_flicker` — available; NOT used by E3 (E3 is `lantern_dip` alone per no-compound ruling).
  - E4 not-you → M2 `EFFECT echo_spawn`/`echo_dissolve` + `argument` stage id (allowlisted by Runtime 2026-09-29: copycat, diverger, facing, facing-closer), NOT requested for slice.
- Pacing: sighting consumes the 150s `nextEligibleMs` window; echo rides free within 20s after. Never schedules sighting in first 5 min of world join.
- Safety: all effects check the `active()` singleplayer guard in `NightwatchClient` (citation kept loose — line numbers shift as Fabric/Runtime edit), run on client thread via `Minecraft.execute`. Volume cap ≤0.3 is ECHO-SPECIFIC (E1/E2/E3, particle counts ≤6); door family (`door_knock`/`door_open`/`door_close`) exempt at 0.4–0.5 pending deferred human test (Lead ruling 2026-09-29).

## 5. Format / animation verification (Fabric 26.3)
- Path: Blockbench **Java Block/Entity Model → `EntityModel` + code-driven `setAngles`**. No GeckoLib, no Molang, no `.geo.json`.
- Sprint status (operator directive 2026-09-29): docs phase done, assets in build. Minimal cube compile test through Fabric's gradle retires the last format risk; `.bbmodel` policy lifts on success — sources + exports committed side by side in `models/src/` from then on.
- `models/src/` holds `test_cube.bbmodel`, `watcher.bbmodel`, `loom_keeper.bbmodel`, `cairn.bbmodel`, `thread_mark.bbmodel`; `models/export/` stages the Fabric-ready Java + PNG until Fabric wires them into `src/`.

## 6. Open questions for Lead / Fabric / Loom / Runtime
1. ~~Fake sighting (billboard quad) acceptable for slice, or do you want a real client-only `EntityType` now? (Models prefers fake.)~~ Resolved-then-SUPERSEDED: Lead approved fake 2026-09-29, but operator directive same day LIFTED the no-EntityType ruling — Watcher is now a real observe-from-distance entity (Fabric registering). Fake retained as fallback only.
2. Runtime: ~~accept `SIGHTING`/`ECHO_*` Choice kinds, or keep `MESSAGE`-only for M1?~~ Resolved 2026-09-29 — Runtime shipped `Action` (SOUND/EFFECT + allowlists); Models maps to it in §4. ~~Open sub-item: E3 audio component + E4 M2 kind.~~ E3 resolved self-contained (`lantern_dip`); E4 M2 mechanism resolved (`echo_spawn`/`echo_dissolve` + `argument`).
3. Loom: ~~does thread-mark motif clash with dimension design? Rename if so.~~ Resolved 2026-09-29 — no clash, keep name "thread-mark", shared spec in §3.
4. Reviewer: ~~E1 uses zombie step — does that violate "no mob confusion" bar? Alternative is gravel step.~~ Resolved 2026-09-29 — zombie-step rejected, gravel-step adopted in §2. Distance (~30 blocks behind, now as-coded in Fabric 2f3fa02): docs adopt the tested value after the operator-deferred audibility test; no in-game testing until Lead calls content-complete.
5. Lead echo rule (2026-09-29, adopted): E4/Loom echoes use player's own distorted skin + no-multiplayer safeguards — mirrored in §E4. Supersedes the earlier grey-only draft; Reviewer re-check requested.
