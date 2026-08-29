package com.timemachine;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the past in the past, tick after tick.
 *
 * <p>{@link TimeTravel} handles the moment of the trip; this handles everything afterwards. You
 * can still open a chest full of netherite in Beta 1.7.3, so the sweep runs on a timer and takes
 * back anything that has crossed into your hands since the last one. Likewise mobs: rather than
 * fight every spawner, spawn egg and breeding path in the game, anything from the future that
 * turns up simply stops existing a moment later.
 */
public final class Anachronism {

    /** How often the sweep runs. Once a second is fast enough to feel immediate and cheap. */
    private static final int PERIOD = 20;

    private static int ticks;

    private Anachronism() {
    }

    public static void tick(MinecraftServer server) {
        if (Era.isPresent()) {
            return;
        }
        if (++ticks % PERIOD != 0) {
            return;
        }

        TimeState state = TimeState.get(server.getOverworld());
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sweepInventory(player, state);
            enforceDimension(player);
            enforceOffHand(player);
        }
        for (ServerWorld world : server.getWorlds()) {
            if (!world.getPlayers().isEmpty()) {
                sweepMobs(world);
            }
        }
    }

    // ---------------------------------------------------------------- items

    /**
     * Takes everything not yet invented off {@code player} and puts it in the vault. Covers the
     * main inventory, armour and off-hand — {@link PlayerInventory#size()} spans all three — plus
     * the ender chest, which would otherwise be a loophole big enough to walk a beacon through.
     */
    public static void sweepInventory(ServerPlayerEntity player, TimeState state) {
        int taken = confiscate(player, player.getInventory(), state);
        taken += confiscate(player, player.getEnderChestInventory(), state);

        // Whatever is on the cursor of an open screen is in neither inventory.
        ItemStack cursor = player.currentScreenHandler.getCursorStack();
        if (ContentCalendar.isAnachronistic(cursor)) {
            state.store(player.getUuid(), cursor);
            player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
            taken++;
        }

        if (taken > 0) {
            player.sendMessage(Text.literal(taken + " thing" + (taken == 1 ? "" : "s")
                            + " you were carrying " + (taken == 1 ? "has" : "have")
                            + " not been invented yet.").formatted(Formatting.GRAY),
                    true);
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 0.4F, 1.6F);
        }
    }

    private static int confiscate(ServerPlayerEntity player, Inventory inventory, TimeState state) {
        int taken = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (ContentCalendar.isAnachronistic(stack)) {
                state.store(player.getUuid(), stack);
                inventory.setStack(slot, ItemStack.EMPTY);
                taken++;
            }
        }
        return taken;
    }

    /**
     * Before 1.9 there was no off-hand slot, so anything sitting in it is shoved back into the
     * inventory. Confiscating it would be wrong — the item exists, the slot does not.
     */
    private static void enforceOffHand(ServerPlayerEntity player) {
        if (Era.has(Feature.OFFHAND)) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack offHand = inventory.getStack(PlayerInventory.OFF_HAND_SLOT);
        if (offHand.isEmpty()) {
            return;
        }
        inventory.setStack(PlayerInventory.OFF_HAND_SLOT, ItemStack.EMPTY);
        if (!inventory.insertStack(offHand)) {
            player.dropItem(offHand, false);
        }
    }

    // ---------------------------------------------------------------- mobs

    private static void sweepMobs(ServerWorld world) {
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
            world.spawnParticles(ParticleTypes.PORTAL, pos.x, pos.y + 0.5, pos.z, 10,
                    0.3, 0.4, 0.3, 0.05);
            entity.discard();
        }
    }

    // ---------------------------------------------------------------- dimensions

    /** Puts a player back in the overworld if they are standing in a dimension that does not exist yet. */
    public static void enforceDimension(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        if (TimeTravel.dimensionExists(world)) {
            return;
        }

        ServerWorld overworld = world.getServer().getOverworld();
        Vec3d spawn = Vec3d.ofBottomCenter(overworld.getSpawnPoint().getPos());
        player.teleportTo(new TeleportTarget(overworld, spawn, Vec3d.ZERO, player.getYaw(),
                player.getPitch(), TeleportTarget.NO_OP));

        String name = world.getRegistryKey().equals(World.NETHER) ? "The Nether" : "The End";
        player.sendMessage(Text.literal(name + " has not been discovered yet.")
                .formatted(Formatting.LIGHT_PURPLE), false);
        overworld.playSound(null, spawn.x, spawn.y, spawn.z, SoundEvents.BLOCK_PORTAL_TRAVEL,
                SoundCategory.PLAYERS, 0.3F, 0.5F);
    }
}
