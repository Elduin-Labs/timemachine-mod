package com.timemachine;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * A block that turns the whole world back to an older version of Minecraft.
 *
 * <p>It cannot swap out the jar you are running, obviously. What it does instead is put the rules
 * and the contents of the game back the way they were: dial in Beta 1.7.3 and hunger stops
 * draining, sprinting stops working, every swing lands for full damage again, the Nether is not
 * there, and everything invented after July 2011 is taken off you and held until you come
 * forward. Dial in Classic and you are left with the eleven blocks that existed in 2009.
 */
public class TimeMachineMod implements ModInitializer {
    public static final String MOD_ID = "timemachine";

    public static final Identifier TIME_MACHINE_ID = Identifier.of(MOD_ID, "time_machine");

    public static Block TIME_MACHINE;

    @Override
    public void onInitialize() {
        registerBlock();

        PayloadTypeRegistry.playS2C().register(TimePayloads.EraSync.ID, TimePayloads.EraSync.CODEC);
        PayloadTypeRegistry.playS2C().register(TimePayloads.OpenDial.ID, TimePayloads.OpenDial.CODEC);
        PayloadTypeRegistry.playC2S().register(TimePayloads.Engage.ID, TimePayloads.Engage.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TimePayloads.Engage.ID,
                (payload, context) -> engage(payload, context.player()));

        // The era lives in the save, so the static copy has to be reloaded with the world — and
        // reset when the world unloads, or the next world opened in the same client would start
        // wherever the last one left off.
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                Era.set(TimeState.get(server.getOverworld()).index()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> Era.set(Timeline.PRESENT));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ServerPlayNetworking.send(handler.getPlayer(), new TimePayloads.EraSync(Era.index())));

        ServerTickEvents.END_SERVER_TICK.register(Anachronism::tick);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                TimeMachineCommand.register(dispatcher));
    }

    private void registerBlock() {
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, TIME_MACHINE_ID);
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, TIME_MACHINE_ID);

        TIME_MACHINE = Registry.register(Registries.BLOCK, blockKey, new TimeMachineBlock(
                AbstractBlock.Settings.create()
                        .registryKey(blockKey)
                        .strength(3.5F, 1200.0F)
                        .sounds(BlockSoundGroup.NETHERITE)
                        .requiresTool()
                        .luminance(state -> 7)));

        Item item = Registry.register(Registries.ITEM, itemKey, new BlockItem(TIME_MACHINE,
                new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey()));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(item));
    }

    /** The dial has been turned and the lever pulled. */
    private static void engage(TimePayloads.Engage payload, ServerPlayerEntity player) {
        if (!player.getEntityWorld().getBlockState(payload.pos()).isOf(TIME_MACHINE)
                || !TimeMachineBlock.inReach(player, payload.pos())) {
            return;
        }
        TimeTravel.to(player.getEntityWorld().getServer(), payload.index(),
                player.getEntityWorld(), payload.pos());
    }
}
