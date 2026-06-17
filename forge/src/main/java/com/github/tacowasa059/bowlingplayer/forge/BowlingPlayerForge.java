package com.github.tacowasa059.bowlingplayer.forge;

import com.github.tacowasa059.bowlingplayer.BowlingPlayerCommon;
import com.github.tacowasa059.bowlingplayer.BowlingPlayerConstants;
import com.github.tacowasa059.bowlingplayer.command.BowlingPlayerCommands;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BowlingPlayerConstants.MOD_ID)
public final class BowlingPlayerForge {
    public BowlingPlayerForge() {
        BowlingPlayerCommon.init();
        BowlingPlayerForgeEntities.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(BowlingPlayerForgeEvents.class);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        BowlingPlayerCommands.register(event.getDispatcher());
    }
}
