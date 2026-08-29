package com.timemachine.client;

import com.timemachine.ContentCalendar;
import com.timemachine.Era;
import com.timemachine.Feature;
import com.timemachine.TimePayloads;
import com.timemachine.Timeline;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The dial: every version Minecraft has ever been, in a list you scroll through, with a lever at
 * the bottom that commits.
 *
 * <p>Drawn by hand rather than out of vanilla list widgets, because the interesting part of the
 * screen is the right-hand panel — the one that tells you, before you pull the lever, what you
 * are about to lose. Turning the dial is free; pulling the lever is not.
 */
public class DialScreen extends Screen {

    private static final int W = 340;
    private static final int ROW = 13;

    private static final int PANEL = 0xE6101418;
    private static final int PANEL_EDGE = 0xFF2A3944;
    private static final int LIST_BG = 0xFF0A0D10;
    private static final int SELECTED = 0xFF16404E;
    private static final int HOVERED = 0xFF13242C;
    private static final int TEXT = 0xFFD7E4EA;
    private static final int DIM = 0xFF6C7C86;
    private static final int ACCENT = 0xFF4ADFFF;
    private static final int WARN = 0xFFFFC24A;

    private final BlockPos pos;

    private int h;
    private int left;
    private int top;
    private int listX;
    private int listY;
    private int listW;
    private int listH;

    private int selected;
    private int scroll;

    public DialScreen(BlockPos pos) {
        super(Text.translatable("block.timemachine.time_machine"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        h = Math.min(216, Math.max(140, height - 40));
        left = (width - W) / 2;
        top = (height - h) / 2;

        listX = left + 8;
        listY = top + 28;
        listW = 148;
        listH = h - 28 - 30;

        // Open on wherever the world already is, and scroll so you can see it.
        selected = Era.index();
        scroll = Math.max(0, Math.min(selected - visibleRows() / 2, Timeline.size() - visibleRows()));

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(left + 8, top + h - 24, 60, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Engage"), b -> engage())
                .dimensions(left + W - 8 - 90, top + h - 24, 90, 18).build());
    }

    private int visibleRows() {
        return Math.max(1, listH / ROW);
    }

    private void engage() {
        if (selected != Era.index()) {
            ClientPlayNetworking.send(new TimePayloads.Engage(pos, selected));
        }
        close();
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int row = rowAt(click.x(), click.y());
        if (row >= 0) {
            selected = row;
            // A second click on a row you have already picked is the same as pulling the lever.
            if (doubled) {
                engage();
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
            scroll(-(int) Math.signum(vertical) * 3);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput key) {
        // The dial should work without a mouse wheel: arrows step, enter engages.
        int code = key.key();
        if (code == GLFW.GLFW_KEY_DOWN || code == GLFW.GLFW_KEY_UP) {
            selected = Math.max(0, Math.min(Timeline.size() - 1,
                    selected + (code == GLFW.GLFW_KEY_DOWN ? 1 : -1)));
            if (selected < scroll) {
                scroll = selected;
            } else if (selected >= scroll + visibleRows()) {
                scroll = selected - visibleRows() + 1;
            }
            return true;
        }
        if (code == GLFW.GLFW_KEY_ENTER || code == GLFW.GLFW_KEY_KP_ENTER) {
            engage();
            return true;
        }
        return super.keyPressed(key);
    }

    private void scroll(int by) {
        scroll = Math.max(0, Math.min(Math.max(0, Timeline.size() - visibleRows()), scroll + by));
    }

    /** @return the timeline index under the cursor, or -1 if the cursor is not over the list. */
    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < listX || mouseX >= listX + listW || mouseY < listY || mouseY >= listY + listH) {
            return -1;
        }
        int row = scroll + (int) ((mouseY - listY) / ROW);
        return row < Timeline.size() ? row : -1;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.fill(left - 1, top - 1, left + W + 1, top + h + 1, PANEL_EDGE);
        context.fill(left, top, left + W, top + h, PANEL);

        context.drawText(textRenderer, Text.literal("TIME MACHINE"), left + 9, top + 8, ACCENT, false);
        String here = "now: " + Era.current().name();
        context.drawText(textRenderer, Text.literal(here),
                left + W - 9 - textRenderer.getWidth(here), top + 8, DIM, false);
        context.fill(left + 8, top + 21, left + W - 8, top + 22, PANEL_EDGE);

        renderList(context, mouseX, mouseY);
        renderDetail(context);

        // super.render() drew the buttons before the panel went down on top of them, so put them
        // back. Drawing the panel first instead would mean losing the blurred background.
        for (var child : children()) {
            if (child instanceof ButtonWidget button) {
                button.render(context, mouseX, mouseY, delta);
            }
        }
    }

    private void renderList(DrawContext context, int mouseX, int mouseY) {
        context.fill(listX, listY, listX + listW, listY + listH, LIST_BG);
        context.enableScissor(listX, listY, listX + listW, listY + listH);

        int hovered = rowAt(mouseX, mouseY);
        int rows = Math.min(visibleRows() + 1, Timeline.size() - scroll);
        for (int i = 0; i < rows; i++) {
            int index = scroll + i;
            Timeline.Version version = Timeline.get(index);
            int y = listY + i * ROW;

            if (index == selected) {
                context.fill(listX, y, listX + listW, y + ROW, SELECTED);
            } else if (index == hovered) {
                context.fill(listX, y, listX + listW, y + ROW, HOVERED);
            }
            if (index == Era.index()) {
                context.drawText(textRenderer, Text.literal("▶"), listX + 2, y + 3, ACCENT, false);
            }

            int colour = index == selected ? TEXT : (index == Era.index() ? ACCENT : DIM);
            context.drawText(textRenderer, Text.literal(version.name()), listX + 11, y + 3, colour, false);
        }

        context.disableScissor();
        context.fill(listX, listY, listX + listW, listY + 1, PANEL_EDGE);
        context.fill(listX, listY + listH - 1, listX + listW, listY + listH, PANEL_EDGE);

        // Scrollbar: a plain thumb, sized to how much of the list you can see.
        int track = listH - 2;
        int thumb = Math.max(10, track * visibleRows() / Timeline.size());
        int max = Math.max(1, Timeline.size() - visibleRows());
        int offset = (track - thumb) * Math.min(scroll, max) / max;
        context.fill(listX + listW - 3, listY + 1 + offset, listX + listW - 1,
                listY + 1 + offset + thumb, PANEL_EDGE);
    }

    private void renderDetail(DrawContext context) {
        Timeline.Version version = Timeline.get(selected);
        int x = left + 166;
        int right = left + W - 8;
        int y = listY;

        context.drawText(textRenderer, Text.literal(version.name()), x, y, TEXT, false);
        y += 11;
        context.drawText(textRenderer, Text.literal(version.date() + "  ·  " + version.era()),
                x, y, DIM, false);
        y += 13;

        for (var line : textRenderer.wrapLines(Text.literal(version.note()), right - x)) {
            context.drawText(textRenderer, line, x, y, 0xFF9FB2BC, false);
            y += 10;
        }
        y += 4;

        if (selected == Era.index()) {
            context.drawText(textRenderer, Text.literal("You are already here."), x, y, DIM, false);
            return;
        }

        List<String> losses = losses(selected);
        if (losses.isEmpty()) {
            context.drawText(textRenderer, Text.literal("Nothing of yours is lost."), x, y, ACCENT, false);
            return;
        }

        context.drawText(textRenderer, Text.literal("Going there costs you:"), x, y, WARN, false);
        y += 11;

        int bottom = listY + listH;
        int shown = 0;
        for (String line : losses) {
            // Wrapped, not clipped: "the attack cooldown (every swing hits full again)" is the
            // most useful line on the screen and it does not fit on one.
            var wrapped = textRenderer.wrapLines(Text.literal("· " + line), right - x);
            if (y + wrapped.size() * 10 > bottom) {
                context.drawText(textRenderer,
                        Text.literal("… and " + (losses.size() - shown) + " more"),
                        x, y, DIM, false);
                break;
            }
            for (var wrap : wrapped) {
                context.drawText(textRenderer, wrap, x, y, 0xFF9FB2BC, false);
                y += 10;
            }
            shown++;
        }
    }

    /**
     * What the trip would take away, worked out against the player's own inventory and the
     * feature list. This is the whole reason the dial has a right-hand side: "Beta 1.7.3" means
     * nothing until it says "your elytra, your netherite pickaxe, and sprinting".
     */
    private List<String> losses(int target) {
        List<String> out = new ArrayList<>();
        int was = Era.index();

        int stacks = 0;
        String first = null;
        if (client != null && client.player != null) {
            var inventory = client.player.getInventory();
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.getStack(slot);
                if (ContentCalendar.notYetAt(stack, target)) {
                    stacks++;
                    if (first == null) {
                        first = stack.getName().getString();
                    }
                }
            }
        }
        if (stacks == 1) {
            out.add("your " + first);
        } else if (stacks > 1) {
            out.add(stacks + " stacks you are carrying, from " + first);
        }

        for (Feature feature : Feature.values()) {
            if (was >= feature.since() && target < feature.since()) {
                out.add(pretty(feature));
            }
        }
        return out;
    }

    private static String pretty(Feature feature) {
        return switch (feature) {
            case SURVIVAL -> "health, and mobs that fight back";
            case CRAFTING -> "crafting";
            case NETHER -> "the Nether";
            case BEDS -> "beds";
            case WEATHER -> "weather";
            case HUNGER -> "the hunger bar (food heals you directly again)";
            case SPRINTING -> "sprinting";
            case EXPERIENCE -> "experience";
            case THE_END -> "the End";
            case ENCHANTING -> "enchanting";
            case BREWING -> "brewing";
            case TRADING -> "villager trading";
            case ATTACK_COOLDOWN -> "the attack cooldown (every swing hits full again)";
            case OFFHAND -> "the off-hand slot";
            case ELYTRA -> "gliding";
            case SWIMMING -> "swimming";
        };
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
