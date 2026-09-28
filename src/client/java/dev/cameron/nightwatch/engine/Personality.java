package dev.cameron.nightwatch.engine;

/**
 * Per-world personality profile that influences the Director's tone and response patterns.
 * Personality is deterministic (no model needed) but can be overridden by the model when available.
 */
public record Personality(String tone, Verbosity verbosity, ObservationFocus focus) {
    public enum Verbosity { SPARSE, MODERATE, CHATTY }
    public enum ObservationFocus { ENVIRONMENT, PLAYER, BOTH }

    public static Personality forWorld(String dimension) {
        return switch (dimension) {
            case "overworld" -> new Personality("eerie", Verbosity.SPARSE, ObservationFocus.BOTH);
            case "the_nether" -> new Personality("oppressive", Verbosity.SPARSE, ObservationFocus.ENVIRONMENT);
            case "the_end" -> new Personality("distant", Verbosity.SPARSE, ObservationFocus.PLAYER);
            default -> new Personality("eerie", Verbosity.SPARSE, ObservationFocus.BOTH);
        };
    }

    /**
     * Pacing Constraint (binding):
     * Verbosity affects word count per message, NOT message frequency.
     * Director's 150s cooldown remains the hard cap.
     * - SPARSE: 1-6 words per message
     * - MODERATE: 7-9 words per message
     * - CHATTY: 10-12 words per message (capped at 12 to stay inside Action.message 90-char limit)
     */
    public int maxWords() {
        return switch (verbosity) {
            case SPARSE -> 6;
            case MODERATE -> 9;
            case CHATTY -> 12;
        };
    }
}
