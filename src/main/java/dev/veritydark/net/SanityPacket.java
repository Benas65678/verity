package dev.veritydark.net;

import dev.veritydark.client.ClientSanity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: current sanity and max sanity. */
public record SanityPacket(float value, float max) {

    public static void encode(SanityPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.value);
        buf.writeFloat(msg.max);
    }

    public static SanityPacket decode(FriendlyByteBuf buf) {
        return new SanityPacket(buf.readFloat(), buf.readFloat());
    }

    public static void handle(SanityPacket msg, Supplier<NetworkEvent.Context> ctx) {
        // ClientSanity is a plain data holder, safe to touch from the common side.
        ctx.get().enqueueWork(() -> ClientSanity.set(msg.value, msg.max));
        ctx.get().setPacketHandled(true);
    }
}
