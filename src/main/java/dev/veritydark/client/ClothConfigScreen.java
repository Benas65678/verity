package dev.veritydark.client;

import dev.veritydark.VerityClientConfig;
import dev.veritydark.VerityConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The settings screen behind the "Config" button in the Mods menu (needs Cloth Config, which Verity already requires).
 * Every switch of the addon is here; changes are written to the .toml files and apply immediately.
 */
public final class ClothConfigScreen {

    public static Screen build(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Verity: Darkness & Containment"))
                .setSavingRunnable(() -> {
                    VerityConfig.SPEC.save();
                    VerityClientConfig.SPEC.save();
                });
        ConfigEntryBuilder eb = builder.entryBuilder();

        // ---------------- switches ----------------
        ConfigCategory sw = builder.getOrCreateCategory(Component.literal("Switches"));
        toggle(sw, eb, "ENABLE THE ADDON (master switch)", "Off = nothing in the addon runs.", VerityConfig.MOD_ENABLED);
        toggle(sw, eb, "Sanity system", "The sanity meter and everything that drains or restores it.", VerityConfig.SANITY_ENABLED);
        toggle(sw, eb, "Insanity tiers", "Lasting penalties at Fractured / Broken sanity.", VerityConfig.TIERS_ENABLED);
        toggle(sw, eb, "Broken tier summons", "Anomalies hunt you when your mind is broken.", VerityConfig.TIER_SPAWNS);
        toggle(sw, eb, "Anomalies", "Verity summons anomalies when angry.", VerityConfig.ANOMALIES_ENABLED);
        toggle(sw, eb, "Stalkers", "Weeping-angel style stalkers at low sanity.", VerityConfig.STALKER_ENABLED);
        toggle(sw, eb, "Jumpscare finisher", "The scare animation that kills a weakened player.", VerityConfig.JUMPSCARE_ENABLED);
        toggle(sw, eb, "Verity gets stronger when angry", "Anger-scaled Verity stats.", VerityConfig.STATS_ENABLED);
        toggle(sw, eb, "Containment armor bonuses", "Stealth, damage reduction and set bonus.", VerityConfig.ARMOR_ENABLED);
        toggle(sw, eb, "Anomaly Detector", "Beeping while held.", VerityConfig.DETECTOR_ENABLED);
        toggle(sw, eb, "Gifts and trust", "Trust with Verity, presents and defence.", VerityConfig.TRUST_ENABLED);

        // ---------------- visuals & audio (client) ----------------
        ConfigCategory vis = builder.getOrCreateCategory(Component.literal("Visuals & Audio"));
        toggle(vis, eb, "Sanity bar", "The bar in the top-right corner.", VerityClientConfig.SHOW_HUD);
        toggle(vis, eb, "Dark screen edges", "Vignette at low sanity.", VerityClientConfig.VIGNETTE);
        toggle(vis, eb, "Camera effects", "Sway, roll and FOV pulsing.", VerityClientConfig.CAMERA_EFFECTS);
        toggle(vis, eb, "Glitch overlay", "Colour fringes, tearing bars, static.", VerityClientConfig.GLITCH_OVERLAY);
        number(vis, eb, "Visual effect strength", "0 = none, 1 = full.", VerityClientConfig.EFFECT_INTENSITY, 0.0, 1.0);
        toggle(vis, eb, "Phantom sounds", "Fake footsteps, knocks, whispers, hisses.", VerityClientConfig.PHANTOM_SOUNDS);
        toggle(vis, eb, "Heartbeat", "Below 25% sanity.", VerityClientConfig.HEARTBEAT);
        toggle(vis, eb, "Hallucinations", "Harmless see-through figures only you see.", VerityClientConfig.HALLUCINATIONS);
        number(vis, eb, "Hallucination opacity", "1 = solid.", VerityClientConfig.HALLUCINATION_OPACITY, 0.1, 1.0);
        number(vis, eb, "Anomaly opacity", "1 = solid. Slightly ghostly by default.", VerityClientConfig.ANOMALY_OPACITY, 0.3, 1.0);

        // ---------------- sanity ----------------
        ConfigCategory san = builder.getOrCreateCategory(Component.literal("Sanity"));
        number(san, eb, "Max sanity", "", VerityConfig.MAX_SANITY, 10.0, 1000.0);
        number(san, eb, "Dark drain / second", "", VerityConfig.DARK_DRAIN, 0.0, 50.0);
        number(san, eb, "Near anomaly drain / second", "", VerityConfig.NEAR_ANOMALY_DRAIN, 0.0, 50.0);
        number(san, eb, "Near angry Verity drain / second", "", VerityConfig.NEAR_VERITY_DRAIN, 0.0, 50.0);
        number(san, eb, "Light regeneration / second", "", VerityConfig.LIGHT_REGEN, 0.0, 50.0);
        number(san, eb, "Sanity lost per anomaly hit", "", VerityConfig.ANOMALY_HIT_PENALTY, 0.0, 1000.0);
        number(san, eb, "Tonic restore", "", VerityConfig.TONIC_RESTORE, 0.0, 1000.0);

        // ---------------- monsters ----------------
        ConfigCategory mon = builder.getOrCreateCategory(Component.literal("Monsters"));
        number(mon, eb, "Anger needed for anomalies", "0-100", VerityConfig.SPAWN_ANGER_THRESHOLD, 0.0, 100.0);
        number(mon, eb, "Anomaly summon chance / second at max anger", "", VerityConfig.SPAWN_CHANCE_MAX, 0.0, 1.0);
        whole(mon, eb, "Anomaly cap", "", VerityConfig.ANOMALY_CAP, 0, 30);
        whole(mon, eb, "Anomaly lifetime (seconds)", "", VerityConfig.ANOMALY_LIFETIME_SECONDS, 10, 3600);
        number(mon, eb, "Stalkers appear below this sanity fraction", "", VerityConfig.STALKER_SANITY, 0.0, 1.0);
        number(mon, eb, "Stalker chance / second", "", VerityConfig.STALKER_CHANCE, 0.0, 1.0);
        whole(mon, eb, "Stalker cap", "", VerityConfig.STALKER_CAP, 0, 10);
        whole(mon, eb, "Stalker lifetime (seconds)", "", VerityConfig.STALKER_LIFETIME, 10, 3600);
        number(mon, eb, "Stalker stare drain / second", "", VerityConfig.STALKER_STARE_DRAIN, 0.0, 50.0);
        number(mon, eb, "Jumpscare: health fraction", "Low health threshold.", VerityConfig.JUMPSCARE_HEALTH, 0.0, 1.0);
        number(mon, eb, "Jumpscare: sanity fraction", "Low sanity threshold.", VerityConfig.JUMPSCARE_SANITY, 0.0, 1.0);

        // ---------------- items & trust ----------------
        ConfigCategory it = builder.getOrCreateCategory(Component.literal("Items & Trust"));
        number(it, eb, "Armor stealth per piece", "0.15 = -15% detection range per piece.", VerityConfig.ARMOR_STEALTH, 0.0, 0.24);
        number(it, eb, "Armor damage reduction per piece", "Against anomalies and stalkers.", VerityConfig.ARMOR_REDUCTION, 0.0, 0.24);
        number(it, eb, "Armor set sanity regeneration / second", "", VerityConfig.ARMOR_SET_REGEN, 0.0, 50.0);
        whole(it, eb, "Detector range (blocks)", "", VerityConfig.DETECTOR_RANGE, 4, 128);
        number(it, eb, "Starting trust", "0-100", VerityConfig.TRUST_START, 0.0, 100.0);
        number(it, eb, "Gift trust multiplier", "", VerityConfig.TRUST_GIFT_MULT, 0.0, 10.0);
        number(it, eb, "Trust needed for Verity to defend you", "", VerityConfig.TRUST_DEFEND_AT, 0.0, 100.0);
        whole(it, eb, "Minutes between Verity's presents", "", VerityConfig.TRUST_GIFT_MINUTES, 1, 600);
        number(it, eb, "Lantern calm / second", "", VerityConfig.LANTERN_CALM, 0.0, 100.0);
        number(it, eb, "Smile Lamp sanity / second", "", VerityConfig.LAMP_REGEN, 0.0, 50.0);

        return builder.build();
    }

    private static void toggle(ConfigCategory cat, ConfigEntryBuilder eb, String name, String tip,
                               ForgeConfigSpec.BooleanValue value) {
        var entry = eb.startBooleanToggle(Component.literal(name), value.get())
                .setDefaultValue(value.getDefault())
                .setSaveConsumer(value::set);
        if (!tip.isEmpty()) entry.setTooltip(Component.literal(tip));
        cat.addEntry(entry.build());
    }

    private static void number(ConfigCategory cat, ConfigEntryBuilder eb, String name, String tip,
                               ForgeConfigSpec.DoubleValue value, double min, double max) {
        var entry = eb.startDoubleField(Component.literal(name), value.get())
                .setDefaultValue(value.getDefault())
                .setMin(min)
                .setMax(max)
                .setSaveConsumer(value::set);
        if (!tip.isEmpty()) entry.setTooltip(Component.literal(tip));
        cat.addEntry(entry.build());
    }

    private static void whole(ConfigCategory cat, ConfigEntryBuilder eb, String name, String tip,
                              ForgeConfigSpec.IntValue value, int min, int max) {
        var entry = eb.startIntField(Component.literal(name), value.get())
                .setDefaultValue(value.getDefault())
                .setMin(min)
                .setMax(max)
                .setSaveConsumer(value::set);
        if (!tip.isEmpty()) entry.setTooltip(Component.literal(tip));
        cat.addEntry(entry.build());
    }

    private ClothConfigScreen() {}
}
