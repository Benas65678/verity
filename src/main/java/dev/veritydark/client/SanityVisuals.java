package dev.veritydark.client;

import dev.veritydark.InsanityTier;
import dev.veritydark.VerityClientConfig;
import dev.veritydark.VerityConfig;
import dev.veritydark.VerityDark;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * How the world warps as your sanity drops: camera roll and sway, a breathing field of view, and a glitch overlay
 * (tinted edges, tearing bars, static, black flashes). Everything scales with the insanity tier and can be
 * switched off or toned down in veritydark-client.toml.
 */
@Mod.EventBusSubscriber(modid = VerityDark.MODID, value = Dist.CLIENT)
public final class SanityVisuals {

    /** 0 = no effect, 1 = fully broken. Zero when the system is disabled. */
    private static float strength() {
        if (!VerityConfig.sanityOn() || !VerityConfig.TIERS_ENABLED.get()) return 0.0F;
        float pct = ClientSanity.pct();
        if (pct >= 0.6F) return 0.0F;
        return Mth.clamp((0.6F - pct) / 0.6F, 0.0F, 1.0F) * VerityClientConfig.EFFECT_INTENSITY.get().floatValue();
    }

    private static boolean ready() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && !mc.player.isSpectator();
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (!VerityClientConfig.CAMERA_EFFECTS.get() || !ready()) return;
        float s = strength();
        if (s <= 0.0F) return;
        Minecraft mc = Minecraft.getInstance();
        double t = (mc.level.getGameTime() + event.getPartialTick());
        InsanityTier tier = InsanityTier.of(ClientSanity.pct());

        float roll = (float) (Math.sin(t * 0.045) * 2.2 + Math.sin(t * 0.13) * 0.8) * s;
        float sway = (float) Math.sin(t * 0.07) * 0.9F * s;
        event.setRoll(event.getRoll() + roll);
        event.setYaw(event.getYaw() + sway);
        event.setPitch(event.getPitch() + (float) Math.cos(t * 0.05) * 0.6F * s);

        // sudden twitches in the worst tiers
        if (tier.atLeast(InsanityTier.FRACTURED)) {
            RandomSource r = RandomSource.create(mc.level.getGameTime() / 7L);
            if (r.nextInt(14) == 0) event.setRoll(event.getRoll() + (r.nextFloat() - 0.5F) * 14.0F * s);
        }
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov event) {
        if (!VerityClientConfig.CAMERA_EFFECTS.get() || !ready()) return;
        float s = strength();
        if (s <= 0.0F) return;
        Minecraft mc = Minecraft.getInstance();
        double t = mc.level.getGameTime() + event.getPartialTick();
        double breathe = Math.sin(t * 0.09) * 2.5 * s;
        // faster pulse like a heartbeat below 25%
        if (ClientSanity.pct() < 0.25F) breathe += Math.max(0.0, Math.sin(t * 0.55)) * 4.0 * s;
        event.setFOV(event.getFOV() + breathe);
    }

    /** The glitch layer. Registered under the vignette in ClientModEvents. */
    public static final IGuiOverlay GLITCH = (gui, g, partialTick, width, height) -> {
        if (!VerityClientConfig.GLITCH_OVERLAY.get() || !ready()) return;
        float s = strength();
        if (s <= 0.0F) return;
        Minecraft mc = Minecraft.getInstance();
        long time = mc.level.getGameTime();
        InsanityTier tier = InsanityTier.of(ClientSanity.pct());
        RandomSource r = RandomSource.create(time / 2L * 31L + 7L); // changes every 2 ticks

        // Colour fringes at the edges (chromatic-aberration look).
        int fringe = (int) (2 + 10 * s);
        int a = (int) (60 * s);
        g.fill(0, 0, fringe, height, (a << 24) | 0xFF0000);
        g.fill(width - fringe, 0, width, height, (a << 24) | 0x00FFFF);

        // Tearing glitch bars.
        if (tier.atLeast(InsanityTier.SHAKEN)) {
            int bars = tier == InsanityTier.SHAKEN ? 1 : tier == InsanityTier.FRACTURED ? 3 : 6;
            for (int i = 0; i < bars; i++) {
                if (r.nextInt(7) != 0) continue;
                int y = r.nextInt(height);
                int h = 1 + r.nextInt(6);
                int shift = r.nextInt(30) - 15;
                int col = r.nextBoolean() ? 0xFFFFFF : 0x9B30FF;
                g.fill(Math.max(0, shift), y, Math.min(width, width + shift), y + h, ((int) (50 * s) << 24) | col);
            }
        }

        // Static in the worst tiers.
        if (tier.atLeast(InsanityTier.FRACTURED)) {
            int dots = tier == InsanityTier.BROKEN ? 90 : 35;
            for (int i = 0; i < dots; i++) {
                int x = r.nextInt(width);
                int y = r.nextInt(height);
                int shade = 100 + r.nextInt(155);
                g.fill(x, y, x + 1 + r.nextInt(2), y + 1, (0x55 << 24) | (shade << 16) | (shade << 8) | shade);
            }
        }

        // Brief black flashes when broken.
        if (tier == InsanityTier.BROKEN && (time / 3L) % 47L == 0L) {
            g.fill(0, 0, width, height, 0x99000000);
        }
        // Slow red pulse under heavy strain.
        if (tier.atLeast(InsanityTier.FRACTURED)) {
            float pulse = (float) (Math.sin(time * 0.12) * 0.5 + 0.5);
            g.fill(0, 0, width, height, ((int) (28 * pulse * s) << 24) | 0xFF0000);
        }
    };

    private SanityVisuals() {}
}
