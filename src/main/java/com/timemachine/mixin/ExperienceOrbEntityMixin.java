package com.timemachine.mixin;

import com.timemachine.Era;
import com.timemachine.Feature;

import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Experience arrived with the Adventure Update. In anything older, nothing drops orbs — which is
 * most of the point of visiting Beta 1.7.3, where a diamond pickaxe was as good as a pickaxe got.
 *
 * <p>The three-argument {@code spawn} delegates to this one, so catching it here catches every
 * orb in the game.
 */
@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin {

    @Inject(method = "spawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/Vec3d;"
            + "Lnet/minecraft/util/math/Vec3d;I)V", at = @At("HEAD"), cancellable = true)
    private static void timemachine$noExperienceYet(ServerWorld world, Vec3d pos, Vec3d velocity,
                                                    int amount, CallbackInfo ci) {
        if (!Era.has(Feature.EXPERIENCE)) {
            ci.cancel();
        }
    }
}
