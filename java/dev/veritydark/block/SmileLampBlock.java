package dev.veritydark.block;

import dev.veritydark.AngerManager;
import dev.veritydark.ModSounds;
import dev.veritydark.SanityManager;
import dev.veritydark.VerityConfig;
import dev.veritydark.VerityUtil;
import dev.veritydark.entity.AnomalyEntity;
import dev.veritydark.entity.StalkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A lamp with a smile on every side. Place it and the area around it becomes a haven:
 * players regain sanity, Verity calms down, anomalies are pushed away.
 * It re-schedules its own tick once a second, so it needs no block entity.
 */
public class SmileLampBlock extends Block {
    public SmileLampBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** Lamps generated with a structure never got onPlace, so a random tick starts their pulse. */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, 20);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 20);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        double r = VerityConfig.LAMP_RADIUS.get();
        Vec3 center = Vec3.atCenterOf(pos);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(r))) {
            if (e.position().distanceToSqr(center) > r * r) continue;
            if (e instanceof ServerPlayer player) {
                SanityManager.add(player, VerityConfig.LAMP_REGEN.get().floatValue());
            } else if (VerityUtil.isVerity(e)) {
                AngerManager.addAnger(e, -VerityConfig.LAMP_CALM.get().floatValue());
            } else if (e instanceof AnomalyEntity || e instanceof StalkerEntity) {
                net.minecraft.world.entity.Mob mob = (net.minecraft.world.entity.Mob) e;
                mob.setTarget(null);
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
                Vec3 away = mob.position().subtract(center).multiply(1.0, 0.0, 1.0);
                if (away.lengthSqr() > 1.0E-4) {
                    Vec3 push = away.normalize().scale(0.4);
                    mob.setDeltaMovement(mob.getDeltaMovement().add(push.x, 0.1, push.z));
                    mob.hurtMarked = true;
                }
            }
        }
        if ((level.getGameTime() / 20L) % 8L == 0L) {
            level.playSound(null, pos, ModSounds.LAMP_PULSE.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
        }
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.7, center.z, 2, 0.3, 0.2, 0.3, 0.01);
        level.scheduleTick(pos, this, 20);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.05, pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                    0.0, 0.02, 0.0);
        }
    }
}
