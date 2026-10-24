package dev.veritydark.net;

import dev.veritydark.client.ClientJournal;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: which journal pages the player has unlocked. */
public record JournalPacket(long mask) {

    public static void encode(JournalPacket msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.mask);
    }

    public static JournalPacket decode(FriendlyByteBuf buf) {
        return new JournalPacket(buf.readLong());
    }

    public static void handle(JournalPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientJournal.mask = msg.mask);
        ctx.get().setPacketHandled(true);
    }
}
