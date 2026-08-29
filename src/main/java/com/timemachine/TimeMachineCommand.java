package com.timemachine;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * {@code /timemachine} — the dial without the block.
 *
 * <p>Mostly this exists so the mod can be tested without building a machine first, and so a
 * server operator can undo a trip somebody else took. It is also the escape hatch if the only
 * machine in the world ends up on the wrong side of a lava flow.
 */
public final class TimeMachineCommand {

    private TimeMachineCommand() {
    }

    private static final SuggestionProvider<ServerCommandSource> VERSIONS = (context, builder) -> {
        String typed = builder.getRemaining().toLowerCase();
        for (Timeline.Version version : Timeline.all()) {
            if (version.id().toLowerCase().startsWith(typed)) {
                builder.suggest(version.id(), Text.literal(version.name()));
            }
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("timemachine")
                .then(CommandManager.literal("now")
                        .executes(TimeMachineCommand::now))
                .then(CommandManager.literal("list")
                        .executes(TimeMachineCommand::list))
                .then(CommandManager.literal("vault")
                        .executes(TimeMachineCommand::vault))
                .then(CommandManager.literal("set")
                        .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                        .then(CommandManager.argument("version", StringArgumentType.string())
                                .suggests(VERSIONS)
                                .executes(TimeMachineCommand::set)))
                .executes(TimeMachineCommand::now));
    }

    private static int now(CommandContext<ServerCommandSource> context) {
        Timeline.Version version = Era.current();
        context.getSource().sendFeedback(() -> Text.literal("The world is in ")
                .append(Text.literal(version.name()).formatted(Formatting.AQUA))
                .append(Text.literal(" (" + version.date() + ", " + version.era() + ")")
                        .formatted(Formatting.GRAY)), false);
        context.getSource().sendFeedback(() -> Text.literal(version.note())
                .formatted(Formatting.ITALIC, Formatting.DARK_AQUA), false);

        StringBuilder missing = new StringBuilder();
        for (Feature feature : Feature.values()) {
            if (!Era.has(feature)) {
                missing.append(missing.isEmpty() ? "" : ", ").append(feature.name().toLowerCase());
            }
        }
        if (!missing.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("Not invented yet: " + missing)
                    .formatted(Formatting.GRAY), false);
        }
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> context) {
        int here = Era.index();
        for (int i = 0; i < Timeline.size(); i++) {
            Timeline.Version version = Timeline.get(i);
            boolean current = i == here;
            context.getSource().sendFeedback(() -> Text.literal(
                            (current ? " > " : "   ") + version.id())
                    .formatted(current ? Formatting.AQUA : Formatting.GRAY)
                    .append(Text.literal("  " + version.name() + "  " + version.date())
                            .formatted(Formatting.DARK_GRAY)), false);
        }
        return Timeline.size();
    }

    private static int vault(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.literal("Only a player has a vault."));
            return 0;
        }
        int held = TimeState.get(context.getSource().getServer().getOverworld())
                .heldCount(player.getUuid());
        context.getSource().sendFeedback(() -> Text.literal(held == 0
                        ? "The future is holding nothing of yours."
                        : "The future is holding " + held + " stack" + (held == 1 ? "" : "s")
                        + " of yours.").formatted(Formatting.GRAY), false);
        return held;
    }

    private static int set(CommandContext<ServerCommandSource> context) {
        String id = StringArgumentType.getString(context, "version");
        int index = Timeline.indexOf(id);
        if (index < 0) {
            context.getSource().sendError(Text.literal("No such version: " + id
                    + ". Try /timemachine list."));
            return 0;
        }
        TimeTravel.to(context.getSource().getServer(), index, null, null);
        return 1;
    }
}
