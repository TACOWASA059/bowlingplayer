package com.github.tacowasa059.bowlingplayergame.fabric;

import com.github.tacowasa059.bowlingplayergame.BowlingPlayerGameCommon;
import com.github.tacowasa059.bowlingplayergame.command.GameCommands;
import com.github.tacowasa059.bowlingplayergame.game.GameManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;

public final class BowlingPlayerGameFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BowlingPlayerGameCommon.init();

        ServerLifecycleEvents.SERVER_STARTING.register(GameManager::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(GameManager::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(server -> GameManager.get(server).tick());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> GameCommands.register(dispatcher));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                GameManager.get(server).onPlayerJoin(handler.player));

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            MinecraftServer server = newPlayer.getServer();
            if (server != null) {
                GameManager.get(server).onRespawn(newPlayer);
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player && player.getServer() != null) {
                GameManager.get(player.getServer()).onDeath(player, source);
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide && player instanceof ServerPlayer serverPlayer && serverPlayer.getServer() != null) {
                if (GameManager.get(serverPlayer.getServer()).onAttack(serverPlayer, entity)) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide && player instanceof ServerPlayer serverPlayer && serverPlayer.getServer() != null) {
                if (GameManager.get(serverPlayer.getServer()).onUseItem(serverPlayer, hand)) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClientSide && player instanceof ServerPlayer serverPlayer && serverPlayer.getServer() != null) {
                if (GameManager.get(serverPlayer.getServer()).onUseItem(serverPlayer, hand)) {
                    return InteractionResultHolder.success(player.getItemInHand(hand));
                }
            }
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        });
    }
}
