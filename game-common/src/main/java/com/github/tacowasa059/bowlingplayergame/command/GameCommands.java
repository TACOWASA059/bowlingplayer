package com.github.tacowasa059.bowlingplayergame.command;

import com.github.tacowasa059.bowlingplayergame.config.GameConfig;
import com.github.tacowasa059.bowlingplayergame.game.GameManager;
import com.github.tacowasa059.bowlingplayergame.game.GamePos;
import com.github.tacowasa059.bowlingplayergame.game.GameTeam;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class GameCommands {
    private GameCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("bpg")
                .requires(source -> source.hasPermission(2));

        root.then(buildTeam());
        root.then(buildConfig());
        root.then(Commands.literal("start").executes(ctx -> {
            Component error = GameManager.get(ctx.getSource().getServer()).start();
            if (error != null) {
                ctx.getSource().sendFailure(error);
                return 0;
            }
            ok(ctx, "bpg.command.started");
            return 1;
        }));
        root.then(Commands.literal("stop").executes(ctx -> {
            Component error = GameManager.get(ctx.getSource().getServer()).stop();
            if (error != null) {
                ctx.getSource().sendFailure(error);
                return 0;
            }
            return 1;
        }));
        root.then(Commands.literal("status").executes(ctx -> status(ctx.getSource())));
        root.then(Commands.literal("score").executes(ctx -> score(ctx.getSource())));

        dispatcher.register(root);
    }

    // ------------------------------------------------------------------
    // team
    // ------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> buildTeam() {
        LiteralArgumentBuilder<CommandSourceStack> team = Commands.literal("team");

        team.then(Commands.literal("auto")
                .executes(ctx -> autoAssign(ctx.getSource(), false))
                .then(Commands.literal("force").executes(ctx -> autoAssign(ctx.getSource(), true))));

        team.then(Commands.literal("set")
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(teamSet("red", GameTeam.RED))
                        .then(teamSet("blue", GameTeam.BLUE))
                        .then(teamSet("staff", GameTeam.STAFF))
                        .then(Commands.literal("none").executes(ctx -> {
                            GameManager manager = GameManager.get(ctx.getSource().getServer());
                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                            for (ServerPlayer player : targets) {
                                manager.setTeam(player, null);
                            }
                            ok(ctx, "bpg.command.team_removed", targets.size());
                            return targets.size();
                        }))));

        team.then(Commands.literal("swap").executes(ctx -> {
            GameManager.get(ctx.getSource().getServer()).swapTeams();
            ok(ctx, "bpg.command.teams_swapped");
            return 1;
        }));

        team.then(Commands.literal("clear").executes(ctx -> {
            GameManager.get(ctx.getSource().getServer()).clearTeams();
            ok(ctx, "bpg.command.teams_cleared");
            return 1;
        }));

        return team;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> teamSet(String literal, GameTeam target) {
        return Commands.literal(literal).executes(ctx -> {
            GameManager manager = GameManager.get(ctx.getSource().getServer());
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
            for (ServerPlayer player : targets) {
                manager.setTeam(player, target);
            }
            feedback(ctx.getSource(), Component.translatable("bpg.command.team_set", targets.size(), target.getDisplayName())
                    .withStyle(ChatFormatting.GREEN));
            return targets.size();
        });
    }

    private static int autoAssign(CommandSourceStack source, boolean force) {
        int count = GameManager.get(source.getServer()).autoAssign(force);
        feedback(source, Component.translatable("bpg.command.team_auto", count).withStyle(ChatFormatting.GREEN));
        return count;
    }

    // ------------------------------------------------------------------
    // config
    // ------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> buildConfig() {
        LiteralArgumentBuilder<CommandSourceStack> config = Commands.literal("config");

        config.then(Commands.literal("show").executes(ctx -> showConfig(ctx.getSource())));

        config.then(intSetting("timelimit", 1, 24 * 60 * 60, (cfg, v) -> cfg.timeLimitSeconds = v));
        config.then(intSetting("bluedelay", 0, 600, (cfg, v) -> cfg.blueDeployDelaySeconds = v));
        config.then(intSetting("itemcooldown", 0, 3600, (cfg, v) -> cfg.itemCooldownSeconds = v));
        config.then(intSetting("speedduration", 1, 120, (cfg, v) -> cfg.speedDurationSeconds = v));
        config.then(intSetting("speedlevel", 1, 10, (cfg, v) -> cfg.speedLevel = v));
        config.then(intSetting("detectrange", 1, 256, (cfg, v) -> cfg.detectionRange = v));
        config.then(intSetting("cobwebdespawn", 1, 600, (cfg, v) -> cfg.cobwebDespawnSeconds = v));
        config.then(intSetting("bluespectator", 0, 600, (cfg, v) -> cfg.blueSpectatorSeconds = v));
        config.then(floatSetting("ballsize", 0.1F, 20.0F, (cfg, v) -> cfg.ballSize = v));
        config.then(floatSetting("pinsize", 0.1F, 20.0F, (cfg, v) -> cfg.pinSize = v));
        config.then(floatSetting("damage", 0.0F, 1024.0F, (cfg, v) -> cfg.contactDamage = v));

        config.then(Commands.literal("items")
                .then(itemTeam("red", GameTeam.RED))
                .then(itemTeam("blue", GameTeam.BLUE)));

        config.then(Commands.literal("ratio")
                .then(Commands.argument("red", IntegerArgumentType.integer(1, 100))
                        .then(Commands.argument("blue", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> {
                                    GameManager manager = GameManager.get(ctx.getSource().getServer());
                                    int red = IntegerArgumentType.getInteger(ctx, "red");
                                    int blue = IntegerArgumentType.getInteger(ctx, "blue");
                                    manager.config().teamRatioRed = red;
                                    manager.config().teamRatioBlue = blue;
                                    manager.saveConfig();
                                    ok(ctx, "bpg.command.ratio_set", red, blue);
                                    return 1;
                                }))));

        config.then(Commands.literal("reddeploy")
                .executes(ctx -> {
                    setRedDeploy(ctx.getSource(), 0);
                    return 1;
                })
                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0, 256))
                        .executes(ctx -> {
                            setRedDeploy(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "radius"));
                            return 1;
                        })));

        config.then(Commands.literal("bluedeploy").executes(ctx -> {
            GameManager manager = GameManager.get(ctx.getSource().getServer());
            manager.config().blueDeploy = GamePos.fromSource(ctx.getSource());
            manager.saveConfig();
            ok(ctx, "bpg.command.blue_deploy_set");
            return 1;
        }));

        config.then(Commands.literal("jail")
                .then(Commands.literal("add").executes(ctx -> {
                    GameManager manager = GameManager.get(ctx.getSource().getServer());
                    manager.config().jailPositions.add(GamePos.fromSource(ctx.getSource()));
                    manager.saveConfig();
                    int count = manager.config().jailPositions.size();
                    ok(ctx, "bpg.command.jail_added", count);
                    return 1;
                }))
                .then(Commands.literal("clear").executes(ctx -> {
                    GameManager manager = GameManager.get(ctx.getSource().getServer());
                    manager.config().jailPositions.clear();
                    manager.saveConfig();
                    ok(ctx, "bpg.command.jails_cleared");
                    return 1;
                })));

        return config;
    }

    private static void setRedDeploy(CommandSourceStack source, double radius) {
        GameManager manager = GameManager.get(source.getServer());
        manager.config().redDeploy = GamePos.fromSource(source);
        manager.config().redDeployRadius = radius;
        manager.saveConfig();
        feedback(source, Component.translatable("bpg.command.red_deploy_set", radius).withStyle(ChatFormatting.GREEN));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> intSetting(String name, int min, int max, BiConsumer<GameConfig, Integer> setter) {
        return Commands.literal(name).then(Commands.argument("value", IntegerArgumentType.integer(min, max))
                .executes(ctx -> {
                    GameManager manager = GameManager.get(ctx.getSource().getServer());
                    int value = IntegerArgumentType.getInteger(ctx, "value");
                    setter.accept(manager.config(), value);
                    manager.saveConfig();
                    ok(ctx, "bpg.command.setting_set", name, value);
                    return 1;
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> floatSetting(String name, float min, float max, BiConsumer<GameConfig, Float> setter) {
        return Commands.literal(name).then(Commands.argument("value", FloatArgumentType.floatArg(min, max))
                .executes(ctx -> {
                    GameManager manager = GameManager.get(ctx.getSource().getServer());
                    float value = FloatArgumentType.getFloat(ctx, "value");
                    setter.accept(manager.config(), value);
                    manager.saveConfig();
                    ok(ctx, "bpg.command.setting_set", name, value);
                    return 1;
                }));
    }

    // ------------------------------------------------------------------
    // config items <red|blue> ...
    // ------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> itemTeam(String literal, GameTeam team) {
        return Commands.literal(literal)
                .then(Commands.literal("steak").then(Commands.argument("count", IntegerArgumentType.integer(0, 64))
                        .executes(ctx -> setLoadout(ctx, team, "bpg.loadout.steak",
                                lo -> lo.steak = IntegerArgumentType.getInteger(ctx, "count")))))
                .then(Commands.literal("cobwebs").then(Commands.argument("count", IntegerArgumentType.integer(0, 64))
                        .executes(ctx -> setLoadout(ctx, team, "bpg.loadout.cobwebs",
                                lo -> lo.cobwebs = IntegerArgumentType.getInteger(ctx, "count")))))
                .then(Commands.literal("detector").then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setLoadout(ctx, team, "bpg.loadout.detector",
                                lo -> lo.detector = BoolArgumentType.getBool(ctx, "enabled")))))
                .then(Commands.literal("speed").then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setLoadout(ctx, team, "bpg.loadout.speed",
                                lo -> lo.speed = BoolArgumentType.getBool(ctx, "enabled")))))
                // Extensible kit: add/remove arbitrary vanilla items.
                .then(Commands.literal("add")
                        .then(Commands.argument("item", StringArgumentType.string())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        BuiltInRegistries.ITEM.keySet().stream().map(ResourceLocation::toString), b))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> addExtraItem(ctx, team)))))
                .then(Commands.literal("removeextra")
                        .then(Commands.argument("index", IntegerArgumentType.integer(0, 1000))
                                .executes(ctx -> removeExtraItem(ctx, team))))
                .then(Commands.literal("clearextra").executes(ctx -> {
                    GameManager manager = GameManager.get(ctx.getSource().getServer());
                    manager.config().loadoutOf(team).extraItems.clear();
                    manager.saveConfig();
                    ok(ctx, "bpg.command.extras_cleared", team.getDisplayName());
                    return 1;
                }));
    }

    private static int setLoadout(CommandContext<CommandSourceStack> ctx, GameTeam team, String label,
                                  Consumer<GameConfig.TeamLoadout> mutator) {
        GameManager manager = GameManager.get(ctx.getSource().getServer());
        mutator.accept(manager.config().loadoutOf(team));
        manager.saveConfig();
        feedback(ctx.getSource(), Component.translatable("bpg.command.loadout_set", team.getDisplayName(), Component.translatable(label))
                .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int addExtraItem(CommandContext<CommandSourceStack> ctx, GameTeam team) {
        String id = StringArgumentType.getString(ctx, "item");
        int count = IntegerArgumentType.getInteger(ctx, "count");
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || BuiltInRegistries.ITEM.getOptional(key).isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("bpg.error.unknown_item", id));
            return 0;
        }
        GameManager manager = GameManager.get(ctx.getSource().getServer());
        manager.config().loadoutOf(team).extraItems.add(new GameConfig.ItemEntry(key.toString(), count));
        manager.saveConfig();
        feedback(ctx.getSource(), Component.translatable("bpg.command.extra_added", team.getDisplayName(), key, count)
                .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int removeExtraItem(CommandContext<CommandSourceStack> ctx, GameTeam team) {
        int index = IntegerArgumentType.getInteger(ctx, "index");
        GameManager manager = GameManager.get(ctx.getSource().getServer());
        List<GameConfig.ItemEntry> items = manager.config().loadoutOf(team).extraItems;
        if (index < 0 || index >= items.size()) {
            ctx.getSource().sendFailure(Component.translatable("bpg.error.index_out_of_range", items.size() - 1));
            return 0;
        }
        GameConfig.ItemEntry removed = items.remove(index);
        manager.saveConfig();
        ok(ctx, "bpg.command.extra_removed", removed.item, removed.count);
        return 1;
    }

    // ------------------------------------------------------------------
    // status / score / show
    // ------------------------------------------------------------------

    private static int showConfig(CommandSourceStack source) {
        GameConfig cfg = GameManager.get(source.getServer()).config();
        header(source, "bpg.header.config");
        line(source, "bpg.config.time_limit", cfg.timeLimitSeconds);
        line(source, "bpg.config.ratio", cfg.teamRatioRed + ":" + cfg.teamRatioBlue);
        line(source, "bpg.config.blue_delay", cfg.blueDeployDelaySeconds);
        line(source, "bpg.config.item_cooldown", cfg.itemCooldownSeconds);
        line(source, "bpg.config.speed_duration", cfg.speedDurationSeconds);
        line(source, "bpg.config.speed_level", cfg.speedLevel);
        line(source, "bpg.config.detect_range", cfg.detectionRange);
        line(source, "bpg.config.cobweb_despawn", cfg.cobwebDespawnSeconds);
        line(source, "bpg.config.blue_spectator", cfg.blueSpectatorSeconds);
        line(source, "bpg.config.ball_size", cfg.ballSize);
        line(source, "bpg.config.pin_size", cfg.pinSize);
        line(source, "bpg.config.damage", cfg.contactDamage);
        line(source, "bpg.config.red_items", loadoutText(cfg.redLoadout));
        line(source, "bpg.config.blue_items", loadoutText(cfg.blueLoadout));
        line(source, "bpg.config.red_deploy", cfg.redDeploy == null ? Component.translatable("bpg.config.unset") : cfg.redDeploy + " r=" + cfg.redDeployRadius);
        line(source, "bpg.config.blue_deploy", cfg.blueDeploy == null ? Component.translatable("bpg.config.unset") : cfg.blueDeploy.toString());
        line(source, "bpg.config.jail_count", cfg.jailPositions.size());
        return 1;
    }

    private static int status(CommandSourceStack source) {
        GameManager manager = GameManager.get(source.getServer());
        feedback(source, Component.translatable("bpg.status.phase",
                Component.translatable("bpg.phase." + manager.phase().name().toLowerCase(java.util.Locale.ROOT)))
                .withStyle(ChatFormatting.GRAY));
        feedback(source, Component.translatable("bpg.status.red", manager.playersOf(GameTeam.RED).size()).withStyle(ChatFormatting.RED));
        feedback(source, Component.translatable("bpg.status.blue", manager.playersOf(GameTeam.BLUE).size()).withStyle(ChatFormatting.BLUE));
        feedback(source, Component.translatable("bpg.status.staff", manager.playersOf(GameTeam.STAFF).size()).withStyle(ChatFormatting.AQUA));
        return 1;
    }

    private static int score(CommandSourceStack source) {
        GameManager manager = GameManager.get(source.getServer());
        header(source, "bpg.header.score");
        feedback(source, Component.translatable("bpg.score.blue_kills").withStyle(ChatFormatting.BLUE));
        for (ServerPlayer player : manager.playersOf(GameTeam.BLUE)) {
            scoreLine(source, player.getName().getString(), manager.killsOf(player));
        }
        feedback(source, Component.translatable("bpg.score.red_rescues").withStyle(ChatFormatting.RED));
        for (ServerPlayer player : manager.playersOf(GameTeam.RED)) {
            scoreLine(source, player.getName().getString(), manager.rescuesOf(player));
        }
        return 1;
    }

    // ------------------------------------------------------------------
    // formatting helpers
    // ------------------------------------------------------------------

    /**
     * Sends feedback to the command sender ONLY. For a player source it is delivered with a
     * direct system message, so it is never duplicated to other ops nor suppressed by the
     * {@code sendCommandFeedback} gamerule. Console / command-block sources fall back to
     * normal (non-broadcasting) command feedback.
     */
    private static void feedback(CommandSourceStack source, Component message) {
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            player.sendSystemMessage(message);
        } else {
            source.sendSuccess(() -> message, false);
        }
    }

    private static void ok(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        feedback(ctx.getSource(), Component.translatable(key, args).withStyle(ChatFormatting.GREEN));
    }

    private static void header(CommandSourceStack source, String key) {
        feedback(source, Component.translatable(key).withStyle(ChatFormatting.GOLD));
    }

    private static void line(CommandSourceStack source, String labelKey, Object value) {
        feedback(source, Component.translatable("bpg.config.line", Component.translatable(labelKey), value).withStyle(ChatFormatting.GRAY));
    }

    private static void scoreLine(CommandSourceStack source, String name, int value) {
        feedback(source, Component.literal("  " + name + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.YELLOW)));
    }

    private static Component loadoutText(GameConfig.TeamLoadout lo) {
        StringBuilder sb = new StringBuilder();
        if (lo.extraItems != null && !lo.extraItems.isEmpty()) {
            for (int i = 0; i < lo.extraItems.size(); i++) {
                GameConfig.ItemEntry entry = lo.extraItems.get(i);
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(i).append(':').append(entry.item).append('x').append(entry.count);
            }
        }
        return Component.translatable("bpg.config.loadout", lo.steak, lo.cobwebs,
                Component.translatable(lo.detector ? "bpg.config.enabled" : "bpg.config.disabled"),
                Component.translatable(lo.speed ? "bpg.config.enabled" : "bpg.config.disabled"), sb.toString());
    }
}
