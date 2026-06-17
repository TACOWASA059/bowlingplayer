package com.github.tacowasa059.bowlingplayergame.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializable snapshot of an in-progress round, used to resume after a server
 * restart/crash. Timers are stored relative (elapsed / remaining ticks) rather than
 * as absolute tick deadlines, because {@code server.getTickCount()} resets to 0 on
 * each launch; the values are re-anchored to the current tick when restored.
 */
public final class GameState {
    public String phase = GamePhase.IDLE.name();
    public boolean blueDeployed;
    public int elapsedTicks;
    public int jailIndex;

    public List<String> jailed = new ArrayList<>();
    public Map<String, Integer> kills = new HashMap<>();
    public Map<String, Integer> rescues = new HashMap<>();
    /** UUID -> remaining cooldown ticks. */
    public Map<String, Integer> detectorCooldownRemaining = new HashMap<>();
    public Map<String, Integer> speedCooldownRemaining = new HashMap<>();
    /** UUID -> remaining ticks until the blue player respawns. */
    public Map<String, Integer> blueRespawnRemaining = new HashMap<>();

    public List<CobwebState> cobwebs = new ArrayList<>();

    /** A placed cobweb with its remaining lifetime. */
    public static final class CobwebState {
        public String dimension;
        public int x;
        public int y;
        public int z;
        public int remainingTicks;

        public CobwebState() {
        }

        public CobwebState(String dimension, int x, int y, int z, int remainingTicks) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.remainingTicks = remainingTicks;
        }
    }
}
