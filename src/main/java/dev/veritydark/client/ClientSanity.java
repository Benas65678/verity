package dev.veritydark.client;

/** Plain data holder for the synced sanity value. Contains no client-only classes on purpose. */
public final class ClientSanity {
    public static float value = 100.0F;
    public static float max = 100.0F;

    public static void set(float newValue, float newMax) {
        value = newValue;
        max = Math.max(1.0F, newMax);
    }

    public static float pct() {
        return Math.max(0.0F, Math.min(1.0F, value / max));
    }

    private ClientSanity() {}
}
