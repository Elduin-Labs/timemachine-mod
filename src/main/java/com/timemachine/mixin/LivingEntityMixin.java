package com.timemachine.mixin;

import com.timemachine.Era;
import com.timemachine.Feature;

import net.minecraft.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Elytra are 1.9. Before then nothing glides, and since the elytra item itself is confiscated on
 * the way back this is really a belt-and-braces measure — but a pair already equipped when the
 * dial turns would otherwise stay strapped on for the moment before the sweep runs.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "canGlide", at = @At("HEAD"), cancellable = true)
    private void timemachine$noGliding(CallbackInfoReturnable<Boolean> cir) {
        if (!Era.has(Feature.ELYTRA)) {
            cir.setReturnValue(false);
        }
    }
}
