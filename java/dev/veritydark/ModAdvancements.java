package dev.veritydark;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Awards advancements whose criterion is "impossible" in the JSON and is granted from code. */
public final class ModAdvancements {
    public static void grant(ServerPlayer player, String path) {
        if (player.getServer() == null) return;
        Advancement advancement = player.getServer().getAdvancements().getAdvancement(new ResourceLocation(VerityDark.MODID, path));
        if (advancement != null) player.getAdvancements().award(advancement, "c");
    }

    private ModAdvancements() {}
}
