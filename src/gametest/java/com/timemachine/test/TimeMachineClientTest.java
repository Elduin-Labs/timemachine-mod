package com.timemachine.test;

import com.timemachine.Era;
import com.timemachine.Timeline;
import com.timemachine.client.DialScreen;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Takes a real client back in time and checks the past actually arrived.
 *
 * <p>Everything here is checked from whichever side is authoritative. Inventory contents and the
 * era itself are read on the <em>client</em>, because a pass then also proves the sync packet
 * landed and the client-side half of the mechanic mixins agrees. Hunger is read on the
 * <em>server</em>, because the client runs the same freeze mixin and would happily report a full
 * bar even if the server were draining one.
 *
 * <p>Run with {@code ./gradlew runClientGameTest}. The last thing it does is write
 * {@code run/timemachine-gametest-passed.txt}: the harness exits 0 if the client dies partway
 * through, so the marker is the only honest proof the whole run finished.
 */
public class TimeMachineClientTest implements FabricClientGameTest {

    private static final String PRESENT = "1.21.11";
    private static final String BETA = "b1.7.3";
    private static final String CLASSIC = "c0.30";

    /** The sweep runs once a second; two seconds is comfortably past it. */
    private static final int SWEEP_TICKS = 45;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientWorld().waitForChunksRender();
            arena(context, singleplayer);

            theDialMovesTheWholeWorld(context, singleplayer);
            theMachineItselfWorks(context, singleplayer);
            futureThingsAreTakenAwayAndGivenBack(context, singleplayer);
            futureMobsStopExisting(context, singleplayer);
            beforeBetaEightNobodySprints(context, singleplayer);
            beforeTheCombatUpdateEverySwingLandsFull(context, singleplayer);
            beforeBetaEightHungerDoesNotDrain(context, singleplayer);
            beforeBetaEightNothingDropsExperience(context, singleplayer);
            youCannotCraftWhatHasNotBeenInvented(context, singleplayer);
            theNetherIsNotThereYet(context, singleplayer);
            classicLeavesYouWithAlmostNothing(context, singleplayer);
            comingHomeUndoesAllOfIt(context, singleplayer);

            markPassed();
        }
    }

    /** Flat ground, daylight, nothing wandering in. */
    private void arena(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        command(singleplayer, "gamerule spawn_mobs false");
        command(singleplayer, "gamerule advance_time false");
        command(singleplayer, "gamerule mob_griefing false");
        // Easy, not peaceful: peaceful refuses to summon a hostile mob and tops the hunger bar
        // back up every tick, which would make two of the checks below pass without proving
        // anything. Nothing spawns of its own accord anyway with spawn_mobs off.
        command(singleplayer, "difficulty easy");
        command(singleplayer, "time set noon");
        command(singleplayer, "gamemode creative @a");

        // The first teleport only starts the chunks loading; /fill does nothing in a chunk that
        // is not there yet.
        command(singleplayer, "tp @a 0 101 0 0 0");
        context.waitTicks(40);
        command(singleplayer, "tp @a 0 101 0 0 0");
        context.waitTicks(20);

        fill(context, singleplayer, "-8 99 -8 8 99 8 minecraft:stone");
        fill(context, singleplayer, "-8 100 -8 8 104 8 minecraft:air");

        command(singleplayer, "tp @a 0 100 0 0 0");
        context.waitTicks(10);
        setEra(context, singleplayer, PRESENT);
    }

    // ------------------------------------------------------------------ the dial

    private void theDialMovesTheWholeWorld(ClientGameTestContext context,
                                           TestSingleplayerContext singleplayer) {
        setEra(context, singleplayer, BETA);

        // Read on the client: this only passes if the era sync packet arrived and was applied.
        int seen = context.computeOnClient(client -> Era.index());
        assertTrue(seen == Timeline.indexOf(BETA),
                "the client should be in " + BETA + ", it thinks it is in "
                        + Timeline.get(seen).id());

        int onServer = singleplayer.getServer().computeOnServer(server -> Era.index());
        assertTrue(onServer == Timeline.indexOf(BETA), "the server should be in " + BETA);

        context.takeScreenshot("dialled_back_to_beta");
    }

    /**
     * The whole chain the player actually uses, with no commands in it: place a machine,
     * right-click it, turn the dial with the keyboard, pull the lever. A pass means the block's
     * use handler fired, the open-dial packet crossed to the client, the screen built and drew
     * itself, and the engage packet crossed back and moved the world.
     */
    private void theMachineItselfWorks(ClientGameTestContext context,
                                       TestSingleplayerContext singleplayer) {
        setEra(context, singleplayer, PRESENT);
        command(singleplayer, "clear @a");
        command(singleplayer, "setblock 0 100 3 timemachine:time_machine");
        // Standing three blocks north of it, yaw 0 (facing south) and pitched down far enough for
        // the crosshair to land on the machine. This version of the harness has no lookAt().
        command(singleplayer, "tp @a 0.5 100 0.5 0 20");
        context.waitTicks(15);

        context.getInput().pressKey(options -> options.useKey);

        context.waitForScreen(DialScreen.class);
        context.waitTicks(5);
        context.takeScreenshot("the_dial");

        // Wind the dial back to Beta 1.7.3 one press at a time, the way a player would.
        int steps = Timeline.PRESENT - Timeline.indexOf(BETA);
        for (int i = 0; i < steps; i++) {
            context.getInput().pressKey(GLFW.GLFW_KEY_UP);
        }
        context.waitTicks(5);
        context.takeScreenshot("the_dial_wound_back");

        context.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
        context.waitTicks(30);

        assertTrue(context.computeOnClient(client -> client.currentScreen) == null,
                "engaging should have closed the dial");
        assertTrue(context.computeOnClient(client -> Era.index()) == Timeline.indexOf(BETA),
                "pulling the lever should have taken the world to " + BETA + ", it is in "
                        + Timeline.get(context.computeOnClient(client -> Era.index())).id());

        command(singleplayer, "setblock 0 100 3 minecraft:air");
        context.waitTicks(5);
    }

    // ------------------------------------------------------------------ items

    private void futureThingsAreTakenAwayAndGivenBack(ClientGameTestContext context,
                                                      TestSingleplayerContext singleplayer) {
        setEra(context, singleplayer, PRESENT);

        command(singleplayer, "clear @a");
        command(singleplayer, "give @a minecraft:netherite_pickaxe");
        command(singleplayer, "give @a minecraft:elytra");
        command(singleplayer, "give @a minecraft:shield");
        command(singleplayer, "give @a minecraft:cobblestone 12");
        context.waitTicks(20);

        assertTrue(carrying(context, Items.NETHERITE_PICKAXE),
                "the pickaxe should be in the inventory before the trip");

        setEra(context, singleplayer, BETA);
        context.waitTicks(SWEEP_TICKS);

        assertTrue(!carrying(context, Items.NETHERITE_PICKAXE),
                "netherite is 1.16 — the pickaxe should have been taken in Beta 1.7.3");
        assertTrue(!carrying(context, Items.ELYTRA), "elytra are 1.9 — they should have been taken");
        assertTrue(!carrying(context, Items.SHIELD), "shields are 1.9 — they should have been taken");
        assertTrue(carrying(context, Items.COBBLESTONE),
                "cobblestone is older than the game — it should have survived the trip");

        context.takeScreenshot("inventory_in_beta");

        setEra(context, singleplayer, PRESENT);
        context.waitTicks(SWEEP_TICKS);

        assertTrue(carrying(context, Items.NETHERITE_PICKAXE),
                "the pickaxe should have come back out of the vault in the present");
        assertTrue(carrying(context, Items.ELYTRA), "the elytra should have come back too");
        assertTrue(carrying(context, Items.SHIELD), "the shield should have come back too");
    }

    // ------------------------------------------------------------------ mobs

    private void futureMobsStopExisting(ClientGameTestContext context,
                                        TestSingleplayerContext singleplayer) {
        setEra(context, singleplayer, PRESENT);

        command(singleplayer, "summon minecraft:warden 4 100 0");
        command(singleplayer, "summon minecraft:pig -4 100 0");
        context.waitTicks(20);

        // Control first: a Warden that never spawned would make the real check pass for the
        // wrong reason.
        assertTrue(count(singleplayer, EntityType.WARDEN) > 0,
                "control: the Warden should be standing there in the present");

        setEra(context, singleplayer, BETA);
        context.waitTicks(SWEEP_TICKS);

        assertTrue(count(singleplayer, EntityType.WARDEN) == 0,
                "the Warden is 1.19 — it should not have survived a second in Beta 1.7.3");
        assertTrue(count(singleplayer, EntityType.PIG) > 0,
                "pigs are older than Alpha — the pig should still be standing there");
    }

    // ------------------------------------------------------------------ movement

    private void beforeBetaEightNobodySprints(ClientGameTestContext context,
                                              TestSingleplayerContext singleplayer) {
        setEra(context, singleplayer, PRESENT);

        // Set and read inside one client task, so the movement code cannot clear the flag in
        // between and make the control look like the result.
        assertTrue(context.computeOnClient(client -> {
            client.player.setSprinting(true);
            return client.player.isSprinting();
        }), "control: sprinting should work in the present");

        context.runOnClient(client -> client.player.setSprinting(false));
        setEra(context, singleplayer, BETA);

        assertTrue(!context.computeOnClient(client -> {
            client.player.setSprinting(true);
            return client.player.isSprinting();
        }), "sprinting is Beta 1.8 — it should not turn on in Beta 1.7.3");
    }

    private void beforeTheCombatUpdateEverySwingLandsFull(ClientGameTestContext context,
                                                          TestSingleplayerContext singleplayer) {
        setEra(context, singleplayer, PRESENT);

        float charging = context.computeOnClient(client -> {
            client.player.resetTicksSinceLastAttack();
            return client.player.getAttackCooldownProgress(0.0F);
        });
        assertTrue(charging < 0.5F,
                "control: a swing in the present should start uncharged, was " + charging);

        setEra(context, singleplayer, BETA);

        float always = context.computeOnClient(client -> {
            client.player.resetTicksSinceLastAttack();
            return client.player.getAttackCooldownProgress(0.0F);
        });
        assertTrue(always == 1.0F,
                "the attack cooldown is 1.9 — every swing should land full in Beta, was " + always);
    }

    // ------------------------------------------------------------------ hunger

    private void beforeBetaEightHungerDoesNotDrain(ClientGameTestContext context,
                                                   TestSingleplayerContext singleplayer) {
        command(singleplayer, "gamemode survival @a");

        setEra(context, singleplayer, PRESENT);
        command(singleplayer, "effect give @a minecraft:hunger 20 255");
        context.waitTicks(160);
        int drained = food(singleplayer);
        assertTrue(drained < 20,
                "control: the hunger effect should drain the bar in the present, it sat at " + drained);

        setEra(context, singleplayer, BETA);
        command(singleplayer, "effect give @a minecraft:hunger 20 255");
        context.waitTicks(160);
        int frozen = food(singleplayer);
        assertTrue(frozen == 20,
                "hunger is Beta 1.8 — the bar should be pinned full in Beta 1.7.3, was " + frozen);

        command(singleplayer, "effect clear @a");
        command(singleplayer, "gamemode creative @a");
        context.waitTicks(10);
    }

    // ------------------------------------------------------------------ experience

    private void beforeBetaEightNothingDropsExperience(ClientGameTestContext context,
                                                       TestSingleplayerContext singleplayer) {
        setEra(context, singleplayer, PRESENT);
        spawnOrb(singleplayer);
        context.waitTicks(10);
        assertTrue(count(singleplayer, EntityType.EXPERIENCE_ORB) > 0,
                "control: an orb spawned in the present should exist");

        command(singleplayer, "kill @e[type=minecraft:experience_orb]");
        setEra(context, singleplayer, BETA);
        context.waitTicks(10);

        spawnOrb(singleplayer);
        context.waitTicks(10);
        assertTrue(count(singleplayer, EntityType.EXPERIENCE_ORB) == 0,
                "experience is Beta 1.8 — no orb should have appeared in Beta 1.7.3");
    }

    /**
     * Well out of the player's reach. An orb dropped at their feet is swallowed within a tick or
     * two, and a control that vanishes because it was collected looks exactly like a control that
     * never existed.
     */
    private static void spawnOrb(TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> ExperienceOrbEntity.spawn(
                server.getOverworld(), new Vec3d(0.0, 101.0, 32.0), 7));
    }

    // ------------------------------------------------------------------ recipes

    private void youCannotCraftWhatHasNotBeenInvented(ClientGameTestContext context,
                                                      TestSingleplayerContext singleplayer) {
        setEra(context, singleplayer, PRESENT);
        assertTrue(shieldCraftable(singleplayer),
                "control: a shield should be craftable in the present");

        setEra(context, singleplayer, BETA);
        assertTrue(!shieldCraftable(singleplayer),
                "shields are 1.9 — planks and an ingot should make nothing in Beta 1.7.3");
    }

    /**
     * The shield is the recipe worth testing: iron and planks were both around in Beta, so nothing
     * about its ingredients stops it. Only the date on the result does.
     */
    private static boolean shieldCraftable(TestSingleplayerContext singleplayer) {
        return singleplayer.getServer().computeOnServer(server -> {
            ItemStack planks = new ItemStack(Items.OAK_PLANKS);
            ItemStack iron = new ItemStack(Items.IRON_INGOT);
            ItemStack none = ItemStack.EMPTY;
            CraftingRecipeInput input = CraftingRecipeInput.create(3, 3, List.of(
                    planks, iron, planks,
                    planks, planks, planks,
                    none, planks, none));
            ServerWorld world = server.getOverworld();
            return server.getRecipeManager()
                    .getFirstMatch(RecipeType.CRAFTING, input, world)
                    .isPresent();
        });
    }

    // ------------------------------------------------------------------ dimensions

    private void theNetherIsNotThereYet(ClientGameTestContext context,
                                        TestSingleplayerContext singleplayer) {
        command(singleplayer, "gamemode creative @a");

        // Control: the Nether is Alpha 1.2, so Beta 1.7.3 has one and the mod should keep its
        // hands off. Without this the check below would pass even if the mod yanked players out
        // of the Nether in every era.
        setEra(context, singleplayer, BETA);
        command(singleplayer, "execute in minecraft:the_nether run tp @a 0 100 0");
        context.waitTicks(SWEEP_TICKS + 20);
        assertTrue(dimension(singleplayer).equals(World.NETHER),
                "control: the Nether existed in Beta 1.7.3 — the player should have stayed in it");

        setEra(context, singleplayer, CLASSIC);
        context.waitTicks(SWEEP_TICKS + 20);
        assertTrue(dimension(singleplayer).equals(World.OVERWORLD),
                "the Nether is Alpha 1.2 — dialling back to Classic should have put the player "
                        + "back in the overworld");
    }

    // ------------------------------------------------------------------ the deep past

    private void classicLeavesYouWithAlmostNothing(ClientGameTestContext context,
                                                   TestSingleplayerContext singleplayer) {
        setEra(context, singleplayer, PRESENT);
        command(singleplayer, "clear @a");
        command(singleplayer, "give @a minecraft:cobblestone 8");
        command(singleplayer, "give @a minecraft:diamond_pickaxe");
        command(singleplayer, "give @a minecraft:white_bed");
        context.waitTicks(20);

        setEra(context, singleplayer, CLASSIC);
        context.waitTicks(SWEEP_TICKS);

        assertTrue(carrying(context, Items.COBBLESTONE),
                "cobblestone was there in Classic — it should have survived");
        assertTrue(!carrying(context, Items.WHITE_BED),
                "beds are Beta 1.3 — the bed should have been taken in Classic");
        assertTrue(!carrying(context, Items.DIAMOND_PICKAXE),
                "diamond tools are Alpha — the pickaxe should have been taken in Classic");

        context.takeScreenshot("inventory_in_classic");
    }

    private void comingHomeUndoesAllOfIt(ClientGameTestContext context,
                                         TestSingleplayerContext singleplayer) {
        setEra(context, singleplayer, PRESENT);
        context.waitTicks(SWEEP_TICKS);

        assertTrue(context.computeOnClient(client -> Era.isPresent()),
                "the client should be back in the present");
        assertTrue(carrying(context, Items.DIAMOND_PICKAXE),
                "the pickaxe should have come home with us");
        assertTrue(carrying(context, Items.WHITE_BED), "so should the bed");

        int held = singleplayer.getServer().computeOnServer(server ->
                com.timemachine.TimeState.get(server.getOverworld()).heldCount(player(server).getUuid()));
        assertTrue(held == 0, "the vault should be empty in the present, it still holds " + held);

        context.takeScreenshot("home_again");
    }

    // ------------------------------------------------------------------ helpers

    private static void setEra(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                               String versionId) {
        command(singleplayer, "timemachine set \"" + versionId + "\"");
        context.waitTicks(15);
    }

    private static boolean carrying(ClientGameTestContext context, Item item) {
        return context.computeOnClient(client -> {
            var inventory = client.player.getInventory();
            for (int slot = 0; slot < inventory.size(); slot++) {
                if (inventory.getStack(slot).isOf(item)) {
                    return true;
                }
            }
            return false;
        });
    }

    private static int count(TestSingleplayerContext singleplayer, EntityType<?> type) {
        return singleplayer.getServer().computeOnServer(server -> {
            List<Entity> found = new ArrayList<>();
            for (Entity entity : server.getOverworld().iterateEntities()) {
                if (entity.getType() == type) {
                    found.add(entity);
                }
            }
            return found.size();
        });
    }

    private static RegistryKey<World> dimension(TestSingleplayerContext singleplayer) {
        return singleplayer.getServer().computeOnServer(server ->
                player(server).getEntityWorld().getRegistryKey());
    }

    private static int food(TestSingleplayerContext singleplayer) {
        return singleplayer.getServer().computeOnServer(server ->
                player(server).getHungerManager().getFoodLevel());
    }

    private static ServerPlayerEntity player(MinecraftServer server) {
        return server.getPlayerManager().getPlayerList().getFirst();
    }

    private static void command(TestSingleplayerContext singleplayer, String command) {
        singleplayer.getServer().runCommand(command);
    }

    /** One fill per tick — queueing a batch into a single server tick deadlocks the harness. */
    private static void fill(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                             String arguments) {
        command(singleplayer, "fill " + arguments);
        context.waitTicks(2);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * The harness exits 0 when the client dies partway through a run, so a green build proves
     * nothing on its own. This file existing does.
     */
    private static void markPassed() {
        try {
            Files.writeString(Path.of("timemachine-gametest-passed.txt"),
                    "every check passed at " + java.time.Instant.now() + "\n");
        } catch (IOException e) {
            throw new AssertionError("could not write the completion marker", e);
        }
    }
}
