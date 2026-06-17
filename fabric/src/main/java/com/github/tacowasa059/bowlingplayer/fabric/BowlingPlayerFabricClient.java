package com.github.tacowasa059.bowlingplayer.fabric;

import com.github.tacowasa059.bowlingplayer.client.render.BowlingDisplayEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class BowlingPlayerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(BowlingPlayerFabricEntities.BOWLING_DISPLAY, BowlingDisplayEntityRenderer::new);
    }
}
