package com.github.tacowasa059.bowlingplayer.forge;

import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerStateAccess;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BowlingPlayerForgeEvents {
    private BowlingPlayerForgeEvents() {
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        ((BowlingPlayerStateAccess) event.getEntity()).bowlingPlayer$copyStateFrom((BowlingPlayerStateAccess) event.getOriginal());
    }
}
