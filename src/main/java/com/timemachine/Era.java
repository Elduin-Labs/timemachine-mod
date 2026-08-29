package com.timemachine;

/**
 * What year it is, as far as the game is concerned.
 *
 * <p>The authoritative copy lives in {@link TimeState} on disk, but the mechanic mixins that ask
 * "does sprinting exist yet?" run on both sides — sprinting is set client-side, the attack
 * cooldown bar is drawn client-side — and a mixin cannot reach a {@code ServerWorld}. So the
 * current index is mirrored into this static: the server sets it when the world loads or the dial
 * turns, and pushes it to every client, which sets it on receipt. In single-player both halves
 * live in one JVM and write the same value.
 *
 * <p>{@code volatile} because the server thread writes it and the render thread reads it.
 */
public final class Era {

    private static volatile int index = Timeline.PRESENT;

    private Era() {
    }

    public static int index() {
        return index;
    }

    public static Timeline.Version current() {
        return Timeline.get(index);
    }

    public static void set(int newIndex) {
        index = Timeline.clamp(newIndex);
    }

    /** True if {@code feature} had been invented by the version the dial is set to. */
    public static boolean has(Feature feature) {
        return index >= feature.since();
    }

    /** True if the world is sitting in the present, where the mod does nothing at all. */
    public static boolean isPresent() {
        return index >= Timeline.PRESENT;
    }
}
