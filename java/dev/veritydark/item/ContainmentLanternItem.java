package dev.veritydark.item;

import dev.veritydark.AngerManager;
import dev.veritydark.ModSounds;
import dev.veritydark.SanityManager;
import dev.veritydark.VerityConfig;
import dev.veritydark.VerityUtil;
import dev.veritydark.entity.AnomalyEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Right-click to light. While lit and held (either hand) it:
 *  - calms every Verity in range (drains their anger, drops their target),
 *  - repels and slows anomalies,
 *  - slowly restores the holder's sanity and stops darkness drain.
 * Burns 1 durability per second. Repair with glowstone dust on an anvil.
 */
public class ContainmentLanternItem extends Item {
    private static final String LIT = "Lit";

    public ContainmentLanternItem(Properties properties) {
        super(properties);
    }

    public static boolean isLit(ItemStack stack) {
        return stack.getItem() instanceof ContainmentLanternItem && stack.hasTag() && stack.getTag().getBoolean(LIT);
    }

    public static boolean isActiveIn(Player player) {
        return isLit(player.getMainHandItem()) || isLit(player.getOffhandItem());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean nowLit = !isLit(stack);
        if (nowLit && stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            return InteractionResultHolder.fail(stack); // out of fuel
        }
        if (!level.isClientSide) {
            stack.getOrCreateTag().putBoolean(LIT, nowLit);
            level.playSound(null, player.blockPosition(),
                    nowLit ? ModSounds.LANTERN_IGNITE.get() : ModSounds.LANTERN_EXTINGUISH.get(),
                    SoundSource.PLAYERS, 0.8F, nowLit ? 1.0F : 1.2F);
        }
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;
        if (!isLit(stack)) return;
        boolean held = selected || player.getOffhandItem() == stack;
        if (!held || player.tickCount % 20 != 0) return;

        // Fuel
        if (stack.hurt(1, level.getRandom(), player)) {
            stack.shrink(1);
            level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
            return;
        }
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            stack.getOrCreateTag().putBoolean(LIT, false);
            level.playSound(null, player.blockPosition(), ModSounds.LANTERN_EXTINGUISH.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            return;
        }

        SanityManager.add(player, VerityConfig.LANTERN_REGEN.get().floatValue());

        ServerLevel serverLevel = (ServerLevel) level;
        serverLevel.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.0, player.getZ(),
                3, 0.4, 0.4, 0.4, 0.0);

        double radius = VerityConfig.LANTERN_RADIUS.get();
        List<Entity> targets = level.getEntities(player, player.getBoundingBox().inflate(radius),
                e -> e instanceof AnomalyEntity || VerityUtil.isVerity(e));

        for (Entity target : targets) {
            if (target instanceof AnomalyEntity anomaly) {
                anomaly.setTarget(null);
                anomaly.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
                Vec3 away = anomaly.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
                if (away.lengthSqr() > 1.0E-4) {
                    Vec3 push = away.normalize().scale(0.35);
                    anomaly.setDeltaMovement(anomaly.getDeltaMovement().add(push.x, 0.1, push.z));
                    anomaly.hurtMarked = true;
                }
            } else {
                AngerManager.addAnger(target, -VerityConfig.LANTERN_CALM.get().floatValue());
                if (target instanceof Mob mob) {
                    mob.setTarget(null);
                    mob.setAggressive(false);
                }
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 0.8, target.getZ(),
                        2, 0.25, 0.25, 0.25, 0.0);
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isLit(stack);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.GLOWSTONE_DUST);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.veritydark.containment_lantern.tooltip1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.veritydark.containment_lantern.tooltip2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
