package com.github.tacowasa059.bowlingplayer.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public final class BowlingPlayerQuaternionUtils {
    private BowlingPlayerQuaternionUtils() {
    }

    public static Quaternionf getUpdatedQuaternion(Vec3 currentPosition, Vec3 previousPosition, float radius, Quaternionf currentQuaternion) {
        if (previousPosition == null || radius <= 0.0F) {
            return new Quaternionf(currentQuaternion);
        }

        Vec3 movement = currentPosition.subtract(previousPosition);
        double horizontalDistance = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        if (horizontalDistance < 1.0E-6D) {
            return new Quaternionf(currentQuaternion);
        }

        double angle = horizontalDistance / radius;
        Vec3 axisDirection = new Vec3(movement.z, 0.0D, -movement.x).normalize();
        Quaternionf rotation = new Quaternionf(new AxisAngle4f((float) angle, (float) axisDirection.x, 0.0F, (float) axisDirection.z));
        return normalize(rotation.mul(new Quaternionf(currentQuaternion)));
    }

    public static Quaternionf getQuaternionFromEntity(Entity entity) {
        Quaternionf pitch = new Quaternionf().rotateX((float) Math.toRadians(entity.getXRot()));
        Quaternionf yaw = new Quaternionf().rotateY((float) Math.toRadians(-entity.getYRot()));
        return yaw.mul(pitch);
    }

    public static Quaternionf slerp(Quaternionf start, Quaternionf end, float delta) {
        return new Quaternionf(start).slerp(end, delta);
    }

    public static Quaternionf normalize(Quaternionf quaternion) {
        if (quaternion.lengthSquared() < 1.0E-6F) {
            return new Quaternionf();
        }
        return quaternion.normalize(new Quaternionf());
    }
}
