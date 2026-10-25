package dev.veritydark;

import dev.veritydark.entity.AnomalyEntity;
import dev.veritydark.entity.StalkerEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * How much Verity trusts you (0-100, stored on the Verity entity). Raised by gifts and calm company, lowered by
 * hurting it, angering it and handing it things it hates.
 *
 *   Stranger 0 | Acquaintance 20 (calms on its own) | Friend 40 (leaves you presents) |
 *   Companion 60 (zaps anomalies near it) | Bonded 80 (shields you, restores sanity)
 */
public final class TrustManager {
    private static final String KEY = "veritydark_trust";
    private static final String GIFTS = "veritydark_gifts";
    private static final String PRESENT = "veritydark_present";

    public enum Level {
        STRANGER("stranger", 0, ChatFormatting.GRAY),
        ACQUAINTANCE("acquaintance", 20, ChatFormatting.WHITE),
        FRIEND("friend", 40, ChatFormatting.GREEN),
        COMPANION("companion", 60, ChatFormatting.AQUA),
        BONDED("bonded", 80, ChatFormatting.LIGHT_PURPLE);

        public final String key;
        public final int min;
        public final ChatFormatting color;

        Level(String key, int min, ChatFormatting color) {
            this.key = key;
            this.min = min;
            this.color = color;
        }

        public Component display() {
            return Component.translatable("veritydark.trust." + key).withStyle(color);
        }
    }

    /** What a gift does: trust change and anger change (negative anger = calms Verity). */
    public record Gift(float trust, float anger) {}

    public static Level levelOf(float trust) {
        Level result = Level.STRANGER;
        for (Level l : Level.values()) if (trust >= l.min) result = l;
        return result;
    }

    public static float get(Entity verity) {
        CompoundTag tag = verity.getPersistentData();
        return tag.contains(KEY) ? tag.getFloat(KEY) : VerityConfig.TRUST_START.get().floatValue();
    }

    public static void add(Entity verity, float delta, @Nullable ServerPlayer who) {
        float before = get(verity);
        float after = Mth.clamp(before + delta, 0.0F, 100.0F);
        verity.getPersistentData().putFloat(KEY, after);
        Level lb = levelOf(before);
        Level la = levelOf(after);
        if (who != null && la.ordinal() > lb.ordinal()) {
            who.sendSystemMessage(Component.translatable("veritydark.trust.up", la.display()).withStyle(ChatFormatting.GOLD));
            Journal.unlock(who, Journal.Entry.TRUST);
            if (la.ordinal() >= Level.FRIEND.ordinal()) ModAdvancements.grant(who, "good_friends");
            if (la == Level.BONDED) ModAdvancements.grant(who, "best_friends");
        } else if (who != null && la.ordinal() < lb.ordinal()) {
            who.sendSystemMessage(Component.translatable("veritydark.trust.down", la.display()).withStyle(ChatFormatting.RED));
        }
    }

    /** The gift table. Returns null for items Verity has no opinion about. */
    @Nullable
    public static Gift giftFor(ItemStack stack) {
        if (stack.is(ModContent.COMFORT_COOKIE.get())) return new Gift(6.0F, -VerityConfig.COOKIE_FEED_CALM.get().floatValue());
        if (stack.is(ModContent.TONIC.get())) return new Gift(4.0F, -10.0F);
        if (stack.is(Items.CAKE)) return new Gift(8.0F, -20.0F);
        if (stack.is(Items.PUMPKIN_PIE)) return new Gift(4.0F, -8.0F);
        if (stack.is(Items.GOLDEN_APPLE)) return new Gift(5.0F, -10.0F);
        if (stack.is(Items.GOLDEN_CARROT)) return new Gift(3.0F, -6.0F);
        if (stack.is(Items.HONEY_BOTTLE)) return new Gift(3.0F, -6.0F);
        if (stack.is(Items.COOKIE)) return new Gift(2.0F, -5.0F);
        if (stack.is(Items.GLOWSTONE_DUST)) return new Gift(1.0F, -2.0F);
        // things it dislikes
        if (stack.is(ModContent.DISTORTION_SHARD.get())) return new Gift(-4.0F, 8.0F);
        if (stack.is(Items.ROTTEN_FLESH)) return new Gift(-3.0F, 5.0F);
        if (stack.is(Items.SPIDER_EYE)) return new Gift(-2.0F, 4.0F);
        return null;
    }

    /** Applies a gift. Returns true if it was accepted (the item is consumed). */
    public static void applyGift(ServerLevel level, ServerPlayer player, Entity verity, ItemStack stack, Gift gift) {
        boolean trustOn = VerityConfig.TRUST_ENABLED.get();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String itemKey = id == null ? "unknown" : id.toString();

        CompoundTag gifts = verity.getPersistentData().getCompound(GIFTS);
        long now = level.getGameTime();
        boolean stale = gifts.contains(itemKey) && now - gifts.getLong(itemKey) < 6000L; // the same thing again within 5 min
        gifts.putLong(itemKey, now);
        verity.getPersistentData().put(GIFTS, gifts);

        float trustDelta = gift.trust() * VerityConfig.TRUST_GIFT_MULT.get().floatValue();
        if (trustDelta > 0.0F && stale) trustDelta *= 0.25F;

        AngerManager.addAnger(verity, gift.anger() * (stale && gift.anger() < 0 ? 0.5F : 1.0F));
        if (trustOn) add(verity, trustDelta, player);

        boolean good = gift.trust() > 0.0F;
        level.sendParticles(good ? ParticleTypes.HEART : ParticleTypes.ANGRY_VILLAGER,
                verity.getX(), verity.getY() + 0.9, verity.getZ(), good ? 6 : 4, 0.35, 0.35, 0.35, 0.0);
        if (good) {
            level.playSound(null, verity.blockPosition(), ModSounds.VERITY_PLEASED.get(), SoundSource.NEUTRAL, 1.0F, stale ? 0.7F : 1.0F);
        }
        if (trustOn) {
            Component msg = stale && good
                    ? Component.translatable("veritydark.gift.stale")
                    : Component.translatable(good ? "veritydark.gift.good" : "veritydark.gift.bad",
                    levelOf(get(verity)).display(), Math.round(get(verity)));
            player.displayClientMessage(msg.copy().withStyle(good ? ChatFormatting.GREEN : ChatFormatting.RED), true);
            Journal.unlock(player, Journal.Entry.GIFTS);
            if (good) ModAdvancements.grant(player, "first_gift");
        }
    }

    /** Called once a second for each Verity that has a player nearby. */
    public static void tick(ServerLevel level, Entity verity, ServerPlayer player) {
        if (!VerityConfig.TRUST_ENABLED.get()) return;
        float trust = get(verity);
        float anger = AngerManager.getAnger(verity);
        double distSq = verity.distanceToSqr(player);

        if (distSq < 16.0 * 16.0) {
            if (anger < 25.0F) add(verity, 0.02F, player);
            else if (anger >= 75.0F) add(verity, -0.05F, null);
        }
        if (trust >= 20.0F) AngerManager.addAnger(verity, -(trust / 100.0F) * 0.15F);

        // Friends leave presents.
        long now = level.getGameTime();
        CompoundTag data = verity.getPersistentData();
        if (!data.contains(PRESENT)) data.putLong(PRESENT, now);
        long interval = VerityConfig.TRUST_GIFT_MINUTES.get() * 1200L;
        if (trust >= Level.FRIEND.min && distSq < 16.0 * 16.0 && anger < 40.0F && now - data.getLong(PRESENT) >= interval) {
            data.putLong(PRESENT, now);
            giveBack(level, verity, player, trust);
        }

        // Companions defend you, bonded friends shield you.
        if (trust >= VerityConfig.TRUST_DEFEND_AT.get() && distSq < 32.0 * 32.0) {
            AABB box = verity.getBoundingBox().inflate(12.0);
            List<LivingEntity> threats = level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e instanceof AnomalyEntity || e instanceof StalkerEntity);
            if (!threats.isEmpty()) {
                LivingEntity target = threats.get(level.random.nextInt(threats.size()));
                target.hurt(level.damageSources().magic(), 3.0F + trust / 20.0F);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.5, target.getZ(), 14, 0.3, 0.8, 0.3, 0.1);
                level.sendParticles(ParticleTypes.ENCHANT, verity.getX(), verity.getY() + 1.0, verity.getZ(), 10, 0.4, 0.6, 0.4, 0.5);
                if (trust >= Level.BONDED.min) {
                    for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, verity.getBoundingBox().inflate(16.0))) {
                        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
                    }
                }
            }
        }
        if (trust >= Level.BONDED.min && distSq < 12.0 * 12.0) {
            SanityManager.add(player, 0.4F);
        }
    }

    private static void giveBack(ServerLevel level, Entity verity, ServerPlayer player, float trust) {
        ItemStack present;
        int roll = level.random.nextInt(100);
        if (trust >= Level.BONDED.min && roll < 12) present = new ItemStack(ModContent.SMILE_CHARM.get());
        else if (trust >= Level.COMPANION.min && roll < 40) present = new ItemStack(ModContent.SPLASH_TONIC.get(), 1 + level.random.nextInt(2));
        else if (roll < 70) present = new ItemStack(ModContent.COMFORT_COOKIE.get(), 2 + level.random.nextInt(3));
        else if (roll < 88) present = new ItemStack(ModContent.TONIC.get());
        else present = new ItemStack(ModContent.DISTORTION_SHARD.get(), 1 + level.random.nextInt(2));

        ItemEntity drop = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), present);
        drop.setPickUpDelay(10);
        level.addFreshEntity(drop);
        level.sendParticles(ParticleTypes.HEART, verity.getX(), verity.getY() + 0.9, verity.getZ(), 8, 0.4, 0.4, 0.4, 0.0);
        level.playSound(null, verity.blockPosition(), ModSounds.VERITY_PLEASED.get(), SoundSource.NEUTRAL, 1.0F, 1.2F);
        player.sendSystemMessage(Component.translatable("veritydark.trust.present").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private TrustManager() {}
}
