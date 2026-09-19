package com.github.tacowasa059.bowlingplayer.mixin;

import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerMode;
import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerStateAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ball handling that is identical on both loaders. The {@code travel} override itself lives in the per-loader mixin
 * configs, because Forge patches an extra local into that method and the captured signature has to match exactly.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    protected LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
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
