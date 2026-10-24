package dev.veritydark.effect;

import dev.veritydark.SanityManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Dread: sanity bleeds away and you move a little slower. Applied by anomaly hits. */
public class DreadEffect extends MobEffect {
    public DreadEffect() {
        super(MobEffectCategory.HARMFUL, 0x3A2A5C);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, "7a1c3b9e-5d44-4f0a-9a3e-0f1d2c3b4a55",
                -0.10, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;
        if (entity instanceof ServerPlayer player) {
            SanityManager.add(player, -1.2F * (amplifier + 1));
        }
    }
}
