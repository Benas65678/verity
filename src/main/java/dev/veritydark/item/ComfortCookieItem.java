package dev.veritydark.item;

import dev.veritydark.ModContent;
import dev.veritydark.SanityManager;
import dev.veritydark.VerityConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A warm cookie with a smile on it. Eat it for sanity, or hand it to Verity (see VerityInteractions). */
public class ComfortCookieItem extends Item {
    public ComfortCookieItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            SanityManager.add(player, VerityConfig.COOKIE_SANITY.get().floatValue());
            player.addEffect(new MobEffectInstance(ModContent.CLARITY.get(), 200, 0));
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.veritydark.comfort_cookie.tooltip1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.veritydark.comfort_cookie.tooltip2").withStyle(ChatFormatting.YELLOW));
    }
}
