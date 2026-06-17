package com.github.tacowasa059.bowlingplayergame.fabric;

import com.github.tacowasa059.bowlingplayergame.BowlingPlayerGameCommon;
import net.fabricmc.api.ModInitializer;

public final class BowlingPlayerGameFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BowlingPlayerGameCommon.init();
    }
}
