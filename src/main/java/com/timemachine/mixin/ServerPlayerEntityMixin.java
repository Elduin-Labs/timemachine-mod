package com.timemachine.mixin;

import com.timemachine.Era;
import com.timemachine.Feature;

import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Before Beta 1.8 there was no hunger.
 *
 * <p>Cancelling the tick stops the bar draining and stops the passive regeneration that arrived
 * with it in the same update — in Beta you healed by eating, which {@link FoodComponentMixin}
 * puts back.
 *
 * <p>This has to sit on {@code ServerPlayerEntity} and not on {@code PlayerEntity}. Both declare
 * {@code tickHunger}, but the one on {@code PlayerEntity} is an empty stub: only the server
 * player's override actually calls {@link HungerManager#update}. An injection into the base class
 * applies cleanly, reports no error, and does nothing at all.
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "tickHunger", at = @At("HEAD"), cancellable = true)
    private void timemachine$freezeHunger(CallbackInfo ci) {
        if (Era.has(Feature.HUNGER)) {
            return;
        }
        HungerManager hunger = ((ServerPlayerEntity) (Object) this).getHungerManager();
        if (hunger.getFoodLevel() < 20) {
            hunger.setFoodLevel(20);
            hunger.setSaturationLevel(5.0F);
        }
        ci.cancel();
    }
}
