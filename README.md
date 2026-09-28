# Nightwatch

An AI horror mod prototype for Minecraft Java 26.3 / Fabric. It remembers the last few chat and voice lines, watches the nearby scene, and sometimes sends a short, unsettling response. Silence is intentional.

## What runs today

- Singleplayer, client-side chat presence. The mod only speaks locally in your chat; it doesn't impersonate another player on the server.
- Observations: biome, dimension, light, whether you're underground/moving, and the block under your crosshair. It only references observations it actually has.
- Sparse chat responses, a 150-second cooldown, randomized silence, brief session memory, and occasional unsolicited messages.
- Local Ollama for AI text; a deterministic fallback works when Ollama is stopped.
- Optional microphone input: three-second clips go to a local faster-whisper process, then only the transcript enters the director. Raw audio is neither logged nor saved.

This is the first playable slice. There is no monster model, chase, world manipulation, multiplayer sync, persistent world memory, or fake typing animation yet. The OpenComms tasks below add these in stages.

## Windows setup

1. Install JDK 25 and confirm `java -version` says 25. Use a dedicated Minecraft 26.3 Fabric profile. Fabric API `0.161.0+26.3` is required.
2. In PowerShell, from this folder, run `powershell -ExecutionPolicy Bypass -File .\tools\bootstrap-windows.ps1`. This copies the Gradle wrapper from Fabric's official 26.3 template. Then run `.\gradlew.bat build`.
3. Put `build\libs\nightwatch-0.1.0.jar` in the profile's `mods` folder, alongside Fabric API. Open a singleplayer world. The first launch creates `config\nightwatch.properties` in your Minecraft profile.
4. For AI responses, run `ollama pull qwen3:4b` and keep Ollama running. If you prefer another installed model, edit `ai.model` in the config and restart Minecraft. Without Ollama, the mod uses its short offline responses.

For mic awareness, run `python -m venv .venv` in this folder, activate `.venv\Scripts\Activate.ps1`, then `pip install -r voice\requirements.txt` and `python voice\sidecar.py`. The first start downloads a local speech model. Set `microphone.enabled=true` in the Minecraft profile's `config\nightwatch.properties` and restart the game. Disable it again to stop microphone capture on the next launch. The transcription service binds to `127.0.0.1:8765` only. The Ollama endpoint is fixed to `127.0.0.1:11434`.

## Working with OpenComms

Open this folder in OpenCode. In your existing OpenComms setup, start a room for `nightwatch`; attach one OpenCode session per role from [OPENCOMMS.md](OPENCOMMS.md). Give the lead the full goal and the other sessions their specific task sections. The project has its own `claudeplan.md` and `handoff.md` for shared state. Check `opencomms --help` against the version installed on your PC if your GUI doesn't expose room setup.

OpenCode Go Plus is for the coding sessions. It is not required for people running the mod: runtime text generation uses Ollama locally.

## Current test limits

`bash tools/check-core.sh` tests the director policy with the local JDK. A full Fabric build needs Java 25, Gradle, Minecraft/Fabric dependencies and network access; this package was made in a workspace without those, so the mod jar has not been built or launched here. Treat build and in-game behavior as the first OpenComms review gate.

Only install client-side in a singleplayer world at this stage. The message timing deliberately makes it possible to play for several minutes without a response.
