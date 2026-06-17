package com.github.tacowasa059.bowlingplayer.mixin;

import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerMode;
import com.github.tacowasa059.bowlingplayer.player.BowlingPlayerStateAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique
    private static final double bowlingPlayer$BALL_KILL_SPEED_SQR = 0.16D;
    @Unique
    private static final double bowlingPlayer$BALL_CONTACT_MARGIN = 0.15D;
    @Unique
    private Vec3 bowlingPlayer$attemptedMove = Vec3.ZERO;
    @Unique
    private AABB bowlingPlayer$moveStartBox;

    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void bowlingPlayer$captureMove(MoverType type, Vec3 pos, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof ServerPlayer)) {
            return;
        }
        bowlingPlayer$attemptedMove = pos;
        bowlingPlayer$moveStartBox = self.getBoundingBox();
    }

    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("TAIL"))
    private void bowlingPlayer$onMove(MoverType type, Vec3 pos, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof ServerPlayer ballPlayer)) {
            return;
        }
        bowlingPlayer$tryDamagePins(ballPlayer, bowlingPlayer$moveStartBox, bowlingPlayer$attemptedMove);
    }

    @Unique
    private static void bowlingPlayer$tryDamagePins(ServerPlayer ballPlayer, AABB startBox, Vec3 attemptedMove) {
        if (startBox == null) {
            return;
        }
        BowlingPlayerStateAccess ballState = (BowlingPlayerStateAccess) ballPlayer;
        if (ballState.bowlingPlayer$getMode() != BowlingPlayerMode.BALL) {
            return;
        }
        if (!ballPlayer.isAlive() || ballPlayer.isSpectator()) {
            return;
        }
        if (attemptedMove.horizontalDistanceSqr() < bowlingPlayer$BALL_KILL_SPEED_SQR) {
            return;
        }

        AABB hitBox = startBox.expandTowards(attemptedMove).inflate(bowlingPlayer$BALL_CONTACT_MARGIN);
        List<Player> targets = ballPlayer.level().getEntitiesOfClass(Player.class, hitBox, candidate ->
                candidate != ballPlayer
                        && candidate instanceof ServerPlayer
                        && candidate.isAlive()
                        && !candidate.isSpectator()
                        && candidate.getBoundingBox().inflate(bowlingPlayer$BALL_CONTACT_MARGIN).intersects(hitBox)
        );
        if (targets.isEmpty()) {
            return;
        }

        DamageSource source = ballPlayer.damageSources().playerAttack(ballPlayer);
        Vec3 push = attemptedMove.horizontalDistanceSqr() > 1.0E-7D
                ? new Vec3(attemptedMove.x, 0.0D, attemptedMove.z).normalize().scale(1.1D)
                : Vec3.ZERO;
        float damage = ballState.bowlingPlayer$getPinContactDamage();
        for (Player target : targets) {
            ServerPlayer pinPlayer = (ServerPlayer) target;
            if (!bowlingPlayer$canDamagePin(pinPlayer)) {
                continue;
            }
            pinPlayer.hurtMarked = true;
            pinPlayer.push(push.x, 0.18D, push.z);
            pinPlayer.hurt(source, damage);
        }
    }

    @Unique
    private static boolean bowlingPlayer$canDamagePin(ServerPlayer pinPlayer) {
        BowlingPlayerStateAccess pinState = (BowlingPlayerStateAccess) pinPlayer;
        if (pinState.bowlingPlayer$getMode() != BowlingPlayerMode.PIN) {
            return false;
        }
        if (!pinPlayer.isAlive() || pinPlayer.isSpectator() || pinPlayer.invulnerableTime > 0 || pinPlayer.hurtTime > 0) {
            return false;
        }
        GameType gameType = pinPlayer.gameMode.getGameModeForPlayer();
        return gameType == GameType.SURVIVAL || gameType == GameType.ADVENTURE;
    }
}
