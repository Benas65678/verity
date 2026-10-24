package dev.veritydark;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** How broken the player's mind is, by sanity percentage. Shared by server effects and client visuals. */
public enum InsanityTier {
    STABLE("stable", 0.60F, ChatFormatting.GREEN),
    UNEASY("uneasy", 0.40F, ChatFormatting.YELLOW),
    SHAKEN("shaken", 0.25F, ChatFormatting.GOLD),
    FRACTURED("fractured", 0.10F, ChatFormatting.RED),
    BROKEN("broken", 0.0F, ChatFormatting.DARK_RED);

    public final String key;
    public final float min; // lowest sanity fraction that still counts as this tier
    public final ChatFormatting color;

    InsanityTier(String key, float min, ChatFormatting color) {
        this.key = key;
        this.min = min;
        this.color = color;
    }

    public static InsanityTier of(float pct) {
        if (pct >= STABLE.min) return STABLE;
        if (pct >= UNEASY.min) return UNEASY;
        if (pct >= SHAKEN.min) return SHAKEN;
        if (pct >= FRACTURED.min) return FRACTURED;
        return BROKEN;
    }

    public boolean atLeast(InsanityTier other) {
        return this.ordinal() >= other.ordinal();
    }

    public Component display() {
        return Component.translatable("veritydark.tier." + key).withStyle(color);
    }
}
