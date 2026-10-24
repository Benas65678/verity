package dev.veritydark;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;

public final class VerityUtil {
    /**
     * The addon never touches Verity's classes; it recognises Verity by registry id.
     * That keeps it working across Verity updates. Run /veritydark list to see the real ids.
     */
    public static boolean isVerity(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key == null) return false;
        if (!key.getNamespace().equals(VerityConfig.VERITY_NAMESPACE.get())) return false;
        var ids = VerityConfig.VERITY_ENTITY_IDS.get();
        return ids.isEmpty() || ids.contains(key.toString());
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
