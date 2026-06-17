package com.github.tacowasa059.bowlingplayer.mixin.client;

import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerMode;
import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerStateAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void bowlingPlayer$bobView(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player != null && ((BowlingPlayerStateAccess) player).bowlingPlayer$getMode() == BowlingPlayerMode.BALL) {
            ci.cancel();
        }
    }
}
