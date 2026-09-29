# Nightwatch — operator session log (Reviewer, evidence hub)

Session window opened: 2026-09-29, content-complete called at HEAD e39921e (Lead). Code freeze in effect for all roles until session verdicts land. Evidence flows: operator → Reviewer (this log) → Lead routes fixes to owners. Nobody marks a script row PASS except the operator.

## Pre-session confirmations (Reviewer, on record)

1. NPE-ordering angle (Lead's explicit confirm): VERIFIED — TypingIndicator.java:61-65: the null-safe liveness re-checks run first (player null → return at 61; level null → return at 62; singleplayerServer null → return at 63), and ONLY THEN the Loom check dereferences client.level.dimension() (65). A disconnect straddling the typing window returns at the null guards — no NPE path exists. (Same conclusion as my earlier guard inspection; recorded here per Lead's request.)
2. Authoritative artifact: reproducible from committed HEAD e39921e by THREE independent builds — Reviewer's own `gradlew clean build` (100,841 B @ 10:48:06), Fabric's (100,841 B @ 10:48:36), and Lead's at 4e01b3e (100,841 B @ 10:50:48; 4e01b3e verified docs-only — NO source changes between e39921e and 4e01b3e), all byte-identical in size from the same code state. Jar listing: 76 entries, fabric.mod.json, all slice classes (Stage-1, providers, entity/model, loom, effects), dimension JSONs, textures — no config/.properties/secret file inside. PRE-FLIGHT REBUILD VERIFICATION: the artifact in build\libs at session start (100,841 B @ 10:50:48) IS the authoritative reproducible artifact; if the operator re-runs pre-flight step 2 (`gradlew clean build`, JAVA_HOME = PrismLauncher JDK) the output must match 100,841 B — Reviewer checks whichever path is used. Gate re-run at HEAD by Reviewer: green (all three checks); no source changed since e39921e.
3. Standing gates at freeze: check-core three-check run green (DirectorCheck, LoomSequenceCheck, LoomCueMapperCheck — Reviewer's own run); engine/ dependency-clean PASS; Settings first-run store never writes ai.api_key; provider key config-only/header-only/never-logged.

## Session evidence (to be filled per the script's evidence section)

- Pre-flight rebuild verified (commit + jar size/time): ☐
- Per-row results (A1–A8, B1–B4, C1–C9) with timestamps: ☐
- latest.log (full) + config/nightwatch.properties (key redacted after B3): ☐
- E1 audibility verdict (clear / faint / inaudible @ 0.25 vol, 30 blocks behind) — decides the gain/position reconciliation: ☐
- Verbatim `<...>` chat lines received: ☐

## FAIL triage (if any)

Any FAIL → triage note to Lead with owner routing; no agent edits during the window.
