package com.github.tacowasa059.bowlingplayer.mixin;

import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerMode;
import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerQuaternionUtils;
import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerStateAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Player.class)
public abstract class PlayerMixin implements BowlingPlayerStateAccess {
    @Unique
    private static final EntityDataAccessor<Integer> bowlingPlayer$MODE = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
    @Unique
    private static final EntityDataAccessor<Float> bowlingPlayer$RESTITUTION = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> bowlingPlayer$BALL_SIZE = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> bowlingPlayer$PIN_SIZE = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> bowlingPlayer$BALL_SPEED_MULTIPLIER = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> bowlingPlayer$PIN_CONTACT_DAMAGE = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final String bowlingPlayer$NBT_MODE = "BowlingPlayerMode";
    @Unique
    private static final String bowlingPlayer$NBT_RESTITUTION = "BowlingPlayerRestitution";
    @Unique
    private static final String bowlingPlayer$NBT_BALL_SIZE = "BowlingPlayerBallSize";
    @Unique
    private static final String bowlingPlayer$NBT_PIN_SIZE = "BowlingPlayerPinSize";
    @Unique
    private static final String bowlingPlayer$NBT_BALL_SPEED_MULTIPLIER = "BowlingPlayerBallSpeedMultiplier";
    @Unique
    private static final String bowlingPlayer$NBT_PIN_CONTACT_DAMAGE = "BowlingPlayerPinContactDamage";
    @Unique
    private static final float bowlingPlayer$BALL_DIAMETER = 0.95F;
    @Unique
    private static final float bowlingPlayer$PIN_WIDTH = 0.268F * 2;
    @Unique
    private static final float bowlingPlayer$PIN_HEIGHT = 1.5F;
    @Unique
    private static final float bowlingPlayer$MIN_SIZE = 0.1F;
    @Unique
    private static final float bowlingPlayer$MAX_SIZE = 20.0F;
    @Unique
    private static final float bowlingPlayer$MIN_SPEED_MULTIPLIER = 0.0F;
    @Unique
    private static final float bowlingPlayer$MAX_SPEED_MULTIPLIER = 5.0F;
    @Unique
    private static final float bowlingPlayer$MIN_PIN_CONTACT_DAMAGE = 0.0F;
    @Unique
    private static final float bowlingPlayer$MAX_PIN_CONTACT_DAMAGE = 1024.0F;
    @Unique
    private static final float bowlingPlayer$DEFAULT_RESTITUTION = 0.55F;
    @Unique
    private static final float bowlingPlayer$DEFAULT_BALL_SPEED_MULTIPLIER = 0.3F;
    @Unique
    private static final float bowlingPlayer$DEFAULT_PIN_CONTACT_DAMAGE = 1024.0F;
    @Unique
    private static final UUID bowlingPlayer$BALL_SPEED_MODIFIER_ID = UUID.fromString("8cb4fe8f-bdf8-4a6e-a8f8-b8c0271c7b58");
    @Unique
    private static final AttributeModifier bowlingPlayer$BALL_SPEED_MODIFIER =
            new AttributeModifier(bowlingPlayer$BALL_SPEED_MODIFIER_ID, "Bowling ball speed boost", 1.25D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    @Unique
    private Quaternionf bowlingPlayer$quaternion = new Quaternionf();
    @Unique
    private Quaternionf bowlingPlayer$previousQuaternion = new Quaternionf();
    @Unique
    private Vec3 bowlingPlayer$previousTrackedPosition;
    @Unique
    private boolean bowlingPlayer$wasOnGround;

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void bowlingPlayer$defineSynchedData(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        self.getEntityData().define(bowlingPlayer$MODE, BowlingPlayerMode.NORMAL.ordinal());
        self.getEntityData().define(bowlingPlayer$RESTITUTION, bowlingPlayer$DEFAULT_RESTITUTION);
        self.getEntityData().define(bowlingPlayer$BALL_SIZE, 1.5F);
        self.getEntityData().define(bowlingPlayer$PIN_SIZE, 1.5F);
        self.getEntityData().define(bowlingPlayer$BALL_SPEED_MULTIPLIER, bowlingPlayer$DEFAULT_BALL_SPEED_MULTIPLIER);
        self.getEntityData().define(bowlingPlayer$PIN_CONTACT_DAMAGE, bowlingPlayer$DEFAULT_PIN_CONTACT_DAMAGE);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void bowlingPlayer$readAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        bowlingPlayer$setMode(BowlingPlayerMode.byName(tag.getString(bowlingPlayer$NBT_MODE)));
        bowlingPlayer$setRestitutionCoefficient(tag.getFloat(bowlingPlayer$NBT_RESTITUTION));
        bowlingPlayer$setBallSize(tag.contains(bowlingPlayer$NBT_BALL_SIZE) ? tag.getFloat(bowlingPlayer$NBT_BALL_SIZE) : bowlingPlayer$BALL_DIAMETER);
        bowlingPlayer$setPinSize(tag.contains(bowlingPlayer$NBT_PIN_SIZE) ? tag.getFloat(bowlingPlayer$NBT_PIN_SIZE) : 1.0F);
        bowlingPlayer$setBallSpeedMultiplier(tag.contains(bowlingPlayer$NBT_BALL_SPEED_MULTIPLIER) ? tag.getFloat(bowlingPlayer$NBT_BALL_SPEED_MULTIPLIER) : bowlingPlayer$DEFAULT_BALL_SPEED_MULTIPLIER);
        bowlingPlayer$setPinContactDamage(tag.contains(bowlingPlayer$NBT_PIN_CONTACT_DAMAGE) ? tag.getFloat(bowlingPlayer$NBT_PIN_CONTACT_DAMAGE) : bowlingPlayer$DEFAULT_PIN_CONTACT_DAMAGE);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void bowlingPlayer$addAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        tag.putString(bowlingPlayer$NBT_MODE, bowlingPlayer$getMode().getSerializedName());
        tag.putFloat(bowlingPlayer$NBT_RESTITUTION, bowlingPlayer$getRestitutionCoefficient());
        tag.putFloat(bowlingPlayer$NBT_BALL_SIZE, bowlingPlayer$getBallSize());
        tag.putFloat(bowlingPlayer$NBT_PIN_SIZE, bowlingPlayer$getPinSize());
        tag.putFloat(bowlingPlayer$NBT_BALL_SPEED_MULTIPLIER, bowlingPlayer$getBallSpeedMultiplier());
        tag.putFloat(bowlingPlayer$NBT_PIN_CONTACT_DAMAGE, bowlingPlayer$getPinContactDamage());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void bowlingPlayer$tick(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        bowlingPlayer$updateClientRotation(self);
        bowlingPlayer$updateMovementSpeed(self);

        if (self.level().isClientSide && self.tickCount % 5 == 0) {
            self.refreshDimensions();
        }

        bowlingPlayer$wasOnGround = self.onGround();
    }

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void bowlingPlayer$getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        BowlingPlayerMode mode = bowlingPlayer$getMode();
        if (mode == BowlingPlayerMode.BALL) {
            float ballSize = bowlingPlayer$getBallSize();
            cir.setReturnValue(EntityDimensions.scalable(ballSize, ballSize));
        } else if (mode == BowlingPlayerMode.PIN) {
            float pinScale = bowlingPlayer$getPinSize();
            cir.setReturnValue(EntityDimensions.scalable(bowlingPlayer$PIN_WIDTH * pinScale, bowlingPlayer$PIN_HEIGHT * pinScale));
        }
    }

    @Inject(method = "getStandingEyeHeight", at = @At("HEAD"), cancellable = true)
    private void bowlingPlayer$getStandingEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        BowlingPlayerMode mode = bowlingPlayer$getMode();
        if (mode == BowlingPlayerMode.BALL) {
            cir.setReturnValue(bowlingPlayer$getBallSize() * 0.5F);
        } else if (mode == BowlingPlayerMode.PIN) {
            cir.setReturnValue(bowlingPlayer$getPinSize());
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void bowlingPlayer$causeFallDamage(float fallDistance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (bowlingPlayer$getMode() == BowlingPlayerMode.BALL) {
            Player self = (Player) (Object) this;
            if (!self.level().isClientSide) {
                bowlingPlayer$bounce(self, fallDistance);
            }
            cir.setReturnValue(false);
        }
    }

    @Unique
    private void bowlingPlayer$bounce(Player player, float fallDistance) {
        if (Math.abs(fallDistance) < 1.0F) {
            return;
        }

        double bounceVelocityY = Math.sqrt(2.0D * fallDistance * 0.08D) * bowlingPlayer$getRestitutionCoefficient();
        double restoredMotionX = player.getX() - player.xOld;
        double restoredMotionZ = player.getZ() - player.zOld;
        player.hasImpulse = true;
        player.setDeltaMovement(restoredMotionX, bounceVelocityY, restoredMotionZ);
        player.fallDistance = 0.0F;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    @Unique
    private void bowlingPlayer$updateMovementSpeed(Player player) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }

        AttributeModifier currentModifier = movementSpeed.getModifier(bowlingPlayer$BALL_SPEED_MODIFIER_ID);
        if (bowlingPlayer$getMode() == BowlingPlayerMode.BALL) {
            float multiplier = bowlingPlayer$getBallSpeedMultiplier();
            if (currentModifier == null || currentModifier.getAmount() != multiplier) {
                if (currentModifier != null) {
                    movementSpeed.removeModifier(bowlingPlayer$BALL_SPEED_MODIFIER_ID);
                }
                movementSpeed.addTransientModifier(new AttributeModifier(
                        bowlingPlayer$BALL_SPEED_MODIFIER_ID,
                        bowlingPlayer$BALL_SPEED_MODIFIER.getName(),
                        multiplier,
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                ));
            }
        } else if (currentModifier != null) {
            movementSpeed.removeModifier(bowlingPlayer$BALL_SPEED_MODIFIER_ID);
        }
    }

    @Unique
    private void bowlingPlayer$updateClientRotation(Player player) {
        if (!player.level().isClientSide) {
            return;
        }

        if (bowlingPlayer$previousTrackedPosition == null) {
            bowlingPlayer$previousTrackedPosition = player.position();
            bowlingPlayer$quaternion = new Quaternionf();
            bowlingPlayer$previousQuaternion = new Quaternionf();
            return;
        }

        bowlingPlayer$previousQuaternion = new Quaternionf(bowlingPlayer$quaternion);
        if (bowlingPlayer$getMode() == BowlingPlayerMode.BALL) {
            if (player.getVehicle() != null) {
                bowlingPlayer$quaternion = BowlingPlayerQuaternionUtils.getQuaternionFromEntity(player.getVehicle());
            } else {
                bowlingPlayer$quaternion = BowlingPlayerQuaternionUtils.getUpdatedQuaternion(
                        player.position(),
                        bowlingPlayer$previousTrackedPosition,
                        bowlingPlayer$getBallSize() * 0.5F,
                        bowlingPlayer$quaternion
                );
            }
        } else {
            bowlingPlayer$quaternion = new Quaternionf();
            bowlingPlayer$previousQuaternion = new Quaternionf();
        }
        bowlingPlayer$previousTrackedPosition = player.position();
    }

    @Override
    public BowlingPlayerMode bowlingPlayer$getMode() {
        Player self = (Player) (Object) this;
        return BowlingPlayerMode.byId(self.getEntityData().get(bowlingPlayer$MODE));
    }

    @Override
    public void bowlingPlayer$setMode(BowlingPlayerMode mode) {
        Player self = (Player) (Object) this;
        self.getEntityData().set(bowlingPlayer$MODE, mode.ordinal());
        self.refreshDimensions();
    }

    @Override
    public float bowlingPlayer$getBallSize() {
        Player self = (Player) (Object) this;
        return self.getEntityData().get(bowlingPlayer$BALL_SIZE);
    }

    @Override
    public void bowlingPlayer$setBallSize(float value) {
        Player self = (Player) (Object) this;
        self.getEntityData().set(bowlingPlayer$BALL_SIZE, bowlingPlayer$clampSize(value, bowlingPlayer$BALL_DIAMETER));
        self.refreshDimensions();
    }

    @Override
    public float bowlingPlayer$getPinSize() {
        Player self = (Player) (Object) this;
        return self.getEntityData().get(bowlingPlayer$PIN_SIZE);
    }

    @Override
    public void bowlingPlayer$setPinSize(float value) {
        Player self = (Player) (Object) this;
        self.getEntityData().set(bowlingPlayer$PIN_SIZE, bowlingPlayer$clampSize(value, 1.0F));
        self.refreshDimensions();
    }

    @Override
    public float bowlingPlayer$getBallSpeedMultiplier() {
        Player self = (Player) (Object) this;
        return self.getEntityData().get(bowlingPlayer$BALL_SPEED_MULTIPLIER);
    }

    @Override
    public void bowlingPlayer$setBallSpeedMultiplier(float value) {
        Player self = (Player) (Object) this;
        self.getEntityData().set(bowlingPlayer$BALL_SPEED_MULTIPLIER, bowlingPlayer$clampBallSpeedMultiplier(value));
    }

    @Override
    public float bowlingPlayer$getPinContactDamage() {
        Player self = (Player) (Object) this;
        return self.getEntityData().get(bowlingPlayer$PIN_CONTACT_DAMAGE);
    }

    @Override
    public void bowlingPlayer$setPinContactDamage(float value) {
        Player self = (Player) (Object) this;
        self.getEntityData().set(bowlingPlayer$PIN_CONTACT_DAMAGE, bowlingPlayer$clampPinContactDamage(value));
    }

    @Override
    public float bowlingPlayer$getRestitutionCoefficient() {
        Player self = (Player) (Object) this;
        return self.getEntityData().get(bowlingPlayer$RESTITUTION);
    }

    @Override
    public void bowlingPlayer$setRestitutionCoefficient(float value) {
        Player self = (Player) (Object) this;
        self.getEntityData().set(bowlingPlayer$RESTITUTION, value <= 0.0F ? bowlingPlayer$DEFAULT_RESTITUTION : value);
    }

    @Override
    public Quaternionf bowlingPlayer$getInterpolatedQuaternion(float partialTick) {
        return BowlingPlayerQuaternionUtils.slerp(bowlingPlayer$previousQuaternion, bowlingPlayer$quaternion, partialTick);
    }

    @Override
    public void bowlingPlayer$copyStateFrom(BowlingPlayerStateAccess other) {
        bowlingPlayer$setBallSize(other.bowlingPlayer$getBallSize());
        bowlingPlayer$setPinSize(other.bowlingPlayer$getPinSize());
        bowlingPlayer$setBallSpeedMultiplier(other.bowlingPlayer$getBallSpeedMultiplier());
        bowlingPlayer$setPinContactDamage(other.bowlingPlayer$getPinContactDamage());
        bowlingPlayer$setMode(other.bowlingPlayer$getMode());
        bowlingPlayer$setRestitutionCoefficient(other.bowlingPlayer$getRestitutionCoefficient());
    }

    @Unique
    private static float bowlingPlayer$clampSize(float value, float fallback) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return fallback;
        }
        return Math.max(bowlingPlayer$MIN_SIZE, Math.min(bowlingPlayer$MAX_SIZE, value));
    }

    @Unique
    private static float bowlingPlayer$clampBallSpeedMultiplier(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return bowlingPlayer$DEFAULT_BALL_SPEED_MULTIPLIER;
        }
        return Math.max(bowlingPlayer$MIN_SPEED_MULTIPLIER, Math.min(bowlingPlayer$MAX_SPEED_MULTIPLIER, value));
    }

    @Unique
    private static float bowlingPlayer$clampPinContactDamage(float value) {
        if (Float.isNaN(value)) {
            return bowlingPlayer$DEFAULT_PIN_CONTACT_DAMAGE;
        }
        if (Float.isInfinite(value)) {
            return bowlingPlayer$MAX_PIN_CONTACT_DAMAGE;
        }
        return Math.max(bowlingPlayer$MIN_PIN_CONTACT_DAMAGE, Math.min(bowlingPlayer$MAX_PIN_CONTACT_DAMAGE, value));
    }

}
