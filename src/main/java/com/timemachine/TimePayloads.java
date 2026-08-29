package com.timemachine;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Everything the time machine says over the wire. */
public final class TimePayloads {

    private TimePayloads() {
    }

    /**
     * "The world is now in this version." Sent on join and after every trip, so the client-side
     * half of the mechanic mixins — sprinting, the attack cooldown bar, the elytra — agrees with
     * the server about what year it is.
     */
    public record EraSync(int index) implements CustomPayload {
        public static final CustomPayload.Id<EraSync> ID =
                new CustomPayload.Id<>(Identifier.of(TimeMachineMod.MOD_ID, "era_sync"));
        public static final PacketCodec<RegistryByteBuf, EraSync> CODEC =
                PacketCodec.tuple(PacketCodecs.VAR_INT, EraSync::index, EraSync::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** "Open the dial for the machine at this position, showing this version." */
    public record OpenDial(BlockPos pos, int index) implements CustomPayload {
        public static final CustomPayload.Id<OpenDial> ID =
                new CustomPayload.Id<>(Identifier.of(TimeMachineMod.MOD_ID, "open_dial"));
        public static final PacketCodec<RegistryByteBuf, OpenDial> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, OpenDial::pos,
                PacketCodecs.VAR_INT, OpenDial::index,
                OpenDial::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** "Engage." The dial has been turned to this version and the lever pulled. */
    public record Engage(BlockPos pos, int index) implements CustomPayload {
        public static final CustomPayload.Id<Engage> ID =
                new CustomPayload.Id<>(Identifier.of(TimeMachineMod.MOD_ID, "engage"));
        public static final PacketCodec<RegistryByteBuf, Engage> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, Engage::pos,
                PacketCodecs.VAR_INT, Engage::index,
                Engage::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
