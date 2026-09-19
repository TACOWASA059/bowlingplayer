package com.github.tacowasa059.bowlingplayergame.game;

import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerMode;
import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerStateAccess;
import com.github.tacowasa059.bowlingplayergame.BowlingPlayerGameConstants;
import com.github.tacowasa059.bowlingplayergame.config.GameConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.server.level.ServerBossEvent;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Holds and drives a single Kaidoro (cops &amp; robbers) round for one running server.
 * Loader specific event glue forwards to the {@code on*} handlers and {@link #tick()}.
 */
public final class GameManager {
    private static final Map<MinecraftServer, GameManager> INSTANCES = new HashMap<>();
    private static final String CONFIG_FILE = "bowlingplayergame.json";
    private static final String STATE_FILE = "bowlingplayergame_state.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** How often (ticks) the live game state is autosaved while running. */
    private static final int STATE_SAVE_INTERVAL = 100;

    // Vanilla scoreboard objectives used to track scores.
    private static final String OBJ_KILLS = "bpg_kills";
    private static final String OBJ_RESCUES = "bpg_rescues";
    /** After a game ends, the sidebar alternates between the two scores every this many ticks. */
    private static final int SIDEBAR_SWAP_TICKS = 100;
    /** Number of players shown per team in the end-of-game chat summary. */
    private static final int SCORE_TOP_N = 5;

    /** Entity shared-flags data (id 0); bit 0x40 is the glowing flag. */
    private static final EntityDataAccessor<Byte> SHARED_FLAGS = new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);
    private static final int GLOWING_FLAG = 0x40;

    // Fixed hotbar slots for distributed items (locked in place during a game).
    private static final int SLOT_STEAK = 0;
    private static final int SLOT_COBWEB = 1;
    private static final int SLOT_DETECTOR = 2;
    private static final int SLOT_SPEED = 3;

    private final MinecraftServer server;
    private final Path configPath;
    private final Path statePath;
    private final Random random = new Random();

    private GameConfig config;
    private GamePhase phase = GamePhase.IDLE;
    private boolean blueDeployed;
    private int startTick;
    private int endTick;
    private int jailIndex;

    /** A round snapshot loaded at startup, applied on the first tick (null if none). */
    @Nullable
    private GameState pendingRestore;

    private final Set<UUID> jailed = new HashSet<>();
    private final Map<UUID, Integer> detectorCooldown = new HashMap<>();
    private final Map<UUID, Integer> speedCooldown = new HashMap<>();
    private final Map<UUID, Integer> blueRespawnAt = new HashMap<>();
    private final List<CobwebEntry> cobwebs = new ArrayList<>();
    /** Per-viewer detector glow sessions (visible only to the detector user). */
    private final List<GlowSession> glows = new ArrayList<>();
    /** Transient per-player hotbar notes, appended to the composed action bar until they expire. */
    private final Map<UUID, ActionbarNote> actionbarNotes = new HashMap<>();

    @Nullable
    private ServerBossEvent bossBar;

    private GameManager(MinecraftServer server) {
        this.server = server;
        this.configPath = server.getWorldPath(LevelResource.ROOT).resolve(CONFIG_FILE);
        this.statePath = server.getWorldPath(LevelResource.ROOT).resolve(STATE_FILE);
        this.config = GameConfig.load(configPath);
        this.pendingRestore = loadStateFile();
    }

    public static GameManager get(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(server, GameManager::new);
    }

    public static void onServerStarting(MinecraftServer server) {
        get(server);
    }

    public static void onServerStopping(MinecraftServer server) {
        GameManager manager = INSTANCES.remove(server);
        if (manager != null) {
            manager.saveConfig();
            // Persist on graceful shutdown so an in-progress round resumes next launch.
            if (manager.phase == GamePhase.RUNNING) {
                manager.saveState();
            }
        }
    }

    public GameConfig config() {
        return config;
    }

    public GamePhase phase() {
        return phase;
    }

    public void saveConfig() {
        config.save(configPath);
    }

    // ------------------------------------------------------------------
    // Round state persistence (resume after restart/crash)
    // ------------------------------------------------------------------

    @Nullable
    private GameState loadStateFile() {
        if (!Files.exists(statePath)) {
            return null;
        }
        try (var reader = Files.newBufferedReader(statePath, java.nio.charset.StandardCharsets.UTF_8)) {
            GameState state = GSON.fromJson(reader, GameState.class);
            if (state != null && GamePhase.RUNNING.name().equals(state.phase)) {
                return state;
            }
        } catch (Exception e) {
            BowlingPlayerGameConstants.LOG.error("Failed to load game state from {}", statePath, e);
        }
        return null;
    }

    private void saveState() {
        GameState state = captureState();
        Path tmp = statePath.resolveSibling(STATE_FILE + ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(tmp, java.nio.charset.StandardCharsets.UTF_8)) {
                GSON.toJson(state, writer);
            }
            // Atomic replace so a crash mid-write never corrupts the live state file.
            Files.move(tmp, statePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            BowlingPlayerGameConstants.LOG.error("Failed to save game state to {}", statePath, e);
        }
    }

    private void deleteState() {
        try {
            Files.deleteIfExists(statePath);
        } catch (Exception e) {
            BowlingPlayerGameConstants.LOG.error("Failed to delete game state {}", statePath, e);
        }
    }

    private GameState captureState() {
        int now = server.getTickCount();
        GameState state = new GameState();
        state.phase = phase.name();
        state.blueDeployed = blueDeployed;
        state.elapsedTicks = now - startTick;
        state.jailIndex = jailIndex;
        for (UUID uuid : jailed) {
            state.jailed.add(uuid.toString());
        }
        // Scores live in vanilla scoreboard objectives, which persist on their own.
        detectorCooldown.forEach((uuid, deadline) -> state.detectorCooldownRemaining.put(uuid.toString(), Math.max(0, deadline - now)));
        speedCooldown.forEach((uuid, deadline) -> state.speedCooldownRemaining.put(uuid.toString(), Math.max(0, deadline - now)));
        blueRespawnAt.forEach((uuid, deadline) -> state.blueRespawnRemaining.put(uuid.toString(), Math.max(0, deadline - now)));
        for (CobwebEntry entry : cobwebs) {
            state.cobwebs.add(new GameState.CobwebState(
                    entry.dimension.location().toString(), entry.pos.getX(), entry.pos.getY(), entry.pos.getZ(),
                    Math.max(0, entry.removeTick - now)));
        }
        return state;
    }

    /** Re-anchors a loaded snapshot to the current tick and rebuilds runtime objects. */
    private void applyRestore(GameState state) {
        int now = server.getTickCount();
        phase = GamePhase.RUNNING;
        blueDeployed = state.blueDeployed;
        startTick = now - state.elapsedTicks;
        jailIndex = state.jailIndex;

        jailed.clear();
        for (String uuid : state.jailed) {
            jailed.add(UUID.fromString(uuid));
        }
        detectorCooldown.clear();
        state.detectorCooldownRemaining.forEach((uuid, remaining) -> detectorCooldown.put(UUID.fromString(uuid), now + remaining));
        speedCooldown.clear();
        state.speedCooldownRemaining.forEach((uuid, remaining) -> speedCooldown.put(UUID.fromString(uuid), now + remaining));
        blueRespawnAt.clear();
        state.blueRespawnRemaining.forEach((uuid, remaining) -> blueRespawnAt.put(UUID.fromString(uuid), now + remaining));
        cobwebs.clear();
        for (GameState.CobwebState cobweb : state.cobwebs) {
            ResourceKey<Level> dim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    new net.minecraft.resources.ResourceLocation(cobweb.dimension));
            cobwebs.add(new CobwebEntry(dim, new BlockPos(cobweb.x, cobweb.y, cobweb.z), now + cobweb.remainingTicks));
        }

        bossBar = new ServerBossEvent(Component.translatable("bpg.bossbar.survivors"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            bossBar.addPlayer(player);
            GameTeam team = teamOf(player);
            if (team == GameTeam.RED || team == GameTeam.BLUE) {
                ((BowlingPlayerStateAccess) player).bowlingPlayer$setMode(team.getMode());
                player.refreshDimensions();
            }
        }
        updateBossBar();
        BowlingPlayerGameConstants.LOG.info("Resumed Kaidoro game ({}s elapsed)", state.elapsedTicks / 20);
    }

    // ------------------------------------------------------------------
    // Team management
    // ------------------------------------------------------------------

    public void ensureTeams() {
        Scoreboard scoreboard = server.getScoreboard();
        for (GameTeam team : GameTeam.values()) {
            PlayerTeam playerTeam = scoreboard.getPlayerTeam(team.getTeamName());
            if (playerTeam == null) {
                playerTeam = scoreboard.addPlayerTeam(team.getTeamName());
            }
            playerTeam.setColor(team.getColor());
            playerTeam.setAllowFriendlyFire(false);
            playerTeam.setDisplayName(Component.literal(team.getDisplayName()).withStyle(team.getColor()));
        }
    }

    // ------------------------------------------------------------------
    // Scores (vanilla scoreboard objectives)
    // ------------------------------------------------------------------

    private Objective objective(String name, String display) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(name);
        if (objective == null) {
            objective = scoreboard.addObjective(name, ObjectiveCriteria.DUMMY,
                    Component.translatable(display), ObjectiveCriteria.RenderType.INTEGER);
        }
        return objective;
    }

    /** Deletes and recreates the score objectives (clearing all scores). No sidebar while running. */
    private void resetObjectives() {
        Scoreboard scoreboard = server.getScoreboard();
        for (String name : new String[]{OBJ_KILLS, OBJ_RESCUES}) {
            Objective objective = scoreboard.getObjective(name);
            if (objective != null) {
                scoreboard.removeObjective(objective);
            }
        }
        objective(OBJ_KILLS, "bpg.score.kills_blue");
        objective(OBJ_RESCUES, "bpg.score.rescues_red");
        clearSidebar();
    }

    private void clearSidebar() {
        server.getScoreboard().setDisplayObjective(Scoreboard.DISPLAY_SLOT_SIDEBAR, null);
    }

    /** While the game is ENDED, alternates the sidebar between the two score objectives. */
    private void tickEndedSidebar() {
        int sinceEnd = server.getTickCount() - endTick;
        if (sinceEnd > 0 && sinceEnd % SIDEBAR_SWAP_TICKS == 0) {
            boolean showKills = (sinceEnd / SIDEBAR_SWAP_TICKS) % 2 == 0;
            Objective objective = showKills ? objective(OBJ_KILLS, "bpg.score.kills_blue") : objective(OBJ_RESCUES, "bpg.score.rescues_red");
            server.getScoreboard().setDisplayObjective(Scoreboard.DISPLAY_SLOT_SIDEBAR, objective);
        }
    }

    private void addScore(String objectiveName, String display, ServerPlayer player, int delta) {
        Objective objective = objective(objectiveName, display);
        Score score = server.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective);
        score.setScore(score.getScore() + delta);
    }

    private int scoreOf(String objectiveName, ServerPlayer player) {
        Objective objective = server.getScoreboard().getObjective(objectiveName);
        if (objective == null) {
            return 0;
        }
        return server.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective).getScore();
    }

    @Nullable
    public GameTeam teamOf(ServerPlayer player) {
        Team team = player.getTeam();
        return team == null ? null : GameTeam.byTeamName(team.getName());
    }

    public void setTeam(ServerPlayer player, @Nullable GameTeam team) {
        ensureTeams();
        Scoreboard scoreboard = server.getScoreboard();
        // Remove from any of our teams first.
        Team current = player.getTeam();
        if (current instanceof PlayerTeam pt && GameTeam.byTeamName(pt.getName()) != null) {
            scoreboard.removePlayerFromTeam(player.getScoreboardName(), pt);
        }
        if (team != null) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), scoreboard.getPlayerTeam(team.getTeamName()));
        }
    }

    public List<ServerPlayer> playersOf(GameTeam team) {
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (teamOf(player) == team) {
                result.add(player);
            }
        }
        return result;
    }

    /** Online red and blue players (the active participants). */
    public List<ServerPlayer> participants() {
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            GameTeam team = teamOf(player);
            if (team == GameTeam.RED || team == GameTeam.BLUE) {
                result.add(player);
            }
        }
        return result;
    }

    /**
     * Auto-assigns red/blue by the configured ratio. Without {@code force} only players
     * not already on a game team are shuffled; staff members are never reassigned.
     *
     * @return the number of players assigned.
     */
    public int autoAssign(boolean force) {
        ensureTeams();
        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            GameTeam team = teamOf(player);
            if (team == GameTeam.STAFF) {
                continue;
            }
            if (force || team == null) {
                candidates.add(player);
            }
        }
        Collections.shuffle(candidates, random);

        List<GameTeam> pattern = new ArrayList<>();
        for (int i = 0; i < Math.max(1, config.teamRatioRed); i++) {
            pattern.add(GameTeam.RED);
        }
        for (int i = 0; i < Math.max(1, config.teamRatioBlue); i++) {
            pattern.add(GameTeam.BLUE);
        }
        for (int i = 0; i < candidates.size(); i++) {
            setTeam(candidates.get(i), pattern.get(i % pattern.size()));
        }
        return candidates.size();
    }

    /** Swaps online red and blue members. */
    public void swapTeams() {
        List<ServerPlayer> reds = playersOf(GameTeam.RED);
        List<ServerPlayer> blues = playersOf(GameTeam.BLUE);
        for (ServerPlayer player : reds) {
            setTeam(player, GameTeam.BLUE);
        }
        for (ServerPlayer player : blues) {
            setTeam(player, GameTeam.RED);
        }
    }

    /** Removes all online players from the game teams. */
    public void clearTeams() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (teamOf(player) != null) {
                setTeam(player, null);
            }
        }
    }

    // ------------------------------------------------------------------
    // Game lifecycle
    // ------------------------------------------------------------------

    /** @return null on success, or an error message. */
    @Nullable
    public Component start() {
        if (phase == GamePhase.RUNNING) {
            return Component.translatable("bpg.error.already_running");
        }
        if (!config.isReady()) {
            return Component.translatable("bpg.error.config_incomplete");
        }
        ensureTeams();
        applyGameRules();

        phase = GamePhase.RUNNING;
        blueDeployed = false;
        startTick = server.getTickCount();
        jailIndex = 0;
        jailed.clear();
        resetObjectives();
        detectorCooldown.clear();
        speedCooldown.clear();
        blueRespawnAt.clear();
        actionbarNotes.clear();
        clearGlows();
        clearCobwebs();

        bossBar = new ServerBossEvent(Component.translatable("bpg.bossbar.survivors"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            bossBar.addPlayer(player);
        }

        // Staff become spectators.
        for (ServerPlayer player : playersOf(GameTeam.STAFF)) {
            applyTeamState(player, GameTeam.STAFF);
        }
        // Red deploys immediately.
        for (ServerPlayer player : playersOf(GameTeam.RED)) {
            applyTeamState(player, GameTeam.RED);
            teleport(player, config.redDeploy, config.redDeployRadius);
            equip(player, GameTeam.RED);
            showTitle(player, Component.translatable("bpg.title.start").withStyle(ChatFormatting.RED),
                    Component.translatable("bpg.title.run"));
        }
        // Blue waits as spectator until the deploy delay elapses.
        for (ServerPlayer player : playersOf(GameTeam.BLUE)) {
            player.setGameMode(GameType.SPECTATOR);
            teleport(player, config.blueDeploy, 0);
            showTitle(player, Component.translatable("bpg.title.waiting").withStyle(ChatFormatting.BLUE),
                    Component.translatable("bpg.title.deploy_in", config.blueDeployDelaySeconds));
        }
        // Game-start sound for everyone (anvil place).
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.playNotifySound(SoundEvents.ANVIL_PLACE, SoundSource.MASTER, 1.0F, 1.0F);
        }
        updateBossBar();
        saveState();
        BowlingPlayerGameConstants.LOG.info("Kaidoro game started");
        return null;
    }

    private void deployBlue() {
        blueDeployed = true;
        for (ServerPlayer player : playersOf(GameTeam.BLUE)) {
            applyTeamState(player, GameTeam.BLUE);
            teleport(player, config.blueDeploy, 0);
            equip(player, GameTeam.BLUE);
            showTitle(player, Component.translatable("bpg.title.start").withStyle(ChatFormatting.BLUE),
                    Component.translatable("bpg.title.chase"));
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (teamOf(player) != GameTeam.BLUE) {
                showTitle(player, Component.translatable("bpg.title.blue_deployed").withStyle(ChatFormatting.AQUA), null);
            }
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0F, 1.0F);
        }
        broadcast(Component.translatable("bpg.message.blue_deployed").withStyle(ChatFormatting.AQUA));
        saveState();
    }

    /** @return null on success, or an error message. */
    @Nullable
    public Component stop() {
        if (phase == GamePhase.IDLE) {
            return Component.translatable("bpg.error.not_running");
        }
        resetToIdle();
        broadcast(Component.translatable("bpg.message.stopped").withStyle(ChatFormatting.GRAY));
        return null;
    }

    private void resetToIdle() {
        phase = GamePhase.IDLE;
        deleteState();
        blueDeployed = false;
        jailed.clear();
        blueRespawnAt.clear();
        actionbarNotes.clear();
        clearGlows();
        clearCobwebs();
        clearSidebar();
        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar.setVisible(false);
            bossBar = null;
        }
        // Reset bowling modes so collisions stop.
        for (ServerPlayer player : participants()) {
            ((BowlingPlayerStateAccess) player).bowlingPlayer$setMode(BowlingPlayerMode.NORMAL);
            player.refreshDimensions();
        }
    }

    private void endGame(GameTeam winner) {
        phase = GamePhase.ENDED;
        endTick = server.getTickCount();
        Component result = winner == GameTeam.RED
                ? Component.translatable("bpg.title.red_wins").withStyle(ChatFormatting.RED)
                : Component.translatable("bpg.title.blue_wins").withStyle(ChatFormatting.BLUE);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            showTitle(player, Component.translatable("bpg.title.end").withStyle(ChatFormatting.GOLD), result);
            player.playNotifySound(SoundEvents.ANVIL_USE, SoundSource.MASTER, 1.0F, 1.0F);
            // Everyone watches the result as a spectator.
            player.setGameMode(GameType.SPECTATOR);
            ((BowlingPlayerStateAccess) player).bowlingPlayer$setMode(BowlingPlayerMode.NORMAL);
            player.refreshDimensions();
        }
        broadcast(Component.translatable("bpg.header.results").withStyle(ChatFormatting.GOLD));
        broadcast(result);
        broadcastScores();
        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar.setVisible(false);
            bossBar = null;
        }
        clearGlows();
        clearCobwebs();
        deleteState();
        // Show the scoreboard sidebar now (alternates between scores afterwards via tick).
        server.getScoreboard().setDisplayObjective(Scoreboard.DISPLAY_SLOT_SIDEBAR, objective(OBJ_KILLS, "bpg.score.kills_blue"));
        BowlingPlayerGameConstants.LOG.info("Kaidoro game ended, winner={}", winner);
    }

    private void applyGameRules() {
        server.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, server);
        server.getGameRules().getRule(GameRules.RULE_FALL_DAMAGE).set(false, server);
        server.getGameRules().getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(true, server);
    }

    private void applyTeamState(ServerPlayer player, GameTeam team) {
        player.setGameMode(team.getGameType());
        BowlingPlayerStateAccess state = (BowlingPlayerStateAccess) player;
        state.bowlingPlayer$setMode(team.getMode());
        if (team == GameTeam.RED) {
            state.bowlingPlayer$setPinSize(config.pinSize);
        } else if (team == GameTeam.BLUE) {
            state.bowlingPlayer$setBallSize(config.ballSize);
            state.bowlingPlayer$setPinContactDamage(config.contactDamage);
        }
        player.refreshDimensions();
    }

    private void equip(ServerPlayer player, GameTeam team) {
        GameConfig.TeamLoadout loadout = config.loadoutOf(team);
        Inventory inv = player.getInventory();
        inv.clearContent();
        if (loadout.steak > 0) {
            inv.setItem(SLOT_STEAK, GameItems.steak(loadout.steak));
        }
        if (loadout.cobwebs > 0) {
            inv.setItem(SLOT_COBWEB, GameItems.cobweb(loadout.cobwebs));
        }
        if (loadout.detector) {
            inv.setItem(SLOT_DETECTOR, GameItems.detector());
        }
        if (loadout.speed) {
            inv.setItem(SLOT_SPEED, GameItems.speed());
        }
        // Extra configurable items (extensible kit), placed in free slots.
        for (GameConfig.ItemEntry entry : loadout.extraItems) {
            if (entry == null || entry.item == null || entry.count <= 0) {
                continue;
            }
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(entry.item);
            if (id == null) {
                continue;
            }
            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) {
                inv.add(new ItemStack(item, entry.count));
            }
        }
        inv.setChanged();
    }

    private void giveCobwebs(ServerPlayer player, GameTeam team) {
        int cobwebs = config.loadoutOf(team).cobwebs;
        if (cobwebs > 0) {
            player.getInventory().setItem(SLOT_COBWEB, GameItems.cobweb(cobwebs));
            player.getInventory().setChanged();
        }
    }

    // ------------------------------------------------------------------
    // Per tick
    // ------------------------------------------------------------------

    public void tick() {
        if (pendingRestore != null) {
            GameState restore = pendingRestore;
            pendingRestore = null;
            applyRestore(restore);
        }
        if (phase == GamePhase.ENDED) {
            tickEndedSidebar();
            return;
        }
        if (phase != GamePhase.RUNNING) {
            return;
        }
        int now = server.getTickCount();
        int elapsed = now - startTick;

        if (elapsed % STATE_SAVE_INTERVAL == 0) {
            saveState();
        }

        if (!blueDeployed) {
            if (elapsed >= config.blueDeployDelayTicks()) {
                deployBlue();
            } else {
                tickDeployCountdown(elapsed);
            }
        }

        removeExpiredCobwebs(now);
        enforceItemLock();
        tickGlows();
        handleBlueRespawns(now);

        // Compose the hotbar a few times a second so it stays solid and counts down smoothly.
        if (elapsed % 5 == 0) {
            updateActionbar();
        }
        if (elapsed % 20 == 0) {
            updateBossBar();
        }

        // Win checks.
        List<ServerPlayer> reds = playersOf(GameTeam.RED);
        if (!reds.isEmpty() && allJailed(reds)) {
            endGame(GameTeam.BLUE);
        } else if (elapsed >= config.timeLimitTicks()) {
            endGame(GameTeam.RED);
        }
    }

    private boolean allJailed(List<ServerPlayer> reds) {
        for (ServerPlayer player : reds) {
            if (!jailed.contains(player.getUUID())) {
                return false;
            }
        }
        return true;
    }

    private void handleBlueRespawns(int now) {
        if (blueRespawnAt.isEmpty()) {
            return;
        }
        List<UUID> ready = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : blueRespawnAt.entrySet()) {
            if (now >= entry.getValue()) {
                ready.add(entry.getKey());
            }
        }
        for (UUID uuid : ready) {
            blueRespawnAt.remove(uuid);
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null && teamOf(player) == GameTeam.BLUE) {
                applyTeamState(player, GameTeam.BLUE);
                teleport(player, config.blueDeploy, 0);
                giveCobwebs(player, GameTeam.BLUE);
                notify(player, Component.translatable("bpg.message.respawned").withStyle(ChatFormatting.BLUE), 3);
            }
        }
    }

    private void updateBossBar() {
        if (bossBar == null) {
            return;
        }
        List<ServerPlayer> reds = playersOf(GameTeam.RED);
        int total = reds.size();
        int alive = 0;
        for (ServerPlayer player : reds) {
            if (!jailed.contains(player.getUUID())) {
                alive++;
            }
        }
        bossBar.setName(Component.translatable("bpg.bossbar.count", alive, total));
        bossBar.setProgress(total == 0 ? 0f : (float) alive / total);
    }

    /** Sets a transient note appended to a player's hotbar action bar for {@code seconds}. */
    private void notify(ServerPlayer player, Component text, int seconds) {
        actionbarNotes.put(player.getUUID(), new ActionbarNote(text, server.getTickCount() + seconds * 20));
    }

    /**
     * Composes the whole hotbar action bar in one message so the pieces never wipe each other:
     * "残り MM:SS ｜ 復帰まで N秒 ｜ &lt;通知&gt;". Sent frequently so it stays solid.
     */
    private void updateActionbar() {
        int now = server.getTickCount();
        int elapsed = now - startTick;
        int remaining = Math.max(0, config.timeLimitSeconds - elapsed / 20);
        MutableComponent time = Component.translatable("bpg.actionbar.remaining", String.format("%02d:%02d", remaining / 60, remaining % 60))
                .withStyle(remaining <= 60 ? ChatFormatting.RED : ChatFormatting.WHITE);
        Component separator = Component.literal("  ｜  ").withStyle(ChatFormatting.DARK_GRAY);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            MutableComponent line = time.copy();

            // Per-player deploy / respawn countdown.
            if (teamOf(player) == GameTeam.BLUE) {
                int countdownTicks = -1;
                String countdownKey = null;
                if (!blueDeployed) {
                    countdownTicks = config.blueDeployDelayTicks() - elapsed;
                    countdownKey = "bpg.actionbar.deploy_in";
                } else {
                    Integer respawnAt = blueRespawnAt.get(player.getUUID());
                    if (respawnAt != null) {
                        countdownTicks = respawnAt - now;
                        countdownKey = "bpg.actionbar.respawn_in";
                    }
                }
                if (countdownKey != null && countdownTicks > 0) {
                    int secs = (int) Math.ceil(countdownTicks / 20.0);
                    line.append(separator).append(Component.translatable(countdownKey, secs).withStyle(ChatFormatting.AQUA));
                }
            }

            // Transient note (capture / rescue / detector / cooldown ...).
            ActionbarNote note = actionbarNotes.get(player.getUUID());
            if (note != null) {
                if (now < note.expireTick) {
                    line.append(separator).append(note.text);
                } else {
                    actionbarNotes.remove(player.getUUID());
                }
            }

            player.displayClientMessage(line, true);
        }
    }

    /** Counts down (title + sound) the remaining seconds until the blue team deploys. */
    private void tickDeployCountdown(int elapsed) {
        if (elapsed % 20 != 0) {
            return;
        }
        int remainingSeconds = (int) Math.ceil((config.blueDeployDelayTicks() - elapsed) / 20.0);
        if (remainingSeconds <= 0 || remainingSeconds > 10) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 22, 4));
            player.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.translatable("bpg.title.deploy_countdown").withStyle(ChatFormatting.AQUA)));
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal(String.valueOf(remainingSeconds)).withStyle(ChatFormatting.YELLOW)));
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 0.7F, 1.0F);
        }
    }

    /**
     * Keeps the distributed items pinned to their fixed hotbar slots so participants
     * cannot rearrange or move them ("配布アイテムのみ移動禁止"). Other slots are unaffected.
     */
    private void enforceItemLock() {
        for (ServerPlayer player : participants()) {
            if (player.isSpectator()) {
                continue;
            }
            Inventory inv = player.getInventory();
            pinSlot(inv, SLOT_STEAK, stack -> stack.is(Items.COOKED_BEEF));
            pinSlot(inv, SLOT_COBWEB, stack -> GameItems.idOf(stack).equals(GameItems.ID_COBWEB));
            pinSlot(inv, SLOT_DETECTOR, stack -> GameItems.idOf(stack).equals(GameItems.ID_DETECTOR));
            pinSlot(inv, SLOT_SPEED, stack -> GameItems.idOf(stack).equals(GameItems.ID_SPEED));
        }
    }

    private void pinSlot(Inventory inv, int slot, Predicate<ItemStack> matcher) {
        if (matcher.test(inv.getItem(slot))) {
            return;
        }
        int size = inv.getContainerSize();
        for (int j = 0; j < size; j++) {
            if (j == slot) {
                continue;
            }
            if (matcher.test(inv.getItem(j))) {
                ItemStack moved = inv.getItem(slot);
                inv.setItem(slot, inv.getItem(j));
                inv.setItem(j, moved);
                inv.setChanged();
                return;
            }
        }
    }

    // ------------------------------------------------------------------
    // Event handlers (called from loader glue)
    // ------------------------------------------------------------------

    public void onDeath(ServerPlayer victim, DamageSource source) {
        if (phase != GamePhase.RUNNING) {
            return;
        }
        GameTeam team = teamOf(victim);
        if (team == GameTeam.RED) {
            jailed.add(victim.getUUID());
            Entity killer = source.getEntity();
            if (killer instanceof ServerPlayer killerPlayer && teamOf(killerPlayer) == GameTeam.BLUE) {
                addScore(OBJ_KILLS, "bpg.score.kills_blue", killerPlayer, 1);
                notify(killerPlayer,
                        Component.translatable("bpg.message.capture", scoreOf(OBJ_KILLS, killerPlayer)).withStyle(ChatFormatting.GREEN), 3);
            }
        } else if (team == GameTeam.BLUE) {
            blueRespawnAt.put(victim.getUUID(), server.getTickCount() + config.blueSpectatorSeconds * 20);
        }
        saveState();
    }

    /** Called when a player joins; re-attaches them to a running round (bossbar + mode). */
    public void onPlayerJoin(ServerPlayer player) {
        if (phase != GamePhase.RUNNING) {
            return;
        }
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
        GameTeam team = teamOf(player);
        if (team == GameTeam.RED || team == GameTeam.BLUE) {
            ((BowlingPlayerStateAccess) player).bowlingPlayer$setMode(team.getMode());
            player.refreshDimensions();
        }
    }

    public void onRespawn(ServerPlayer player) {
        if (phase != GamePhase.RUNNING) {
            return;
        }
        GameTeam team = teamOf(player);
        if (team == GameTeam.RED) {
            applyTeamState(player, GameTeam.RED);
            teleport(player, nextJail(), 0);
            giveCobwebs(player, GameTeam.RED);
            notify(player, Component.translatable("bpg.message.jailed").withStyle(ChatFormatting.RED), 4);
        } else if (team == GameTeam.BLUE) {
            player.setGameMode(GameType.SPECTATOR);
            teleport(player, config.blueDeploy, 0);
            blueRespawnAt.putIfAbsent(player.getUUID(), server.getTickCount() + config.blueSpectatorSeconds * 20);
        } else if (team == GameTeam.STAFF) {
            player.setGameMode(GameType.SPECTATOR);
        }
    }

    /** @return true if the attack should be cancelled. */
    public boolean onAttack(ServerPlayer attacker, Entity target) {
        if (phase != GamePhase.RUNNING) {
            return false;
        }
        GameTeam attackerTeam = teamOf(attacker);
        if (attackerTeam == null) {
            return false;
        }
        if (target instanceof ServerPlayer targetPlayer) {
            GameTeam targetTeam = teamOf(targetPlayer);
            if (attackerTeam == GameTeam.RED && targetTeam == GameTeam.RED) {
                if (!jailed.contains(attacker.getUUID()) && jailed.contains(targetPlayer.getUUID())) {
                    rescue(attacker, targetPlayer);
                }
                return true; // friendly fire is off anyway
            }
            if (attackerTeam == GameTeam.BLUE && targetTeam == GameTeam.BLUE) {
                return true;
            }
        }
        return false;
    }

    private void rescue(ServerPlayer rescuer, ServerPlayer captured) {
        jailed.remove(captured.getUUID());
        captured.teleportTo((ServerLevel) rescuer.level(), rescuer.getX(), rescuer.getY(), rescuer.getZ(),
                rescuer.getYRot(), rescuer.getXRot());
        addScore(OBJ_RESCUES, "bpg.score.rescues_red", rescuer, 1);
        notify(rescuer,
                Component.translatable("bpg.message.rescue", scoreOf(OBJ_RESCUES, rescuer)).withStyle(ChatFormatting.GREEN), 3);
        notify(captured, Component.translatable("bpg.message.rescued").withStyle(ChatFormatting.GREEN), 3);
        updateBossBar();
        saveState();
    }

    /** @return true if the interaction should be cancelled. */
    public boolean onUseItem(ServerPlayer player, InteractionHand hand) {
        if (phase != GamePhase.RUNNING) {
            return false;
        }
        GameTeam team = teamOf(player);
        if (team != GameTeam.RED && team != GameTeam.BLUE) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        String id = GameItems.idOf(stack);
        switch (id) {
            case GameItems.ID_COBWEB:
                placeCobweb(player, stack);
                return true;
            case GameItems.ID_DETECTOR:
                useDetector(player);
                return true;
            case GameItems.ID_SPEED:
                useSpeed(player);
                return true;
            default:
                return false;
        }
    }

    private void placeCobweb(ServerPlayer player, ItemStack stack) {
        ServerLevel level = (ServerLevel) player.level();
        // Ray-trace from the eyes; place in the air block just before the looked-at block.
        net.minecraft.world.phys.HitResult hit = player.pick(5.0, 1.0F, false);
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            notify(player, Component.translatable("bpg.error.no_placement").withStyle(ChatFormatting.GRAY), 2);
            return;
        }
        net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos().relative(blockHit.getDirection());
        net.minecraft.world.level.block.state.BlockState existing = level.getBlockState(pos);
        if (!existing.isAir() && !existing.getCollisionShape(level, pos).isEmpty()) {
            notify(player, Component.translatable("bpg.error.cannot_place").withStyle(ChatFormatting.GRAY), 2);
            return;
        }
        level.setBlockAndUpdate(pos, Blocks.COBWEB.defaultBlockState());
        cobwebs.add(new CobwebEntry(level.dimension(), pos.immutable(), server.getTickCount() + config.cobwebDespawnTicks()));
        stack.shrink(1);
    }

    private void useDetector(ServerPlayer player) {
        int now = server.getTickCount();
        int next = detectorCooldown.getOrDefault(player.getUUID(), 0);
        if (now < next) {
            notify(player, cooldownMessage(next - now), 2);
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        int range = config.detectionRange;
        double rangeSq = (double) range * range;
        int expire = now + config.detectionGlowSeconds * 20;
        int found = 0;
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == player) {
                continue;
            }
            GameTeam otherTeam = teamOf(other);
            if (otherTeam != GameTeam.RED && otherTeam != GameTeam.BLUE) {
                continue;
            }
            if (other.level() == level && other.distanceToSqr(player) <= rangeSq) {
                // Glow is sent only to the detector user (private detection).
                addGlow(player.getUUID(), other.getUUID(), expire);
                found++;
            }
        }
        detectorCooldown.put(player.getUUID(), now + config.itemCooldownTicks());
        notify(player, Component.translatable("bpg.message.detected", range, found).withStyle(ChatFormatting.GOLD), 3);
    }

    private void addGlow(UUID viewer, UUID target, int expireTick) {
        glows.removeIf(glow -> glow.viewer.equals(viewer) && glow.target.equals(target));
        glows.add(new GlowSession(viewer, target, expireTick));
    }

    /** Re-sends per-viewer glow each tick and clears expired ones (private to the detector user). */
    private void tickGlows() {
        if (glows.isEmpty()) {
            return;
        }
        int now = server.getTickCount();
        glows.removeIf(glow -> {
            ServerPlayer viewer = server.getPlayerList().getPlayer(glow.viewer);
            ServerPlayer target = server.getPlayerList().getPlayer(glow.target);
            if (viewer == null || target == null) {
                return true;
            }
            if (now >= glow.expireTick) {
                sendGlowFlags(viewer, target, false);
                return true;
            }
            sendGlowFlags(viewer, target, true);
            return false;
        });
    }

    private void clearGlows() {
        for (GlowSession glow : glows) {
            ServerPlayer viewer = server.getPlayerList().getPlayer(glow.viewer);
            ServerPlayer target = server.getPlayerList().getPlayer(glow.target);
            if (viewer != null && target != null) {
                sendGlowFlags(viewer, target, false);
            }
        }
        glows.clear();
    }

    /** Sends the target's shared flags to one viewer, optionally forcing the glowing bit. */
    private void sendGlowFlags(ServerPlayer viewer, ServerPlayer target, boolean glow) {
        byte flags = target.getEntityData().get(SHARED_FLAGS);
        byte value = glow ? (byte) (flags | GLOWING_FLAG) : flags;
        java.util.List<SynchedEntityData.DataValue<?>> values =
                java.util.List.of(SynchedEntityData.DataValue.create(SHARED_FLAGS, value));
        viewer.connection.send(new ClientboundSetEntityDataPacket(target.getId(), values));
    }

    private void useSpeed(ServerPlayer player) {
        int now = server.getTickCount();
        int next = speedCooldown.getOrDefault(player.getUUID(), 0);
        if (now < next) {
            notify(player, cooldownMessage(next - now), 2);
            return;
        }
        int amplifier = Math.max(0, config.speedLevel - 1);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, config.speedDurationSeconds * 20, amplifier, false, true));
        speedCooldown.put(player.getUUID(), now + config.itemCooldownTicks());
        notify(player, Component.translatable("bpg.message.speed", config.speedDurationSeconds).withStyle(ChatFormatting.YELLOW), 3);
    }

    private Component cooldownMessage(int ticksLeft) {
        return Component.translatable("bpg.message.cooldown", ticksLeft / 20).withStyle(ChatFormatting.GRAY);
    }

    /** True while a participant must not be able to drop items (used by loader drop hooks). */
    public boolean isDropLocked(ServerPlayer player) {
        if (phase != GamePhase.RUNNING || player.isSpectator()) {
            return false;
        }
        GameTeam team = teamOf(player);
        return team == GameTeam.RED || team == GameTeam.BLUE;
    }

    // ------------------------------------------------------------------
    // Cobweb tracking + drop prevention
    // ------------------------------------------------------------------

    private void removeExpiredCobwebs(int now) {
        if (cobwebs.isEmpty()) {
            return;
        }
        cobwebs.removeIf(entry -> {
            if (now < entry.removeTick) {
                return false;
            }
            ServerLevel level = server.getLevel(entry.dimension);
            if (level != null && level.getBlockState(entry.pos).is(Blocks.COBWEB)) {
                level.setBlockAndUpdate(entry.pos, Blocks.AIR.defaultBlockState());
            }
            return true;
        });
    }

    private void clearCobwebs() {
        for (CobwebEntry entry : cobwebs) {
            ServerLevel level = server.getLevel(entry.dimension);
            if (level != null && level.getBlockState(entry.pos).is(Blocks.COBWEB)) {
                level.setBlockAndUpdate(entry.pos, Blocks.AIR.defaultBlockState());
            }
        }
        cobwebs.clear();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private GamePos nextJail() {
        List<GamePos> jails = config.jailPositions;
        if (jails.isEmpty()) {
            return config.redDeploy;
        }
        GamePos pos = jails.get(jailIndex % jails.size());
        jailIndex++;
        return pos;
    }

    private void teleport(ServerPlayer player, @Nullable GamePos pos, double radius) {
        if (pos == null) {
            return;
        }
        ServerLevel level = pos.level(server);
        if (level == null) {
            return;
        }
        double x = pos.x();
        double z = pos.z();
        if (radius > 0) {
            x += (random.nextDouble() * 2 - 1) * radius;
            z += (random.nextDouble() * 2 - 1) * radius;
        }
        player.teleportTo(level, x, pos.y(), z, pos.yaw(), pos.pitch());
    }

    private void showTitle(ServerPlayer player, Component title, @Nullable Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        if (subtitle != null) {
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        }
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    private void broadcast(Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }

    private void broadcastScores() {
        broadcast(Component.translatable("bpg.header.kills_top", SCORE_TOP_N).withStyle(ChatFormatting.BLUE));
        broadcastTop(playersOf(GameTeam.BLUE), OBJ_KILLS);
        broadcast(Component.translatable("bpg.header.rescues_top", SCORE_TOP_N).withStyle(ChatFormatting.RED));
        broadcastTop(playersOf(GameTeam.RED), OBJ_RESCUES);
    }

    private void broadcastTop(List<ServerPlayer> players, String objectiveName) {
        players.stream()
                .sorted(java.util.Comparator.comparingInt((ServerPlayer p) -> scoreOf(objectiveName, p)).reversed())
                .limit(SCORE_TOP_N)
                .forEach(player -> broadcast(Component.literal("  " + player.getName().getString() + ": ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(String.valueOf(scoreOf(objectiveName, player))).withStyle(ChatFormatting.YELLOW))));
    }

    public int killsOf(ServerPlayer player) {
        return scoreOf(OBJ_KILLS, player);
    }

    public int rescuesOf(ServerPlayer player) {
        return scoreOf(OBJ_RESCUES, player);
    }

    private record GlowSession(UUID viewer, UUID target, int expireTick) {
    }

    private record ActionbarNote(Component text, int expireTick) {
    }

    private record CobwebEntry(ResourceKey<Level> dimension, BlockPos pos, int removeTick) {
    }
}
