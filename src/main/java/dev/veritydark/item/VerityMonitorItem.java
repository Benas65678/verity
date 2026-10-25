package dev.veritydark.item;

import dev.veritydark.AngerManager;
import dev.veritydark.InsanityTier;
import dev.veritydark.TrustManager;
import dev.veritydark.SanityManager;
import dev.veritydark.VerityConfig;
import dev.veritydark.VerityUtil;
import dev.veritydark.entity.AnomalyEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Right-click for a readout: your sanity, the nearest Verity's mood and combat stats, and nearby anomalies. */
public class VerityMonitorItem extends Item {
    public VerityMonitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            float sanity = SanityManager.get(sp);
            float max = SanityManager.max();
            float pct = sanity / max;
            InsanityTier tier = InsanityTier.of(pct);

            sp.sendSystemMessage(Component.translatable("item.veritydark.verity_monitor.header")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            sp.sendSystemMessage(Component.translatable("veritydark.monitor.sanity",
                    Math.round(sanity), Math.round(max),
                    tier.display()));

            net.minecraft.world.entity.Entity verity = VerityUtil.nearestVerity(sp, 64.0);
            if (verity == null) {
                sp.sendSystemMessage(Component.translatable("veritydark.monitor.no_verity").withStyle(ChatFormatting.GRAY));
            } else {
                float anger = AngerManager.getAnger(verity);
                double f = VerityConfig.STATS_ENABLED.get() ? anger / 100.0 : 0.0;
                sp.sendSystemMessage(Component.translatable("veritydark.monitor.verity",
                        AngerManager.tierOf(anger).display(),
                        Math.round(anger)));
                sp.sendSystemMessage(Component.translatable("veritydark.monitor.stats",
                        Math.round(VerityConfig.STAT_SPEED.get() * f * 100.0),
                        Math.round(VerityConfig.STAT_DAMAGE.get() * f * 100.0),
                        String.format("%.1f", VerityConfig.STAT_ARMOR.get() * f)).withStyle(ChatFormatting.GRAY));
                if (VerityConfig.TRUST_ENABLED.get()) {
                    float trust = TrustManager.get(verity);
                    sp.sendSystemMessage(Component.translatable("veritydark.monitor.trust",
                            TrustManager.levelOf(trust).display(), Math.round(trust)));
                }
                sp.sendSystemMessage(Component.translatable("veritydark.monitor.summons",
                        anger >= VerityConfig.SPAWN_ANGER_THRESHOLD.get()
                                ? Component.translatable("veritydark.monitor.summoning").withStyle(ChatFormatting.RED)
                                : Component.translatable("veritydark.monitor.quiet").withStyle(ChatFormatting.GREEN)));
            }

            int anomalies = level.getEntitiesOfClass(AnomalyEntity.class, sp.getBoundingBox().inflate(32.0)).size();
            sp.sendSystemMessage(Component.translatable("veritydark.monitor.anomalies", anomalies));

            level.playSound(null, sp.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 0.6F);
        }
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.veritydark.verity_monitor.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
