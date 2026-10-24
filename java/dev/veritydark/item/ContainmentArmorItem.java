package dev.veritydark.item;

import dev.veritydark.VerityConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Containment armor: wearers are harder for mobs to spot, take less damage from anomalies, and the full set restores sanity. */
public class ContainmentArmorItem extends ArmorItem {
    public ContainmentArmorItem(Type type, Properties properties) {
        super(ModArmorMaterials.CONTAINMENT, type, properties);
    }

    /** How many containment pieces the entity is wearing (0-4). */
    public static int pieces(LivingEntity entity) {
        int n = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            if (stack.getItem() instanceof ContainmentArmorItem) n++;
        }
        return n;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.veritydark.armor.tooltip1",
                Math.round(VerityConfig.ARMOR_STEALTH.get() * 100.0)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.veritydark.armor.tooltip2",
                Math.round(VerityConfig.ARMOR_REDUCTION.get() * 100.0)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.veritydark.armor.tooltip3").withStyle(ChatFormatting.DARK_AQUA));
    }
}
