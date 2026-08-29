package com.timemachine.mixin;

import com.timemachine.Era;
import com.timemachine.Feature;

import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Eating, in the years before the hunger bar.
 *
 * <p>Food went straight into your health then, and the numbers line up almost exactly: modern
 * nutrition is the old healing value in half-hearts. Cooked pork is 8, and it restored four
 * hearts; bread is 5, and it restored two and a half. So the old rule is just
 * {@code heal(nutrition)}.
 *
 * <p>This runs alongside the vanilla method rather than replacing it, so the chewing and the burp
 * still happen. The hunger points it goes on to add do not matter — {@link PlayerEntityMixin}
 * holds the bar at full in these eras anyway.
 */
@Mixin(FoodComponent.class)
public abstract class FoodComponentMixin {

    @Inject(method = "onConsume", at = @At("HEAD"))
    private void timemachine$foodHealsDirectly(World world, LivingEntity entity, ItemStack stack,
                                               ConsumableComponent consumable, CallbackInfo ci) {
        if (world.isClient() || Era.has(Feature.HUNGER) || !(entity instanceof PlayerEntity player)) {
            return;
        }
        player.heal(((FoodComponent) (Object) this).nutrition());
    }
}
