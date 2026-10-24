package dev.veritydark;

import com.mojang.logging.LogUtils;
import dev.veritydark.net.Network;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(VerityDark.MODID)
public class VerityDark {
    public static final String MODID = "veritydark";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VerityDark() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModSounds.register(modBus);
        ModContent.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VerityConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, VerityClientConfig.SPEC);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Network::register);
    }
}
