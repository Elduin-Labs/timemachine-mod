package com.timemachine;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The trip itself.
 *
 * <p>Nothing here rewrites the world. Blocks you already placed stay where they are — a time
 * machine that deleted your base every time you turned the dial would be a griefing tool, not a
 * toy. What changes is what exists <em>from now on</em>: what you are carrying, what walks around,
 * what you can craft, and which of the rules of the game are in force.
 */
public final class TimeTravel {

    private TimeTravel() {
    }

    /**
     * Turns the dial to {@code target} and applies every consequence.
     *
     * @param machine where the noise and the particles come from, or null for the
     *                {@code /timemachine} command, which has no machine to stand next to
     */
    public static void to(MinecraftServer server, int target, @Nullable ServerWorld machineWorld,
                          @Nullable BlockPos machine) {
        TimeState state = TimeState.get(server.getOverworld());
        int from = state.index();
        int to = Timeline.clamp(target);
        if (from == to) {
            return;
        }

        state.setIndex(to);
        Era.set(to);
        syncAll(server);

        // Order matters: sweep with the new era in force, so anything that stopped existing goes
        // away, and only then hand back what has started existing again.
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Anachronism.sweepInventory(player, state);
            returnFromVault(player, state);
            Anachronism.enforceDimension(player);
        }

        for (ServerWorld world : server.getWorlds()) {
            sweepEntities(world);
        }

        announce(server, from, to);
        if (machineWorld != null && machine != null) {
            effects(machineWorld, machine, from, to);
        }
    }

    /** Pushes the current era to every connected client. */
    public static void syncAll(MinecraftServer server) {
        TimePayloads.EraSync payload = new TimePayloads.EraSync(Era.index());
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    // ---------------------------------------------------------------- consequences

    /** Hands back everything of this player's that has been invented again. */
    private static void returnFromVault(ServerPlayerEntity player, TimeState state) {
        List<ItemStack> back = state.reclaim(player.getUuid());
        if (back.isEmpty()) {
            return;
        }
        for (ItemStack stack : back) {
            if (!player.getInventory().insertStack(stack)) {
                player.dropItem(stack, false);
            }
        }
        player.sendMessage(Text.literal(back.size() + " thing" + (back.size() == 1 ? "" : "s")
                + " you left in the future came back with you.").formatted(Formatting.AQUA), false);
    }

    /**
     * Removes every mob that has not been invented yet. Dropped items are left alone — the
     * inventory sweep catches those the moment somebody picks one up, and popping them out of
     * existence on the ground would quietly eat chest contents people had just tipped out.
     */
    private static void sweepEntities(ServerWorld world) {
        List<Entity> doomed = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof PlayerEntity || entity instanceof ItemEntity
                    || entity.getType() == EntityType.EXPERIENCE_ORB) {
                continue;
            }
            if (ContentCalendar.isAnachronistic(entity)) {
                doomed.add(entity);
            }
        }
        for (Entity entity : doomed) {
            Vec3d pos = entity.getEntityPos();
            world.spawnParticles(ParticleTypes.PORTAL, pos.x, pos.y + 0.5, pos.z, 12,
                    0.3, 0.4, 0.3, 0.05);
            entity.discard();
        }
    }

    // ---------------------------------------------------------------- theatre

    private static void announce(MinecraftServer server, int from, int to) {
        Timeline.Version version = Timeline.get(to);
        boolean back = to < from;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(new TitleS2CPacket(
                    Text.literal(version.name()).formatted(back ? Formatting.GOLD : Formatting.AQUA)));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(
                    Text.literal(version.date() + "  ·  " + version.era())
                            .formatted(Formatting.GRAY)));
            player.sendMessage(Text.literal(version.note()).formatted(Formatting.ITALIC,
                    Formatting.DARK_AQUA), false);

            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 0.25F,
                    back ? 0.6F : 1.4F);
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.5F,
                    back ? 0.7F : 1.5F);
        }
    }

    private static void effects(ServerWorld world, BlockPos pos, int from, int to) {
        Vec3d center = Vec3d.ofCenter(pos);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.6, center.z,
                120, 0.6, 0.8, 0.6, 0.15);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.6, center.z,
                40, 0.5, 0.5, 0.5, 0.4);
        world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN,
                SoundCategory.BLOCKS, 1.0F, to < from ? 0.6F : 1.4F);
    }

    // ---------------------------------------------------------------- misc

    /** The dimensions that exist in the current era. */
    public static boolean dimensionExists(World world) {
        var key = world.getRegistryKey();
        if (key.equals(World.NETHER)) {
            return Era.has(Feature.NETHER);
        }
        if (key.equals(World.END)) {
            return Era.has(Feature.THE_END);
        }
        return true;
    }
}
