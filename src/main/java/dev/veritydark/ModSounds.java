package dev.veritydark;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** All custom sound events. The actual audio is described in assets/veritydark/sounds.json. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, VerityDark.MODID);

    public static final RegistryObject<SoundEvent> ANOMALY_AMBIENT = reg("entity.anomaly.ambient");
    public static final RegistryObject<SoundEvent> ANOMALY_HURT = reg("entity.anomaly.hurt");
    public static final RegistryObject<SoundEvent> ANOMALY_DEATH = reg("entity.anomaly.death");
    public static final RegistryObject<SoundEvent> ANOMALY_STEP = reg("entity.anomaly.step");
    public static final RegistryObject<SoundEvent> ANOMALY_ATTACK = reg("entity.anomaly.attack");
    public static final RegistryObject<SoundEvent> ANOMALY_JUMPSCARE = reg("entity.anomaly.jumpscare");

    public static final RegistryObject<SoundEvent> LANTERN_IGNITE = reg("item.lantern.ignite");
    public static final RegistryObject<SoundEvent> LANTERN_EXTINGUISH = reg("item.lantern.extinguish");
    public static final RegistryObject<SoundEvent> TONIC_SHATTER = reg("item.tonic.shatter");
    public static final RegistryObject<SoundEvent> VERITY_PLEASED = reg("entity.verity.pleased");
    public static final RegistryObject<SoundEvent> LAMP_PULSE = reg("block.smile_lamp.pulse");

    public static final RegistryObject<SoundEvent> DETECTOR_BEEP = reg("item.detector.beep");

    public static final RegistryObject<SoundEvent> HEARTBEAT = reg("ambient.heartbeat");
    public static final RegistryObject<SoundEvent> WHISPER = reg("ambient.whisper");
    public static final RegistryObject<SoundEvent> ANGER_RISE = reg("ambient.anger_rise");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(VerityDark.MODID, name)));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

    private ModSounds() {}
}
