package com.github.tacowasa059.bowlingplayergame;

import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerMode;

public final class BowlingPlayerGameCommon {
    private static boolean initialized;

    private BowlingPlayerGameCommon() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        BowlingPlayerGameConstants.LOG.info("Initializing {} with base mode {}", BowlingPlayerGameConstants.MOD_NAME, BowlingPlayerMode.NORMAL.getSerializedName());
    }
}
