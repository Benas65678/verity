package dev.veritydark.client;

import dev.veritydark.InsanityTier;
import dev.veritydark.ModContent;
import dev.veritydark.VerityClientConfig;
import dev.veritydark.VerityConfig;
import dev.veritydark.VerityDark;
import dev.veritydark.entity.HallucinationEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Spawns translucent, harmless figures that only exist on this client when your mind is shaken.
 * They are added straight to the client world, so no other player (and not the server) ever sees them.
 */
@Mod.EventBusSubscriber(modid = VerityDark.MODID, value = Dist.CLIENT)
public final class HallucinationManager {
    private static final List<HallucinationEntity> ACTIVE = new ArrayList<>();
    private static int nextId = -50_000;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        ACTIVE.removeIf(e -> e.isRemoved() || e.level() != level);
        if (level == null || player == null || mc.isPaused() || player.isSpectator()) return;

        if (!VerityClientConfig.HALLUCINATIONS.get() || !VerityConfig.sanityOn() || !VerityConfig.TIERS_ENABLED.get()) {
            for (HallucinationEntity e : ACTIVE) e.discard();
            ACTIVE.clear();
            return;
        }

        InsanityTier tier = InsanityTier.of(ClientSanity.pct());
        if (!tier.atLeast(InsanityTier.SHAKEN)) return;

        int max = tier == InsanityTier.SHAKEN ? 1 : tier == InsanityTier.FRACTURED ? 2 : 3;
        float chance = tier == InsanityTier.SHAKEN ? 0.0008F : tier == InsanityTier.FRACTURED ? 0.002F : 0.005F;
        if (ACTIVE.size() >= max || level.random.nextFloat() > chance) return;

        spawn(level, player);
    }

    private static void spawn(ClientLevel level, LocalPlayer player) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double dist = 14.0 + level.random.nextDouble() * 10.0;
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);
            for (int y = Mth.floor(player.getY()) + 4; y >= Mth.floor(player.getY()) - 6; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockPos below = pos.below();
                if (!level.hasChunkAt(pos)) break;
                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
                if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) break;
                if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) break;

                HallucinationEntity ghost = new HallucinationEntity(ModContent.HALLUCINATION.get(), level);
                ghost.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
                level.putNonPlayerEntity(nextId--, ghost);
                ACTIVE.add(ghost);
                return;
            }
        }
    }

    private HallucinationManager() {}
}
