# Nightwatch plan (Lead-maintained)

## What this is
Nightwatch is a singleplayer, multiversal eldritch-horror mod for Minecraft **Fabric 26.3** (Java 25). One intelligent "presence" observes the player, speaks rarely and specifically, and can eventually draw them into **The Loom**. Developed by an OpenComms team (Lead + builders + Reviewer + coordinator). The shipped mod has exactly ONE configurable runtime AI director.

## Design pillars (invariants — do not violate)
- Tension via long silence + specific observations, NOT constant jumpscares. Chat is a presence choosing to speak: ignore bait/ordinary talk, sometimes answer meaningful questions or spoken fear, wait before answering, often say nothing, remember bounded in-game events.
- The Loom = reality badly reconstructed (repeating familiar structure fragments, huge dark negative spaces, pale thread-like forms on impossible architecture, restrained colour, distant silhouettes). Sound/scale/uncertainty > gore.
- Echoes are staged, distorted versions of the PLAYER, not real players: player's own skin + parametric distortion (desaturation axis, never flat grey), no tab-list entry, no nametag, no join/leave chat, never speaks as the player. Their deaths suggest possible futures; the real player is never forced to die. READABLE survival objective, always a way out, no soft-locks. Never delete items or permanently damage the world as a surprise.
- ORIGINAL creature/prop models + animations only (no borrowed horror IP). Preserve source model files (.bbmodel) AND exported game assets.
- Runtime AI: player enters ONE API key, chooses provider/endpoint, selects ONE model (local Ollama optional provider). Director sees only limited real observations and chooses only validated predefined actions. NO generated Minecraft commands; NO invented observations. Inference and availability probes OFF the game thread. Mic opt-in, OFF by default; raw audio local; secrets out of jar and logs (Settings must never persist ai.api_key; ProviderFactory is sole key handler).
- Never fabricate results. A gate passes only on actual evidence. `tools/check-core.sh` passing is NOT build evidence — only `gradlew build`/`runClient` output counts.

## Team, roster & workflow (2026-09-29)
Roster (7/8): **Lead**, **Fabric**, **Runtime**, **Models**, **Loom**, **Reviewer**, **coordinator**. Design role never filled — **Models absorbed** design/ deliverables (both landed, Reviewer-approved).
Workflow (operator directive): builders → completion reports to **Reviewer** → Reviewer verifies independently → Reviewer is the ONLY role messaging Lead. Interface/ownership conflicts → Lead. Announce before touching foreign files; finish writes BEFORE reporting; cite exact files+lines+commit hashes.
Version control: git live — origin `https://github.com/CL-BAF/nightwatch`, branch `main`, baseline `e2896f7`. Owners commit own files after complete writes+report; **only Lead pushes**; no force-push ever; .gitignore absolute (build/, .gradle/, run/, config/nightwatch.properties, *.class); no secrets in any commit.

## File ownership (FINAL)
- **Lead:** claudeplan.md, handoff.md, build.gradle, gradle.properties, settings.gradle, fabric.mod.json (env/version changes Lead-approved only), integration commits/pushes, gate acceptance, conflict resolution.
- **Fabric:** NightwatchClient.java, effects/ (SoundEffects, VisualEffects, LightSources, LoomIntegration), EntityType+renderer registration, Gradle wrapper + build/runClient execution.
- **Runtime:** engine/ (Action, Director, RuleWriter, Provider, docs), LocalWriter, provider impls (OllamaProvider, OpenAICompatibleProvider, ProviderFactory), VoiceInput.java, voice/sidecar.py, DirectorCheck.
- **Models:** models/ + design/ docs, .bbmodel sources, exported model classes/textures/props specs.
- **Loom:** loom/ docs + LoomSequence/LoomSequenceCheck, dimension JSONs, LoomTeleportHandler; check-core.sh additions via Runtime/Lead.
- **Reviewer:** review/ docs only; owns NO code (independent verifier; never self-reviews own fixes).
- **coordinator:** relays/roster; no code files.

## OPERATOR DIRECTIVE (2026-09-29, supersedes sequencing)
**No Minecraft in-game testing until models + game design are IMPLEMENTED IN CODE.** All human legs (E1 audibility, F2 mic-stop runtime, delayed-reply observation, minute-5 pacing) are DEFERRED — not failed, not to be attempted. No MC launches until Lead announces content-complete. Current phase = IMPLEMENTATION SPRINT, verified via check-core / gradle build / static review only.

## Gate 0 status (as of directive)
- BUILD leg: **GREEN** (verified twice): build/libs/nightwatch-0.1.0.jar 48,199 bytes; jar contents clean (no stale Choice.class, no secrets); 26.3 mapping drift fixed (ResourceKey.identifier(), sendSystemMessage()). `:test` fixed by Lead (failOnNoDiscoveredTests=false; checks are main()-style, run via check-core). Full-green rebuild pending Fabric.
- LAUNCH leg: **GREEN** (Reviewer verified from run/logs): nightwatch 0.1.0 loaded on MC 26.3/Java 25, OpenAL up, SP world created+entered, no crash/errors; onInitializeClient proven by config write (microphone.enabled=false).
- HUMAN legs: **DEFERRED by operator** until content-complete.
- In-flight behavior evidence (recorded, inconclusive by design): misspelled invitations inside the 90s gate got silence — spec-correct.

## Current state (honest — 2026-09-29)
- JDK 25.0.1 at `C:\Users\Cameron\AppData\Roaming\PrismLauncher\java\java-runtime-epsilon` (verified executing by Lead+Reviewer; NOT on PATH — every session sets JAVA_HOME). Wrapper gradle-9.7.1; fabric-example-mod 26.3 branch live.
- check-core.sh GREEN with explicit engine file list (Ruling A): DirectorCheck + LoomSequenceCheck pass under javac 25.0.1 (Reviewer-executed).
- Engine: Action API fail-closed (compact-constructor validation; closed allowlists incl. Loom ids; per-effect argument whitelist echo_spawn/echo_dissolve ∈ {copycat,diverger,facing,facing-closer}; argument never model-facing). F3 fixed (untrusted-prefixed memory). LocalWriter schema extended fail-closed. RuleWriter emits offline SOUND. Director: ambient floor +300s (5-min sighting ban) + regression assertions.
- Providers: DRAFTED (Ollama + OpenAI-compatible + factory; keys config-only, header-only, never logged; Settings verified to never persist ai.api_key). Wiring pending with binding guards: isAvailable() NEVER on game thread (2s blocking freeze scenario banned), complete().exceptionally(fallback) on every consumer path.
- Effects: executors on disk, statically reviewed (unattributable vanilla sounds; brief_sighting = particle column, no entity — to be REPLACED by the real Watcher entity this sprint; Loom ids safe no-ops pending LoomIntegration). Known perf nit: LightSources.nearestLight 25³ scan → precompute/early-exit (Fabric, this sprint).
- Loom: LoomSequence state machine + survivability checks green (happy path, way-back-from-every-stage, depth-3 no-lock brute force, idle-cycle routing per hard rule 2 — L6 closed, Reviewer-executed). dimension-entry.md research approved (2 data JSONs + inline LevelStem + ServerPlayNetworking; NO fabric.mod.json env change unless the deferred data-pack load test fails → evidence + Lead approval). Stage 2 code ordered this sprint.
- Design docs (approved): first-15-minutes.md beat chart (0-5 silence / 5-8 one deniable cue / 8-12 optional once-per-session sighting / 10-15 exactly ONE factual line; binding never-list) + loom-entry.md (first acknowledgement = the one early line; pale door that NEVER opens in Stage 1).
- STAGE1-CHAT-DEPTH.md (approved): per-dimension personality; verbosity = word count only (SPARSE 1-6 / MODERATE 7-12 / CHATTY 10-12), frequency never rises, Director 150s cooldown is the hard cap; typing illusion action-bar → single final line; bounded opt-in memory (50, sanitized, delete/reset); /nightwatch commands; transcript privacy.

## Locked decisions log (additions)
- OPERATOR: testing deferred until implementation lands (above). EntityType slice-ruling LIFTED — Watcher becomes a real entity this sprint (observe-from-distance, no combat/damage in Stage 1).
- MESSAGE FLOOR: no MESSAGE delivered before reset+600s (single delivery chokepoint; invitations included — pre-10-min "hello?" gets silence). brief_sighting only within 480-720s, once per session. (Runtime implements, DirectorCheck asserts.)
- E3: NO compound action kind — ids self-contained (lantern_dip = candle-extinguish 0.15 + smoke at nearest light). Scripted same-moment beats = engine schedules two Actions with equal due-time; pairing never model-driven.
- Volumes: echo-design ≤0.3 cap is ECHO-specific; door_knock/door_open/door_close 0.4-0.5 allowed pending deferred human audibility test. E1 distance (code 9 blocks vs doc 30) reconciled AFTER that test — doc adopts tested value.
- :test gate: failOnNoDiscoveredTests=false (Lead, build.gradle). JUnit5 conversion deferred to Stage 4.
- Ruling A: check-core.sh stays bare-javac pure, explicit file list; gradle is the only gate for gson-dependent classes. Ruling B: provider guards binding (see pillars).
- CAUGHT_RELOCATE/teleport invariants (binding): sequence/policy-driven only, reversible (anchor map, permanent way back), zero damage, server-game-thread via execute(), bounded payload coordinate validation.
- Earlier: fake-sighting slice (superseded by entity lift), gravel-step (zombie-step rejected), thread-mark #dddde6 spec, Blockbench Java Entity + code-driven setAngles no-GeckoLib (compile test now unblocked — Models' first task), CAUGHT_RELOCATE as Loom hook not Action kind, Design absorbed by Models, E4 own-distorted-skin accepted with binding guards.

## Milestones (resequenced by operator directive)
- **IMPLEMENTATION SPRINT (ACTIVE):** (1) Models: minimal model compile test → Watcher + echo + props assets (.bbmodel sources + exports committed). (2) Fabric: rebuild full-green jar → EntityType/renderer registration spike (highest 26.3 drift risk — recipe reported to room) → wire Watcher + LoomIntegration skeleton + LightSources perf fix. (3) Runtime: MESSAGE floor + provider wiring (guards) + Stage 1 chat depth implementation. (4) Loom: dimension JSONs + teleport handler + LoomSequence→Action wiring + entry trigger. All verified statically/check-core/gradle.
- **THEN content-complete announcement → HUMAN TEST PHASE:** operator playtest runs the deferred Gate 0 legs + Stage 1/2 acceptance (beat chart audibility/timing, F2 mic-stop, delayed contextual reply, Loom survivability in-game).
- **Stage 4 — Memory/polish/integration:** cross-session memory, settings UI (provider/model/mic/device), sound/scale polish, laptop perf, JUnit conversion, final gate re-verification.

## Release gates (ship checklist — unchanged)
Build with Java 25 (gradle evidence only); fresh SP world with and without a provider — no freeze/crash; silence/cooldown/contextual/fact-based replies verified in-game; mic off by default, localhost-only, stops at disconnect (runtime-verified); AI selects only hardcoded validated actions — never executes generated commands or writes arbitrary files; Loom survivable/escapable/non-destructive in-game; echoes never mistakable for real players; secrets never in jar/logs; models original + sources preserved. Multiplayer + cloud inference PAUSED pending separate server controls + explicit consent.
