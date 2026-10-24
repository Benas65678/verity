package dev.veritydark;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/** All tuning knobs live here. File: config/veritydark-common.toml */
public final class VerityConfig {
    public static final ForgeConfigSpec SPEC;

    // --- Feature switches ---
    public static final ForgeConfigSpec.BooleanValue MOD_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SANITY_ENABLED;
    public static final ForgeConfigSpec.BooleanValue TIERS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue TIER_SPAWNS;
    public static final ForgeConfigSpec.BooleanValue ARMOR_ENABLED;
    public static final ForgeConfigSpec.BooleanValue DETECTOR_ENABLED;
    public static final ForgeConfigSpec.BooleanValue TRUST_ENABLED;

    // --- Stalker ---
    public static final ForgeConfigSpec.BooleanValue STALKER_ENABLED;
    public static final ForgeConfigSpec.DoubleValue STALKER_SANITY;
    public static final ForgeConfigSpec.DoubleValue STALKER_CHANCE;
    public static final ForgeConfigSpec.IntValue STALKER_CAP;
    public static final ForgeConfigSpec.IntValue STALKER_LIFETIME;
    public static final ForgeConfigSpec.DoubleValue STALKER_STARE_DRAIN;
    public static final ForgeConfigSpec.DoubleValue STALKER_HIT_PENALTY;

    // --- Armor / detector ---
    public static final ForgeConfigSpec.DoubleValue ARMOR_STEALTH;
    public static final ForgeConfigSpec.DoubleValue ARMOR_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue ARMOR_SET_REGEN;
    public static final ForgeConfigSpec.IntValue DETECTOR_RANGE;

    // --- Trust ---
    public static final ForgeConfigSpec.DoubleValue TRUST_START;
    public static final ForgeConfigSpec.DoubleValue TRUST_GIFT_MULT;
    public static final ForgeConfigSpec.DoubleValue TRUST_DEFEND_AT;
    public static final ForgeConfigSpec.IntValue TRUST_GIFT_MINUTES;

    // --- Verity detection ---
    public static final ForgeConfigSpec.ConfigValue<String> VERITY_NAMESPACE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VERITY_ENTITY_IDS;

    // --- Sanity ---
    public static final ForgeConfigSpec.DoubleValue MAX_SANITY;
    public static final ForgeConfigSpec.IntValue DARK_LIGHT_LEVEL;
    public static final ForgeConfigSpec.IntValue BRIGHT_LIGHT_LEVEL;
    public static final ForgeConfigSpec.DoubleValue DARK_DRAIN;
    public static final ForgeConfigSpec.DoubleValue NEAR_VERITY_DRAIN;
    public static final ForgeConfigSpec.DoubleValue NEAR_ANOMALY_DRAIN;
    public static final ForgeConfigSpec.DoubleValue LIGHT_REGEN;
    public static final ForgeConfigSpec.DoubleValue SLEEP_RESTORE;
    public static final ForgeConfigSpec.DoubleValue TONIC_RESTORE;
    public static final ForgeConfigSpec.DoubleValue ANOMALY_HIT_PENALTY;

    // --- Items ---
    public static final ForgeConfigSpec.IntValue LANTERN_RADIUS;
    public static final ForgeConfigSpec.DoubleValue LANTERN_REGEN;
    public static final ForgeConfigSpec.DoubleValue LANTERN_CALM;
    public static final ForgeConfigSpec.DoubleValue SPLASH_RESTORE;
    public static final ForgeConfigSpec.DoubleValue SPLASH_CALM;
    public static final ForgeConfigSpec.DoubleValue SPLASH_ANOMALY_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue COOKIE_SANITY;
    public static final ForgeConfigSpec.DoubleValue COOKIE_FEED_CALM;
    public static final ForgeConfigSpec.DoubleValue CHARM_DRAIN_MULT;
    public static final ForgeConfigSpec.DoubleValue CHARM_CALM;
    public static final ForgeConfigSpec.IntValue LAMP_RADIUS;
    public static final ForgeConfigSpec.DoubleValue LAMP_REGEN;
    public static final ForgeConfigSpec.DoubleValue LAMP_CALM;
    public static final ForgeConfigSpec.DoubleValue BLADE_ANOMALY_BONUS;
    public static final ForgeConfigSpec.DoubleValue BLADE_HOLD_ANGER;
    public static final ForgeConfigSpec.DoubleValue BLADE_HIT_ANGER;

    // --- Anger ---
    public static final ForgeConfigSpec.DoubleValue ANGER_BASE;
    public static final ForgeConfigSpec.DoubleValue ANGER_NIGHT;
    public static final ForgeConfigSpec.DoubleValue ANGER_LOW_SANITY;
    public static final ForgeConfigSpec.DoubleValue ANGER_DAY_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue ANGER_PER_DAY;
    public static final ForgeConfigSpec.DoubleValue ANGER_HURT;

    // --- Verity stats (scale with anger) ---
    public static final ForgeConfigSpec.BooleanValue STATS_ENABLED;
    public static final ForgeConfigSpec.DoubleValue STAT_SPEED;
    public static final ForgeConfigSpec.DoubleValue STAT_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue STAT_ARMOR;
    public static final ForgeConfigSpec.DoubleValue STAT_KNOCKBACK_RES;

    // --- Jumpscare finisher ---
    public static final ForgeConfigSpec.BooleanValue JUMPSCARE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue JUMPSCARE_HEALTH;
    public static final ForgeConfigSpec.DoubleValue JUMPSCARE_SANITY;

    // --- Anomalies ---
    public static final ForgeConfigSpec.BooleanValue ANOMALIES_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SPAWN_ANGER_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue SPAWN_CHANCE_MAX;
    public static final ForgeConfigSpec.IntValue ANOMALY_CAP;
    public static final ForgeConfigSpec.IntValue ANOMALY_LIFETIME_SECONDS;

    /** Master switch: when false every gameplay system of the addon is off. */
    public static boolean on() {
        return MOD_ENABLED.get();
    }

    public static boolean sanityOn() {
        return MOD_ENABLED.get() && SANITY_ENABLED.get();
    }

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("general");
        MOD_ENABLED = b.comment("MASTER SWITCH. false = the whole addon does nothing (items and blocks stay registered).")
                .define("enabled", true);
        SANITY_ENABLED = b.comment("The sanity meter and everything that drains or restores it.")
                .define("sanity", true);
        TIERS_ENABLED = b.comment("Insanity tiers: lasting penalties as sanity drops (Shaken / Fractured / Broken).")
                .define("insanityTiers", true);
        TIER_SPAWNS = b.comment("At the Broken tier the dark sends anomalies and stalkers after you.")
                .define("insanitySpawns", true);
        ARMOR_ENABLED = b.comment("Containment armor bonuses (stealth, damage reduction, set bonus).")
                .define("armorBonuses", true);
        DETECTOR_ENABLED = b.comment("Anomaly Detector beeping.")
                .define("detector", true);
        TRUST_ENABLED = b.comment("Gifts and trust with Verity.")
                .define("trust", true);
        b.pop();

        b.push("stalker");
        STALKER_ENABLED = b.comment("Stalkers: they only move while nobody is looking at them.")
                .define("enabled", true);
        STALKER_SANITY = b.comment("Stalkers may appear when sanity is at or below this fraction of max.")
                .defineInRange("sanityFraction", 0.4, 0.0, 1.0);
        STALKER_CHANCE = b.comment("Chance per second that a stalker appears (when the conditions are met).")
                .defineInRange("chancePerSecond", 0.02, 0.0, 1.0);
        STALKER_CAP = b.comment("Most stalkers near one player.").defineInRange("cap", 1, 0, 10);
        STALKER_LIFETIME = b.comment("Seconds before a stalker fades away.").defineInRange("lifetimeSeconds", 180, 10, 3600);
        STALKER_STARE_DRAIN = b.comment("Sanity lost per second while you stare at a stalker.")
                .defineInRange("stareDrain", 1.5, 0.0, 50.0);
        STALKER_HIT_PENALTY = b.comment("Sanity lost when a stalker hits you.").defineInRange("hitPenalty", 10.0, 0.0, 1000.0);
        b.pop();

        b.push("armorAndDetector");
        ARMOR_STEALTH = b.comment("Per armor piece: how much shorter mobs see you from (0.15 = -15% per piece, -60% for the full set).")
                .defineInRange("stealthPerPiece", 0.15, 0.0, 0.24);
        ARMOR_REDUCTION = b.comment("Per armor piece: damage reduction against anomalies and stalkers.")
                .defineInRange("damageReductionPerPiece", 0.07, 0.0, 0.24);
        ARMOR_SET_REGEN = b.comment("Sanity per second restored by wearing the full set.")
                .defineInRange("setSanityRegen", 0.3, 0.0, 50.0);
        DETECTOR_RANGE = b.comment("Anomaly Detector range in blocks.").defineInRange("range", 32, 4, 128);
        b.pop();

        b.push("trust");
        TRUST_START = b.comment("Trust a brand new Verity starts with (0-100).").defineInRange("start", 10.0, 0.0, 100.0);
        TRUST_GIFT_MULT = b.comment("Multiplier on the trust gained from gifts.").defineInRange("giftMultiplier", 1.0, 0.0, 10.0);
        TRUST_DEFEND_AT = b.comment("Trust at which Verity starts zapping anomalies near it.")
                .defineInRange("defendAt", 60.0, 0.0, 100.0);
        TRUST_GIFT_MINUTES = b.comment("Minutes between presents from a friendly Verity.").defineInRange("giftMinutes", 8, 1, 600);
        b.pop();

        b.push("verity");
        VERITY_NAMESPACE = b.comment("Registry namespace of the Verity mod. Any living entity in this namespace counts as 'Verity'.")
                .define("namespace", "verity");
        VERITY_ENTITY_IDS = b.comment("Optional: restrict to specific entity ids, e.g. [\"verity:verity\"]. Leave empty to match the whole namespace.",
                        "Use /veritydark list in game to see the real ids.")
                .defineList("entityIds", List.of(), o -> o instanceof String);
        b.pop();

        b.push("sanity");
        MAX_SANITY = b.defineInRange("maxSanity", 100.0, 10.0, 1000.0);
        DARK_LIGHT_LEVEL = b.comment("Light level at or below this counts as dark.")
                .defineInRange("darkLightLevel", 4, 0, 15);
        BRIGHT_LIGHT_LEVEL = b.comment("Light level at or above this slowly restores sanity.")
                .defineInRange("brightLightLevel", 10, 0, 15);
        DARK_DRAIN = b.comment("Sanity lost per second in the dark.").defineInRange("darkDrain", 0.35, 0.0, 50.0);
        NEAR_VERITY_DRAIN = b.comment("Max sanity lost per second near an angry Verity (scaled by its anger).")
                .defineInRange("nearVerityDrain", 0.5, 0.0, 50.0);
        NEAR_ANOMALY_DRAIN = b.comment("Sanity lost per second per anomaly within 16 blocks.")
                .defineInRange("nearAnomalyDrain", 0.6, 0.0, 50.0);
        LIGHT_REGEN = b.comment("Sanity gained per second in bright light with no anomalies nearby.")
                .defineInRange("lightRegen", 0.12, 0.0, 50.0);
        SLEEP_RESTORE = b.comment("Sanity restored by sleeping through the night.")
                .defineInRange("sleepRestore", 40.0, 0.0, 1000.0);
        TONIC_RESTORE = b.comment("Sanity restored instantly by a drunk Sanity Tonic (it also gives the Clarity effect).")
                .defineInRange("tonicRestore", 35.0, 0.0, 1000.0);
        ANOMALY_HIT_PENALTY = b.comment("Sanity lost when an anomaly hits you.")
                .defineInRange("anomalyHitPenalty", 6.0, 0.0, 1000.0);
        b.pop();

        b.push("items");
        LANTERN_RADIUS = b.comment("Containment Lantern radius.").defineInRange("lanternRadius", 12, 2, 48);
        LANTERN_REGEN = b.comment("Sanity per second while the lantern is lit in your hand.")
                .defineInRange("lanternSanityRegen", 0.5, 0.0, 50.0);
        LANTERN_CALM = b.comment("Verity anger removed per second inside the lantern radius.")
                .defineInRange("lanternCalmPerSecond", 4.0, 0.0, 100.0);
        SPLASH_RESTORE = b.comment("Sanity restored to a player standing in the middle of a Splash Sanity Tonic.")
                .defineInRange("splashRestore", 25.0, 0.0, 1000.0);
        SPLASH_CALM = b.comment("Verity anger removed by a Splash Sanity Tonic landing next to it.")
                .defineInRange("splashCalm", 20.0, 0.0, 100.0);
        SPLASH_ANOMALY_DAMAGE = b.comment("Magic damage a Splash Sanity Tonic deals to anomalies.")
                .defineInRange("splashAnomalyDamage", 8.0, 0.0, 100.0);
        COOKIE_SANITY = b.comment("Sanity restored by eating a Comfort Cookie.")
                .defineInRange("cookieSanity", 12.0, 0.0, 1000.0);
        COOKIE_FEED_CALM = b.comment("Verity anger removed when you feed it a Comfort Cookie (right-click Verity).")
                .defineInRange("cookieFeedCalm", 25.0, 0.0, 100.0);
        CHARM_DRAIN_MULT = b.comment("Sanity drain multiplier while the Smile Charm is in your inventory (0.5 = half drain).")
                .defineInRange("charmDrainMultiplier", 0.5, 0.0, 1.0);
        CHARM_CALM = b.comment("Verity anger removed per second while you carry the Smile Charm.")
                .defineInRange("charmCalmPerSecond", 0.1, 0.0, 10.0);
        LAMP_RADIUS = b.comment("Smile Lamp radius.").defineInRange("lampRadius", 10, 2, 48);
        LAMP_REGEN = b.comment("Sanity per second restored to players near a Smile Lamp.")
                .defineInRange("lampSanityRegen", 1.5, 0.0, 50.0);
        LAMP_CALM = b.comment("Verity anger removed per second near a Smile Lamp.")
                .defineInRange("lampCalmPerSecond", 3.0, 0.0, 100.0);
        BLADE_ANOMALY_BONUS = b.comment("Extra damage the Null Blade deals to anomalies.")
                .defineInRange("bladeAnomalyBonus", 6.0, 0.0, 100.0);
        BLADE_HOLD_ANGER = b.comment("Anger per second Verity gains while you hold the Null Blade.")
                .defineInRange("bladeHoldAnger", 1.5, 0.0, 50.0);
        BLADE_HIT_ANGER = b.comment("Anger Verity gains when it is hit with the Null Blade (on top of the normal hurt anger).")
                .defineInRange("bladeHitAnger", 12.0, 0.0, 100.0);
        b.pop();

        b.push("anger");
        ANGER_BASE = b.comment("Anger gained per second, always.").defineInRange("base", 0.05, 0.0, 10.0);
        ANGER_NIGHT = b.comment("Extra anger per second at night.").defineInRange("night", 0.12, 0.0, 10.0);
        ANGER_LOW_SANITY = b.comment("Extra anger per second at zero sanity (scales linearly).")
                .defineInRange("lowSanity", 0.4, 0.0, 10.0);
        ANGER_DAY_COOLDOWN = b.comment("Anger lost per second in daylight while the player's sanity is above 70%.")
                .defineInRange("dayCooldown", 0.25, 0.0, 10.0);
        ANGER_PER_DAY = b.comment("Extra anger per second per world day (capped at 40 days).")
                .defineInRange("perDay", 0.005, 0.0, 1.0);
        ANGER_HURT = b.comment("Anger gained when Verity is damaged.").defineInRange("onHurt", 10.0, 0.0, 100.0);
        b.pop();

        b.push("verityStats");
        STATS_ENABLED = b.comment("If true, Verity gets stronger as its anger rises (the bonuses below are what it gets at 100 anger).")
                .define("enabled", true);
        STAT_SPEED = b.comment("Movement speed bonus at 100 anger (0.5 = +50%).").defineInRange("speedBonus", 0.5, 0.0, 5.0);
        STAT_DAMAGE = b.comment("Attack damage bonus at 100 anger (1.0 = +100%).").defineInRange("damageBonus", 1.0, 0.0, 10.0);
        STAT_ARMOR = b.comment("Extra armor points at 100 anger.").defineInRange("armorBonus", 6.0, 0.0, 30.0);
        STAT_KNOCKBACK_RES = b.comment("Extra knockback resistance at 100 anger (1.0 = immune).")
                .defineInRange("knockbackResistanceBonus", 0.6, 0.0, 1.0);
        b.pop();

        b.push("jumpscare");
        JUMPSCARE_ENABLED = b.comment("If true, an anomaly that lands the final hit on a player plays the scare animation, then kills them.")
                .define("enabled", true);
        JUMPSCARE_HEALTH = b.comment("A hit triggers the jumpscare when it would be lethal, OR when health is at or below this fraction of max health...")
                .defineInRange("healthFraction", 0.35, 0.0, 1.0);
        JUMPSCARE_SANITY = b.comment("...AND sanity is at or below this fraction of max sanity.")
                .defineInRange("sanityFraction", 0.35, 0.0, 1.0);
        b.pop();

        b.push("anomalies");
        ANOMALIES_ENABLED = b.define("enabled", true);
        SPAWN_ANGER_THRESHOLD = b.comment("Verity starts summoning anomalies above this anger (0-100).")
                .defineInRange("angerThreshold", 25.0, 0.0, 100.0);
        SPAWN_CHANCE_MAX = b.comment("Chance per second of a summon at 100 anger.")
                .defineInRange("maxChancePerSecond", 0.25, 0.0, 1.0);
        ANOMALY_CAP = b.comment("Hard cap on anomalies near a player. The live cap also scales with anger.")
                .defineInRange("cap", 6, 0, 30);
        ANOMALY_LIFETIME_SECONDS = b.comment("Anomalies fade away after this long.")
                .defineInRange("lifetimeSeconds", 120, 10, 3600);
        b.pop();

        SPEC = b.build();
    }

    private VerityConfig() {}
}
