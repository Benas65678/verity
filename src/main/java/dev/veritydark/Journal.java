package dev.veritydark;

import dev.veritydark.net.JournalPacket;
import dev.veritydark.net.Network;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Pages of the Verity Journal. Each player discovers them by playing; the unlocked set is a bit mask stored on the
 * player and synced to the client, where the journal screen shows locked pages as "???".
 */
@Mod.EventBusSubscriber(modid = VerityDark.MODID)
public final class Journal {
    private static final String KEY = "veritydark_journal";

    public enum Entry {
        SANITY("sanity", true),
        TONICS("tonics", true),
        VERITY("verity", false),
        ANGER("anger", false),
        ANOMALY("anomaly", false),
        STALKER("stalker", false),
        TIER_SHAKEN("tier_shaken", false),
        TIER_FRACTURED("tier_fractured", false),
        TIER_BROKEN("tier_broken", false),
        HALLUCINATIONS("hallucinations", false),
        GIFTS("gifts", false),
        TRUST("trust", false),
        ARMOR("armor", false),
        DETECTOR("detector", false),
        FACILITY("facility", false);

        public final String key;
        public final boolean startsUnlocked;

        Entry(String key, boolean startsUnlocked) {
            this.key = key;
            this.startsUnlocked = startsUnlocked;
        }

        public long bit() {
            return 1L << ordinal();
        }
    }

    public static long defaultMask() {
        long m = 0L;
        for (Entry e : Entry.values()) if (e.startsUnlocked) m |= e.bit();
        return m;
    }

    public static long mask(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return (persisted.contains(KEY) ? persisted.getLong(KEY) : 0L) | defaultMask();
    }

    public static boolean has(Player player, Entry entry) {
        return (mask(player) & entry.bit()) != 0L;
    }

    public static void unlock(ServerPlayer player, Entry entry) {
        long mask = mask(player);
        if ((mask & entry.bit()) != 0L) return;
        mask |= entry.bit();
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putLong(KEY, mask);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
        sync(player);
        player.displayClientMessage(Component.translatable("veritydark.journal.updated",
                Component.translatable("journal.veritydark." + entry.key + ".title")).withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.7F, 0.8F);
    }

    public static void sync(ServerPlayer player) {
        Network.sendTo(player, new JournalPacket(mask(player)));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) sync(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) sync(p);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) sync(p);
    }

    private Journal() {}
}
