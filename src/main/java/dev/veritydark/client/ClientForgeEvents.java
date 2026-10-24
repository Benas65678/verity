package dev.veritydark.client;

import dev.veritydark.InsanityTier;
import dev.veritydark.ModSounds;
import dev.veritydark.VerityClientConfig;
import dev.veritydark.VerityConfig;
import dev.veritydark.VerityDark;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sounds only you hear as your mind frays:
 *  - Uneasy and below: knocks, doors, glass, a creeper hiss that never explodes, footsteps creeping up behind you,
 *  - Shaken and below: whispers from behind you,
 *  - below 25%: a heartbeat that speeds up and gets louder.
 */
@Mod.EventBusSubscriber(modid = VerityDark.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private static final SoundEvent[] PHANTOM_SOUNDS = {
            SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, // knocking
            SoundEvents.WOODEN_DOOR_OPEN,
            SoundEvents.WOODEN_DOOR_CLOSE,
            SoundEvents.GLASS_BREAK,
            SoundEvents.CHEST_OPEN,
            SoundEvents.CREEPER_PRIMED
    };
    private static int heartTimer = 0;
    private static int stepsLeft = 0;
    private static int stepTimer = 0;
    private static double stepAngle = 0.0;
    private static double stepDist = 0.0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.isPaused() || player.isSpectator()) return;
        if (!VerityConfig.sanityOn()) return;

        float pct = ClientSanity.pct();
        InsanityTier tier = InsanityTier.of(pct);

        // heartbeat
        if (pct < 0.25F && VerityClientConfig.HEARTBEAT.get()) {
            if (--heartTimer <= 0) {
                float urgency = 1.0F - pct / 0.25F;                       // 0..1
                float volume = 0.35F + 0.65F * urgency;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HEARTBEAT.get(), 1.0F + 0.15F * urgency, volume));
                heartTimer = (int) (46 - 22 * urgency);
            }
        } else {
            heartTimer = 0;
        }

        if (!VerityClientConfig.PHANTOM_SOUNDS.get()) return;

        // footsteps that creep closer from behind
        if (stepsLeft > 0 && --stepTimer <= 0) {
            mc.level.playLocalSound(
                    player.getX() + Math.cos(stepAngle) * stepDist, player.getY(), player.getZ() + Math.sin(stepAngle) * stepDist,
                    SoundEvents.STONE_STEP, SoundSource.AMBIENT, 0.8F, 0.8F, false);
            stepDist = Math.max(1.5, stepDist - 0.9);
            stepTimer = 9;
            stepsLeft--;
        }

        // phantom world sounds and whispers
        if (pct >= 0.4F) return;
        float chancePerTick = (0.4F - pct) / 0.4F * 0.002F; // ~1 per 25s at zero sanity
        if (mc.level.random.nextFloat() > chancePerTick) return;

        double angle = mc.level.random.nextDouble() * Math.PI * 2.0;
        double dist = 5.0 + mc.level.random.nextDouble() * 5.0;
        int roll = mc.level.random.nextInt(10);

        if (roll < 2 && stepsLeft == 0 && tier.atLeast(InsanityTier.SHAKEN)) {   // someone is walking up behind you
            double yawRad = Math.toRadians(player.getYRot() + 90.0);
            stepAngle = yawRad + Math.PI + (mc.level.random.nextDouble() - 0.5) * 0.8;
            stepDist = 9.0;
            stepsLeft = 8;
            stepTimer = 0;
            return;
        }

        boolean whisper = tier.atLeast(InsanityTier.SHAKEN) && roll < 6;
        SoundEvent sound = whisper ? ModSounds.WHISPER.get()
                : PHANTOM_SOUNDS[mc.level.random.nextInt(PHANTOM_SOUNDS.length)];
        mc.level.playLocalSound(
                player.getX() + Math.cos(angle) * dist, player.getY(), player.getZ() + Math.sin(angle) * dist,
                sound, SoundSource.AMBIENT, whisper ? 0.9F : 0.6F, 0.7F + mc.level.random.nextFloat() * 0.3F, false);
    }

    private ClientForgeEvents() {}
}
