package com.timemachine.mixin;

import com.timemachine.ContentCalendar;
import com.timemachine.Era;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * You cannot craft what has not been thought of.
 *
 * <p>Most future recipes are already impossible in the past because their ingredients get
 * confiscated — no netherite without ancient debris. The ones that need blocking are the recipes
 * whose ingredients are ancient but whose result is not: a shield is iron and planks, a piston is
 * cobblestone and redstone, a bed is wool and planks. So the test is on the result, not the
 * inputs.
 *
 * <p>Both overloads are hooked. The one that takes a hint short-circuits on the hint before it
 * ever reaches the general lookup, so filtering only the general one would let the last recipe
 * you crafted keep working forever.
 */
@Mixin(ServerRecipeManager.class)
public abstract class ServerRecipeManagerMixin {

    @Inject(method = "getFirstMatch(Lnet/minecraft/recipe/RecipeType;"
            + "Lnet/minecraft/recipe/input/RecipeInput;Lnet/minecraft/world/World;)"
            + "Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private void timemachine$hideFutureRecipes(RecipeType<?> type, RecipeInput input, World world,
                                               CallbackInfoReturnable<Optional<RecipeEntry<?>>> cir) {
        filter(input, world, cir);
    }

    @Inject(method = "getFirstMatch(Lnet/minecraft/recipe/RecipeType;"
            + "Lnet/minecraft/recipe/input/RecipeInput;Lnet/minecraft/world/World;"
            + "Lnet/minecraft/recipe/RecipeEntry;)Ljava/util/Optional;",
            at = @At("RETURN"), cancellable = true)
    private void timemachine$hideFutureHintedRecipes(RecipeType<?> type, RecipeInput input,
                                                     World world, RecipeEntry<?> hint,
                                                     CallbackInfoReturnable<Optional<RecipeEntry<?>>> cir) {
        filter(input, world, cir);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void filter(RecipeInput input, World world,
                               CallbackInfoReturnable<Optional<RecipeEntry<?>>> cir) {
        if (Era.isPresent()) {
            return;
        }
        Optional<RecipeEntry<?>> found = cir.getReturnValue();
        if (found == null || found.isEmpty()) {
            return;
        }

        Recipe recipe = found.get().value();
        ItemStack result = recipe.craft(input, world.getRegistryManager());
        if (ContentCalendar.isAnachronistic(result)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
