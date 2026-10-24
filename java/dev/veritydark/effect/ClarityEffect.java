package dev.veritydark.effect;

import dev.veritydark.AngerManager;
import dev.veritydark.SanityManager;
import dev.veritydark.VerityUtil;
import dev.veritydark.entity.AnomalyEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Clarity: once per second it
 *  - restores sanity to players,
 *  - calms Verity,
 *  - burns anomalies.
 * Applied by tonics, lingering clouds and cookies.
 */
public class ClarityEffect extends MobEffect {
    public ClarityEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF5D142);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;
        if (entity instanceof ServerPlayer player) {
            SanityManager.add(player, 0.6F * (amplifier + 1));
        } else if (VerityUtil.isVerity(entity)) {
            AngerManager.addAnger(entity, -4.0F * (amplifier + 1));
        } else if (entity instanceof AnomalyEntity) {
            entity.hurt(entity.level().damageSources().magic(), 1.5F * (amplifier + 1));
        }
    }
}
