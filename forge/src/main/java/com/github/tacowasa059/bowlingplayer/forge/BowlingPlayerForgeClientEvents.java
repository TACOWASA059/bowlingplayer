package com.github.tacowasa059.bowlingplayer.forge;

import com.github.tacowasa059.bowlingplayer.BowlingPlayerConstants;
import com.github.tacowasa059.bowlingplayer.client.render.BowlingDisplayEntityRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BowlingPlayerConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BowlingPlayerForgeClientEvents {
    private BowlingPlayerForgeClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BowlingPlayerForgeEntities.BOWLING_DISPLAY.get(), BowlingDisplayEntityRenderer::new);
    }
}
