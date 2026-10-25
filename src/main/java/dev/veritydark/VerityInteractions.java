package dev.veritydark;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Right-click Verity with a gift (cookie, tonic, cake...) to calm it and earn its trust. See TrustManager. */
@Mod.EventBusSubscriber(modid = VerityDark.MODID)
public final class VerityInteractions {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void giveGift(PlayerInteractEvent.EntityInteract event) {
        if (!VerityConfig.on()) return;
        ItemStack stack = event.getItemStack();
        Entity target = event.getTarget();
        if (!VerityUtil.isVerity(target)) return;
        Entity verity = target;
        TrustManager.Gift gift = TrustManager.giftFor(stack);
        if (gift == null) return;

        boolean client = event.getLevel().isClientSide();
        event.setCancellationResult(InteractionResult.sidedSuccess(client));
        event.setCanceled(true);
        if (client) return;

        Player player = event.getEntity();
        if (player instanceof ServerPlayer sp) {
            TrustManager.applyGift((ServerLevel) event.getLevel(), sp, verity, stack, gift);
        }
        if (gift.trust() > 0.0F && target instanceof Mob mob) mob.setTarget(null);
        if (!player.getAbilities().instabuild) stack.shrink(1);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void giftSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!VerityConfig.on()) return;
        if (!VerityUtil.isVerity(event.getTarget()) || TrustManager.giftFor(event.getItemStack()) == null) return;
        // Stop Verity's own "pick up" handling; the EntityInteract event does the actual gift.
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        event.setCanceled(true);
        if (!event.getLevel().isClientSide()) {
            Entity t = event.getTarget();
            ItemStack stack = event.getItemStack();
            Player player = event.getEntity();
            TrustManager.Gift gift = TrustManager.giftFor(stack);
            if (player instanceof ServerPlayer sp) TrustManager.applyGift((ServerLevel) event.getLevel(), sp, t, stack, gift);
            if (gift.trust() > 0.0F && t instanceof Mob mob) mob.setTarget(null);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
    }

    private VerityInteractions() {}
}
