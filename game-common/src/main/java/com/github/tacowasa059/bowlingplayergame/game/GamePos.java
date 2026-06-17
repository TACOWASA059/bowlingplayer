package com.github.tacowasa059.bowlingplayergame.game;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A world location (dimension + coordinates + facing). Gson-serialisable via plain fields.
 */
public final class GamePos {
    private String dimension = "minecraft:overworld";
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public GamePos() {
    }

    public GamePos(String dimension, double x, double y, double z, float yaw, float pitch) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static GamePos fromSource(CommandSourceStack source) {
        Vec3 pos = source.getPosition();
        Vec2 rot = source.getRotation();
        String dim = source.getLevel().dimension().location().toString();
        return new GamePos(dim, pos.x, pos.y, pos.z, rot.y, rot.x);
    }

    public ResourceKey<Level> dimensionKey() {
        return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimension));
    }

    @Nullable
    public ServerLevel level(MinecraftServer server) {
        return server.getLevel(dimensionKey());
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public String dimension() {
        return dimension;
    }

    public BlockPos blockPos() {
        return BlockPos.containing(x, y, z);
    }

    @Override
    public String toString() {
        return String.format("%s [%.1f, %.1f, %.1f]", dimension, x, y, z);
    }
}
