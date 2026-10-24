package dev.veritydark;

import dev.veritydark.block.SmileLampBlock;
import dev.veritydark.effect.ClarityEffect;
import dev.veritydark.effect.DreadEffect;
import dev.veritydark.entity.AnomalyEntity;
import dev.veritydark.entity.HallucinationEntity;
import dev.veritydark.entity.StalkerEntity;
import dev.veritydark.entity.ThrownSanityTonic;
import dev.veritydark.item.AnomalyDetectorItem;
import dev.veritydark.item.ComfortCookieItem;
import dev.veritydark.item.ContainmentArmorItem;
import dev.veritydark.item.VerityJournalItem;
import dev.veritydark.item.ContainmentLanternItem;
import dev.veritydark.item.ModTiers;
import dev.veritydark.item.NullBladeItem;
import dev.veritydark.item.SanityTonicItem;
import dev.veritydark.item.SmileCharmItem;
import dev.veritydark.item.ThrowableTonicItem;
import dev.veritydark.item.VerityMonitorItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModContent {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, VerityDark.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, VerityDark.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, VerityDark.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, VerityDark.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VerityDark.MODID);

    // ---- effects ----
    public static final RegistryObject<MobEffect> CLARITY = EFFECTS.register("clarity", ClarityEffect::new);
    public static final RegistryObject<MobEffect> DREAD = EFFECTS.register("dread", DreadEffect::new);

    // ---- blocks ----
    public static final RegistryObject<Block> SMILE_LAMP = BLOCKS.register("smile_lamp",
            () -> new SmileLampBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(1.2F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 15)));

    // ---- items ----
    public static final RegistryObject<Item> LANTERN = ITEMS.register("containment_lantern",
            () -> new ContainmentLanternItem(new Item.Properties().stacksTo(1).durability(600)));

    public static final RegistryObject<Item> TONIC = ITEMS.register("sanity_tonic",
            () -> new SanityTonicItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> SPLASH_TONIC = ITEMS.register("splash_sanity_tonic",
            () -> new ThrowableTonicItem(new Item.Properties().stacksTo(16), false));

    public static final RegistryObject<Item> LINGERING_TONIC = ITEMS.register("lingering_sanity_tonic",
            () -> new ThrowableTonicItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), true));

    public static final RegistryObject<Item> COMFORT_COOKIE = ITEMS.register("comfort_cookie",
            () -> new ComfortCookieItem(new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(3).saturationMod(0.4F).alwaysEat().build())));

    public static final RegistryObject<Item> SMILE_CHARM = ITEMS.register("smile_charm",
            () -> new SmileCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> VERITY_MONITOR = ITEMS.register("verity_monitor",
            () -> new VerityMonitorItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DISTORTION_SHARD = ITEMS.register("distortion_shard",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> NULL_BLADE = ITEMS.register("null_blade",
            () -> new NullBladeItem(ModTiers.NULL, 3, -2.4F, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> DETECTOR = ITEMS.register("anomaly_detector",
            () -> new AnomalyDetectorItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> JOURNAL = ITEMS.register("verity_journal",
            () -> new VerityJournalItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> CONTAINMENT_HELMET = ITEMS.register("containment_helmet",
            () -> new ContainmentArmorItem(ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> CONTAINMENT_CHESTPLATE = ITEMS.register("containment_chestplate",
            () -> new ContainmentArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> CONTAINMENT_LEGGINGS = ITEMS.register("containment_leggings",
            () -> new ContainmentArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> CONTAINMENT_BOOTS = ITEMS.register("containment_boots",
            () -> new ContainmentArmorItem(ArmorItem.Type.BOOTS, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> SMILE_LAMP_ITEM = ITEMS.register("smile_lamp",
            () -> new BlockItem(SMILE_LAMP.get(), new Item.Properties()));

    // ---- entities ----
    public static final RegistryObject<EntityType<AnomalyEntity>> ANOMALY = ENTITIES.register("anomaly",
            () -> EntityType.Builder.of(AnomalyEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 3.0F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(VerityDark.MODID, "anomaly").toString()));

    public static final RegistryObject<EntityType<StalkerEntity>> STALKER = ENTITIES.register("stalker",
            () -> EntityType.Builder.of(StalkerEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 3.0F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(VerityDark.MODID, "stalker").toString()));

    /** Client-side only: created by the HallucinationManager, never saved or sent by the server. */
    public static final RegistryObject<EntityType<HallucinationEntity>> HALLUCINATION = ENTITIES.register("hallucination",
            () -> EntityType.Builder.of(HallucinationEntity::new, MobCategory.MISC)
                    .sized(0.7F, 3.0F)
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(0)
                    .build(new ResourceLocation(VerityDark.MODID, "hallucination").toString()));

    public static final RegistryObject<EntityType<ThrownSanityTonic>> THROWN_TONIC = ENTITIES.register("thrown_tonic",
            () -> EntityType.Builder.<ThrownSanityTonic>of(ThrownSanityTonic::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(new ResourceLocation(VerityDark.MODID, "thrown_tonic").toString()));

    public static final RegistryObject<Item> ANOMALY_SPAWN_EGG = ITEMS.register("anomaly_spawn_egg",
            () -> new ForgeSpawnEggItem(ANOMALY, 0x0B0B10, 0xF5D142, new Item.Properties()));

    public static final RegistryObject<Item> STALKER_SPAWN_EGG = ITEMS.register("stalker_spawn_egg",
            () -> new ForgeSpawnEggItem(STALKER, 0x050507, 0xC0392B, new Item.Properties()));

    // ---- creative tab ----
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.veritydark"))
                    .icon(() -> new ItemStack(SMILE_CHARM.get()))
                    .displayItems((params, out) -> {
                        out.accept(LANTERN.get());
                        out.accept(TONIC.get());
                        out.accept(SPLASH_TONIC.get());
                        out.accept(LINGERING_TONIC.get());
                        out.accept(COMFORT_COOKIE.get());
                        out.accept(SMILE_CHARM.get());
                        out.accept(VERITY_MONITOR.get());
                        out.accept(DETECTOR.get());
                        out.accept(JOURNAL.get());
                        out.accept(CONTAINMENT_HELMET.get());
                        out.accept(CONTAINMENT_CHESTPLATE.get());
                        out.accept(CONTAINMENT_LEGGINGS.get());
                        out.accept(CONTAINMENT_BOOTS.get());
                        out.accept(SMILE_LAMP_ITEM.get());
                        out.accept(DISTORTION_SHARD.get());
                        out.accept(NULL_BLADE.get());
                        out.accept(ANOMALY_SPAWN_EGG.get());
                        out.accept(STALKER_SPAWN_EGG.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        TABS.register(bus);
    }

    private ModContent() {}
}
