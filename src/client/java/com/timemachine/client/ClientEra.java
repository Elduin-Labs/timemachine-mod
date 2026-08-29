package com.timemachine.client;

import com.timemachine.Era;
import com.timemachine.Timeline;

/**
 * The client's copy of the dial, plus the flash that plays when it moves.
 *
 * <p>{@link Era} is deliberately dumb — a single integer that mixins on both sides can read
 * without knowing which side they are on. Everything that is only interesting to a player looking
 * at a screen lives here.
 */
public final class ClientEra {

    /** How long the screen tints over after a jump, in milliseconds. */
    private static final long FLASH_MS = 900L;

    private static long flashStart;
    private static boolean flashBackwards;

    private ClientEra() {
    }

    /** A fresh era from the server. */
    public static void accept(int index) {
        int before = Era.index();
        Era.set(index);
        if (Era.index() != before) {
            flashStart = System.currentTimeMillis();
            flashBackwards = Era.index() < before;
        }
    }

    public static void reset() {
        Era.set(Timeline.PRESENT);
        flashStart = 0L;
    }

    /** @return 0 when no jump is playing, otherwise how far through the flash we are, 1 down to 0. */
    public static float flash() {
        if (flashStart == 0L) {
            return 0.0F;
        }
        long elapsed = System.currentTimeMillis() - flashStart;
        if (elapsed >= FLASH_MS) {
            flashStart = 0L;
            return 0.0F;
        }
        return 1.0F - (float) elapsed / FLASH_MS;
    }

    /** Gold going back, cyan going forward — the same colours the title uses. */
    public static int flashColour() {
        return flashBackwards ? 0xFFC24A : 0x4ADFFF;
    }
}
