package com.github.tacowasa059.bowlingplayergame.mixin;

import com.github.tacowasa059.bowlingplayergame.game.GameManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hard-blocks item dropping (Q key) for game participants while a round is running.
 * Cancelling at the entry point means the stack is never removed from the inventory,
 * so nothing ever hits the ground. Used by Fabric; Forge uses {@code ItemTossEvent}.
 */
@Mixin(Player.class)
public abstract class PlayerDropMixin {
    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void bowlingPlayerGame$preventDrop(boolean dropEntireStack, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer serverPlayer
                && serverPlayer.getServer() != null
                && GameManager.get(serverPlayer.getServer()).isDropLocked(serverPlayer)) {
            cir.setReturnValue(false);
        }
    }
}
