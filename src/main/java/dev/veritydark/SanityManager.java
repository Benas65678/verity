package dev.veritydark;

import dev.veritydark.entity.AnomalyEntity;
import dev.veritydark.entity.StalkerEntity;
import dev.veritydark.item.ContainmentArmorItem;
import dev.veritydark.item.ContainmentLanternItem;
import dev.veritydark.item.SmileCharmItem;
import dev.veritydark.net.Network;
import dev.veritydark.net.SanityPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Server-side sanity: storage, passive drain/regen, syncing to the client. */
@Mod.EventBusSubscriber(modid = VerityDark.MODID)
public final class SanityManager {
    private static final String KEY = "veritydark_sanity";
    private static final String TIER_KEY = "veritydark_insanity_tier";
    private static final String WAS_BROKEN_KEY = "veritydark_was_broken";

    public static float max() {
        return VerityConfig.MAX_SANITY.get().floatValue();
    }

    public static float get(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return persisted.contains(KEY) ? persisted.getFloat(KEY) : max();
    }

    public static void set(ServerPlayer player, float value) {
        value = Mth.clamp(value, 0.0F, max());
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putFloat(KEY, value);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
        sync(player);
    }

    public static void add(ServerPlayer player, float delta) {
        float before = get(player);
        float after = Mth.clamp(before + delta, 0.0F, max());
        if (after != before) set(player, after);
    }

    public static void sync(ServerPlayer player) {
        Network.sendTo(player, new SanityPacket(get(player), max()));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) return;
        if (!(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        if (!VerityConfig.sanityOn()) return;
        if (player.isCreative() || player.isSpectator()) return;

        Level level = player.level();
        float before = get(player);
        double drain = 0.0;
        double gain = 0.0;

        int light = level.getMaxLocalRawBrightness(BlockPos.containing(player.getEyePosition()));
        boolean lanternActive = ContainmentLanternItem.isActiveIn(player);
        boolean dark = light <= VerityConfig.DARK_LIGHT_LEVEL.get();

        boolean anomalyNear = false;
        List<Entity> nearby = level.getEntities(player, player.getBoundingBox().inflate(16.0), e -> true);
        for (Entity e : nearby) {
            if (e instanceof AnomalyEntity anomaly) {
                anomalyNear = true;
                Journal.unlock(player, Journal.Entry.ANOMALY);
                if (!anomaly.isScaring()) drain += VerityConfig.NEAR_ANOMALY_DRAIN.get();
                else drain += VerityConfig.NEAR_ANOMALY_DRAIN.get() * 2.0; // the jumpscare is terrifying
            } else if (e instanceof StalkerEntity) {
                anomalyNear = true;
                drain += VerityConfig.NEAR_ANOMALY_DRAIN.get() * 0.5;
            } else if (VerityUtil.isVerity(e)) {
                float anger = AngerManager.getAnger(e);
                drain += VerityConfig.NEAR_VERITY_DRAIN.get() * (anger / 100.0F);
                Journal.unlock(player, Journal.Entry.VERITY);
                if (anger >= 25.0F) Journal.unlock(player, Journal.Entry.ANGER);
            }
        }

        if (dark && !lanternActive) {
            drain += VerityConfig.DARK_DRAIN.get();
        } else if (light >= VerityConfig.BRIGHT_LIGHT_LEVEL.get() && !anomalyNear) {
            gain += VerityConfig.LIGHT_REGEN.get();
        }

        // The full containment set slowly steadies the mind.
        if (VerityConfig.ARMOR_ENABLED.get() && ContainmentArmorItem.pieces(player) == 4) {
            gain += VerityConfig.ARMOR_SET_REGEN.get();
        }

        // Item and effect modifiers on the drain side.
        double mult = 1.0;
        if (SmileCharmItem.isCarriedBy(player)) mult *= VerityConfig.CHARM_DRAIN_MULT.get();
        if (player.hasEffect(ModContent.CLARITY.get())) mult *= 0.6;

        float after = Mth.clamp(before + (float) (gain - drain * mult), 0.0F, max());
        if (after != before) set(player, after);

        InsanityTier tier = InsanityTier.of(after / max());
        if (VerityConfig.TIERS_ENABLED.get()) {
            trackTier(player, tier);
            applyTier(player, tier, level);
        }
        if (VerityConfig.on() && VerityConfig.STALKER_ENABLED.get()) trySpawnStalker(player, level, after);
    }

    // ------------------------------------------------------------------ insanity tiers

    private static void trackTier(ServerPlayer player, InsanityTier tier) {
        CompoundTag data = player.getPersistentData();
        int last = data.contains(TIER_KEY) ? data.getInt(TIER_KEY) : 0;
        if (tier.ordinal() == last) return;
        data.putInt(TIER_KEY, tier.ordinal());

        if (tier.ordinal() > last) { // got worse
            player.displayClientMessage(Component.translatable("veritydark.tier.worse", tier.display())
                    .withStyle(ChatFormatting.RED), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.6F);
            if (tier.atLeast(InsanityTier.SHAKEN)) {
                Journal.unlock(player, Journal.Entry.TIER_SHAKEN);
                Journal.unlock(player, Journal.Entry.HALLUCINATIONS);
                ModAdvancements.grant(player, "seeing_things");
            }
            if (tier.atLeast(InsanityTier.FRACTURED)) Journal.unlock(player, Journal.Entry.TIER_FRACTURED);
            if (tier == InsanityTier.BROKEN) {
                Journal.unlock(player, Journal.Entry.TIER_BROKEN);
                ModAdvancements.grant(player, "broken_mind");
                data.putBoolean(WAS_BROKEN_KEY, true);
            }
        } else {                    // got better
            player.displayClientMessage(Component.translatable("veritydark.tier.better", tier.display())
                    .withStyle(ChatFormatting.GREEN), true);
            if (tier == InsanityTier.STABLE && data.getBoolean(WAS_BROKEN_KEY)) {
                ModAdvancements.grant(player, "pulling_through");
            }
        }
    }

    /** Lasting penalties. They refresh every second, so they fade the moment you recover. */
    private static void applyTier(ServerPlayer player, InsanityTier tier, Level level) {
        if (tier.atLeast(InsanityTier.FRACTURED)) {
            int weakness = tier == InsanityTier.BROKEN ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, weakness, true, false, true));
            if (player.tickCount % 300 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
            }
        }
        if (tier == InsanityTier.BROKEN) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false, true));
            if (player.tickCount % 200 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));
            }
            // The dark comes for you.
            if (VerityConfig.TIER_SPAWNS.get() && VerityConfig.ANOMALIES_ENABLED.get()
                    && level instanceof ServerLevel server && server.getDifficulty() != Difficulty.PEACEFUL
                    && level.random.nextFloat() < 0.04F) {
                int near = server.getEntitiesOfClass(AnomalyEntity.class, player.getBoundingBox().inflate(40.0)).size();
                if (near < 3) {
                    AnomalyEntity a = SpawnHelper.spawnAround(server, ModContent.ANOMALY.get(), player.position(),
                            10.0, 18.0, Double.NaN, 9, null);
                    if (a != null) a.scaleToAnger(60.0F);
                }
            }
        }
    }

    // ------------------------------------------------------------------ stalkers

    private static void trySpawnStalker(ServerPlayer player, Level level, float sanity) {
        if (!(level instanceof ServerLevel server) || server.getDifficulty() == Difficulty.PEACEFUL) return;
        float frac = sanity / max();
        double limit = VerityConfig.STALKER_SANITY.get();
        if (frac > limit) return;
        int cap = VerityConfig.STALKER_CAP.get();
        if (cap <= 0) return;
        int near = server.getEntitiesOfClass(StalkerEntity.class, player.getBoundingBox().inflate(48.0)).size();
        if (near >= cap) return;
        int light = level.getMaxLocalRawBrightness(BlockPos.containing(player.getEyePosition()));
        if (light > 8 && frac > 0.1F) return; // they like the dark (unless your mind is truly gone)

        double chance = VerityConfig.STALKER_CHANCE.get() * (1.0 + (limit - frac) / Math.max(0.01, limit) * 2.0);
        if (level.random.nextDouble() > chance) return;

        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        double behind = Math.atan2(-look.z, -look.x);
        SpawnHelper.spawnAround(server, ModContent.STALKER.get(), player.position(), 16.0, 26.0, behind, 9, null);
    }

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent event) {
        if (event.wakeImmediately()) return; // woken early, no rest
        if (event.getEntity() instanceof ServerPlayer player) {
            add(player, VerityConfig.SLEEP_RESTORE.get().floatValue());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player && event.isWasDeath()) {
            set(player, max() * 0.6F);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    private SanityManager() {}
}
