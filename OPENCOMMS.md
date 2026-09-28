# OpenComms assignment: Nightwatch

Run independent OpenCode sessions in the same OpenComms room. The lead owns `claudeplan.md` and `handoff.md`; each builder reports exact files changed and a test result in the room. Check the room before editing shared files. No agent should overwrite another agent's uncommitted work.

## Lead / integrator — use your strongest Go Plus coding model

"You are integrating Nightwatch, a Fabric 26.3 AI horror mod. Read README.md, claudeplan.md and handoff.md. First get `./gradlew build` passing with Java 25 and launch a singleplayer dev client. Fix any API drift. Confirm typing `hello?` may get a delayed, scene-aware chat response, and `yo entity is it pink?` never becomes a prompt. Keep gameplay on the client/main thread and model requests off it. Assign the work below after the build gate; review each contribution and report a playable jar path and exact remaining limitations."

## Agent 1 / world events

"Implement a small allowlist of client-side horror actions: one distant footstep sound, one brief sighting, and a rare door knock. Trigger them through the director's paced choices. Do not alter terrain or destroy items. Own new classes under `.../effects/` plus the smallest possible integration hooks; coordinate modifications to NightwatchClient.java with the lead. Test effects in a real 26.3 singleplayer client, including with shaders."

## Agent 2 / memory and chat

"Improve Nightwatch's dialogue pacing: give each world a consistent personality and bounded, opt-in world memory, and implement a typing illusion using action-bar previews followed by a single final chat message. Do not flood chat with one line per letter. Preserve prompt-injection resistance and strictly validate model output. Keep transcripts free of private info, add delete/reset controls, and test message frequency over 20 minutes. Own `.../engine/` plus memory code; coordinate shared integration with lead."

## Agent 3 / microphone and quality review

"Review microphone capture and `voice/sidecar.py` on Windows. Verify explicit opt-in, no recording persistence, clean shutdown, STT recovery, no audio outside localhost, and gameplay performance on a laptop. Make a settings UI for enabling/disabling mic and for choosing input device; do not turn capture on silently. Test inaccurate speech, no mic device, and Ollama down. Report bugs and fix your owned voice/settings files."

## Release gates

1. Build with Java 25: `./gradlew build`.
2. Enter a fresh singleplayer world with and without Ollama; no freezes or crashes.
3. Verify chat silence, cooldown, contextual replies and actual observed facts.
4. Verify mic is off by default, optional sidecar is localhost-only, and capture stops at disconnect.
5. Review that the AI can select only hardcoded, validated actions; never execute a generated command or write arbitrary files.

Pause before adding multiplayer or cloud inference: those need separate server controls and player consent.
