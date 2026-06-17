package com.github.tacowasa059.bowlingplayer.mixin.client;

import com.github.tacowasa059.bowlingplayer.client.render.BowlingBallRenderer;
import com.github.tacowasa059.bowlingplayer.client.render.BowlingPinRenderer;
import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerMode;
import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerQuaternionUtils;
import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerStateAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Shadow
    public abstract ResourceLocation getTextureLocation(AbstractClientPlayer player);

    @Shadow
    public abstract Vec3 getRenderOffset(AbstractClientPlayer player, float partialTick);

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void bowlingPlayer$render(AbstractClientPlayer player, float entityYaw, float partialTick, PoseStack poseStack,
                                      MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        BowlingPlayerMode mode = ((BowlingPlayerStateAccess) player).bowlingPlayer$getMode();
        if (mode == BowlingPlayerMode.NORMAL) {
            return;
        }

        poseStack.pushPose();
        Vec3 renderOffset = this.getRenderOffset(player, partialTick);
        poseStack.translate(renderOffset.x, renderOffset.y, renderOffset.z);
        BowlingPlayerStateAccess state = (BowlingPlayerStateAccess) player;

        if (mode == BowlingPlayerMode.BALL) {
            float ballRadius = state.bowlingPlayer$getBallSize() * 0.5F;
            poseStack.translate(0.0F, ballRadius, 0.0F);
            bowlingPlayer$applyDeathRotation(player, partialTick, poseStack);
            if (player.getVehicle() != null) {
                poseStack.mulPose(BowlingPlayerQuaternionUtils.getQuaternionFromEntity(player.getVehicle()));
            } else {
                poseStack.mulPose(state.bowlingPlayer$getInterpolatedQuaternion(partialTick));
            }
            BowlingBallRenderer.render(poseStack, buffer, packedLight, LivingEntityRenderer.getOverlayCoords(player, 0.0F), this.getTextureLocation(player), ballRadius);
        } else {
            float pinScale = state.bowlingPlayer$getPinSize();
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entityYaw));
            poseStack.scale(pinScale, pinScale, pinScale);
            float walkSpeed = player.walkAnimation.speed(partialTick);
            float healthTilt = BowlingPinRenderer.computeHealthTilt(player.getHealth(), player.getMaxHealth());
            float tilt = player.deathTime > 0
                    ? BowlingPinRenderer.computeDeathTilt(player.deathTime, partialTick)
                    : healthTilt + BowlingPinRenderer.computeMoveTilt(walkSpeed, player.tickCount + partialTick);
            float yaw = player.deathTime > 0
                    ? 0.0F
                    : BowlingPinRenderer.computeMoveYaw(walkSpeed, player.tickCount + partialTick);
            BowlingPinRenderer.render(poseStack, buffer, packedLight, LivingEntityRenderer.getOverlayCoords(player, 0.0F), tilt, yaw, this.getTextureLocation(player));
        }

        poseStack.popPose();
        ci.cancel();
    }

    private static void bowlingPlayer$applyDeathRotation(AbstractClientPlayer player, float partialTick, PoseStack poseStack) {
        if (player.deathTime <= 0) {
            return;
        }

        float progress = ((float) player.deathTime + partialTick - 1.0F) / 20.0F * 1.6F;
        progress = Mth.sqrt(Math.min(progress, 1.0F));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(progress * 90.0F));
    }
}
