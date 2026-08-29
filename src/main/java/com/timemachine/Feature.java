package com.timemachine;

/**
 * Rules of the game that arrived on a particular date, as opposed to content that arrived on a
 * particular date. Items and mobs date themselves through {@link ContentCalendar}; these are the
 * things with no item to take away — you cannot confiscate sprinting.
 *
 * <p>Each one is answered with {@link Era#has(Feature)}, which is a single integer comparison, so
 * it is safe to call from a mixin on a hot path.
 */
public enum Feature {

    /** Health, hunger-free survival and mobs that fight back (Survival Test). */
    SURVIVAL("c0.24_st"),

    /** Crafting tables, furnaces and tools (Indev). */
    CRAFTING("in-20100128"),

    /** The Nether (Halloween Update). */
    NETHER("a1.2.0"),

    /** Beds, and skipping the night. */
    BEDS("b1.3"),

    /** Rain, snow and thunderstorms. */
    WEATHER("b1.5"),

    /** The hunger bar. Before this, food went straight into your health. */
    HUNGER("b1.8.1"),

    /** Double-tap to run. */
    SPRINTING("b1.8.1"),

    /** Experience orbs and levels. */
    EXPERIENCE("b1.8.1"),

    /** The End, the dragon, and the credits. */
    THE_END("1.0.0"),

    /** Enchanting tables. */
    ENCHANTING("1.0.0"),

    /** Brewing stands and potions. */
    BREWING("1.0.0"),

    /** Villager trading. */
    TRADING("1.3.2"),

    /** The 1.9 attack cooldown. Off means every swing lands for full damage. */
    ATTACK_COOLDOWN("1.9.4"),

    /** The off-hand slot. */
    OFFHAND("1.9.4"),

    /** Gliding with elytra. */
    ELYTRA("1.9.4"),

    /** Swimming: the crawl pose and the speed that comes with it. */
    SWIMMING("1.13.2");

    private final String sinceId;
    private int sinceIndex = -1;

    Feature(String sinceId) {
        this.sinceId = sinceId;
    }

    /** Timeline index of the version that introduced this feature. */
    public int since() {
        if (sinceIndex < 0) {
            sinceIndex = Timeline.require(sinceId);
        }
        return sinceIndex;
    }

    public Timeline.Version sinceVersion() {
        return Timeline.get(since());
    }
}
