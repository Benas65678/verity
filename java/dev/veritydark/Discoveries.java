package dev.veritydark;

import dev.veritydark.item.AnomalyDetectorItem;
import dev.veritydark.item.ContainmentArmorItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/** Unlocks journal pages for things the player owns or has visited. */
@Mod.EventBusSubscriber(modid = VerityDark.MODID)
public final class Discoveries {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) return;
        if (!(event.player instanceof ServerPlayer player) || player.tickCount % 40 != 0) return;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof AnomalyDetectorItem) Journal.unlock(player, Journal.Entry.DETECTOR);
            else if (stack.getItem() instanceof ContainmentArmorItem) Journal.unlock(player, Journal.Entry.ARMOR);
        }
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getAdvancement().getId().equals(new net.minecraft.resources.ResourceLocation(VerityDark.MODID, "facility"))) {
            Journal.unlock(player, Journal.Entry.FACILITY);
        }
    }

    private Discoveries() {}
}
