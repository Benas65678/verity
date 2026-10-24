package dev.veritydark.entity;

import dev.veritydark.Journal;
import dev.veritydark.ModAdvancements;
import dev.veritydark.ModContent;
import dev.veritydark.ModSounds;
import dev.veritydark.SanityManager;
import dev.veritydark.VerityConfig;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * A distorted thing Verity summons. Uses the Blockbench model + animations (idle / walk / jump / scare).
 *
 * Hits drain sanity and apply Dread. When it lands the FINAL hit on a player (the hit would be lethal, or the
 * player is low on both health and sanity) it plays the "scare" animation as a jumpscare: it snaps in front of
 * the player, their view is locked onto it, and at the moment of the lunge the player dies.
 * Killing the anomaly during those ~2.8 seconds saves the player.
 */
public class AnomalyEntity extends Monster implements GeoEntity {
    private static final int SCARE_TICKS = 65;  // length of the "scare" animation (3.25 s)
    private static final int KILL_TICK = 56;    // the second head-lunge in the animation (about 2.8 s)
    private static final EntityDataAccessor<Boolean> SCARING =
            SynchedEntityData.defineId(AnomalyEntity.class, EntityDataSerializers.BOOLEAN);

    private static final UUID HP_ID = UUID.fromString("3f0e6a52-8d1b-4c0f-a7e4-9b21d5c6a001");
    private static final UUID DMG_ID = UUID.fromString("3f0e6a52-8d1b-4c0f-a7e4-9b21d5c6a002");
    private static final UUID SPD_ID = UUID.fromString("3f0e6a52-8d1b-4c0f-a7e4-9b21d5c6a003");

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.anomaly.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.anomaly.walk");
    private static final RawAnimation JUMP = RawAnimation.begin().thenPlay("animation.anomaly.jump");
    private static final RawAnimation SCARE = RawAnimation.begin().thenPlayAndHold("animation.anomaly.scare");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int life;
    private int scareElapsed = -1;               // -1 = not scaring
    @Nullable private ServerPlayer scareTarget;
    private boolean scareKilled;

    public AnomalyEntity(EntityType<? extends AnomalyEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.life = VerityConfig.ANOMALY_LIFETIME_SECONDS.get() * 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 28.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SCARING, false);
    }

    public boolean isScaring() {
        return this.entityData.get(SCARING);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.45F));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Called by AngerManager when Verity summons it: angrier Verity = tougher anomaly. */
    public void scaleToAnger(float anger) {
        float f = Mth.clamp(anger / 100.0F, 0.0F, 1.0F);
        setMod(Attributes.MAX_HEALTH, HP_ID, "veritydark.anomaly_hp", 20.0 * f, AttributeModifier.Operation.ADDITION);
        setMod(Attributes.ATTACK_DAMAGE, DMG_ID, "veritydark.anomaly_dmg", 4.0 * f, AttributeModifier.Operation.ADDITION);
        setMod(Attributes.MOVEMENT_SPEED, SPD_ID, "veritydark.anomaly_spd", 0.25 * f, AttributeModifier.Operation.MULTIPLY_BASE);
        this.setHealth(this.getMaxHealth());
    }

    private void setMod(Attribute attribute, UUID id, String name, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = this.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount > 1.0E-4) inst.addPermanentModifier(new AttributeModifier(id, name, amount, op));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data, tag);
        level.playSound(null, this.blockPosition(), ModSounds.ANOMALY_AMBIENT.get(), SoundSource.HOSTILE, 1.4F, 0.6F);
        return result;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.entityData.get(SCARING);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;

        if (this.scareElapsed >= 0) {
            tickScare();
        } else if (!VerityConfig.on() || !VerityConfig.ANOMALIES_ENABLED.get() || --this.life <= 0) {
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.LARGE_SMOKE,
                    getX(), getY() + 1.5, getZ(), 16, 0.3, 1.0, 0.3, 0.02);
            this.discard();
        }
    }

    // ------------------------------------------------------------------ jumpscare finisher

    /** True when this attack should become the jumpscare: it is lethal, or the player is low on health AND sanity. */
    private boolean shouldJumpscare(ServerPlayer player) {
        if (!VerityConfig.on() || !VerityConfig.JUMPSCARE_ENABLED.get() || player.isCreative() || player.isSpectator()) return false;
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float hp = player.getHealth() + player.getAbsorptionAmount();
        boolean lethal = hp <= damage;
        boolean lowHealth = hp <= player.getMaxHealth() * VerityConfig.JUMPSCARE_HEALTH.get();
        boolean lowSanity = VerityConfig.sanityOn()
                && SanityManager.get(player) / SanityManager.max() <= VerityConfig.JUMPSCARE_SANITY.get();
        return lethal || (lowHealth && lowSanity);
    }

    private void startScare(ServerPlayer player) {
        this.scareTarget = player;
        Journal.unlock(player, Journal.Entry.ANOMALY);
        this.scareElapsed = 0;
        this.scareKilled = false;
        this.entityData.set(SCARING, true);
        this.getNavigation().stop();

        // Snap to a spot right in front of the player (if there is room), facing them.
        Vec3 away = this.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
        if (away.lengthSqr() < 1.0E-4) away = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (away.lengthSqr() < 1.0E-4) away = new Vec3(0.0, 0.0, 1.0);
        Vec3 spot = player.position().add(away.normalize().scale(2.4));
        if (this.level().noCollision(this, this.getBoundingBox().move(spot.x - getX(), player.getY() - getY(), spot.z - getZ()))) {
            this.teleportTo(spot.x, player.getY(), spot.z);
        }
        this.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();

        this.level().playSound(null, this.blockPosition(), ModSounds.ANOMALY_JUMPSCARE.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        SanityManager.set(player, 0.0F);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 10, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
    }

    private void tickScare() {
        ServerPlayer player = this.scareTarget;
        if (player == null || player.isRemoved() || !player.isAlive() || player.level() != this.level()) {
            endScare();
            return;
        }
        this.scareElapsed++;
        this.getNavigation().stop();
        if (this.scareElapsed % 3 == 0) lockView(player);

        if (this.scareElapsed == KILL_TICK) {
            this.scareKilled = true;
            player.hurt(this.level().damageSources().mobAttack(this), 10000.0F); // a Totem of Undying can still save them
            SanityManager.set(player, 0.0F);
        }
        if (this.scareElapsed >= SCARE_TICKS) endScare();
    }

    /** Forces the player's camera onto the anomaly's face (and holds them in place). */
    private void lockView(ServerPlayer player) {
        if (player.connection == null) return;
        Vec3 from = player.getEyePosition();
        Vec3 to = this.getEyePosition();
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) (-(Mth.atan2(dy, horizontal) * (180.0 / Math.PI)));
        player.connection.teleport(player.getX(), player.getY(), player.getZ(), yaw, pitch);
    }

    private void endScare() {
        boolean killed = this.scareKilled;
        if (!killed && this.scareTarget != null && this.scareTarget.isAlive()) {
            ModAdvancements.grant(this.scareTarget, "close_call"); // survived the jumpscare
        }
        this.scareElapsed = -1;
        this.scareTarget = null;
        this.scareKilled = false;
        this.entityData.set(SCARING, false);
        if (killed && this.level() instanceof ServerLevel server) {
            // it has done its job: it vanishes
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 24, 0.4, 1.2, 0.4, 0.03);
            this.discard();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (isScaring()) return false;
        if (target instanceof ServerPlayer victim && shouldJumpscare(victim)) {
            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            startScare(victim);
            return true;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.playSound(ModSounds.ANOMALY_ATTACK.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
            if (target instanceof ServerPlayer player) {
                SanityManager.add(player, -VerityConfig.ANOMALY_HIT_PENALTY.get().floatValue());
                player.addEffect(new MobEffectInstance(ModContent.DREAD.get(), 120, 0));
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

    // ---------- sounds ----------
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
        this.playSound(ModSounds.ANOMALY_STEP.get(), 0.6F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    // ---------- GeckoLib ----------
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, this::animate));
    }

    private PlayState animate(AnimationState<AnomalyEntity> state) {
        if (isScaring()) return state.setAndContinue(SCARE);
        if (!this.onGround() && !this.isInWater() && this.getDeltaMovement().y > 0.1) return state.setAndContinue(JUMP);
        if (state.isMoving()) return state.setAndContinue(WALK);
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
