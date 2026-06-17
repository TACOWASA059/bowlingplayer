package com.github.tacowasa059.bowlingplayergame.forge;

import com.github.tacowasa059.bowlingplayergame.BowlingPlayerGameCommon;
import com.github.tacowasa059.bowlingplayergame.BowlingPlayerGameConstants;
import com.github.tacowasa059.bowlingplayergame.command.GameCommands;
import com.github.tacowasa059.bowlingplayergame.game.GameManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod(BowlingPlayerGameConstants.MOD_ID)
public final class BowlingPlayerGameForge {
    public BowlingPlayerGameForge() {
        BowlingPlayerGameCommon.init();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        GameCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        GameManager.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        GameManager.onServerStopping(event.getServer());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            GameManager.get(server).tick();
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            GameManager.get(player.getServer()).onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            GameManager.get(player.getServer()).onRespawn(player);
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            GameManager.get(player.getServer()).onDeath(player, event.getSource());
        }
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            if (GameManager.get(player.getServer()).onAttack(player, event.getTarget())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            if (GameManager.get(player.getServer()).onUseItem(player, event.getHand())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && player.getServer() != null
                && GameManager.get(player.getServer()).isDropLocked(player)) {
            player.getInventory().add(event.getEntity().getItem());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onUseItemOnBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            if (GameManager.get(player.getServer()).onUseItem(player, event.getHand())) {
                event.setCanceled(true);
            }
        }
    }
}
