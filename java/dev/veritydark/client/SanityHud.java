package dev.veritydark.client;

import dev.veritydark.InsanityTier;
import dev.veritydark.ModContent;
import dev.veritydark.VerityClientConfig;
import dev.veritydark.VerityConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class SanityHud {

    private static int lerpColor(int from, int to, float t) {
        int r = (int) (((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
        int g = (int) (((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
        int b = (int) ((from & 0xFF) * (1 - t) + (to & 0xFF) * t);
        return (r << 16) | (g << 8) | b;
    }

    /** The bar in the top-right corner. Sits below the vanilla potion-effect icons. */
    public static final IGuiOverlay BAR = (gui, g, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null || mc.player.isSpectator()) return;
        if (!VerityClientConfig.SHOW_HUD.get() || !VerityConfig.sanityOn()) return;

        float pct = ClientSanity.pct();
        int barW = 84;
        int barH = 8;

        boolean beneficial = false;
        boolean harmful = false;
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            if (!effect.showIcon()) continue;
            if (effect.getEffect().isBeneficial()) beneficial = true;
            else harmful = true;
        }
        int yOffset = harmful ? 52 : (beneficial ? 26 : 0);

        int x = width - barW - 10;
        int y = 6 + yOffset;

        long time = mc.level.getGameTime();
        if (pct < 0.25F && (time / 2) % 7 == 0) {
            x += mc.level.random.nextInt(3) - 1; // nervous jitter
        }

        int fill = 0xFF000000 | lerpColor(0xC0392B, 0xF5D142, pct);
        int fillWidth = Math.round(barW * pct);

        g.fill(x - 2, y - 2, x + barW + 2, y + barH + 2, 0xCC000000);
        int border = 0xFF3A3A3A;
        if (mc.player.hasEffect(ModContent.CLARITY.get())) border = 0xFFF5D142;          // golden while Clarity is active
        else if (mc.player.hasEffect(ModContent.DREAD.get()) && (time / 6) % 2 == 0) border = 0xFF8A2BE2; // flickers under Dread
        g.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, border);
        g.fill(x, y, x + barW, y + barH, 0xFF111111);
        g.fill(x, y, x + fillWidth, y + barH, fill);
        g.fill(x, y, x + fillWidth, y + 2, 0x44FFFFFF);
        for (int i = 1; i < 4; i++) { // quarter ticks
            int tx = x + barW * i / 4;
            g.fill(tx, y, tx + 1, y + barH, 0x66000000);
        }

        String face = pct > 0.6F ? ":)" : (pct > 0.3F ? ":|" : ":(");
        g.drawString(mc.font, face, x - 14, y, fill, true);

        String label = "SANITY " + Math.round(pct * 100.0F) + "%";
        g.drawString(mc.font, label, x + barW - mc.font.width(label), y + barH + 3, 0xFFB8B8B8, true);
        if (VerityConfig.TIERS_ENABLED.get()) {
            net.minecraft.network.chat.Component tierName = InsanityTier.of(pct).display();
            g.drawString(mc.font, tierName, x + barW - mc.font.width(tierName), y + barH + 13, 0xFFFFFFFF, true);
        }
    };

    /** Darkening screen edges that creep in as sanity drops below 60%. */
    public static final IGuiOverlay VIGNETTE = (gui, g, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.player.isSpectator()) return;
        if (!VerityClientConfig.VIGNETTE.get() || !VerityConfig.sanityOn()) return;

        float pct = ClientSanity.pct();
        if (pct >= 0.6F) return;

        float intensity = (0.6F - pct) / 0.6F; // 0..1
        int maxAlpha = (int) (intensity * 190);
        int edge = (int) (Math.min(width, height) * 0.3F * intensity) + 10;

        drawEdges(g, width, height, edge, maxAlpha);
    };

    private static void drawEdges(GuiGraphics g, int w, int h, int edge, int maxAlpha) {
        int top = (maxAlpha << 24);
        g.fillGradient(0, 0, w, edge, top, 0);
        g.fillGradient(0, h - edge, w, h, 0, top);
        for (int i = 0; i < edge; i++) {
            float t = 1.0F - (float) i / edge;
            int a = (int) (maxAlpha * t * t);
            int c = a << 24;
            g.fill(i, 0, i + 1, h, c);
            g.fill(w - i - 1, 0, w - i, h, c);
        }
    }

    private SanityHud() {}
}
