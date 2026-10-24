package dev.veritydark.entity;

import dev.veritydark.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A figure that exists only on YOUR screen. It is created on the client by the HallucinationManager and never
 * sent to or stored by the server, so it can't hurt anyone. Rendered see-through. It creeps closer while you look
 * away, and dissolves if you stare at it or let it get too close.
 */
public class HallucinationEntity extends Mob implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.anomaly.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.anomaly.walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int life = 260;
    private int watchedTicks;
    private float fade;
    private float prevFade;

    public HallucinationEntity(EntityType<? extends HallucinationEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setNoAi(true);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes();
    }

    /** 0..1, how far the figure has faded in (the renderer multiplies it into the alpha). */
    public float getFade(float partialTick) {
        return Mth.lerp(partialTick, this.prevFade, this.fade);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevFade = this.fade;
        this.fade = Math.min(1.0F, this.fade + 0.04F);

        Player player = this.level().getNearestPlayer(this, 80.0);
        if (player == null || --this.life <= 0) {
            vanish(false);
            return;
        }

        Vec3 toPlayer = player.position().subtract(this.position());
        double dist = toPlayer.horizontalDistance();

        // face the player
        float targetYaw = (float) (Mth.atan2(toPlayer.z, toPlayer.x) * (180.0 / Math.PI)) - 90.0F;
        this.setYRot(Mth.approachDegrees(this.getYRot(), targetYaw, 12.0F));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();

        boolean watched = isLookedAtBy(player);
        this.watchedTicks = watched ? this.watchedTicks + 1 : Math.max(0, this.watchedTicks - 1);
        if (this.watchedTicks > 30) {          // stared it down
            vanish(false);
            return;
        }
        if (dist < 4.0) {                      // too close: it pops with a whisper
            vanish(true);
            return;
        }
        if (!watched && dist > 4.5) {
            double step = 0.09;
            double nx = this.getX() + toPlayer.x / Math.max(dist, 0.001) * step;
            double nz = this.getZ() + toPlayer.z / Math.max(dist, 0.001) * step;
            double ny = this.getY();
            BlockPos feet = BlockPos.containing(nx, ny, nz);
            if (!this.level().getBlockState(feet).getCollisionShape(this.level(), feet).isEmpty()) ny += 0.5;
            else if (this.level().getBlockState(feet.below()).getCollisionShape(this.level(), feet.below()).isEmpty()) ny -= 0.3;
            this.setPos(nx, ny, nz);
        }
    }

    private boolean isLookedAtBy(Player player) {
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 to = this.getBoundingBox().getCenter().subtract(player.getEyePosition());
        double d = to.length();
        return d > 0.001 && look.dot(to.scale(1.0 / d)) > 0.8;
    }

    private void vanish(boolean scary) {
        if (this.level().isClientSide) {
            for (int i = 0; i < 14; i++) {
                this.level().addParticle(ParticleTypes.SMOKE,
                        getX() + (this.random.nextDouble() - 0.5) * 0.8, getY() + this.random.nextDouble() * 2.8,
                        getZ() + (this.random.nextDouble() - 0.5) * 0.8, 0.0, 0.02, 0.0);
            }
            if (scary) {
                this.level().playLocalSound(getX(), getY(), getZ(), ModSounds.WHISPER.get(), SoundSource.AMBIENT,
                        1.0F, 0.8F, false);
            }
        }
        this.discard();
    }

    // harmless, intangible and invisible to the world
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean shouldShowName() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return true; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, this::animate));
    }

    private PlayState animate(AnimationState<HallucinationEntity> state) {
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
