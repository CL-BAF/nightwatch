package dev.cameron.nightwatch.engine;

public record Scene(String player, String biome, String dimension, String lookingAt,
                    boolean underground, boolean moving, int light, String recentEvent) {
    public String summary() {
        return "player=" + player + "; biome=" + biome + "; dimension=" + dimension
            + "; looking_at=" + lookingAt + "; underground=" + underground
            + "; moving=" + moving + "; light=" + light + "; recent=" + recentEvent;
    }
}
