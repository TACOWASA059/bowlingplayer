package com.github.tacowasa059.bowlingplayer.mixin;

import com.github.tacowasa059.bowlingplayer.player.BowlingBallMotion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Ball movement on Fabric. Injects in front of the two {@code setDeltaMovement(DDD)} calls that close out
 * {@code LivingEntity#travel} and captures vanilla's locals; the gravity to apply is the {@code d0} local, which
 * vanilla already lowered to 0.01 when slow falling is active.
 *
 * <p>Forge has its own copy of this because it patches an {@code AttributeInstance gravity} local into the same method,
 * which shifts the captured signature. Keep the two in step.
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
    private void bowlingPlayer$injectCustomMotion(Vec3 travelVector, CallbackInfo ci, double gravity, boolean falling,
                                                 FluidState fluidState, BlockPos blockPos, float blockFriction,
                                                 float friction, Vec3 movement, double motionY) {
        bowlingPlayer$applyBallMotion(ci, movement, gravity);
    }

    @Inject(
            method = "travel",
            at = @At(value = "INVOKE", target = SET_DELTA_MOVEMENT, ordinal = 3),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    private void bowlingPlayer$injectCustomMotion2(Vec3 travelVector, CallbackInfo ci, double gravity, boolean falling,
                                                  FluidState fluidState, BlockPos blockPos, float blockFriction,
                                                  float friction, Vec3 movement, double motionY) {
        bowlingPlayer$applyBallMotion(ci, movement, gravity);
    }

    @Unique
    private void bowlingPlayer$applyBallMotion(CallbackInfo ci, Vec3 movement, double gravity) {
        if (BowlingBallMotion.apply((LivingEntity) (Object) this, movement, gravity)) {
            ci.cancel();
        }
    }
}
