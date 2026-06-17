package com.github.tacowasa059.bowlingplayergame.config;

import com.github.tacowasa059.bowlingplayergame.BowlingPlayerGameConstants;
import com.github.tacowasa059.bowlingplayergame.game.GamePos;
import com.github.tacowasa059.bowlingplayergame.game.GameTeam;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * All tunable settings for the Kaidoro game, with the spec's default values.
 * Persisted to {@code <world>/bowlingplayergame.json}.
 */
public final class GameConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** An extra vanilla item handed out at deploy (extensible kit). */
    public static final class ItemEntry {
        /** Item resource id, e.g. {@code minecraft:golden_apple}. */
        public String item;
        public int count = 1;

        public ItemEntry() {
        }

        public ItemEntry(String item, int count) {
            this.item = item;
            this.count = count;
        }
    }

    /** Configurable item set given to one team. */
    public static final class TeamLoadout {
        /** Steak handed out on initial deploy (0 = none). */
        public int steak = 64;
        /** Cobwebs handed out on deploy and replenished on each respawn (0 = none). */
        public int cobwebs = 5;
        /** Whether the detector item is included. */
        public boolean detector = true;
        /** Whether the speed item is included. */
        public boolean speed = true;
        /** Additional items given on deploy (extend the kit without code changes). */
        public List<ItemEntry> extraItems = new ArrayList<>();

        /** Red default: steak + cobwebs, no detector/speed. */
        public static TeamLoadout red() {
            TeamLoadout loadout = new TeamLoadout();
            loadout.steak = 64;
            loadout.cobwebs = 5;
            loadout.detector = false;
            loadout.speed = false;
            return loadout;
        }

        /** Blue default: steak + detector + speed, no cobwebs. */
        public static TeamLoadout blue() {
            TeamLoadout loadout = new TeamLoadout();
            loadout.steak = 64;
            loadout.cobwebs = 0;
            loadout.detector = true;
            loadout.speed = true;
            return loadout;
        }
    }

    /** Total round length in seconds (default 30 min). */
    public int timeLimitSeconds = 30 * 60;

    /** Team size ratio red:blue (default 3:1). */
    public int teamRatioRed = 3;
    public int teamRatioBlue = 1;

    /** Delay before the blue team is deployed after the red team (default 30s). */
    public int blueDeployDelaySeconds = 30;

    /** Cooldown shared by the detector and speed items (default 2 min). */
    public int itemCooldownSeconds = 2 * 60;

    /** How long the speed boost lasts (default 15s). */
    public int speedDurationSeconds = 15;

    /** Speed effect level (1 = Speed I, 2 = Speed II, ...); internally amplifier = level - 1. */
    public int speedLevel = 2;

    /** Detector range in blocks (default 60). */
    public int detectionRange = 60;

    /** How long the detector glow lasts in seconds (default 10s). */
    public int detectionGlowSeconds = 10;

    /** How long placed cobwebs survive before being removed (default 10s). */
    public int cobwebDespawnSeconds = 10;

    /** Blue spectator cooldown after death before respawning (default 15s). */
    public int blueSpectatorSeconds = 15;

    /** Ball (blue team) size applied at deploy. */
    public float ballSize = 0.95F;

    /** Pin (red team) size applied at deploy. */
    public float pinSize = 1.0F;

    /** Contact damage the ball (blue) deals to pins on a hit (1024 = guaranteed kill). */
    public float contactDamage = 1024.0F;

    /** Per-team item loadout (handed out on deploy/respawn). */
    public TeamLoadout redLoadout = TeamLoadout.red();
    public TeamLoadout blueLoadout = TeamLoadout.blue();

    /** Radius around the red deploy point that players are scattered across (0 = exact). */
    public double redDeployRadius = 0;

    /** Red jail respawn positions (one is chosen round-robin). */
    public List<GamePos> jailPositions = new ArrayList<>();

    /** Red team initial deploy location. */
    public GamePos redDeploy;

    /** Blue team initial deploy location. */
    public GamePos blueDeploy;

    public int timeLimitTicks() {
        return timeLimitSeconds * 20;
    }

    public int blueDeployDelayTicks() {
        return blueDeployDelaySeconds * 20;
    }

    public int itemCooldownTicks() {
        return itemCooldownSeconds * 20;
    }

    public int cobwebDespawnTicks() {
        return cobwebDespawnSeconds * 20;
    }

    public boolean isReady() {
        return redDeploy != null && blueDeploy != null && !jailPositions.isEmpty();
    }

    public TeamLoadout loadoutOf(GameTeam team) {
        return team == GameTeam.BLUE ? blueLoadout : redLoadout;
    }

    public static GameConfig load(Path path) {
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                GameConfig config = GSON.fromJson(reader, GameConfig.class);
                if (config != null) {
                    if (config.jailPositions == null) {
                        config.jailPositions = new ArrayList<>();
                    }
                    if (config.redLoadout == null) {
                        config.redLoadout = TeamLoadout.red();
                    }
                    if (config.blueLoadout == null) {
                        config.blueLoadout = TeamLoadout.blue();
                    }
                    if (config.redLoadout.extraItems == null) {
                        config.redLoadout.extraItems = new ArrayList<>();
                    }
                    if (config.blueLoadout.extraItems == null) {
                        config.blueLoadout.extraItems = new ArrayList<>();
                    }
                    return config;
                }
            } catch (Exception e) {
                BowlingPlayerGameConstants.LOG.error("Failed to load game config from {}", path, e);
            }
        }
        GameConfig config = new GameConfig();
        config.save(path);
        return config;
    }

    public void save(Path path) {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            BowlingPlayerGameConstants.LOG.error("Failed to save game config to {}", path, e);
        }
    }
}
