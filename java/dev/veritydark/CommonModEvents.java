package dev.veritydark;

import dev.veritydark.entity.AnomalyEntity;
import dev.veritydark.entity.HallucinationEntity;
import dev.veritydark.entity.StalkerEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VerityDark.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CommonModEvents {
    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ModContent.ANOMALY.get(), AnomalyEntity.createAttributes().build());
        event.put(ModContent.STALKER.get(), StalkerEntity.createAttributes().build());
        event.put(ModContent.HALLUCINATION.get(), HallucinationEntity.createAttributes().build());
    }

    private CommonModEvents() {}
}
