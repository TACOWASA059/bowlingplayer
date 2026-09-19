package com.github.tacowasa059.bowlingplayer.mixin;

import com.github.tacowasa059.bowlingplayer.player.BowlingBallMotion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Ball movement on Forge. Same injection points as the Fabric copy, but Forge patches an
 * {@code AttributeInstance gravity} local into {@code LivingEntity#travel}, so it appears in the captured signature and
 * supplies the gravity value (it is where Forge parks the slow falling modifier). Keep the two copies in step.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTravelMixin {
    private static final String SET_DELTA_MOVEMENT =
            "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V";

    @Inject(
            method = "travel",
            at = @At(value = "INVOKE", target = SET_DELTA_MOVEMENT, ordinal = 2),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    private void bowlingPlayer$injectCustomMotion(Vec3 travelVector, CallbackInfo ci, double fallbackGravity,
                                                 AttributeInstance gravity, boolean falling, FluidState fluidState,
                                                 BlockPos blockPos, float blockFriction, float friction,
                                                 Vec3 movement, double motionY) {
        bowlingPlayer$applyBallMotion(ci, movement, gravity, fallbackGravity);
    }

    @Inject(
            method = "travel",
            at = @At(value = "INVOKE", target = SET_DELTA_MOVEMENT, ordinal = 3),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    private void bowlingPlayer$injectCustomMotion2(Vec3 travelVector, CallbackInfo ci, double fallbackGravity,
                                                  AttributeInstance gravity, boolean falling, FluidState fluidState,
                                                  BlockPos blockPos, float blockFriction, float friction,
                                                  Vec3 movement, double motionY) {
        bowlingPlayer$applyBallMotion(ci, movement, gravity, fallbackGravity);
    }

    @Unique
    private void bowlingPlayer$applyBallMotion(CallbackInfo ci, Vec3 movement, AttributeInstance gravity,
                                               double fallbackGravity) {
        double value = gravity == null ? fallbackGravity : gravity.getValue();
        if (BowlingBallMotion.apply((LivingEntity) (Object) this, movement, value)) {
            ci.cancel();
        }
    }
}
