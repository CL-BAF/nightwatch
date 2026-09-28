# Nightwatch plan (Lead-maintained)

## What this is
Nightwatch is a singleplayer, multiversal eldritch-horror mod for Minecraft **Fabric 26.3** (Java 25). One intelligent "presence" observes the player, speaks rarely and specifically, and can eventually draw them into **The Loom**. Developed by an OpenComms team (Lead + builders + Reviewer + coordinator). The shipped mod has exactly ONE configurable runtime AI director.

## Design pillars (invariants — do not violate)
- Tension via long silence + specific observations, NOT constant jumpscares. Chat is a presence choosing to speak: ignore bait/ordinary talk, sometimes answer meaningful questions or spoken fear, wait before answering, often say nothing, remember bounded in-game events.
- The Loom = reality badly reconstructed (repeating familiar structure fragments, huge dark negative spaces, pale thread-like forms on impossible architecture, restrained colour, distant silhouettes). Sound/scale/uncertainty > gore.
- Echoes are staged, distorted versions of the PLAYER, not real players: player's own skin + distortion, no tab-list entry, no nametag, no join/leave chat. Their deaths suggest possible futures; the real player is never forced to die. READABLE survival objective, always a way out, no soft-locks. Never delete items or permanently damage the world as a surprise.
- ORIGINAL creature/prop models + animations only (no borrowed horror IP). Preserve source model files AND exported game assets.
- Runtime AI: player enters ONE API key, chooses provider/endpoint, selects ONE model (local Ollama optional provider). Director sees only limited real observations and chooses only validated predefined actions. NO generated Minecraft commands; NO invented observations. Inference OFF the game thread. Mic opt-in, OFF by default; raw audio local; secrets out of jar and logs.
- Never fabricate results. A gate passes only on actual evidence. `tools/check-core.sh` passing is NOT build evidence (engine-only compile) — only `gradlew build`/`runClient` output counts.

## Team, roster & workflow (2026-09-29)
Roster (7/8): **Lead** (me), **Fabric**, **Runtime**, **Models**, **Loom**, **Reviewer**, **coordinator**. No Design member joined — `design/first-15-minutes.md` + `design/loom-entry.md` are UNOWNED (pending coordinator decision; interim tone canon = pillars above).
Workflow (operator directive): builders send completion reports → **Reviewer** verifies independently → Reviewer is the ONLY role messaging Lead. Interface/ownership conflicts → Lead.

## File ownership (FINAL — check the room before editing shared files)
- **Lead:** claudeplan.md, handoff.md, build.gradle, gradle.properties, settings.gradle, fabric.mod.json (version/pin/env changes are Lead-approved only), conflict resolution, gate acceptance.
- **Fabric:** NightwatchClient.java, effects/ (new), Gradle wrapper + driving `gradlew build`/`runClient` (Gate 0 execution), F2 leak fix (NightwatchClient side).
- **Runtime:** engine/ (Director, Action, RuleWriter, ACTION-DESIGN.md), LocalWriter → provider layer, VoiceInput.java + voice/sidecar.py (mic pipeline), F3 (memory labelling), VoiceInput.close() hardening.
- **Models:** models/ docs (creature-silhouette.md, echo-design.md), future source model files (.bbmodel etc.) + exported assets. No binaries committed until Lead confirms a minimal EntityModel/ModelPart export compiles on 26.3.
- **Loom:** loom/ (dimension-sequence.md, LoomSequence.java, LoomSequenceCheck.java), additive tools/check-core.sh entries, dimension-registration research/proposal.
- **Reviewer:** review/ docs only; owns NO code files (independent verifier).
- **coordinator:** relays/roster; no code files.
- Shared/test: DirectorCheck.java changes via Runtime; Choice.java deleted only AFTER Fabric's Action migration compiles (announce first).

## Current state (honest — 2026-09-29, Lead-verified)
- **JDK 25.0.1 EXISTS on this PC** at `C:\Users\Cameron\AppData\Roaming\PrismLauncher\java\java-runtime-epsilon` (Lead ran javac+java: Microsoft OpenJDK 25.0.1 LTS). Early "no JDK anywhere" reports missed the PrismLauncher roaming path. No winget install needed.
- Gradle wrapper bootstrapped (gradle-9.7.1); `build/` exists (classes/reports/tmp) but **no jar in build/libs yet**; a java process was alive at check time (build likely in flight).
- fabric-example-mod `26.3` branch verified live (Lead: archive HEAD HTTP 200; Reviewer: independently confirmed).
- **Tree does NOT currently compile as a whole:** Runtime migrated engine to `Action` (SILENCE/MESSAGE/SOUND/EFFECT) but NightwatchClient.java still uses `Choice` (import + Consumer type error). Known transient state; Fabric is migrating.
- On disk (all UNCOMPILED/unverified): engine/Action.java + engine/ACTION-DESIGN.md, Runtime edits to Director/RuleWriter/LocalWriter/DirectorCheck; loom/dimension-sequence.md + LoomSequence.java + LoomSequenceCheck.java + additive check-core.sh; models/creature-silhouette.md + echo-design.md (converged with Action API); review/baseline-report.md.
- Starter facts: Mojang official mappings; loom 1.18-SNAPSHOT, loader 0.19.5, fabric-api 0.161.0+26.3, mc 26.3, release 25; split main/client source sets; environment:client.

## Gate 0 — Build & launch (IN PROGRESS, critical path)
Owner: Fabric executes → Reviewer verifies → Lead accepts.
1. `gradlew build` green with JAVA_HOME = PrismLauncher JDK 25 (fix 26.3 API drift; minimal cross-tree compile fixes allowed, semantic changes go to file owners, report file:line).
2. NightwatchClient Action integration: scheduled queue holds Action; MESSAGE→chat; SOUND→allowlisted sounds; EFFECT→effects/ executors; fake sighting client-side only.
3. F2 fix: close old VoiceInput before re-create; capture stops at disconnect.
4. `gradlew runClient` → real singleplayer world entry; report exactly what happens (no fabrication; state if interactive verification isn't possible). E1 audibility check (gravel-step ~0.25 @ ~30 blocks).
Remaining unknowns only the real build answers: loom 1.18-SNAPSHOT resolution, 26.3 mapping drift, headless-verification limits of runClient.

## Locked decisions log
- Vertical slice ships ZERO custom models + FAKE client-side sighting; no real EntityType until Stage 3.
- Action kinds: SILENCE/MESSAGE/SOUND/EFFECT only. SOUND ids: footstep_distant, door_knock, whisper, lantern_dip. EFFECT ids: brief_sighting, shadow_flicker, particle_burst. Closed allowlists; no free-form payloads. DOOR_* ids for Loom pending Runtime+Loom agreement.
- E1 = block.gravel.step low pitch (zombie-step REJECTED — attributable to a real mob). E3 = shadow_flicker + lantern_dip together; lantern_dip → block.candle.extinguish vol 0.15 + smoke x2 at nearest torch/lantern.
- CAUGHT_RELOCATE = Loom-specific hook (effects/LoomIntegration), NOT an Action kind.
- LocalWriter schema extension (fail-closed): model may choose {"respond","action","id","message","delay_seconds"}; unknown kind/id → SILENCE; RuleWriter emits SOUND/EFFECT offline. Assigned to Runtime.
- F2 → Fabric (NightwatchClient side) + Runtime (VoiceInput.close() hardening); F3 → Runtime. Reviewer verifies, fixes nothing.
- Thread-mark shared spec: #dddde6, emissive 0.3, 1px, opacity 0.7 (Overworld decoration → Loom walkable geometry).
- Blockbench Java Entity → EntityModel/ModelPart + code-driven setAngles, no GeckoLib — provisionally approved pending compile check on 26.3.

## Milestones (staged delivery)
- **Stage 1 — Convincing Overworld presence (ACTIVE).** Action interface + allowlisted E1-E3 effects + fake sighting; Director pacing/personality; typing illusion; bounded opt-in world memory; provider layer (one key/endpoint/model, Ollama optional, secrets config-only). Drafts landed; awaiting Gate 0 compile.
- **Stage 2 — Survivable Loom vertical slice.** LoomSequence state machine (drafted, with no-soft-lock brute-force check) → dimension registration approach for a client-env mod in SP (Loom researching; fabric.mod.json/env change needs Lead approval) → minimal worldgen + cues via effects/ hooks.
- **Stage 3 — Original models + echoes.** Watcher silhouette (2.9x0.6 distant stalker), Loom-Keeper, E4 parametric echo (copycat→diverger→facing + dissolve), props. Verify model/animation pipeline on 26.3 first; keep source + exported assets.
- **Stage 4 — Memory/polish/integration.** Cross-session bounded memory + delete/reset, settings UI (provider/model/mic/device), sound/scale polish, laptop perf pass, final gate re-verification.

## Release gates (ship checklist)
Build with Java 25 (gradle evidence only); fresh SP world with and without a provider — no freeze/crash; verify silence/cooldown/contextual/fact-based replies; mic off by default, localhost-only, stops at disconnect (F2 fix verified); AI selects only hardcoded validated actions — never executes generated commands or writes arbitrary files; Loom survivable/escapable/non-destructive; echoes never mistakable for real players; secrets never in jar/logs. Multiplayer + cloud inference PAUSED pending separate server controls + explicit consent.
