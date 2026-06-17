package com.github.tacowasa059.bowlingplayer.fabric;

import com.github.tacowasa059.bowlingplayer.BowlingPlayerCommon;
import com.github.tacowasa059.bowlingplayer.command.BowlingPlayerCommands;
import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerStateAccess;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

public final class BowlingPlayerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BowlingPlayerCommon.init();
        BowlingPlayerFabricEntities.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> BowlingPlayerCommands.register(dispatcher));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
                ((BowlingPlayerStateAccess) newPlayer).bowlingPlayer$copyStateFrom((BowlingPlayerStateAccess) oldPlayer));
    }
}
