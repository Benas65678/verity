package dev.veritydark;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class VerityUtil {
    /**
     * The addon never touches Verity's classes; it recognises Verity from outside:
     *  1. if config entityIds is not empty, only those ids count;
     *  2. otherwise anything in the configured namespace counts;
     *  3. otherwise (detectLoosely) anything whose registry id or display name contains "verity".
     * Use /veritydark nearby in game to see what is around you and what the addon thinks of it.
     */
    public static boolean isVerity(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof Player) return false;
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key == null || key.getNamespace().equals(VerityDark.MODID)) return false;

        var ids = VerityConfig.VERITY_ENTITY_IDS.get();
        if (!ids.isEmpty()) return ids.contains(key.toString());
        if (key.getNamespace().equals(VerityConfig.VERITY_NAMESPACE.get())) return true;

        if (VerityConfig.DETECT_LOOSELY.get()) {
            if (key.getNamespace().toLowerCase(Locale.ROOT).contains("verity")) return true;
            if (key.getPath().toLowerCase(Locale.ROOT).contains("verity")) return true;
            return entity.getName().getString().toLowerCase(Locale.ROOT).contains("verity");
        }
        return false;
    }

    /** Closest Verity to the given entity within the radius, or null. */
    public static LivingEntity nearestVerity(Entity around, double radius) {
        List<Entity> found = around.level().getEntities(around, around.getBoundingBox().inflate(radius), VerityUtil::isVerity);
        return found.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(around)))
                .map(e -> (LivingEntity) e)
                .orElse(null);
    }

    private VerityUtil() {}
}
