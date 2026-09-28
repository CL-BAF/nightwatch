# Nightwatch — Creature Silhouette (Models v2, canon: THE UNRAVELLER)

Status: canon v2 per Lead operator-canon ruling 2026-09-29 (pillars). Original designs only — hard bans (§5) apply.
Priority: silhouette + animation + atmosphere over detail. All forms are low-poly, dark, fog-readable.

## 0. Canon (binding, from Lead — the frame everything below obeys)
- What's behind the Loom is THE UNRAVELLER. You never see its full body. The monsters are PARTS of it pushing through into the Overworld; each dead echo is a version of the player it tried to pull apart.
- Core feeling: "it knows which version of you survives, and it's trying to make that stop being true."
- Model language: mismatched anatomy, repeated faces, movements slightly out of sync.

## 1. Overworld Stalker ("the Watcher") — a PART pushing through

Role: distant, never close, never chases in Stage 1. One brief sighting per session max (engine window 480–720s, binding).
Readability goal: at 20–40 blocks, at night / fog / low light, player sees "a piece of something, wrong stillness" — never a whole creature.

### Silhouette (fragment, not creature)
- Envelope ~2.9 blocks tall, ~0.6 wide. Hitbox: 0.6 x 2.9 (unchanged — Fabric's EntityType work proceeds on these numbers).
- Mismatched anatomy (deliberate, baked into geometry): left arm reaches the knees (21 units), right arm stops at the hip (17); left leg full-length (22), right leg shorter (19) with raised pivot so both feet land level. Shoulders canted (arm pivots ±0.05/−0.09 z-rot). Nothing mirrors.
- Face-like motifs that REPEAT: three blank ovoid plates, featureless (no eyes, no mouth, never a face that looks back) — head (6x6x6, tilt 8° + 3.4° cant), chest plate (4x5x0.5, marginally lighter #1a1a22), left-thigh plate (3x4x0.5). Same motif three times = "it" recurring, not three creatures.
- Thread continuity edges: 1px strands (#dddde6 shared spec) cut off mid-air above the right shoulder (8 units up) and below the right ankle (7 down) — the form visibly CONTINUES past what you can see.
- Material: matte near-black (#0a0a0e body, #0d0d13 torso region), no emissive except thread pixels. 64x64 texture, flat fills + 2% 1px noise.
- Deliberate non-features: no glow eyes, no smile, no static-noise skin, no extra heads, no tentacles, no wings, no mouth anywhere.

### Blockbench source spec (editable source of truth)
- File: `models/src/watcher.bbmodel` (committed — hand-authored Java-Entity source equivalent, format `java_block`, box_uv; re-save in Blockbench 5.x when available).
- Texture 64x64, single skin `textures/entity/watcher.png`.
- Bones (pivot in brackets — each animatable part is a group; static plates merge into parents): `torso [0,8,0] > head [0,8,0], arm_left [-6.5,20,0], arm_right [6.5,20,0], thread_shoulder [6.5,20,0]`; root-level `leg_left [-2,2,0]`, `leg_right [2,5,0] > thread_ankle [2,5,0]`. Face plates are static cubes inside torso / leg_left (no bones).
- Export: `model/WatcherModel.java` (`EntityModel<LivingEntityRenderState>`, `createBodyLayer()` + `LAYER`), verified compiling with javac 25.0.1 against the 26.3 mapped jars (geom.ModelPart API — note: 26.3 moved ModelPart to `client.model.geom`, EntityModel is state-parameterized).

### Animation (code-driven DESYNC, no GeckoLib — canon movement language)
- Rule: parts move slightly OUT OF SYNC with the whole, with lag in follow-through. The form must never read as one creature walking. All motion on `state.ageInTicks`, subtle at distance.
- Per-part phase/amplitude spec (implemented in `WatcherModel.setupAnim`, Fabric's renderer must call it per tick AND route per-part phase offsets if it builds its own layer on top):
  | part | freq × | phase | amplitude (rad) |
  |---|---|---|---|
  | torso | 1.0 | 0.0 | 0.02 pitch |
  | head | 0.3 | +0.6 | 0.05 yaw |
  | arm_left | 1.0 | +1.0 | 0.03 pitch |
  | arm_right | 0.9 | +2.1 | 0.025 pitch |
  | leg_left | 0.7 | +0.3 | 0.015 pitch |
  | leg_right | 0.8 | +1.7 | 0.015 pitch |
  | thread_shoulder | 1.3 | +2.6 | 0.06 pitch |
  | thread_ankle | 1.1 | +0.9 | 0.06 pitch |
  Threads swing widest (they're loose); legs barely move (it doesn't walk — it stands).
- Seen-reaction (canonical staging, Models+Fabric 2026-09-29 — Reviewer C1 wording): freeze while directly seen → sink/fade on vanish/6s. Concretely: view-dot > 0.9 holds all parts frozen (`setSeen(true)`); on look-away break, 6s expiry, or distance < 25, the form sinks 1 block over 0.8s and discards (Fabric `vanishAll` covers world-leave). No walk-away, no attack, no teleport particles, no scale-up.

### Spawn / despawn rules (for Fabric entity + `effects/`)
- Conditions: singleplayer, Overworld, light < 7, player on ground, 20–40 blocks line-of-sight.
- Distance bias (Lead ruling 2026-09-29 — design band wins over code/script values): spawner default ~30 blocks ahead, clamped 20–40. Models confirms 30 as the legibility bias point: at 30 blocks the 2.9-tall matte form resolves as a standing fragment at night/fog (limb mismatch + face-plates readable, thread pixels sub-pixel shimmer only), while staying far enough that no feature invites approach.
- Frequency: engine window 480–720s post-reset, once per session (binding); never in first 8 min (protects the first-15-minutes arc + MESSAGE floor).
- Lifetime: max 6s or until looked-at (above), then remove + echo window (see echo-design.md).
- Implementation (operator directive 2026-09-29): real observe-from-distance entity — Fabric registering `EntityType` + renderer now; fake billboard retained as fallback only. No combat, no damage; behaviour: watch, occasionally vanish.

## 2. Loom Form ("the Loom-Keeper") — reserved for Loom role, stubbed here

Not built for vertical slice. Silhouette locked so Loom can plan dimension sequence around it.
- Seated, wide, low: ~1.6 high x 2.2 wide. Inverted from Watcher (wide vs tall) so players feel "wrong room".
- Blank loom-frame back: two vertical posts + crossbar, strung with 3–5 threads (thin cuboids, not item renders).
- Head bowed, arms forward as if weaving. No face. Same matte material but charcoal-grey (#17171d) to read under Loom dimension lighting.
- Animation stub: shuttle-arm loop (armR pitch oscillate 0.5 Hz ±0.25 rad), threads vibrate. No locomotion.
- Source: `models/source/loom_keeper.bbmodel` (future). Same Java Entity Model pipeline as Watcher.

## 3. What we explicitly do NOT do (hard bans — canon + pillars)
- No blood, gore, corpses, childlike figures, real-world tragedy references.
- No borrowed horror IP (no Herobrine / Siren Head / Enderman / SCP reads) and NO named Lovecraft entities, NO Cthulhu-like silhouettes (no tentacles, wings, beards, cosmic-head shapes), NO boss-monster framing — the Watcher is a fragment, never a boss.
- No glowing red/white eyes — overused and breaks fog readability.
- No whole-creature gestalt: symmetry, synchronized gait, or readable "body" are design failures for this asset.

## 4. Fabric 26.3 integration note (verified this sprint — no JDK gap)
- Implemented: Blockbench Java Entity shape → `EntityModel<LivingEntityRenderState>` + code-driven `setupAnim`. No GeckoLib, no Bedrock `.animation.json`, no new dependency.
- 26.3 drift found and handled (javap against loom-cache mapped jars 2026-09-29): `ModelPart` lives at `client.model.geom` (not `client.model`), `EntityModel`/`Model` are render-state-parameterized (`setupAnim(S)` takes the state, clock is `state.ageInTicks`), builders are `CubeListBuilder`/`PartDefinition`/`MeshDefinition`/`LayerDefinition`/`PartPose` under `geom`/`geom.builders`. Old Yarn `setAngles` naming is gone — Fabric must call `setupAnim(state)`.
- Minimal compile test: `model/TestCubeModel.java` (single cube, `models/src/test_cube.bbmodel`, 16x16 texture) + `model/WatcherModel.java` both compile clean under `javac 25.0.1` against the mapped jars — handed to Fabric for the gradle pass.
- Editable sources (`models/src/*.bbmodel`) committed alongside exports (`model/*.java`, `textures/entity/*.png`) — never export-only.
