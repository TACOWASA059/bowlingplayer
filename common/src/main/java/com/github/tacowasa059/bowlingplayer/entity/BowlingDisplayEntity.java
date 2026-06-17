package com.github.tacowasa059.bowlingplayer.entity;

import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BowlingDisplayEntity extends Entity {
    public static final float DEFAULT_PIN_WIDTH = 0.268F * 2.0F;
    public static final float DEFAULT_PIN_HEIGHT = 1.5F;
    public static final float DEFAULT_BALL_DIAMETER = 0.95F;

    private static final EntityDataAccessor<Integer> DISPLAY_MODE = SynchedEntityData.defineId(BowlingDisplayEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DISPLAY_SCALE = SynchedEntityData.defineId(BowlingDisplayEntity.class, EntityDataSerializers.FLOAT);
    private static final String NBT_DISPLAY_MODE = "DisplayMode";
    private static final String NBT_DISPLAY_SCALE = "DisplayScale";

    public BowlingDisplayEntity(EntityType<? extends BowlingDisplayEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DISPLAY_MODE, BowlingPlayerMode.PIN.ordinal());
        this.entityData.define(DISPLAY_SCALE, 1.0F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setDisplayMode(BowlingPlayerMode.byName(tag.getString(NBT_DISPLAY_MODE)));
        setDisplayScale(tag.contains(NBT_DISPLAY_SCALE) ? tag.getFloat(NBT_DISPLAY_SCALE) : 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString(NBT_DISPLAY_MODE, getDisplayMode().getSerializedName());
        tag.putFloat(NBT_DISPLAY_SCALE, getDisplayScale());
    }

    @Override
    public void tick() {
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);
        super.tick();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float scale = getDisplayScale();
        if (getDisplayMode() == BowlingPlayerMode.BALL) {
            float diameter = DEFAULT_BALL_DIAMETER * scale;
            return EntityDimensions.scalable(diameter, diameter);
        }
        return EntityDimensions.scalable(DEFAULT_PIN_WIDTH * scale, DEFAULT_PIN_HEIGHT * scale);
    }

    public BowlingPlayerMode getDisplayMode() {
        return BowlingPlayerMode.byId(this.entityData.get(DISPLAY_MODE));
    }

    public void setDisplayMode(BowlingPlayerMode mode) {
        BowlingPlayerMode resolvedMode = mode == BowlingPlayerMode.NORMAL ? BowlingPlayerMode.PIN : mode;
        this.entityData.set(DISPLAY_MODE, resolvedMode.ordinal());
        this.refreshDimensions();
    }

    public float getDisplayScale() {
        return this.entityData.get(DISPLAY_SCALE);
    }

    public void setDisplayScale(float scale) {
        float resolvedScale = Float.isFinite(scale) ? Math.max(0.1F, Math.min(20.0F, scale)) : 1.0F;
        this.entityData.set(DISPLAY_SCALE, resolvedScale);
        this.refreshDimensions();
    }
}
