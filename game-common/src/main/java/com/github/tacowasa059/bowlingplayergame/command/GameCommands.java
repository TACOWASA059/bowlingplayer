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
            ok(ctx, "ゲームを開始しました");
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
                            ok(ctx, targets.size() + "人をチームから外しました");
                            return targets.size();
                        }))));

        team.then(Commands.literal("swap").executes(ctx -> {
            GameManager.get(ctx.getSource().getServer()).swapTeams();
            ok(ctx, "赤チームと青チームを入れ替えました");
            return 1;
        }));

        team.then(Commands.literal("clear").executes(ctx -> {
            GameManager.get(ctx.getSource().getServer()).clearTeams();
            ok(ctx, "全員のチーム割り当てを解除しました");
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
            feedback(ctx.getSource(), Component.literal(targets.size() + "人を ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(target.getDisplayName()).withStyle(target.getColor()))
                    .append(Component.literal(" に設定しました").withStyle(ChatFormatting.GREEN)));
            return targets.size();
        });
    }

    private static int autoAssign(CommandSourceStack source, boolean force) {
        int count = GameManager.get(source.getServer()).autoAssign(force);
        feedback(source, Component.literal(count + "人をチームに割り当てました").withStyle(ChatFormatting.GREEN));
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
                                    ok(ctx, "チーム比を 赤:青 = " + red + ":" + blue + " に設定しました");
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
            ok(ctx, "青チーム展開位置を現在地に設定しました");
            return 1;
        }));

        config.then(Commands.literal("jail")
                .then(Commands.literal("add").executes(ctx -> {
                    GameManager manager = GameManager.get(ctx.getSource().getServer());
                    manager.config().jailPositions.add(GamePos.fromSource(ctx.getSource()));
                    manager.saveConfig();
                    int count = manager.config().jailPositions.size();
                    ok(ctx, "牢獄位置を追加しました (合計 " + count + ")");
                    return 1;
                }))
                .then(Commands.literal("clear").executes(ctx -> {
                    GameManager manager = GameManager.get(ctx.getSource().getServer());
                    manager.config().jailPositions.clear();
                    manager.saveConfig();
                    ok(ctx, "牢獄位置を全てクリアしました");
                    return 1;
                })));

        return config;
    }

    private static void setRedDeploy(CommandSourceStack source, double radius) {
        GameManager manager = GameManager.get(source.getServer());
        manager.config().redDeploy = GamePos.fromSource(source);
        manager.config().redDeployRadius = radius;
        manager.saveConfig();
        feedback(source, Component.literal("赤チーム展開位置を現在地に設定しました (半径 " + radius + ")").withStyle(ChatFormatting.GREEN));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> intSetting(String name, int min, int max, BiConsumer<GameConfig, Integer> setter) {
        return Commands.literal(name).then(Commands.argument("value", IntegerArgumentType.integer(min, max))
                .executes(ctx -> {
                    GameManager manager = GameManager.get(ctx.getSource().getServer());
                    int value = IntegerArgumentType.getInteger(ctx, "value");
                    setter.accept(manager.config(), value);
                    manager.saveConfig();
                    ok(ctx, name + " = " + value + " に設定しました");
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
                    ok(ctx, name + " = " + value + " に設定しました");
                    return 1;
                }));
    }

    // ------------------------------------------------------------------
    // config items <red|blue> ...
    // ------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> itemTeam(String literal, GameTeam team) {
        return Commands.literal(literal)
                .then(Commands.literal("steak").then(Commands.argument("count", IntegerArgumentType.integer(0, 64))
                        .executes(ctx -> setLoadout(ctx, team, "ステーキ",
                                lo -> lo.steak = IntegerArgumentType.getInteger(ctx, "count")))))
                .then(Commands.literal("cobwebs").then(Commands.argument("count", IntegerArgumentType.integer(0, 64))
                        .executes(ctx -> setLoadout(ctx, team, "クモの巣",
                                lo -> lo.cobwebs = IntegerArgumentType.getInteger(ctx, "count")))))
                .then(Commands.literal("detector").then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setLoadout(ctx, team, "探知機",
                                lo -> lo.detector = BoolArgumentType.getBool(ctx, "enabled")))))
                .then(Commands.literal("speed").then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setLoadout(ctx, team, "加速の羽",
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
                    ok(ctx, team.getDisplayName() + " の追加アイテムを全て消去しました");
                    return 1;
                }));
    }

    private static int setLoadout(CommandContext<CommandSourceStack> ctx, GameTeam team, String label,
                                  Consumer<GameConfig.TeamLoadout> mutator) {
        GameManager manager = GameManager.get(ctx.getSource().getServer());
        mutator.accept(manager.config().loadoutOf(team));
        manager.saveConfig();
        feedback(ctx.getSource(), Component.literal(team.getDisplayName()).withStyle(team.getColor())
                .append(Component.literal(" の " + label + " を更新しました").withStyle(ChatFormatting.GREEN)));
        return 1;
    }

    private static int addExtraItem(CommandContext<CommandSourceStack> ctx, GameTeam team) {
        String id = StringArgumentType.getString(ctx, "item");
        int count = IntegerArgumentType.getInteger(ctx, "count");
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || BuiltInRegistries.ITEM.getOptional(key).isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("不明なアイテム: " + id));
            return 0;
        }
        GameManager manager = GameManager.get(ctx.getSource().getServer());
        manager.config().loadoutOf(team).extraItems.add(new GameConfig.ItemEntry(key.toString(), count));
        manager.saveConfig();
        feedback(ctx.getSource(), Component.literal(team.getDisplayName()).withStyle(team.getColor())
                .append(Component.literal(" に " + key + " x" + count + " を追加しました").withStyle(ChatFormatting.GREEN)));
        return 1;
    }

    private static int removeExtraItem(CommandContext<CommandSourceStack> ctx, GameTeam team) {
        int index = IntegerArgumentType.getInteger(ctx, "index");
        GameManager manager = GameManager.get(ctx.getSource().getServer());
        List<GameConfig.ItemEntry> items = manager.config().loadoutOf(team).extraItems;
        if (index < 0 || index >= items.size()) {
            ctx.getSource().sendFailure(Component.literal("インデックスが範囲外です (0.." + (items.size() - 1) + ")"));
            return 0;
        }
        GameConfig.ItemEntry removed = items.remove(index);
        manager.saveConfig();
        ok(ctx, "追加アイテムを削除: " + removed.item + " x" + removed.count);
        return 1;
    }

    // ------------------------------------------------------------------
    // status / score / show
    // ------------------------------------------------------------------

    private static int showConfig(CommandSourceStack source) {
        GameConfig cfg = GameManager.get(source.getServer()).config();
        header(source, "=== 設定 ===");
        line(source, "制限時間(秒)", cfg.timeLimitSeconds);
        line(source, "チーム比 赤:青", cfg.teamRatioRed + ":" + cfg.teamRatioBlue);
        line(source, "青展開遅延(秒)", cfg.blueDeployDelaySeconds);
        line(source, "アイテムCD(秒)", cfg.itemCooldownSeconds);
        line(source, "加速時間(秒)", cfg.speedDurationSeconds);
        line(source, "加速レベル", cfg.speedLevel);
        line(source, "探知範囲", cfg.detectionRange);
        line(source, "クモの巣消滅(秒)", cfg.cobwebDespawnSeconds);
        line(source, "青スペクテーター(秒)", cfg.blueSpectatorSeconds);
        line(source, "ボールサイズ", cfg.ballSize);
        line(source, "ピンサイズ", cfg.pinSize);
        line(source, "接触ダメージ", cfg.contactDamage);
        line(source, "赤アイテム", loadoutText(cfg.redLoadout));
        line(source, "青アイテム", loadoutText(cfg.blueLoadout));
        line(source, "赤展開", cfg.redDeploy == null ? "未設定" : cfg.redDeploy + " r=" + cfg.redDeployRadius);
        line(source, "青展開", cfg.blueDeploy == null ? "未設定" : cfg.blueDeploy.toString());
        line(source, "牢獄数", cfg.jailPositions.size());
        return 1;
    }

    private static int status(CommandSourceStack source) {
        GameManager manager = GameManager.get(source.getServer());
        feedback(source, Component.literal("状態: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(manager.phase().name()).withStyle(ChatFormatting.YELLOW)));
        feedback(source, Component.literal("赤(Pin): ").withStyle(ChatFormatting.RED)
                .append(Component.literal(String.valueOf(manager.playersOf(GameTeam.RED).size())).withStyle(ChatFormatting.WHITE)));
        feedback(source, Component.literal("青(Ball): ").withStyle(ChatFormatting.BLUE)
                .append(Component.literal(String.valueOf(manager.playersOf(GameTeam.BLUE).size())).withStyle(ChatFormatting.WHITE)));
        feedback(source, Component.literal("運営(Staff): ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(String.valueOf(manager.playersOf(GameTeam.STAFF).size())).withStyle(ChatFormatting.WHITE)));
        return 1;
    }

    private static int score(CommandSourceStack source) {
        GameManager manager = GameManager.get(source.getServer());
        header(source, "=== スコア ===");
        feedback(source, Component.literal("青チーム(Ball) 捕獲数:").withStyle(ChatFormatting.BLUE));
        for (ServerPlayer player : manager.playersOf(GameTeam.BLUE)) {
            scoreLine(source, player.getName().getString(), manager.killsOf(player));
        }
        feedback(source, Component.literal("赤チーム(Pin) 救出数:").withStyle(ChatFormatting.RED));
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

    private static void ok(CommandContext<CommandSourceStack> ctx, String message) {
        feedback(ctx.getSource(), Component.literal(message).withStyle(ChatFormatting.GREEN));
    }

    private static void header(CommandSourceStack source, String text) {
        feedback(source, Component.literal(text).withStyle(ChatFormatting.GOLD));
    }

    private static void line(CommandSourceStack source, String label, Object value) {
        feedback(source, Component.literal(label + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.WHITE)));
    }

    private static void scoreLine(CommandSourceStack source, String name, int value) {
        feedback(source, Component.literal("  " + name + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.YELLOW)));
    }

    private static String loadoutText(GameConfig.TeamLoadout lo) {
        StringBuilder sb = new StringBuilder();
        sb.append("ステーキ").append(lo.steak)
                .append(" クモの巣").append(lo.cobwebs)
                .append(" 探知:").append(lo.detector ? "有" : "無")
                .append(" 加速:").append(lo.speed ? "有" : "無");
        if (lo.extraItems != null && !lo.extraItems.isEmpty()) {
            sb.append(" 追加[");
            for (int i = 0; i < lo.extraItems.size(); i++) {
                GameConfig.ItemEntry entry = lo.extraItems.get(i);
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(i).append(':').append(entry.item).append('x').append(entry.count);
            }
            sb.append(']');
        }
        return sb.toString();
    }
}
