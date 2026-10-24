package dev.veritydark.entity;

import dev.veritydark.AngerManager;
import dev.veritydark.ModContent;
import dev.veritydark.ModSounds;
import dev.veritydark.SanityManager;
import dev.veritydark.VerityConfig;
import dev.veritydark.VerityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The flying bottle. One class for both items:
 *  - Splash: instant burst. Restores sanity, calms Verity, burns anomalies.
 *  - Lingering: leaves a Clarity cloud (a vanilla AreaEffectCloud) that does the same over time.
 */
public class ThrownSanityTonic extends ThrowableItemProjectile {
    private static final double RADIUS = 4.0;

    public ThrownSanityTonic(EntityType<? extends ThrownSanityTonic> type, Level level) {
        super(type, level);
    }

    public ThrownSanityTonic(Level level, LivingEntity owner) {
        super(ModContent.THROWN_TONIC.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModContent.SPLASH_TONIC.get();
    }

    @Override
    protected float getGravity() {
        return 0.05F;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide) return;

        ServerLevel level = (ServerLevel) this.level();
        BlockPos pos = this.blockPosition();
        level.levelEvent(2002, pos, 0xF5D142);
        level.playSound(null, pos, ModSounds.TONIC_SHATTER.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);

        boolean lingering = this.getItem().is(ModContent.LINGERING_TONIC.get());
        if (lingering) {
            spawnCloud(level);
        } else {
            burst(level);
        }
        this.discard();
    }

    private void spawnCloud(ServerLevel level) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, this.getX(), this.getY(), this.getZ());
        if (this.getOwner() instanceof LivingEntity owner) cloud.setOwner(owner);
        cloud.setRadius(3.5F);
        cloud.setRadiusOnUse(-0.25F);
        cloud.setWaitTime(10);
        cloud.setDuration(600);
        cloud.setRadiusPerTick(-cloud.getRadius() / (float) cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(ModContent.CLARITY.get(), 100, 1));
        cloud.setFixedColor(0xF5D142);
        level.addFreshEntity(cloud);
    }

    private void burst(ServerLevel level) {
        Vec3 center = this.position();
        AABB box = this.getBoundingBox().inflate(RADIUS, 2.0, RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box)) {
            double dist = target.position().distanceTo(center);
            if (dist > RADIUS + 1.0) continue;
            double strength = 1.0 - Math.min(1.0, dist / (RADIUS + 1.0)) * 0.6; // 1.0 at centre, 0.4 at the edge

            if (target instanceof ServerPlayer player) {
                SanityManager.add(player, (float) (VerityConfig.SPLASH_RESTORE.get() * strength));
                player.addEffect(new MobEffectInstance(ModContent.CLARITY.get(), 400, 0));
            } else if (VerityUtil.isVerity(target)) {
                AngerManager.addAnger(target, (float) (-VerityConfig.SPLASH_CALM.get() * strength));
                level.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 0.8, target.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
            } else if (target instanceof AnomalyEntity) {
                target.hurt(level.damageSources().indirectMagic(this, this.getOwner()),
                        (float) (VerityConfig.SPLASH_ANOMALY_DAMAGE.get() * strength));
                target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            }
        }
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.3, center.z, 24, 1.2, 0.5, 1.2, 0.04);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y + 0.3, center.z, 18, 1.5, 0.5, 1.5, 0.0);
    }
}
