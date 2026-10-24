package dev.veritydark.item;

import dev.veritydark.VerityConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Hold it (either hand). It beeps faster the closer an anomaly or stalker is. No direction, just proximity. */
public class AnomalyDetectorItem extends Item {
    public AnomalyDetectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.veritydark.anomaly_detector.tooltip1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.veritydark.anomaly_detector.tooltip2",
                VerityConfig.DETECTOR_RANGE.get()).withStyle(ChatFormatting.GRAY));
    }
}
