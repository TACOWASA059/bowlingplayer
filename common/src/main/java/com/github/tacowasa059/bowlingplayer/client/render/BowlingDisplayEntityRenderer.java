package com.github.tacowasa059.bowlingplayer.client.render;

import com.github.tacowasa059.bowlingplayer.entity.BowlingDisplayEntity;
import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class BowlingDisplayEntityRenderer extends EntityRenderer<BowlingDisplayEntity> {
    public BowlingDisplayEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BowlingDisplayEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ResourceLocation texture = getTextureLocation(entity);
        poseStack.pushPose();
        if (entity.getDisplayMode() == BowlingPlayerMode.BALL) {
            float radius = BowlingDisplayEntity.DEFAULT_BALL_DIAMETER * entity.getDisplayScale() * 0.5F;
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entityYaw));
            poseStack.translate(0.0F, radius, 0.0F);
            BowlingBallRenderer.render(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, texture, radius);
        } else {
            float scale = entity.getDisplayScale();
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entityYaw));
            poseStack.scale(scale, scale, scale);
            BowlingPinRenderer.render(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, 0.0F, 0.0F, texture);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BowlingDisplayEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            return minecraft.player.getSkinTextureLocation();
        }
        return DefaultPlayerSkin.getDefaultSkin(entity.getUUID());
    }
}
