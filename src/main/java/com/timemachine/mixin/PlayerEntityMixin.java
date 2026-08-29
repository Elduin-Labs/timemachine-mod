package com.timemachine.mixin;

import com.timemachine.Era;
import com.timemachine.Feature;

import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The 1.9 attack cooldown. Hunger is next door in {@link ServerPlayerEntityMixin}: the method
 * that drains it is declared on the server player, and {@code PlayerEntity}'s own copy is an
 * empty stub that nothing ever calls.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    /**
     * Before 1.9 every swing hit for full damage, however fast you clicked. The cooldown progress
     * is what scales the damage and draws the bar, so pinning it to 1 restores pre-Combat-Update
     * melee on both sides at once.
     */
    @Inject(method = "getAttackCooldownProgress", at = @At("HEAD"), cancellable = true)
    private void timemachine$noAttackCooldown(float baseTime, CallbackInfoReturnable<Float> cir) {
        if (!Era.has(Feature.ATTACK_COOLDOWN)) {
            cir.setReturnValue(1.0F);
        }
    }
}
