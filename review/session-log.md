# Nightwatch — operator session log (Reviewer, evidence hub)

Session window opened: 2026-09-29, content-complete called at HEAD e39921e (Lead). Code freeze in effect for all roles until session verdicts land. Evidence flows: operator → Reviewer (this log) → Lead routes fixes to owners. Nobody marks a script row PASS except the operator.

## Pre-session confirmations (Reviewer, on record)

1. NPE-ordering angle (Lead's explicit confirm): VERIFIED — TypingIndicator.java:61-65: the null-safe liveness re-checks run first (player null → return at 61; level null → return at 62; singleplayerServer null → return at 63), and ONLY THEN the Loom check dereferences client.level.dimension() (65). A disconnect straddling the typing window returns at the null guards — no NPE path exists. (Same conclusion as my earlier guard inspection; recorded here per Lead's request.)
2. Authoritative artifact: reproducible from committed HEAD e39921e by THREE independent builds — Reviewer's own `gradlew clean build` (100,841 B @ 10:48:06), Fabric's (100,841 B @ 10:48:36), and Lead's at 4e01b3e (100,841 B @ 10:50:48; 4e01b3e verified docs-only — NO source changes between e39921e and 4e01b3e), all byte-identical in size from the same code state. Jar listing: 76 entries, fabric.mod.json, all slice classes (Stage-1, providers, entity/model, loom, effects), dimension JSONs, textures — no config/.properties/secret file inside. PRE-FLIGHT REBUILD VERIFICATION: the artifact in build\libs at session start (100,841 B @ 10:50:48) IS the authoritative reproducible artifact; if the operator re-runs pre-flight step 2 (`gradlew clean build`, JAVA_HOME = PrismLauncher JDK) the output must match 100,841 B — Reviewer checks whichever path is used. Gate re-run at HEAD by Reviewer: green (all three checks); no source changed since e39921e.
3. Standing gates at freeze: check-core three-check run green (DirectorCheck, LoomSequenceCheck, LoomCueMapperCheck — Reviewer's own run); engine/ dependency-clean PASS; Settings first-run store never writes ai.api_key; provider key config-only/header-only/never-logged.

## Session evidence (to be filled per the script's evidence section)

### Session start — 2026-09-29 14:39 local (Lead-launched runClient, PID 29408)

- Boot evidence verified by Reviewer from %TEMP%\nw-session.log + run\logs\latest.log: nightwatch 0.1.0 / minecraft 26.3 / java 25 loaded among 52 mods; OpenAL initialized on the operator's G522 headset; sound engine started; benign Realms-auth errors only in the boot phase.
- Artifact at session start: the verified authoritative jar (100,841 B @ 10:50:48, triple-reproduced from e39921e-code-identical HEAD) — no rebuild re-run; matches the session-log rule.
- **SESSION FINDING F1 (FAIL-side, 14:41:05, Worker-Main-4)**: registry load error — `Failed to parse nightwatch:loom from pack nightwatch` in `minecraft:dimension_type` (IllegalStateException via RegistryLoadTask.PendingRegistration.loadFromResource). Loom's deferred assumption (a) — the dimension_type JSON field set matches 26.3 — is answered NEGATIVELY; assumption (b) — client-env mod data packs apply — is CONFIRMED (the nightwatch pack was read; this is a codec rejection, not a missing pack). Fail-closed behavior CONFIRMED LIVE: no crash, no damage; server.getLevel(LOOM) will be null → the door never opens. Impact: script rows C2–C9 (Loom) are BLOCKED this session; rows A1–A8 and B1–B4 remain fully testable. Triage routed to Lead + Loom (owner); diagnosis read-only during freeze; fixes wait for the post-session window. NOTE for the boot-check standard: the registry-load phase happens AFTER the startup lines (this error at 14:41:05 postdates the boot check) — full-session log review, not just startup lines.
- Operator trigger-commands question answered on record: only /nightwatch memory status|reset|delete exist; a dev cue-trigger command is a possible post-session item pending operator interest (Lead's log).

### Session Finding F1 — CORRECTED IMPACT + Reviewer process errors on record

**CORRECTION (Lead's, confirmed first-hand by Reviewer):** F1 did NOT stay fail-closed — it ESCALATED FATAL: `net.minecraft.ReportedException: Registry Loading` → `Caused by: IllegalStateException: Failed to load registries due to errors` → `[14:43:19] Stopping!` — the client EXITED; the operator was stuck at "preparing for world creation"; no window remained. The 26.3 RegistryDataLoader treats mod-data parse failures as FATAL at world creation — the fail-closed posture in LoomTeleportHandler (silent door) only covers the teleport path, NOT the registry-load path. **Impact: ALL script rows are blocked (A/B rows require a world), not just C2–C9.** Reviewer's earlier assessment ("fail-closed confirmed live, operator continues with A/B") was WRONG and is withdrawn.

**Deepest cause (read by Reviewer after Lead's correction — process error on record):** the log DOES name the field, one Caused-by deeper than Reviewer's original read: `Caused by: java.lang.IllegalStateException: No key has_ender_dragon_fight in MapLike[{...dimension_type/loom.json verbatim...}]`. Reviewer's triage claim "the log's generic message doesn't name the field" was WRONG — the standard (read to the deepest cause before routing suspects) applies to the evidence hub itself. The named field confirms 26.3 requires has_ender_dragon_fight (all three vanilla files carry it; Loom's proposed replacement includes it — pre-verification stands).

**Process additions encoded (Lead-mandated):**
1. Boot-check standard: include the FULL registry-load phase (world-creation data loading postdates main-menu boot by ~80s+ — this error class is invisible to startup-line checks).
2. NEW STANDING GATE: any resource/registry-affecting change requires a quickPlay WORLD-CREATION SMOKE before content-complete — main-menu boot is insufficient evidence; only world creation exercises RegistryDataLoader on mod data.
3. Deferred assumptions with fatal-block potential must be smoke-tested BEFORE a content-complete call, never deferred to the operator session. (Lead's content-complete call stands corrected on record — the world-creation smoke gate did not exist; lesson recorded in claudeplan at resolution.)

**Session state:** SUSPENDED at world-creation failure; narrow freeze exception (Loom JSON fix → Fabric rebuild + authorized quickPlay world-creation smoke → Reviewer verifies the registry-phase-clean log → Lead resumes the operator session). Reviewer's step in the fix flow: registry-phase-clean log verification.

### Loom diagnosis + Reviewer pre-verification (read-only, freeze held)

Loom's root-cause diagnosis (26.3 EnvironmentAttributeMap codec rebuild) CONFIRMED against the jar's own vanilla data (extracted by Reviewer from minecraft-client.jar: data/minecraft/dimension_type/{the_end,the_nether,overworld}.json + worldgen/flat_level_generator_preset/the_void.json):

- Loom's proposed replacement JSON is SHAPE-CORRECT on every statically checkable field: bed_rule {can_set_spawn/can_sleep "never" strings + destroy_on_use} matches the_end exactly; respawn_anchor_works bare boolean (the_nether form); ambient_light_color/fog_color/sky_color are #-prefixed hex strings (Loom's hunch RESOLVED: yes, #-prefixed — vanilla #3f473f etc.); sky_light_factor 0.0 form confirmed; monster_spawn_light_level 15 / block_light_limit 0 are bare ints matching the_end's EXACT values (overworld's IntProvider object proves both forms accepted); skybox "none" is a valid value (the_nether uses it); timelines "#minecraft:in_end" tag form confirmed (all vanilla files use "#minecraft:in_*"); has_ender_dragon_fight present in ALL vanilla files (required-form, included); height/logical_height/min_y 384/384/-64 matches overworld; infiniburn tag form confirmed. cardinal_light is optional (the_nether only) — correctly omitted. sky_light_color omitted — attributes appear per-dimension optional (the_nether omits sky_color/fog_color) — low risk.
- REMAINING LOW-RISK verify-at-fix items (registry refs from a foreign dimension, only the real client can confirm): default_clock "minecraft:the_end" referenced by a non-End dimension (a registry reference — should parse); timelines "#minecraft:in_end" same class. structure_overrides: [] addition to dimension/loom.json is likely REQUIRED, not optional — the vanilla the_void flat preset carries it; without it the LevelStem may fail to parse next once the dimension_type is fixed.
- Verdict: the proposed fix has high first-try probability; the fix-time relaunch (post-freeze) is the confirmation gate for C2–C9. Fix authorship: Loom, post-session per Lead's call.

### F1 fix — world-creation smoke VERIFICATION (Reviewer, 2026-09-29 ~14:52)

**PASS — verified first-hand from %TEMP%\nw-smoke2.log + run\logs\latest.log (read to the deepest cause: no Caused-by chains exist).** Fabric's smoke (build f43a2b0 @ HEAD 8afaecc; first launch failed on a space-in-name arg split — honestly disclosed, relaunched space-free): integrated server started 14:50:34; fresh world created + entered (Player530 logged in 14:50:38, joined the game; run\saves\New World (1) @ 14:51:36); **the nightwatch:loom dimension is REGISTERED and saving** (`Saving chunks for level 'ServerLevel[New World]'/nightwatch:loom`, 14:50:36) — registry + dimension_type ACCEPTED; **ZERO registry/exception/stop lines, zero non-benign errors** (only the empty-main-source-set warning + offline Realms noise). Loom's two low-risk foreign-registry references (the_end clock + in_end timeline tag) PARSED — both resolved by the smoke. The new standing world-creation smoke gate is satisfied for the f43a2b0/8afaecc tree. Registry gate: GREEN from the evidence hub. Lead's resume call pending; on resume, ALL script rows (A/B/C) are unblocked — the Loom exists at runtime.

### SESSION RESUMED — 2026-09-29 14:52 (Lead), F1 formally CLOSED

- F1 CLOSED: Lead's resume on the smoke gate; Reviewer's formal log verification recorded above (world created+entered, nightwatch:loom registered+saving, zero registry/exception/stop lines, foreign clock/tag refs parsed — read to the deepest cause: no Caused-by chains). The world-creation smoke gate (new standing, pre-flight step 2b) is satisfied for the f43a2b0-era tree.
- Resumed-session boot verified by Reviewer from %TEMP%\nw-session2.log (created 14:52:59): nightwatch 0.1.0 in the 52-mod list, OpenAL initialized on the operator's G522 headset, sound engine started, no registry errors. run\logs\latest.log rotated fresh at 14:53:05 (the session log; growing).
- Artifact note (Lead, verified): session runs the smoke-rebuild tree — jar 100,972 B @ 14:47:56 from the f43a2b0-era tree (grew from 100,841 B with the dimension JSON fixes) via runClient classpath; HEAD = bac26a3, tree clean.
- Session clock: row A timings start at the operator's fresh-world join (script step 5: survival / normal difficulty / daylight). ALL rows (A/B/C) unblocked — the Loom exists at runtime. Evidence collection LIVE: per-row PASS/FAIL + timestamps, latest.log, config, audibility verdict, verbatim chat lines. FAILs → Lead as triage notes with owner routing. Freeze holds through the session.

### Config change during session window — operator-directed cloud provider (recorded ~15:00)

- CONSENT BASIS: operator-directed (their key, their game, their machine) — the OPENCOMMS "cloud inference paused pending player consent" clause is satisfied by the operator's own explicit instruction. Direction: OpenCode Zen (OpenAI-compatible), paid qwen3.8-max.
- RUNTIME CONFIG — verified KEY-BLIND (run/config/nightwatch.properties, gitignored): ai.provider=openai-compatible, ai.endpoint=https://opencode.ai/zen, ai.model=qwen3.8-max, ai.api_key EMPTY (operator supplies directly; the value is never read or logged by the Reviewer), microphone.enabled=false.
- PRIVACY RULING CORROBORATED against the actual Zen docs: paid models are zero-retention with no training use; the docs' exception list is exactly the free/trial models (incl. NVIDIA security-logged endpoints, OpenAI/Anthropic 30-day retention, contributor-training models). The Lead's free/stealth exclusion is stricter than the docs require (some free stealth models are also zero-retention) — conservative posture endorsed: prompts carry game chat + optional mic transcripts.
- PRE-FLIGHT (Lead): curl of ProviderWriter's exact prompt shape against Zen before client restart. Known risk: no response_format → markdown-fenced JSON parses to silence (fail-closed but invisible). If fences appear: alternates, else a scoped-freeze-exception Runtime fix (strip fences pre-parse) — Reviewer verification bar: strip-only, fail-closed to silence on unparseable, no other behavior change, check-core green, rebuild + world-creation smoke re-run.
- On restart: row B3 exercises the LIVE cloud path — key-never-in-logs verified by pattern grep (Authorization/Bearer/api_key/sk-) without ever reading the key value, plus the structural guarantees (header-only, no logging call sites).

### Pre-flight vs Zen + operator free-model override (~15:05)

- PRE-FLIGHT RESULTS (Lead, key-blind): paid models unavailable on the operator's keys (403 model-access / 402 funds); free tier app-gated (longcat/nemotron 403 "only from within OpenCode" — NOT usable, no spoofing); space-bunny-free 200 OK. Measured with ProviderWriter's exact prompt: max_tokens=150 → EMPTY visible content 3/3 (finish=length — hidden reasoning eats the budget → universal silence); 600 → clean grounded JSON (13s, sample references looking_at=oak_log, lowercase, ≤12 words); 1200 → regresses to empty (reasoning inflation).
- OPERATOR OVERRIDE (on record): the operator explicitly consented to a free model ("a free model is fine too") — Lead's free-model exclusion ruling is lifted BY OPERATOR OVERRIDE. space-bunny-free is documented zero-retention/no-training by its provider (Zen docs — corroborated by Reviewer's earlier docs search: "Space Bunny Free is a stealth model... zero-retention policy, does not use your data for model training") — the best of the available free set. Mic stays OFF.
- CONFIG (verified key-blind): ai.provider=openai-compatible, ai.endpoint=https://opencode.ai/zen, ai.model=space-bunny-free, ai.api_key PRESENT (operator's key in place — value never read/logged), microphone.enabled=false.
- SCOPED FREEZE EXCEPTION (Lead-authorized, Runtime tasked): OpenAICompatibleProvider.java — max_tokens 150→600, request timeout 15s→30s, ZERO other behavior change. Reviewer bar: diff-scope check (exactly two literals), compile green, rebuild + Fabric world-creation smoke re-run, this log's record. B3 live on restart with the leak-grep set (Authorization/Bearer/api_key/sk- AND oc_sk_ for this key family) against full session logs. EXPECTED (not bugs): ~12-15s per decision call (off-thread, rare) and occasional empty→silence fail-closed results (on-brand for the presence).

### Pending collection

- Per-row results (A1–A8, B1–B4, C1–C9) with timestamps: ☐
- latest.log (full) + config/nightwatch.properties (key redacted after B3): ☐
- E1 audibility verdict (clear / faint / inaudible @ 0.25 vol, 30 blocks behind) — decides the gain/position reconciliation: ☐
- Verbatim `<...>` chat lines received: ☐

- Per-row results (A1–A8, B1–B4, C1–C9) with timestamps: ☐
- latest.log (full) + config/nightwatch.properties (key redacted after B3): ☐
- E1 audibility verdict (clear / faint / inaudible @ 0.25 vol, 30 blocks behind) — decides the gain/position reconciliation: ☐
- Verbatim `<...>` chat lines received: ☐

## FAIL triage (if any)

Any FAIL → triage note to Lead with owner routing; no agent edits during the window.
