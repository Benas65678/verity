package dev.veritydark.item;

import dev.veritydark.ModContent;
import dev.veritydark.VerityConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Passive trinket. Works from anywhere in the inventory (see SanityManager / AngerManager). */
public class SmileCharmItem extends Item {
    public SmileCharmItem(Properties properties) {
        super(properties);
    }

    public static boolean isCarriedBy(Player player) {
        Item charm = ModContent.SMILE_CHARM.get();
        for (ItemStack s : player.getInventory().items) if (s.is(charm)) return true;
        for (ItemStack s : player.getInventory().offhand) if (s.is(charm)) return true;
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int pct = (int) Math.round((1.0 - VerityConfig.CHARM_DRAIN_MULT.get()) * 100.0);
        tooltip.add(Component.translatable("item.veritydark.smile_charm.tooltip1", pct).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.veritydark.smile_charm.tooltip2").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("item.veritydark.smile_charm.tooltip3").withStyle(ChatFormatting.DARK_GRAY));
    }
}
