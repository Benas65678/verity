package dev.veritydark.client;

import dev.veritydark.InsanityTier;
import dev.veritydark.Journal;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** The Verity Journal: a list of pages on the left, the selected page on the right. Locked pages read "???". */
public class JournalScreen extends Screen {
    private static final int W = 330;
    private static final int H = 210;
    private static final int ROW = 11;

    private int selected = 0;
    private int scroll = 0;

    public JournalScreen() {
        super(Component.translatable("item.veritydark.verity_journal"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int left() { return (this.width - W) / 2; }
    private int top() { return (this.height - H) / 2; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        int l = left();
        int t = top();

        // book body
        g.fill(l - 3, t - 3, l + W + 3, t + H + 3, 0xFF2B1A0E);
        g.fill(l, t, l + W, t + H, 0xFFD9C7A0);
        g.fill(l, t, l + 116, t + H, 0xFFCBB48A);
        g.fill(l + 116, t, l + 117, t + H, 0xFF8A6B3F);

        // title
        g.drawString(this.font, Component.translatable("veritydark.journal.title"), l + 8, t + 8, 0xFF2B1D0E, false);
        g.fill(l + 6, t + 20, l + 110, t + 21, 0xFF8A6B3F);

        // page list
        Journal.Entry[] entries = Journal.Entry.values();
        for (int i = 0; i < entries.length; i++) {
            int y = t + 26 + i * ROW;
            boolean unlocked = ClientJournal.has(entries[i]);
            boolean hover = mouseX >= l + 4 && mouseX < l + 112 && mouseY >= y && mouseY < y + ROW;
            if (i == this.selected) g.fill(l + 4, y, l + 112, y + ROW, 0xFF9C7F4A);
            else if (hover) g.fill(l + 4, y, l + 112, y + ROW, 0x55000000);
            Component name = unlocked
                    ? Component.translatable("journal.veritydark." + entries[i].key + ".title")
                    : Component.literal("???");
            g.drawString(this.font, name, l + 8, y + 2, unlocked ? 0xFF2B1D0E : 0xFF7A6A52, false);
        }

        // selected page
        Journal.Entry entry = entries[Math.min(this.selected, entries.length - 1)];
        int px = l + 126;
        int pw = W - 126 - 10;
        if (ClientJournal.has(entry)) {
            g.drawString(this.font, Component.translatable("journal.veritydark." + entry.key + ".title")
                    .withStyle(net.minecraft.ChatFormatting.BOLD), px, t + 10, 0xFF2B1D0E, false);
            g.fill(px, t + 22, px + pw, t + 23, 0xFF8A6B3F);
            List<FormattedCharSequence> lines = this.font.split(Component.translatable("journal.veritydark." + entry.key + ".body"), pw);
            int maxLines = (H - 60) / 10;
            this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, lines.size() - maxLines)));
            for (int i = 0; i < maxLines && i + this.scroll < lines.size(); i++) {
                g.drawString(this.font, lines.get(i + this.scroll), px, t + 30 + i * 10, 0xFF2B1D0E, false);
            }
            if (lines.size() > maxLines) {
                g.drawString(this.font, "\u25bc", px + pw - 8, t + H - 34, 0xFF7A6A52, false);
            }
        } else {
            g.drawString(this.font, Component.translatable("veritydark.journal.locked"), px, t + 10, 0xFF7A6A52, false);
            List<FormattedCharSequence> hint = this.font.split(Component.translatable("journal.veritydark." + entry.key + ".hint"), pw);
            for (int i = 0; i < hint.size(); i++) {
                g.drawString(this.font, hint.get(i), px, t + 30 + i * 10, 0xFF7A6A52, false);
            }
        }

        // footer: current sanity
        InsanityTier tier = InsanityTier.of(ClientSanity.pct());
        Component footer = Component.translatable("veritydark.journal.footer", Math.round(ClientSanity.pct() * 100.0F), tier.display());
        g.drawString(this.font, footer, px, t + H - 14, 0xFF2B1D0E, false);
        int count = Long.bitCount(ClientJournal.mask & ((1L << entries.length) - 1));
        g.drawString(this.font, count + "/" + entries.length, l + 8, t + H - 14, 0xFF2B1D0E, false);

        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int l = left();
        int t = top();
        Journal.Entry[] entries = Journal.Entry.values();
        for (int i = 0; i < entries.length; i++) {
            int y = t + 26 + i * ROW;
            if (mouseX >= l + 4 && mouseX < l + 112 && mouseY >= y && mouseY < y + ROW) {
                this.selected = i;
                this.scroll = 0;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.scroll -= (int) Math.signum(delta);
        if (this.scroll < 0) this.scroll = 0;
        return true;
    }
}
