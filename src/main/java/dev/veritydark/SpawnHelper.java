package dev.veritydark;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** Finds a floor around a point and spawns a mob there. */
public final class SpawnHelper {

    /**
     * @param angleHint radians (0 = +X, PI/2 = +Z) the search is centred on, or NaN for any direction
     * @param maxLight  spawn only where block/sky light is at most this (15 = anywhere)
     */
    public static <T extends Mob> T spawnAround(ServerLevel level, EntityType<T> type, Vec3 center,
                                                double minDist, double maxDist, double angleHint, int maxLight,
                                                Consumer<T> init) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = Double.isNaN(angleHint)
                    ? level.random.nextDouble() * Math.PI * 2.0
                    : angleHint + (level.random.nextDouble() - 0.5) * Math.toRadians(140.0);
            double dist = minDist + level.random.nextDouble() * (maxDist - minDist);
            int x = Mth.floor(center.x + Math.cos(angle) * dist);
            int z = Mth.floor(center.z + Math.sin(angle) * dist);
            int top = Mth.floor(center.y) + 4;
            int bottom = Mth.floor(center.y) - 8;

            for (int y = top; y >= bottom; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockPos below = pos.below();
                if (!level.hasChunkAt(pos)) break;
                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
                if (level.getMaxLocalRawBrightness(pos) > maxLight) break;

                T mob = type.create(level);
                if (mob == null) return null;
                mob.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
                if (!level.noCollision(mob)) {
                    mob.discard();
                    continue;
                }
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
                if (init != null) init.accept(mob);
                level.addFreshEntity(mob);
                return mob;
            }
        }
        return null;
    }

    private SpawnHelper() {}
}
