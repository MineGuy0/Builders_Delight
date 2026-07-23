package com.zrollus.bd;

import com.zrollus.bd.Lib.FluidBreakerHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class ModNetworking {
    public record UpdateNudgePayload(int x, int y, int z) implements CustomPayload {
        public static final Id<UpdateNudgePayload> ID = new Id<>(Identifier.of("buildersdelight", "update_nudge"));
        public static final PacketCodec<RegistryByteBuf, UpdateNudgePayload> CODEC = PacketCodec.ofStatic(
                (buf, payload) -> { buf.writeInt(payload.x); buf.writeInt(payload.y); buf.writeInt(payload.z); },
                buf -> new UpdateNudgePayload(buf.readInt(), buf.readInt(), buf.readInt()));
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ConfirmNudgePayload() implements CustomPayload {
        public static final Id<ConfirmNudgePayload> ID = new Id<>(Identifier.of("buildersdelight", "confirm_nudge"));
        public static final PacketCodec<RegistryByteBuf, ConfirmNudgePayload> CODEC = PacketCodec.unit(new ConfirmNudgePayload());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record FluidMiningPayload(BlockPos pos, boolean mining) implements CustomPayload {
        public static final Id<FluidMiningPayload> ID = new Id<>(Identifier.of("bd", "fluid_mining"));
        public static final PacketCodec<RegistryByteBuf, FluidMiningPayload> CODEC = PacketCodec.ofStatic(
                (buf, payload) -> {
                    buf.writeBlockPos(payload.pos());
                    buf.writeBoolean(payload.mining());
                },
                buf -> new FluidMiningPayload(buf.readBlockPos(), buf.readBoolean()));
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void init() {
        PayloadTypeRegistry.playC2S().register(UpdateNudgePayload.ID, UpdateNudgePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ConfirmNudgePayload.ID, ConfirmNudgePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(FluidMiningPayload.ID, FluidMiningPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(UpdateNudgePayload.ID, (payload, context) ->
                context.server().execute(() -> com.zrollus.bd.utils.Nudge.setFor(
                        context.player().getUuid(),
                        new net.minecraft.util.math.Vec3i(payload.x(), payload.y(), payload.z()))));
        ServerPlayNetworking.registerGlobalReceiver(FluidMiningPayload.ID, (payload, context) ->
                context.server().execute(() -> FluidBreakerHandler.update(
                        context.player(), payload.pos(), payload.mining())));
    }

    private ModNetworking() {}
}
