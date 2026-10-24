package dev.veritydark.item;

import dev.veritydark.VerityConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A sword forged from Distortion Shards.
 * The bonus damage against anomalies and the anger it causes in Verity live in AngerManager (LivingHurtEvent).
 */
public class NullBladeItem extends SwordItem {
    public NullBladeItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.veritydark.null_blade.tooltip1",
                String.format("%.0f", VerityConfig.BLADE_ANOMALY_BONUS.get())).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.veritydark.null_blade.tooltip2").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.veritydark.null_blade.tooltip3").withStyle(ChatFormatting.DARK_GRAY));
    }
}
