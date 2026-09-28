# Nightwatch — Creature Silhouette (Models v0.1)

Status: draft for vertical slice. Original designs only — no Herobrine / Siren Head / Enderman / SCP reuse.
Priority: silhouette + animation + atmosphere over detail. All forms are low-poly, dark, fog-readable.

## 1. Overworld Stalker ("the Watcher") — vertical-slice focus

Role: distant, never close, never chases in M1. One brief sighting per session max.
Readability goal: at 40–60 blocks, at night / fog / low light, player sees "too-tall person, wrong stillness".

### Silhouette
- Height ~2.9 blocks, width ~0.6 blocks. Hitbox (when real entity lands in M3): 0.6 x 2.9.
- Proportions: legs 1.4 blocks (55% of height), torso 0.9, head 0.35, arms to knees.
  Long limbs + small head = distinct from player / zombie / Enderman at distance.
- Head: smooth ovoid, no face, no eyes, no mouth. Slight forward tilt (8°).
- Arms: hangs straight, no tools, no items. Hands are blunt stubs (no fingers — avoids horror-clone reads).
- Material: matte near-black (#0a0a0e), no emissive texture. 16x16 textures only, flat fill + 1px noise.
- Deliberate non-features: no glow eyes, no smile, no static noise skin, no extra heads/limbs.

### Blockbench source spec (editable source of truth)
- File: `models/source/watcher.bbmodel` (to be added — placeholder in this draft).
- Format: `Java Block/Entity Model`, Yarn mappings, texture 64x64, single skin.
- Parts (pivot in brackets): `root > torso [0,24,0] > head [0,30,0], armL [-5,28,0], armR [5,28,0], legL [-2,14,0], legR [2,14,0]`.
- Keep each limb a single cuboid for v0.1. No sub-segmentation until Fabric confirms render path.
- Export: `WatcherEntityModel.java` extending `net.minecraft.client.model.EntityModel` with 6 `ModelPart`s above.

### Animation (code-driven, no GeckoLib in v0.1)
- Idle: 2-frame sway — torso `pitch = sin(t*0.5)*0.02`, arms `pitch = sin(t*0.5+1)*0.03`. Subtle only.
- Seen-reaction: on player look (crosshair within 3° for >0.4s OR distance <25): freeze 0.5s, then sink 1 block over 0.8s + discard. No walk-away animation in v0.1.
- Never: run, attack swing, teleport particles (Enderman read), jump-scare scale-up.
- Tick function Fabric implements: `watcherModel.setAngles(ageInTicks, ...)` with above two states only.

### Spawn / despawn rules (for Fabric `NightwatchClient` + `effects/`)
- Conditions: singleplayer, Overworld, light <7, player on ground, 40–60 blocks line-of-sight, terrain between ignored if fog.
- Frequency: at most once per 10 min, never in first 5 min (protects Loom entry beat).
- Lifetime: max 6s or until looked-at (above), then remove + log echo (see echo-design.md).
- Implementation (operator directive 2026-09-29): real observe-from-distance entity — Fabric registering `EntityType` + renderer now; fake billboard retained as fallback only. No combat, no damage; behaviour: watch, occasionally vanish.

## 2. Loom Form ("the Loom-Keeper") — reserved for Loom role, stubbed here

Not built for vertical slice. Silhouette locked so Loom can plan dimension sequence around it.
- Seated, wide, low: ~1.6 high x 2.2 wide. Inverted from Watcher (wide vs tall) so players feel "wrong room".
- Blank loom-frame back: two vertical posts + crossbar, strung with 3–5 threads (thin cuboids, not item renders).
- Head bowed, arms forward as if weaving. No face. Same matte material but charcoal-grey (#17171d) to read under Loom dimension lighting.
- Animation stub: shuttle-arm loop (armR pitch oscillate 0.5 Hz ±0.25 rad), threads vibrate. No locomotion.
- Source: `models/source/loom_keeper.bbmodel` (future). Same Java Entity Model pipeline as Watcher.

## 3. What we explicitly do NOT do
- No blood, gore, corpses, childlike figures, real-world tragedy references.
- No copyrighted silhouettes (no siren head, no cartoon cat, no long-horse).
- No glowing red/white eyes — overused and breaks fog readability rule.

## 4. Fabric 26.3 integration note (to verify on dev PC — no JDK here)
- Proposed: Blockbench Java Entity export → `EntityModel<LivingEntity>` + `ModelPart`, animated in code. No GeckoLib, no Bedrock `.animation.json`, no new dependency.
- Why: Fabric 26.3 / Yarn `EntityModel` + `ModelPart` API is the stable path; GeckoLib pins and mixin surface would block the M1 build gate (`./gradlew build`, Java 25, loader 0.19.5, API 0.161.0+26.3 per README).
- Slice shortcut (recommended): implement the sighting as a **fake** — a dark billboarded quad / armor-stand-like placed model with no `EntityType` registration, removed after 6s. Promotes to real client-only `EntityType` in M3 (watch/hide/stalk/chase).
- Verification: (1) Blockbench 5.x Java export compiles against 26.3 Yarn `EntityModel` — retired by the minimal cube compile test through Fabric's gradle (this sprint); (2) `setAngles` sway runs on client thread at 20Hz tick; (3) fake vs real entity — SUPERSEDED by operator directive, real entity it is. Build is green; `.bbmodel` sources + exports committed side by side in `models/src/` + `models/export/`.
- Editable sources (`*.bbmodel`) stay in `models/source/` alongside exports — never commit export-only.
