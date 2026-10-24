package dev.veritydark;

import dev.veritydark.entity.AnomalyEntity;
import dev.veritydark.entity.StalkerEntity;
import dev.veritydark.item.AnomalyDetectorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Drives the Anomaly Detector: a proximity beeper with a signal meter above the hotbar and no direction arrow. */
@Mod.EventBusSubscriber(modid = VerityDark.MODID)
public final class DetectorManager {
    private static final String KEY = "veritydark_detector_cd";

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (!VerityConfig.on() || !VerityConfig.DETECTOR_ENABLED.get()) return;
        if (!(player.getMainHandItem().getItem() instanceof AnomalyDetectorItem)
                && !(player.getOffhandItem().getItem() instanceof AnomalyDetectorItem)) return;

        CompoundTag data = player.getPersistentData();
        int cooldown = data.getInt(KEY) - 1;
        if (cooldown > 0) {
            data.putInt(KEY, cooldown);
            return;
        }

        double range = VerityConfig.DETECTOR_RANGE.get();
        AABB box = player.getBoundingBox().inflate(range);
        List<Mob> found = player.level().getEntitiesOfClass(Mob.class, box,
                e -> (e instanceof AnomalyEntity || e instanceof StalkerEntity) && e.isAlive());
        double nearest = Double.MAX_VALUE;
        for (Mob m : found) nearest = Math.min(nearest, Math.sqrt(m.distanceToSqr(player)));

        if (nearest > range) {
            data.putInt(KEY, 20);
            player.displayClientMessage(Component.translatable("veritydark.detector.clear").withStyle(ChatFormatting.DARK_GREEN), true);
            return;
        }

        float closeness = Mth.clamp(1.0F - (float) (nearest / range), 0.0F, 1.0F);
        int bars = Math.max(1, Math.round(closeness * 8.0F));
        data.putInt(KEY, Math.round(Mth.lerp((float) Math.pow(closeness, 0.7), 36.0F, 3.0F)));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DETECTOR_BEEP.get(),
                SoundSource.PLAYERS, 0.55F, 0.7F + closeness * 0.9F);

        ChatFormatting color = closeness > 0.66F ? ChatFormatting.RED : closeness > 0.33F ? ChatFormatting.GOLD : ChatFormatting.YELLOW;
        String meter = "|".repeat(bars) + "·".repeat(8 - bars);
        player.displayClientMessage(Component.translatable("veritydark.detector.signal",
                Component.literal(meter).withStyle(color)).withStyle(ChatFormatting.GRAY), true);
    }

    private DetectorManager() {}
}
