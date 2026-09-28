# Loom entry research — client-env mod, custom dimension, 26.3 singleplayer (M3 prep)

Owner: Loom. Status: research + proposal DOWNLOADED/IMPLEMENTED (commit cb25261); Stage-2 code lives in `loom/net/`, `loom/driver/`, `loom/`. Canon note: the dimension is the Unraveller's workspace (see dimension-sequence.md canon section) — entry design unchanged: door → thread → stages → exit frame. No full-body reveal is ever staged at or after entry. No fabric.mod.json change requested.

Verified against the locally-cached artifacts (`%USERPROFILE%\.gradle\caches\fabric-loom\26.3\m*-*.jar`, `fabric-dimensions-v1-5.1.19`, `fabric-networking-api-v1-6.3.8`) — all names below were decompiled via javap at 2026-09-29.

## What the 26.3 pipeline actually frees us from

1. **Dimension registration is data-driven in vanilla 26.3.** `data/nightwatch/dimension_type/loom.json` (`net.minecraft.world.level.dimension.DimensionType.DIRECT_CODEC`, verified) plus `data/nightwatch/dimension/loom.json` (a LevelStem: dimension_type reference + generator) is *all* registration is. No registry code, no API call to create the dimension. Known Fabric behavior (multi-version wiki + issue history): a mod's `data/` is applied as a world data pack by resource-loader, and the dimension becomes available to the world — for existing worlds it is re-added on load as long as the mod stays installed (three historical bugs around first-load.png/#2345 history confirm the pipeline, they pair it with per-world retry).
2. **`fabric-dimensions-v1` (5.1.19) no longer has the old `FabricDimensions.teleport` util.** Its only public API is `DimensionEvents.MODIFY_ATTRIBUTES` (EnvironmentAttributeMap tweaks). Teleportation is now plain server-side vanilla: `net.minecraft.server.level.ServerPlayer.teleportTo(ServerLevel, double, double, double, Set<Relative>, float, float, boolean)` (verified signature). No portal/PortalForcer plumbing required for a direct relocation — better survivability invariants: teleport is atomic, no portal search can fail doorless.
3. **`ServerPlayNetworking` (6.3.8, `api/networking/v1/ServerPlayNetworking.class`, PlayPayloadHandler, verified present)** is how the client asks the integrated server to move the player. Receiver registered once on the server side; on a dedicated server the mod is client-env anyway — never loaded —, so the slice stays strictly singleplayer as the open plan says.
4. **`environment: "client"` is a dedicated-server gate, not an integrated-server gate** (one FabricLoader per JVM in singleplayer). So main-source/common code (this file lives on the server side of integrated server in the client JVM) is NOT blocked by our current `client` value. Nothing needs to change in fabric.mod.json for the proposal below. **A fallback fallback only if the in-game test shows mod data packs excluded for client-env mods: switch to `"*"` (Lead approval required, or ship an importable world flavor pack — softer path, no environment change).**

## Level stem for the Loom (no worldgen code): 

The vertical slice does NOT need block-by-block islands yet — those are islands of fracture set *by worldgen later* (M3 track). For slice, make it a **void dimension** (`dimension_type.loom.json`: `min_y": -64, "height": 384, ambient_light": 0.0, skybox": "impossible-world", fog": ..., monster settings: no spawning) referencing a `flat` generator via direct JSON (`minecraft:flat` settings baked in the LevelStem). Restrictions in flavor: no `bed_works` (two-way locked), `coordinate_scale" 1 to not break the "knock-back" mapping (though the door knock mapping is a server-side relocate anyway, nothing else is sensitive to the coordinate scale). Player-safe defaults: no fixed time (the Loom doc sets `time_time": "midnight"? — do NOT fix time, keep daylight cycle off so threads-of-dark stays dark — ambient_light 0 keeps it dark, and no mob spawning via monster settings).

## The minimal viable entry hook (proposal, ~2 files + 2 JSONs + 1 payload)

- `src/main/resources/data/nightwatch/dimension_type/loom.json` + `data/nightwatch/dimension/loom.json` + `data/nightwatch/worldgen/noise_settings/...` **nothing** — flat/void generator inline in LevelStem; keep no `worldgen/` folder at all so future NBT-free expansions stay out of scope.
- `.../client/loom/LoomDoorService.java` (client-side, runs in the SAME JVM) — decides the entry/exit moment (already in `LoomSequence` state machine), and sends two payload types; the Loom's "caught" stage (CAUGHT_RELOCATE) is just exit-then-reenter, so relocation needs no third payload (way back with no server round-trip needed by design).
- `.../main/loom/LoomTeleportHandler.java` (Common — runs on the integrated server in singleplayer) — `ServerPlayNetworking.registerGlobalReceiver(LOOM_EXIT, (server, player, handler, buf, ctx) -> ...)`: looks up the anchor (`stored entry BlockPos` per-player UUID in a `Map<UUID, BlockPos>` static — non-persistent across sessions, acceptable: reloading mid-Loom drops player at their overworld last position which the world save records anyway… **verification flag below**). It does: `player.teleportTo(server.getLevel(NIGHTWATCH_LOOM_KEY), doorX, doorY, doorZ, Set.of(), 0f, 0f, false)` — verify boolean arg via javap args as above; wraps in the survivability contract: **never a command, never blocks, never spawns mobs, validated coordinates only**.
- `fabric.mod.json` stays untouched. `"environment": "client"` unchanged. All additions are the two new classes + JSONs.

## Survivability invariants extend unchanged

`LoomSequenceCheck` needs two new assertions once teleport lands (client-side only, since easy to prove): teleports never resolve to a damage event, and the caught-relocation is exactly the stored anchor + knock-back sound, nothing else.

## Open items per Lead

1. **Confirm `environment:"client"` mod data packs apply on the integrated server in 26.3** — needs an in-game dev-run test (Fabric owns building; I own writing the JSON fixtures for a test). Singleplayer dev run is the only ground truth; wiki history says yes but there were regressions in past versions.
2. `ServerPlayer.teleportTo(...)` was verified public via javap (seven-arg) — Fabric/Jar verifier will catch any 26.3 drift; my proposal is mappable-ready as written.
3. Per-player anchor persistence across mid-session disconnects is an assumption, not a verified contract — needs the same in-game check.
