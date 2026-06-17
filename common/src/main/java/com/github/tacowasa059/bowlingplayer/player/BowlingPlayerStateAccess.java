package com.github.tacowasa059.bowlingplayer.player;

import org.joml.Quaternionf;

public interface BowlingPlayerStateAccess {
    float bowlingPlayer$getBallSize();

    void bowlingPlayer$setBallSize(float value);

    float bowlingPlayer$getPinSize();

    void bowlingPlayer$setPinSize(float value);

    float bowlingPlayer$getBallSpeedMultiplier();

    void bowlingPlayer$setBallSpeedMultiplier(float value);

    float bowlingPlayer$getPinContactDamage();

    void bowlingPlayer$setPinContactDamage(float value);

    BowlingPlayerMode bowlingPlayer$getMode();

    void bowlingPlayer$setMode(BowlingPlayerMode mode);

    float bowlingPlayer$getRestitutionCoefficient();

    void bowlingPlayer$setRestitutionCoefficient(float value);

    Quaternionf bowlingPlayer$getInterpolatedQuaternion(float partialTick);

    void bowlingPlayer$copyStateFrom(BowlingPlayerStateAccess other);
}
