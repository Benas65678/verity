package dev.veritydark.client;

import dev.veritydark.ModContent;
import dev.veritydark.VerityDark;
import dev.veritydark.item.ContainmentLanternItem;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = VerityDark.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.VIGNETTE.id(), "sanity_vignette", SanityHud.VIGNETTE);
        event.registerAbove(VanillaGuiOverlay.VIGNETTE.id(), "sanity_glitch", SanityVisuals.GLITCH);
        event.registerAboveAll("sanity_bar", SanityHud.BAR);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModContent.ANOMALY.get(), SpookRenderer::anomaly);
        event.registerEntityRenderer(ModContent.STALKER.get(), SpookRenderer::stalker);
        event.registerEntityRenderer(ModContent.HALLUCINATION.get(), SpookRenderer::hallucination);
        event.registerEntityRenderer(ModContent.THROWN_TONIC.get(), ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModContent.LANTERN.get(),
                new ResourceLocation(VerityDark.MODID, "lit"),
                (stack, level, entity, seed) -> ContainmentLanternItem.isLit(stack) ? 1.0F : 0.0F));

        // The "Config" button in the Mods menu (Cloth Config is already required by The Verity Mod).
        if (ModList.get().isLoaded("cloth_config")) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> ClothConfigScreen.build(parent)));
        }
    }

    private ClientModEvents() {}
}
