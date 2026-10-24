package dev.veritydark;

import net.minecraftforge.common.ForgeConfigSpec;

/** Visual and audio options. File: config/veritydark-client.toml */
public final class VerityClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue SHOW_HUD;
    public static final ForgeConfigSpec.BooleanValue VIGNETTE;
    public static final ForgeConfigSpec.BooleanValue CAMERA_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue GLITCH_OVERLAY;
    public static final ForgeConfigSpec.BooleanValue PHANTOM_SOUNDS;
    public static final ForgeConfigSpec.BooleanValue HEARTBEAT;
    public static final ForgeConfigSpec.BooleanValue HALLUCINATIONS;
    public static final ForgeConfigSpec.DoubleValue HALLUCINATION_OPACITY;
    public static final ForgeConfigSpec.DoubleValue ANOMALY_OPACITY;
    public static final ForgeConfigSpec.DoubleValue EFFECT_INTENSITY;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("hud");
        SHOW_HUD = b.comment("The sanity bar in the top-right corner.").define("sanityBar", true);
        VIGNETTE = b.comment("Dark screen edges at low sanity.").define("vignette", true);
        b.pop();

        b.push("sanityVisuals");
        CAMERA_EFFECTS = b.comment("Camera sway, roll and FOV pulsing as your mind frays.").define("cameraEffects", true);
        GLITCH_OVERLAY = b.comment("Glitch bars, colour fringes and static at low sanity.").define("glitchOverlay", true);
        EFFECT_INTENSITY = b.comment("Overall strength of the visual effects (0 = none, 1 = full).")
                .defineInRange("intensity", 1.0, 0.0, 1.0);
        b.pop();

        b.push("sanityAudio");
        PHANTOM_SOUNDS = b.comment("Fake footsteps, knocks, whispers and hisses.").define("phantomSounds", true);
        HEARTBEAT = b.comment("Heartbeat below 25% sanity.").define("heartbeat", true);
        b.pop();

        b.push("hallucinations");
        HALLUCINATIONS = b.comment("Translucent figures only you can see at low sanity. They are harmless.")
                .define("enabled", true);
        HALLUCINATION_OPACITY = b.comment("How see-through hallucinations are (1 = solid).")
                .defineInRange("opacity", 0.45, 0.1, 1.0);
        ANOMALY_OPACITY = b.comment("How see-through real anomalies are (1 = solid). A little ghostly by default.")
                .defineInRange("anomalyOpacity", 0.9, 0.3, 1.0);
        b.pop();

        SPEC = b.build();
    }

    private VerityClientConfig() {}
}
