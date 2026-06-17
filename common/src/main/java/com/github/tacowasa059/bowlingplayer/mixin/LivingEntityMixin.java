package com.github.tacowasa059.bowlingplayer.mixin;

import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerMode;
import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerStateAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    protected LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(
            method = "travel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V", ordinal = 2),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            cancellable = true
    )
    private void bowlingPlayer$injectCustomMotion(Vec3 travelVector, CallbackInfo ci, double d0, AttributeInstance gravity, boolean flag,
                                                  FluidState fluidState, BlockPos blockPos, float f2, float f3, Vec3 vec35, double d2) {
        bowlingPlayer$setDeltaMovement(ci, vec35, gravity == null ? 0.08D : gravity.getValue());
    }

    @Inject(
            method = "travel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V", ordinal = 3),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            cancellable = true
    )
    private void bowlingPlayer$injectCustomMotion2(Vec3 travelVector, CallbackInfo ci, double d0, AttributeInstance gravity, boolean flag,
                                                   FluidState fluidState, BlockPos blockPos, float f2, float f3, Vec3 vec35, double d2) {
        bowlingPlayer$setDeltaMovement(ci, vec35, gravity == null ? 0.08D : gravity.getValue());
    }

    @Unique
    private void bowlingPlayer$setDeltaMovement(CallbackInfo ci, Vec3 vec35, double gravityValue) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player player)) {
            return;
        }
        BowlingPlayerStateAccess state = (BowlingPlayerStateAccess) player;
        if (state.bowlingPlayer$getMode() != BowlingPlayerMode.BALL) {
            return;
        }

        double motionY = vec35.y;
        if (entity.hasEffect(MobEffects.LEVITATION)) {
            MobEffectInstance effect = entity.getEffect(MobEffects.LEVITATION);
            if (effect != null) {
                motionY += (0.05D * (effect.getAmplifier() + 1) - vec35.y) * 0.2D;
            }
        } else if (!entity.isNoGravity()) {
            motionY -= gravityValue;
        }

        double motionX = vec35.x * 0.95F;
        double motionZ = vec35.z * 0.95F;
        double sqr = motionX * motionX + motionZ * motionZ;
        double maxHorizontalSpeed = 0.8D * Math.max(0.0F, state.bowlingPlayer$getBallSpeedMultiplier());
        if (maxHorizontalSpeed <= 0.0D) {
            motionX = 0.0D;
            motionZ = 0.0D;
        } else if (sqr >= maxHorizontalSpeed * maxHorizontalSpeed) {
            double scale = maxHorizontalSpeed / Math.sqrt(sqr);
            motionX *= scale;
            motionZ *= scale;
        }

        Vec3 newVector = new Vec3(motionX, motionY * 0.9800000190734863D, motionZ);
        entity.setDeltaMovement(newVector);
        ci.cancel();
    }

    @Inject(method = "getFrictionInfluencedSpeed", at = @At("HEAD"), cancellable = true)
    private void bowlingPlayer$modifyFrictionInfluencedSpeed(float friction, CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player player)) {
            return;
        }
        if (((BowlingPlayerStateAccess) player).bowlingPlayer$getMode() != BowlingPlayerMode.BALL) {
            return;
        }

        if (!entity.onGround()) {
            cir.setReturnValue(entity.getSpeed() * (0.21600002F / (friction * friction * friction)));
        }
    }

    @Inject(method = "getJumpPower", at = @At("HEAD"), cancellable = true)
    private void bowlingPlayer$modifyJumpPower(CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player player)) {
            return;
        }
        if (((BowlingPlayerStateAccess) player).bowlingPlayer$getMode() != BowlingPlayerMode.BALL) {
            return;
        }

        float jumpPower = 0.42F * bowlingPlayer$getJumpFactor(entity);
        cir.setReturnValue(jumpPower * 1.75F);
        cir.cancel();
    }

    @Unique
    private float bowlingPlayer$getJumpFactor(LivingEntity entity) {
        float currentBlockFactor = entity.level().getBlockState(entity.blockPosition()).getBlock().getJumpFactor();
        BlockPos onPos = this.getOnPos(0.500001F);
        float belowFactor = entity.level().getBlockState(onPos).getBlock().getJumpFactor();
        return currentBlockFactor == 1.0F ? belowFactor : currentBlockFactor;
    }
}
