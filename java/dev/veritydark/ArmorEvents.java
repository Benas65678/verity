package dev.veritydark;

import dev.veritydark.entity.AnomalyEntity;
import dev.veritydark.entity.StalkerEntity;
import dev.veritydark.item.ContainmentArmorItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Containment armor effects. */
@Mod.EventBusSubscriber(modid = VerityDark.MODID)
public final class ArmorEvents {

    /** Mobs judge how far away they can notice you by this multiplier, so less visibility = shorter detection range. */
    @SubscribeEvent
    public static void visibility(LivingEvent.LivingVisibilityEvent event) {
        if (!VerityConfig.on() || !VerityConfig.ARMOR_ENABLED.get()) return;
        int n = ContainmentArmorItem.pieces(event.getEntity());
        if (n == 0) return;
        event.modifyVisibility(Math.max(0.1, 1.0 - n * VerityConfig.ARMOR_STEALTH.get()));
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (!VerityConfig.on() || !VerityConfig.ARMOR_ENABLED.get()) return;
        if (!(event.getSource().getEntity() instanceof AnomalyEntity) && !(event.getSource().getEntity() instanceof StalkerEntity)) return;
        int n = ContainmentArmorItem.pieces(event.getEntity());
        if (n == 0) return;
        event.setAmount((float) (event.getAmount() * Math.max(0.2, 1.0 - n * VerityConfig.ARMOR_REDUCTION.get())));
    }

    @SubscribeEvent
    public static void equip(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getSlot().getType() != EquipmentSlot.Type.ARMOR) return;
        int n = ContainmentArmorItem.pieces((LivingEntity) player);
        if (n > 0) Journal.unlock(player, Journal.Entry.ARMOR);
        if (n == 4) ModAdvancements.grant(player, "containment_suit");
    }

    private ArmorEvents() {}
}
