package dev.veritydark.item;

import dev.veritydark.entity.ThrownSanityTonic;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Splash or lingering Sanity Tonic. Throw it; ThrownSanityTonic does the rest. */
public class ThrowableTonicItem extends Item {
    private final boolean lingering;

    public ThrowableTonicItem(Properties properties, boolean lingering) {
        super(properties);
        this.lingering = lingering;
    }

    public boolean isLingering() {
        return lingering;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW,
                SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            ThrownSanityTonic thrown = new ThrownSanityTonic(level, player);
            thrown.setItem(stack);
            thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
            level.addFreshEntity(thrown);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(lingering
                ? "item.veritydark.lingering_sanity_tonic.tooltip1"
                : "item.veritydark.splash_sanity_tonic.tooltip1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.veritydark.splash_sanity_tonic.tooltip2").withStyle(ChatFormatting.YELLOW));
    }
}
