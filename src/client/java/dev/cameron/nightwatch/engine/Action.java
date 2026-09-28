package dev.cameron.nightwatch.engine;

/**
 * Defines all actions the Director can emit. The AI receives limited truthful
 * observations and can choose only these validated, predefined actions.
 *
 * <p>Action kinds:
 * <ul>
 *   <li>{@link Kind#SILENCE} - No action. The Director often chooses this.</li>
 *   <li>{@link Kind#MESSAGE} - Chat message with delay. Validated for length and content.</li>
 *   <li>{@link Kind#SOUND} - Play a client-side sound effect (e.g., distant footstep, door knock).</li>
 *   <li>{@link Kind#EFFECT} - Trigger a visual or ambient effect (e.g., brief sighting, particle burst).</li>
 * </ul>
 *
 * <p>All actions are client-side only. No world manipulation, no server commands,
 * no arbitrary code execution. The Director's pacing and cooldown logic applies
 * uniformly across all action kinds.
 *
 * <p>Fabric implements the actual sound/effect playback in {@code .../effects/} classes.
 * The engine only emits validated action descriptors.
 */
public record Action(Kind kind, String message, String soundId, String effectType, String argument, int delaySeconds) {
    public enum Kind { SILENCE, MESSAGE, SOUND, EFFECT }

    public Action {
        if (kind == null) kind = Kind.SILENCE;
        if (message == null) message = "";
        if (soundId == null) soundId = "";
        if (effectType == null) effectType = "";
        if (argument == null) argument = "";
        if (kind == Kind.MESSAGE) {
            if (message.isBlank() || message.length() > 90 || message.contains("\n")
                || message.contains("\r") || message.contains("§")) {
                kind = Kind.SILENCE; message = ""; soundId = ""; effectType = ""; argument = ""; delaySeconds = 0;
            }
            if (delaySeconds < 2 || delaySeconds > 30) {
                kind = Kind.SILENCE; message = ""; soundId = ""; effectType = ""; argument = ""; delaySeconds = 0;
            }
        } else if (kind == Kind.SOUND) {
            if (soundId.isBlank() || soundId.length() > 60 || !isAllowlistedSound(soundId)) {
                kind = Kind.SILENCE; message = ""; soundId = ""; effectType = ""; argument = ""; delaySeconds = 0;
            }
            if (delaySeconds < 0 || delaySeconds > 30) {
                kind = Kind.SILENCE; message = ""; soundId = ""; effectType = ""; argument = ""; delaySeconds = 0;
            }
        } else if (kind == Kind.EFFECT) {
            if (effectType.isBlank() || effectType.length() > 40 || !isAllowlistedEffect(effectType)) {
                kind = Kind.SILENCE; message = ""; soundId = ""; effectType = ""; argument = ""; delaySeconds = 0;
            }
            if (delaySeconds < 0 || delaySeconds > 30) {
                kind = Kind.SILENCE; message = ""; soundId = ""; effectType = ""; argument = ""; delaySeconds = 0;
            }
            if (argument.length() > 80) argument = argument.substring(0, 80);
            // Per-effect argument validation: fail-closed to empty string
            if (kind == Kind.EFFECT) {
                if (effectType.equals("echo_spawn") || effectType.equals("echo_dissolve")) {
                    if (!argument.isEmpty() && !isValidEchoStage(argument)) argument = "";
                } else {
                    argument = "";
                }
            }
        } else {
            message = ""; soundId = ""; effectType = ""; argument = ""; delaySeconds = 0;
        }
    }

    public static Action silence() {
        return new Action(Kind.SILENCE, "", "", "", "", 0);
    }

    public static Action message(String text, int delaySeconds) {
        if (text == null || text.isBlank() || text.length() > 90 || text.contains("\n")
            || text.contains("\r") || text.contains("§")) return silence();
        if (delaySeconds < 2 || delaySeconds > 30) return silence();
        return new Action(Kind.MESSAGE, text.strip(), "", "", "", delaySeconds);
    }

    public static Action sound(String soundId, int delaySeconds) {
        if (soundId == null || soundId.isBlank() || soundId.length() > 60) return silence();
        if (delaySeconds < 0 || delaySeconds > 30) return silence();
        if (!isAllowlistedSound(soundId)) return silence();
        return new Action(Kind.SOUND, "", soundId.strip(), "", "", delaySeconds);
    }

    public static Action effect(String effectType, int delaySeconds) {
        return effect(effectType, "", delaySeconds);
    }

    public static Action effect(String effectType, String argument, int delaySeconds) {
        if (effectType == null || effectType.isBlank() || effectType.length() > 40) return silence();
        if (delaySeconds < 0 || delaySeconds > 30) return silence();
        if (!isAllowlistedEffect(effectType)) return silence();
        String arg = (argument == null) ? "" : argument.strip();
        if (arg.length() > 80) arg = arg.substring(0, 80);
        // Per-effect argument validation: fail-closed to empty string
        if (effectType.equals("echo_spawn") || effectType.equals("echo_dissolve")) {
            if (!arg.isEmpty() && !isValidEchoStage(arg)) arg = "";
        } else {
            // Other effects don't take arguments
            arg = "";
        }
        return new Action(Kind.EFFECT, "", "", effectType.strip(), arg, delaySeconds);
    }

    private static boolean isValidEchoStage(String stage) {
        return stage.equals("copycat") || stage.equals("diverger") || stage.equals("facing") || stage.equals("facing-closer");
    }

    private static boolean isAllowlistedSound(String id) {
        return id.equals("footstep_distant")
            || id.equals("door_knock")
            || id.equals("whisper")
            || id.equals("lantern_dip")
            || id.equals("door_open")
            || id.equals("door_close");
    }

    private static boolean isAllowlistedEffect(String type) {
        return type.equals("brief_sighting")
            || type.equals("shadow_flicker")
            || type.equals("particle_burst")
            || type.equals("pale_thread")
            || type.equals("island_morph")
            || type.equals("echo_spawn")
            || type.equals("echo_dissolve")
            || type.equals("door_appear");
    }
}
