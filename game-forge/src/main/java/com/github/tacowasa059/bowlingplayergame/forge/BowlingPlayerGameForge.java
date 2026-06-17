package com.github.tacowasa059.bowlingplayergame.forge;

import com.github.tacowasa059.bowlingplayergame.BowlingPlayerGameCommon;
import com.github.tacowasa059.bowlingplayergame.BowlingPlayerGameConstants;
import net.minecraftforge.fml.common.Mod;

@Mod(BowlingPlayerGameConstants.MOD_ID)
public final class BowlingPlayerGameForge {
    public BowlingPlayerGameForge() {
        BowlingPlayerGameCommon.init();
    }
}
