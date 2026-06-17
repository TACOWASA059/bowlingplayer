package com.github.tacowasa059.bowlingplayer.player;

import net.minecraft.util.StringRepresentable;

public enum BowlingPlayerMode implements StringRepresentable {
    NORMAL("normal"),
    PIN("pin"),
    BALL("ball");

    private final String serializedName;

    BowlingPlayerMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public static BowlingPlayerMode byId(int id) {
        BowlingPlayerMode[] values = values();
        if (id < 0 || id >= values.length) {
            return NORMAL;
        }
        return values[id];
    }

    public static BowlingPlayerMode byName(String name) {
        for (BowlingPlayerMode mode : values()) {
            if (mode.serializedName.equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return NORMAL;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
