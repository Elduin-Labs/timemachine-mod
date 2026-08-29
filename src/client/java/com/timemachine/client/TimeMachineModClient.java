package com.timemachine.client;

import com.timemachine.Era;
import com.timemachine.TimePayloads;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

/**
 * The client half: keeps {@link Era} in step with the server so the mechanic mixins agree about
 * what year it is, and opens the dial when a machine asks it to.
 */
public class TimeMachineModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(TimePayloads.EraSync.ID, (payload, context) ->
                context.client().execute(() -> ClientEra.accept(payload.index())));

        ClientPlayNetworking.registerGlobalReceiver(TimePayloads.OpenDial.ID, (payload, context) ->
                context.client().execute(() -> {
                    ClientEra.accept(payload.index());
                    MinecraftClient.getInstance().setScreen(new DialScreen(payload.pos()));
                }));

        // Leaving a server has to put the era back, or the next single-player world would open
        // in whatever century the last server was in until its first sync arrives.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientEra.reset());
    }
}
