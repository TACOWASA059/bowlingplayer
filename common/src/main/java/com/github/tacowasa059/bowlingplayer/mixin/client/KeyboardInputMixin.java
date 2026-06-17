package com.github.tacowasa059.bowlingplayer.mixin.client;

import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerMode;
import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerStateAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void bowlingPlayer$disableSneak(boolean slowDown, float amount, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (((BowlingPlayerStateAccess) player).bowlingPlayer$getMode() != BowlingPlayerMode.BALL) {
            return;
        }
        if (player.onGround() && !player.isPassenger()) {
            ((Input) (Object) this).shiftKeyDown = false;
        }
    }
}
