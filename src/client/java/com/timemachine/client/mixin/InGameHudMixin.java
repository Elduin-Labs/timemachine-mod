package com.timemachine.client.mixin;

import com.timemachine.Era;
import com.timemachine.client.ClientEra;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Two things drawn over everything else: the tint that washes across the screen as you jump, and
 * a small standing reminder of what year it is.
 *
 * <p>Neither appears in the present, so a world nobody has taken back in time looks exactly like
 * vanilla.
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void timemachine$drawEra(DrawContext context, RenderTickCounter counter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.player == null) {
            return;
        }

        float flash = ClientEra.flash();
        if (flash > 0.0F) {
            int alpha = (int) (flash * flash * 190.0F) << 24;
            context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(),
                    alpha | ClientEra.flashColour());
        }

        if (Era.isPresent()) {
            return;
        }

        var version = Era.current();
        Text label = Text.literal(version.name());
        int width = client.textRenderer.getWidth(label);
        int x = context.getScaledWindowWidth() / 2 - width / 2;
        context.fill(x - 4, 2, x + width + 4, 14, 0x66000000);
        context.drawText(client.textRenderer, label, x, 4, 0xFF4ADFFF, false);
    }
}
