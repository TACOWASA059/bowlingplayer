package com.github.tacowasa059.bowlingplayer;

public final class BowlingPlayerCommon {
    private static boolean initialized;

    private BowlingPlayerCommon() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        BowlingPlayerConstants.LOG.info("Initializing {}", BowlingPlayerConstants.MOD_NAME);
    }
}
