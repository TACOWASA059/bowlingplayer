package com.github.tacowasa059.bowlingplayer.command;

import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerMode;
import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerStateAccess;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public final class BowlingPlayerCommands {
    private BowlingPlayerCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("bowlingplayer")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("mode")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (BowlingPlayerMode mode : BowlingPlayerMode.values()) {
                                                builder.suggest(mode.getSerializedName());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> setMode(
                                                EntityArgument.getPlayers(context, "targets"),
                                                BowlingPlayerMode.byName(StringArgumentType.getString(context, "mode")),
                                                context.getSource()
                                        ))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer player = EntityArgument.getPlayer(context, "target");
                                            BowlingPlayerMode mode = ((BowlingPlayerStateAccess) player).bowlingPlayer$getMode();
                                            context.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + ": " + mode.getSerializedName()), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("size")
                        .then(Commands.literal("ball")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("size", FloatArgumentType.floatArg(0.1F, 20.0F))
                                                .executes(context -> setBallSize(
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        FloatArgumentType.getFloat(context, "size"),
                                                        context.getSource()
                                                ))))
                                .then(Commands.literal("get")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> {
                                                    ServerPlayer player = EntityArgument.getPlayer(context, "target");
                                                    float size = ((BowlingPlayerStateAccess) player).bowlingPlayer$getBallSize();
                                                    context.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + " ball size: " + size), false);
                                                    return 1;
                                                }))))
                        .then(Commands.literal("pin")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("size", FloatArgumentType.floatArg(0.1F, 20.0F))
                                                .executes(context -> setPinSize(
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        FloatArgumentType.getFloat(context, "size"),
                                                        context.getSource()
                                                ))))
                                .then(Commands.literal("get")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> {
                                                    ServerPlayer player = EntityArgument.getPlayer(context, "target");
                                                    float size = ((BowlingPlayerStateAccess) player).bowlingPlayer$getPinSize();
                                                    context.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + " pin size: " + size), false);
                                                    return 1;
                                                })))))
                .then(Commands.literal("restitution")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.01F, 1.2F))
                                        .executes(context -> setRestitution(
                                                EntityArgument.getPlayers(context, "targets"),
                                                FloatArgumentType.getFloat(context, "value"),
                                                context.getSource()
                                        ))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer player = EntityArgument.getPlayer(context, "target");
                                            float value = ((BowlingPlayerStateAccess) player).bowlingPlayer$getRestitutionCoefficient();
                                            context.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + " restitution: " + value), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("speed")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("multiplier", FloatArgumentType.floatArg(0.0F, 5.0F))
                                        .executes(context -> setBallSpeedMultiplier(
                                                EntityArgument.getPlayers(context, "targets"),
                                                FloatArgumentType.getFloat(context, "multiplier"),
                                                context.getSource()
                                        ))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer player = EntityArgument.getPlayer(context, "target");
                                            float value = ((BowlingPlayerStateAccess) player).bowlingPlayer$getBallSpeedMultiplier();
                                            context.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + " speed multiplier: " + value), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("damage")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, 1024.0F))
                                        .executes(context -> setPinContactDamage(
                                                EntityArgument.getPlayers(context, "targets"),
                                                FloatArgumentType.getFloat(context, "value"),
                                                context.getSource()
                                        ))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer player = EntityArgument.getPlayer(context, "target");
                                            float value = ((BowlingPlayerStateAccess) player).bowlingPlayer$getPinContactDamage();
                                            context.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + " contact damage: " + value), false);
                                            return 1;
                                        }))));
        dispatcher.register(root);
    }

    private static int setMode(Collection<ServerPlayer> players, BowlingPlayerMode mode, CommandSourceStack source) {
        for (ServerPlayer player : players) {
            ((BowlingPlayerStateAccess) player).bowlingPlayer$setMode(mode);
            player.refreshDimensions();
        }
        source.sendSuccess(() -> Component.literal("Set " + players.size() + " player(s) to " + mode.getSerializedName()), true);
        return players.size();
    }

    private static int setBallSize(Collection<ServerPlayer> players, float size, CommandSourceStack source) {
        for (ServerPlayer player : players) {
            ((BowlingPlayerStateAccess) player).bowlingPlayer$setBallSize(size);
            player.refreshDimensions();
        }
        source.sendSuccess(() -> Component.literal("Set ball size to " + size + " for " + players.size() + " player(s)"), true);
        return players.size();
    }

    private static int setPinSize(Collection<ServerPlayer> players, float size, CommandSourceStack source) {
        for (ServerPlayer player : players) {
            ((BowlingPlayerStateAccess) player).bowlingPlayer$setPinSize(size);
            player.refreshDimensions();
        }
        source.sendSuccess(() -> Component.literal("Set pin size to " + size + " for " + players.size() + " player(s)"), true);
        return players.size();
    }

    private static int setRestitution(Collection<ServerPlayer> players, float value, CommandSourceStack source) {
        for (ServerPlayer player : players) {
            ((BowlingPlayerStateAccess) player).bowlingPlayer$setRestitutionCoefficient(value);
        }
        source.sendSuccess(() -> Component.literal("Set restitution to " + value + " for " + players.size() + " player(s)"), true);
        return players.size();
    }

    private static int setBallSpeedMultiplier(Collection<ServerPlayer> players, float value, CommandSourceStack source) {
        for (ServerPlayer player : players) {
            ((BowlingPlayerStateAccess) player).bowlingPlayer$setBallSpeedMultiplier(value);
        }
        source.sendSuccess(() -> Component.literal("Set speed multiplier to " + value + " for " + players.size() + " player(s)"), true);
        return players.size();
    }

    private static int setPinContactDamage(Collection<ServerPlayer> players, float value, CommandSourceStack source) {
        for (ServerPlayer player : players) {
            ((BowlingPlayerStateAccess) player).bowlingPlayer$setPinContactDamage(value);
        }
        source.sendSuccess(() -> Component.literal("Set contact damage to " + value + " for " + players.size() + " player(s)"), true);
        return players.size();
    }
}
