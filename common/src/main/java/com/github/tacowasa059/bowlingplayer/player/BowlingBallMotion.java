package com.github.tacowasa059.bowlingplayer.player;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Ball movement, shared by both loaders.
 *
 * <p>It replaces the tail of {@code LivingEntity#travel}: the same gravity and levitation handling as vanilla, but with
 * a capped horizontal speed and rolling drag instead of the usual friction. The two loaders have to reach it from
 * separate mixins because Forge patches an {@code AttributeInstance gravity} local into {@code travel} that vanilla
 * does not have, so the captured local signature differs - the physics itself does not.
 */
public final class BowlingBallMotion {
    private static final double HORIZONTAL_DRAG = 0.95D;
    private static final double VERTICAL_DRAG = 0.9800000190734863D;
    private static final double BASE_MAX_HORIZONTAL_SPEED = 0.8D;

    private BowlingBallMotion() {
    }

    /**
     * @param movement the movement vector vanilla just calculated (the {@code vec35} local)
     * @param gravity  the gravity to apply this tick: Forge's gravity attribute value, or vanilla's {@code d0}
     * @return true if ball physics took over, in which case {@code travel} must be cancelled
     */
    public static boolean apply(LivingEntity entity, Vec3 movement, double gravity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        BowlingPlayerStateAccess state = (BowlingPlayerStateAccess) player;
        if (state.bowlingPlayer$getMode() != BowlingPlayerMode.BALL) {
            return false;
        }

        double motionY = movement.y;
        if (entity.hasEffect(MobEffects.LEVITATION)) {
            MobEffectInstance effect = entity.getEffect(MobEffects.LEVITATION);
            if (effect != null) {
                motionY += (0.05D * (effect.getAmplifier() + 1) - movement.y) * 0.2D;
            }
        } else if (!entity.isNoGravity()) {
            motionY -= gravity;
        }

        double motionX = movement.x * HORIZONTAL_DRAG;
        double motionZ = movement.z * HORIZONTAL_DRAG;
        double horizontalSqr = motionX * motionX + motionZ * motionZ;
        double maxHorizontalSpeed = BASE_MAX_HORIZONTAL_SPEED
                * Math.max(0.0F, state.bowlingPlayer$getBallSpeedMultiplier());
        if (maxHorizontalSpeed <= 0.0D) {
            motionX = 0.0D;
            motionZ = 0.0D;
        } else if (horizontalSqr >= maxHorizontalSpeed * maxHorizontalSpeed) {
            double scale = maxHorizontalSpeed / Math.sqrt(horizontalSqr);
            motionX *= scale;
            motionZ *= scale;
        }

        entity.setDeltaMovement(motionX, motionY * VERTICAL_DRAG, motionZ);
        return true;
    }
}
