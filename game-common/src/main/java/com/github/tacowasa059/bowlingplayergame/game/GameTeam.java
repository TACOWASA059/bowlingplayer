package com.github.tacowasa059.bowlingplayergame.game;

import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerMode;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.GameType;

import javax.annotation.Nullable;

/**
 * The three logical teams in the Kaidoro (cops and robbers) game.
 * Each maps to a vanilla scoreboard team used for colouring and friendly-fire rules.
 */
public enum GameTeam {
    /** Runners. Flee from the ball team, sent to jail when caught. */
    RED("bpg_red", "Pin", ChatFormatting.RED, GameType.ADVENTURE, BowlingPlayerMode.PIN),
    /** Chasers. Hunt down the pin team. */
    BLUE("bpg_blue", "Ball", ChatFormatting.BLUE, GameType.ADVENTURE, BowlingPlayerMode.BALL),
    /** Staff / referees. Always spectators. */
    STAFF("bpg_staff", "Staff", ChatFormatting.AQUA, GameType.SPECTATOR, BowlingPlayerMode.NORMAL);

    private final String teamName;
    private final String displayName;
    private final ChatFormatting color;
    private final GameType gameType;
    private final BowlingPlayerMode mode;

    GameTeam(String teamName, String displayName, ChatFormatting color, GameType gameType, BowlingPlayerMode mode) {
        this.teamName = teamName;
        this.displayName = displayName;
        this.color = color;
        this.gameType = gameType;
        this.mode = mode;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public GameType getGameType() {
        return gameType;
    }

    public BowlingPlayerMode getMode() {
        return mode;
    }

    @Nullable
    public static GameTeam byTeamName(@Nullable String name) {
        if (name == null) {
            return null;
        }
        for (GameTeam team : values()) {
            if (team.teamName.equals(name)) {
                return team;
            }
        }
        return null;
    }

    @Nullable
    public static GameTeam byName(String name) {
        for (GameTeam team : values()) {
            if (team.name().equalsIgnoreCase(name) || team.displayName.equalsIgnoreCase(name)) {
                return team;
            }
        }
        return null;
    }
}
