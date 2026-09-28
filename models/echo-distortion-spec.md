# Nightwatch — Echo Distortion Spec (Models v1, canon: THE UNRAVELLER)

Owner: Models. Implements: echo-design §E4 + binding E4 rules (own distorted skin, zero multiplayer metadata, first-glance distortion). Consumed by: Fabric (render-layer on the player model). Stage ids arrive via `Action.argument` (`echo_spawn`/`echo_dissolve`); argument is LoomSequence-controlled, never model-controlled.

## Canon frame
Echoes are versions of the player the Unraveller tried to pull apart. Distortion reads as being UNPICKED — seams parting along the distortion axis, thread showing at the seams — while always recognisably the player. Never speaks as the player (existing binding rule).

## Distortion grades (ONE asset, distortion grade is the only axis — no variants)

| stage id | limb scale | head | tint (desaturation grade on own skin) | gait/response | seams |
|---|---|---|---|---|---|
| `copycat` | arms 1.1x | tilt 8° | 10% desat | mirrors last movements, 0.25s delay | none visible; reads almost-you |
| `diverger` | arms 1.3x | tilt 15° | 35% desat | 0.5s delay, takes paths the player cannot | hairline seams at elbows/knees, no particles yet |
| `facing` | arms 1.3x + hands 1.2x | tilt 15° + 3° cant | 60% desat | stands still facing player; copies if player moves first | open seams w/ #dddde6 thread glints at joints |
| `facing-closer` | as facing + torso 1.05x stretch | as facing | 75% desat | one block closer than facing; holds | seams widest; dissolve imminent |

- First-glance rule (binding): at every grade the difference from "a real player" must land instantly — scale + tilt + desat combine so no grade ever passes as unmodified.
- Tint is a desaturation grade on the player's own skin texture (shader/uniform lerp toward grey), NEVER a flat-grey replacement skin. No separate grey variant exists.
- Unpicking, not wounding: seams are clean partings with thread, never blood, tears, or damage decals.

## Dissolve end-state (all stages)
- `echo_dissolve`: figure comes apart into #dddde6 thread particles (shared thread spec: emissive 0.3, opacity 0.7), sound cuts mid-stride, hard light-out. Illusion only — no player HP event, ever.
- Particle budget: ≤12 thread particles per dissolve (echo cap, above ambient ≤6 — Reviewer to confirm the separate budget).

## Fabric interface needs
- Render-layer hook on the player model applying per-grade scale/tilt/desat from the stage id; seam glints as 1px thread quads at elbow/knee joints for facing grades.
- `setSeen`-style freeze not required for echoes (they dissolve, never freeze).
- Texture sizes: player's own skin (no new texture); no layer-count change beyond one overlay pass.
