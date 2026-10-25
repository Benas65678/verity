package dev.veritydark;

import dev.veritydark.entity.AnomalyEntity;
import dev.veritydark.item.NullBladeItem;
import dev.veritydark.item.SmileCharmItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Verity's anger (0-100), stored on the Verity entity itself.
 *
 * Verity's own mood isn't exposed to addons, so this tracks a separate anger value:
 * it rises at night, with low player sanity, as the world ages, when you hold the Null Blade
 * and when Verity is hurt; it falls in daylight (with healthy sanity), near a lit Containment
 * Lantern or Smile Lamp, with the Smile Charm, and when you feed it a Comfort Cookie.
 *
 * The anger also drives Verity's combat stats (speed, damage, armor, knockback resistance)
 * and how often it summons anomalies.
 */
@Mod.EventBusSubscriber(modid = VerityDark.MODID)
public final class AngerManager {
    private static final String KEY = "veritydark_anger";
    private static final String TIER_KEY = "veritydark_tier";

    private static final UUID SPEED_ID = UUID.fromString("9b6f0c11-42a7-4e0d-8c55-1a2b3c4d5e01");
    private static final UUID FLY_ID = UUID.fromString("9b6f0c11-42a7-4e0d-8c55-1a2b3c4d5e02");
    private static final UUID DAMAGE_ID = UUID.fromString("9b6f0c11-42a7-4e0d-8c55-1a2b3c4d5e03");
    private static final UUID ARMOR_ID = UUID.fromString("9b6f0c11-42a7-4e0d-8c55-1a2b3c4d5e04");
    private static final UUID KB_ID = UUID.fromString("9b6f0c11-42a7-4e0d-8c55-1a2b3c4d5e05");

    public enum Tier {
        CALM("calm", ChatFormatting.GREEN),
        UNEASY("uneasy", ChatFormatting.YELLOW),
        AGITATED("agitated", ChatFormatting.GOLD),
        FURIOUS("furious", ChatFormatting.RED);

        public final String key;
        public final ChatFormatting color;

        Tier(String key, ChatFormatting color) {
            this.key = key;
            this.color = color;
        }

        public Component display() {
            return Component.translatable("veritydark.mood." + key).withStyle(color);
        }
    }

    public static Tier tierOf(float anger) {
        if (anger >= 75.0F) return Tier.FURIOUS;
        if (anger >= 50.0F) return Tier.AGITATED;
        if (anger >= 25.0F) return Tier.UNEASY;
        return Tier.CALM;
    }

    public static float getAnger(Entity verity) {
        return verity.getPersistentData().getFloat(KEY);
    }

    public static void setAnger(Entity verity, float value) {
        verity.getPersistentData().putFloat(KEY, Mth.clamp(value, 0.0F, 100.0F));
    }

    public static void addAnger(Entity verity, float delta) {
        setAnger(verity, getAnger(verity) + delta);
    }

    private static boolean holdsBlade(Entity entity) {
        return entity instanceof LivingEntity living && living.getMainHandItem().getItem() instanceof NullBladeItem;
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || !VerityConfig.on()) return;
        Entity attacker = event.getSource().getEntity();

        if (VerityUtil.isVerity(target)) {
            addAnger(target, VerityConfig.ANGER_HURT.get().floatValue());
            if (attacker instanceof ServerPlayer hitter && VerityConfig.TRUST_ENABLED.get()) {
                TrustManager.add(target, -8.0F, hitter);
            }
            if (holdsBlade(attacker)) addAnger(target, VerityConfig.BLADE_HIT_ANGER.get().floatValue());
        }
        if (target instanceof AnomalyEntity && holdsBlade(attacker)) {
            event.setAmount(event.getAmount() + VerityConfig.BLADE_ANOMALY_BONUS.get().floatValue());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) return;
        if (!(event.level instanceof ServerLevel level) || level.getGameTime() % 20 != 0) return;
        if (!VerityConfig.on()) return;

        Set<Entity> done = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            List<Entity> found = level.getEntities(player, player.getBoundingBox().inflate(48.0), VerityUtil::isVerity);
            for (Entity verity : found) {
                if (!done.add(verity)) continue;
                tickAnger(level, verity, player);
                TrustManager.tick(level, verity, player);
                trySummon(level, verity, player);
            }
        }
    }

    private static void tickAnger(ServerLevel level, Entity verity, ServerPlayer player) {
        float sanityPct = SanityManager.get(player) / SanityManager.max();
        long time = level.getDayTime();
        long timeOfDay = time % 24000L;
        boolean night = timeOfDay >= 13000L && timeOfDay < 23000L;
        int days = (int) Math.min(time / 24000L, 40L);

        double d = VerityConfig.ANGER_BASE.get();
        d += days * VerityConfig.ANGER_PER_DAY.get();
        d += (1.0F - sanityPct) * VerityConfig.ANGER_LOW_SANITY.get();
        if (night) d += VerityConfig.ANGER_NIGHT.get();
        if (!night && sanityPct > 0.7F) d -= VerityConfig.ANGER_DAY_COOLDOWN.get();

        // Items the player carries change how Verity feels about them.
        if (holdsBlade(player)) d += VerityConfig.BLADE_HOLD_ANGER.get();
        if (SmileCharmItem.isCarriedBy(player)) d -= VerityConfig.CHARM_CALM.get();

        float before = getAnger(verity);
        addAnger(verity, (float) d);
        float anger = getAnger(verity);

        // Stats follow the anger.
        applyStats(verity, anger);

        // Mood changes are announced with a sound; the furious stage gets a warning for everyone nearby.
        Tier now = tierOf(anger);
        int lastTier = verity.getPersistentData().getInt(TIER_KEY);
        if (now.ordinal() != lastTier) {
            verity.getPersistentData().putInt(TIER_KEY, now.ordinal());
            if (now.ordinal() > lastTier && now == Tier.FURIOUS) {
                level.playSound(null, verity.blockPosition(), ModSounds.ANGER_RISE.get(), SoundSource.HOSTILE, 1.6F, 1.0F);
            }
        }

        // Visible tells so the player can read the mood.
        if (anger >= 75.0F) {
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, verity.getX(), verity.getY() + 0.9, verity.getZ(),
                    1, 0.2, 0.2, 0.2, 0.0);
        } else if (anger >= VerityConfig.SPAWN_ANGER_THRESHOLD.get()) {
            level.sendParticles(ParticleTypes.SMOKE, verity.getX(), verity.getY() + 0.9, verity.getZ(),
                    2, 0.2, 0.2, 0.2, 0.01);
        }
    }

    // ------------------------------------------------------------------ stats

    /** Applies the anger-scaled bonuses to Verity. Safe on any entity: missing attributes are skipped. */
    public static void applyStats(Entity entity, float anger) {
        if (!(entity instanceof LivingEntity verity)) return;
        double f = VerityConfig.STATS_ENABLED.get() ? Mth.clamp(anger / 100.0F, 0.0F, 1.0F) : 0.0;
        setMod(verity, Attributes.MOVEMENT_SPEED, SPEED_ID, "veritydark.anger_speed",
                VerityConfig.STAT_SPEED.get() * f, AttributeModifier.Operation.MULTIPLY_BASE);
        setMod(verity, Attributes.FLYING_SPEED, FLY_ID, "veritydark.anger_fly",
                VerityConfig.STAT_SPEED.get() * f, AttributeModifier.Operation.MULTIPLY_BASE);
        setMod(verity, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "veritydark.anger_damage",
                VerityConfig.STAT_DAMAGE.get() * f, AttributeModifier.Operation.MULTIPLY_BASE);
        setMod(verity, Attributes.ARMOR, ARMOR_ID, "veritydark.anger_armor",
                VerityConfig.STAT_ARMOR.get() * f, AttributeModifier.Operation.ADDITION);
        setMod(verity, Attributes.KNOCKBACK_RESISTANCE, KB_ID, "veritydark.anger_kb",
                VerityConfig.STAT_KNOCKBACK_RES.get() * f, AttributeModifier.Operation.ADDITION);
    }

    private static void setMod(LivingEntity entity, Attribute attribute, UUID id, String name, double amount,
                               AttributeModifier.Operation op) {
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null) {
            if (Math.abs(existing.getAmount() - amount) < 1.0E-3) return;
            inst.removeModifier(id);
        }
        if (amount > 1.0E-4) inst.addTransientModifier(new AttributeModifier(id, name, amount, op));
    }

    // ------------------------------------------------------------------ summoning

    private static void trySummon(ServerLevel level, Entity verity, ServerPlayer player) {
        if (!VerityConfig.ANOMALIES_ENABLED.get()) return;
        if (level.getDifficulty() == Difficulty.PEACEFUL || player.isCreative()) return;
        if (verity.distanceToSqr(player) > 40.0 * 40.0) return;

        float anger = getAnger(verity);
        double threshold = VerityConfig.SPAWN_ANGER_THRESHOLD.get();
        if (anger < threshold) return;

        int cap = Math.min(VerityConfig.ANOMALY_CAP.get(), 1 + (int) (anger / 20.0F));
        int existing = level.getEntitiesOfClass(AnomalyEntity.class, player.getBoundingBox().inflate(40.0)).size();
        if (existing >= cap) return;

        double chance = ((anger - threshold) / Math.max(1.0, 100.0 - threshold)) * VerityConfig.SPAWN_CHANCE_MAX.get();
        if (VerityConfig.TRUST_ENABLED.get()) chance *= 1.0 - TrustManager.get(verity) / 150.0; // a trusted friend summons less
        if (level.random.nextDouble() > chance) return;

        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double dist = 6.0 + level.random.nextDouble() * 8.0;
            int x = Mth.floor(verity.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(verity.getZ() + Math.sin(angle) * dist);
            int top = Mth.floor(verity.getY()) + 3;
            int bottom = Mth.floor(verity.getY()) - 6;

            for (int y = top; y >= bottom; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockPos below = pos.below();
                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
                if (anger < 70.0F && level.getMaxLocalRawBrightness(pos) > 8) break; // too bright for now

                AnomalyEntity anomaly = ModContent.ANOMALY.get().create(level);
                if (anomaly == null) return;
                anomaly.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
                if (!level.noCollision(anomaly)) {
                    anomaly.discard();
                    continue;
                }
                anomaly.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
                anomaly.scaleToAnger(anger);
                level.addFreshEntity(anomaly);

                level.sendParticles(ParticleTypes.LARGE_SMOKE, x + 0.5, y + 1.0, z + 0.5, 12, 0.3, 0.8, 0.3, 0.02);
                return;
            }
        }
    }

    private AngerManager() {}
}
