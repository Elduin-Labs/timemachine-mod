package com.timemachine.mixin;

import com.timemachine.Era;
import com.timemachine.Feature;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Two ways of moving that had to be invented: sprinting (Beta 1.8) and swimming (1.13).
 *
 * <p>Both are a single flag, and both are set from the client's own movement code every tick, so
 * refusing to let the flag turn on is enough to take the ability away — the speed bonus, the
 * pose, the particles and the field of view all hang off it.
 *
 * <p>Only players are affected. A dolphin that could not swim would be a different mod.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void timemachine$noSprinting(boolean sprinting, CallbackInfo ci) {
        if (sprinting && (Object) this instanceof PlayerEntity && !Era.has(Feature.SPRINTING)) {
            ci.cancel();
        }
    }

    @Inject(method = "setSwimming", at = @At("HEAD"), cancellable = true)
    private void timemachine$noSwimming(boolean swimming, CallbackInfo ci) {
        if (swimming && (Object) this instanceof PlayerEntity && !Era.has(Feature.SWIMMING)) {
            ci.cancel();
        }
    }
}
