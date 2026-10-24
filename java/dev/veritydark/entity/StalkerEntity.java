package dev.veritydark.entity;

import dev.veritydark.Journal;
import dev.veritydark.ModContent;
import dev.veritydark.ModSounds;
import dev.veritydark.SanityManager;
import dev.veritydark.VerityConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
 * Moves only while nobody is looking at it. The moment a player has it in view it freezes solid (the walk
 * animation is paused mid-stride); look away and it closes the distance fast and in total silence.
 * Staring at it drains sanity. Model/animations/texture: geo/entity/stalker.geo.json etc. (placeholder for now).
 */
public class StalkerEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Boolean> FROZEN =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.stalker.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.stalker.walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int life;
    private boolean animStarted;

    public StalkerEntity(EntityType<? extends StalkerEntity> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.life = VerityConfig.STALKER_LIFETIME.get() * 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FROZEN, false);
    }

    public boolean isFrozen() {
        return this.entityData.get(FROZEN);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.entityData.get(FROZEN);
    }

    /** True if the player has this stalker in their field of view with a clear line. */
    public boolean isWatchedBy(Player player) {
        if (player.isSpectator() || !player.isAlive()) return false;
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 to = this.getBoundingBox().getCenter().subtract(player.getEyePosition());
        double dist = to.length();
        if (dist > 64.0 || dist < 1.0E-3) return dist < 1.0E-3;
        if (look.dot(to.scale(1.0 / dist)) < 0.5) return false;
        return player.hasLineOfSight(this);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;

        if (!VerityConfig.on() || !VerityConfig.STALKER_ENABLED.get() || --this.life <= 0) {
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.LARGE_SMOKE,
                    getX(), getY() + 1.5, getZ(), 16, 0.3, 1.0, 0.3, 0.02);
            this.discard();
            return;
        }

        boolean watched = false;
        boolean staring = false;
        for (Player p : this.level().players()) {
            if (p.distanceToSqr(this) > 64.0 * 64.0) continue;
            if (isWatchedBy(p)) {
                watched = true;
                if (p instanceof ServerPlayer sp && p.distanceToSqr(this) < 24.0 * 24.0) {
                    staring = true;
                    Journal.unlock(sp, Journal.Entry.STALKER);
                    if (this.tickCount % 20 == 0) {
                        SanityManager.add(sp, -VerityConfig.STALKER_STARE_DRAIN.get().floatValue());
                    }
                }
            }
        }
        this.entityData.set(FROZEN, watched);
        if (watched) {
            this.getNavigation().stop();
            Vec3 m = this.getDeltaMovement();
            this.setDeltaMovement(0.0, Math.min(m.y, 0.0), 0.0);
            if (staring && this.tickCount % 40 == 0) {
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.SMOKE, getX(), getY() + 2.6, getZ(), 2, 0.1, 0.1, 0.1, 0.0);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (isFrozen()) return false;
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.playSound(ModSounds.ANOMALY_ATTACK.get(), 1.0F, 0.6F);
            if (target instanceof ServerPlayer player) {
                SanityManager.add(player, -VerityConfig.STALKER_HIT_PENALTY.get().floatValue());
                player.addEffect(new MobEffectInstance(ModContent.DREAD.get(), 160, 0));
            }
        }
        return hit;
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 2.7F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Life", this.life);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Life")) this.life = tag.getInt("Life");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ANOMALY_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ANOMALY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ANOMALY_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // total silence: you never hear it coming
    }

    // ---------- GeckoLib ----------
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 2, this::animate));
    }

    private PlayState animate(AnimationState<StalkerEntity> state) {
        AnimationController<StalkerEntity> controller = state.getController();
        if (isFrozen()) {
            controller.setAnimationSpeed(0.0);   // freeze mid-stride
            if (!this.animStarted) {
                this.animStarted = true;
                return state.setAndContinue(IDLE);
            }
            return PlayState.CONTINUE;
        }
        controller.setAnimationSpeed(1.0);
        this.animStarted = true;
        if (state.isMoving()) return state.setAndContinue(WALK);
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
