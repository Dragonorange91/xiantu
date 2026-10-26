package com.longju.xiantu.registry;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.item.ArtifactItem;
import com.longju.xiantu.item.PillItem;
import com.longju.xiantu.item.CultivationManualItem;
import com.longju.xiantu.item.DemonCoreItem;
import com.longju.xiantu.item.BeastMaterialItem;
import com.longju.xiantu.item.CultivationProbeItem;
import com.longju.xiantu.item.SpiritStoneItem;
import com.longju.xiantu.item.SpiritStoneTier;
import com.longju.xiantu.item.ReincarnationStoneItem;
import com.longju.xiantu.mining.MiningPickaxeItem;
import com.longju.xiantu.gongfa.GongfaItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.ForgeSpawnEggItem;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, XiantuMod.MOD_ID);
    static { com.longju.xiantu.refining.QitianMaterials.items(ITEMS); com.longju.xiantu.spell.SpellScrolls.bootstrap(); }
    static { com.longju.xiantu.refining.RefiningItems.bootstrap(); com.longju.xiantu.refining.CraftingTreatises.bootstrap(); com.longju.xiantu.refining.SpiritFire.bootstrap(); com.longju.xiantu.equipment.EquipmentPreview.bootstrap(); com.longju.xiantu.equipment.EquipmentModelBatch.bootstrap(); }
    public static final RegistryObject<Item> GUIDING_STONE = ITEMS.register("guiding_stone", com.longju.xiantu.item.GuidingStoneItem::new);
    public static final RegistryObject<Item> FAN_LING_SHI = registerStone(SpiritStoneTier.FAN_LING_SHI);
    public static final RegistryObject<Item> LING_JING = registerStone(SpiritStoneTier.LING_JING);
    public static final RegistryObject<Item> LING_YU = registerStone(SpiritStoneTier.LING_YU);
    public static final RegistryObject<Item> XUAN_JING = registerStone(SpiritStoneTier.XUAN_JING);
    public static final RegistryObject<Item> DI_SUI = registerStone(SpiritStoneTier.DI_SUI);
    public static final RegistryObject<Item> TIAN_JING = registerStone(SpiritStoneTier.TIAN_JING);
    public static final RegistryObject<Item> XIAN_YU = registerStone(SpiritStoneTier.XIAN_YU);
    public static final RegistryObject<Item> DAO_SHI = registerStone(SpiritStoneTier.DAO_SHI);
    public static final RegistryObject<Item> SPIRIT_STONE = FAN_LING_SHI;
    static { com.longju.xiantu.mining.MetalMaterialRegistry.registerItems(ITEMS); com.longju.xiantu.mining.SpecialMineralRegistry.registerItems(ITEMS); com.longju.xiantu.mining.AdvancedMinerals.registerItems(ITEMS); }
    public static final java.util.Map<String, RegistryObject<Item>> METAL_RAW_ORES =
            java.util.Collections.unmodifiableMap(com.longju.xiantu.mining.MetalMaterialRegistry.RAW_ORES);
    public static final java.util.Map<String, RegistryObject<Item>> METAL_INGOTS =
            java.util.Collections.unmodifiableMap(com.longju.xiantu.mining.MetalMaterialRegistry.INGOTS);
    public static final RegistryObject<Item> HANXING_PICKAXE = ITEMS.register("hanxing_pickaxe", MiningPickaxeItem::hanxing);
    public static final RegistryObject<Item> JIUYOU_PICKAXE = ITEMS.register("jiuyou_pickaxe", MiningPickaxeItem::jiuyou);
    public static final RegistryObject<Item> HUNDUN_PICKAXE = ITEMS.register("hundun_pickaxe", MiningPickaxeItem::hundun);
    public static final RegistryObject<Item> DEMON_CORE = ITEMS.register("demon_core", () -> new DemonCoreItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> BEAST_HIDE = ITEMS.register("beast_hide", () -> new com.longju.xiantu.item.BeastHideItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> BEAST_BONE = ITEMS.register("beast_bone", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> BEAST_FANG = ITEMS.register("beast_fang", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> BEAST_CLAW = ITEMS.register("beast_claw", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> BEAST_SINEW = ITEMS.register("beast_sinew", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> BEAST_SCALE = ITEMS.register("beast_scale", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> BEAST_HORN = ITEMS.register("beast_horn", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> BEAST_VENOM_SAC = ITEMS.register("beast_venom_sac", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> BEAST_SILK = ITEMS.register("beast_silk", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> BEAST_FEATHER = beastMaterial("beast_feather", "item.xiantu.beast_feather", true);
    public static final RegistryObject<Item> BEAST_BLOOD_SAC = beastMaterial("beast_blood_sac", "item.xiantu.beast_blood_sac", true);
    public static final RegistryObject<Item> BEAST_CARAPACE = beastMaterial("beast_carapace", "item.xiantu.beast_carapace", true);
    public static final RegistryObject<Item> WOLF_HAIR = beastMaterial("wolf_hair", "item.xiantu.wolf_hair", false);
    public static final RegistryObject<Item> FOX_HAIR = beastMaterial("fox_hair", "item.xiantu.fox_hair", false);
    public static final RegistryObject<Item> VULTURE_FEATHER = beastMaterial("vulture_feather", "item.xiantu.vulture_feather", false);
    public static final RegistryObject<Item> LION_MANE = beastMaterial("lion_mane", "item.xiantu.lion_mane", false);
    public static final RegistryObject<Item> FALCON_FEATHER = beastMaterial("falcon_feather", "item.xiantu.falcon_feather", false);
    public static final RegistryObject<Item> THUNDER_CROW_FEATHER = beastMaterial("thunder_crow_feather", "item.xiantu.thunder_crow_feather", false);
    public static final RegistryObject<Item> INNER_DEMON_CRYSTAL = ITEMS.register("inner_demon_crystal", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> SPIRIT_HERB = com.longju.xiantu.medicine.MedicineRegistry.HERB_ITEMS.get("herb_0058");
    public static final RegistryObject<Item> FIRE_HERB = com.longju.xiantu.medicine.MedicineRegistry.HERB_ITEMS.get("herb_0030");
    public static final RegistryObject<Item> WATER_HERB = com.longju.xiantu.medicine.MedicineRegistry.HERB_ITEMS.get("herb_0006");
    public static final RegistryObject<Item> POISON_MUSHROOM = com.longju.xiantu.medicine.MedicineRegistry.HERB_ITEMS.get("herb_0026");
    public static final RegistryObject<Item> CULTIVATION_MANUAL = ITEMS.register("cultivation_manual", () -> new CultivationManualItem(new Item.Properties()));
    public static final RegistryObject<Item> CULTIVATION_PROBE = ITEMS.register("cultivation_probe", () -> new CultivationProbeItem(new Item.Properties()));
    public static final RegistryObject<Item> REINCARNATION_STONE = ITEMS.register("reincarnation_stone", () -> new ReincarnationStoneItem(new Item.Properties()));
    static { com.longju.xiantu.alchemy.PillCatalog.bootstrap(); com.longju.xiantu.alchemy.PillWaymark.bootstrap(); com.longju.xiantu.alchemy.AlchemyItems.bootstrap(); }
    public static final RegistryObject<Item> BASIC_PILL = com.longju.xiantu.alchemy.PillCatalog.ITEMS.get("pill_01");
    public static final RegistryObject<Item> FOUNDATION_PILL = com.longju.xiantu.alchemy.PillCatalog.ITEMS.get("pill_02");
    public static final RegistryObject<Item> SPIRIT_RECOVERY_PILL = com.longju.xiantu.alchemy.PillCatalog.ITEMS.get("pill_04");
    public static final RegistryObject<Item> BODY_TEMPERING_PILL = com.longju.xiantu.alchemy.PillCatalog.ITEMS.get("pill_14");
    public static final RegistryObject<Item> YAN_TU_JUE = ITEMS.register("yan_tu_jue", () -> new GongfaItem("yan_tu_jue", new Item.Properties()));
    public static final RegistryObject<Item> XUAN_SHUI_JUE = ITEMS.register("xuan_shui_jue", () -> new GongfaItem("xuan_shui_jue", new Item.Properties()));
    public static final RegistryObject<Item> CHI_YAN_JUE = ITEMS.register("chi_yan_jue", () -> new GongfaItem("chi_yan_jue", new Item.Properties()));
    public static final RegistryObject<Item> QING_MU_JUE = ITEMS.register("qing_mu_jue", () -> new GongfaItem("qing_mu_jue", new Item.Properties()));
    public static final RegistryObject<Item> YOU_SHUI_ZHEN_JING = ITEMS.register("you_shui_zhen_jing", () -> new GongfaItem("you_shui_zhen_jing", new Item.Properties()));
    public static final RegistryObject<Item> QING_SHAN_ZHEN_DI = ITEMS.register("qing_shan_zhen_di", () -> new GongfaItem("qing_shan_zhen_di", new Item.Properties()));
    public static final RegistryObject<Item> FEN_CHI_CHI_HUO = ITEMS.register("fen_chi_chi_huo", () -> new GongfaItem("fen_chi_chi_huo", new Item.Properties()));
    public static final RegistryObject<Item> BAI_CAO_ZHEN_JING = ITEMS.register("bai_cao_zhen_jing", () -> new GongfaItem("bai_cao_zhen_jing", new Item.Properties()));
    public static final RegistryObject<Item> JIN_GUANG_XUAN_GONG = ITEMS.register("jin_guang_xuan_gong", () -> new GongfaItem("jin_guang_xuan_gong", new Item.Properties()));
    public static final java.util.Map<String,RegistryObject<Item>> ADDITIONAL_GONGFA = registerAdditionalGongfa();
    private static java.util.Map<String,RegistryObject<Item>> registerAdditionalGongfa() {
        var result=new java.util.LinkedHashMap<String,RegistryObject<Item>>();
        var old=java.util.Set.of("yan_tu_jue","xuan_shui_jue","chi_yan_jue","qing_mu_jue","you_shui_zhen_jing","qing_shan_zhen_di","fen_chi_chi_huo","bai_cao_zhen_jing","jin_guang_xuan_gong");
        for(var definition:com.longju.xiantu.gongfa.GongfaRegistry.ALL) if(!old.contains(definition.id()))
            result.put(definition.id(),ITEMS.register(definition.id(),()->new GongfaItem(definition.id(),new Item.Properties())));
        return java.util.Collections.unmodifiableMap(result);
    }
    public static final java.util.Map<String,RegistryObject<Item>> COMPLETION_EGGS = CompletionEntities.registerEggs();
    public static final RegistryObject<Item> YANWEI_FOX_SPAWN_EGG = ITEMS.register("yanwei_fox_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.YANWEI_FOX, 0xCB441C, 0xFFE85C, new Item.Properties()));
    public static final RegistryObject<Item> CRACKED_BLADE_WOLF_SPAWN_EGG = ITEMS.register("cracked_blade_wolf_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.CRACKED_BLADE_WOLF, 0x777C83, 0xE9E4D9, new Item.Properties()));
    public static final RegistryObject<Item> CRAG_ROCK_SPIDER_SPAWN_EGG = ITEMS.register("crag_rock_spider_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.CRAG_ROCK_SPIDER, 0x8D8175, 0xE6D2A7, new Item.Properties()));
    public static final RegistryObject<Item> COLD_FANG_WOLF_SPAWN_EGG = ITEMS.register("cold_fang_wolf_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.COLD_FANG_WOLF, 0x235C89, 0xDCEBF3, new Item.Properties()));
    public static final RegistryObject<Item> THUNDER_FEATHER_RAVEN_SPAWN_EGG = ITEMS.register("thunder_feather_raven_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.THUNDER_FEATHER_RAVEN, 0x171C29, 0xBCC6DD, new Item.Properties()));
    public static final RegistryObject<Item> VINE_DEER_SPAWN_EGG = ITEMS.register("vine_deer_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.VINE_DEER, 0x584632, 0x6B934A, new Item.Properties()));
    public static final RegistryObject<Item> MOUNTAIN_BEAR_SPAWN_EGG = ITEMS.register("mountain_bear_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.MOUNTAIN_BEAR, 0x302A28, 0xA67D38, new Item.Properties()));
    public static final RegistryObject<Item> GOLD_MANE_LION_SPAWN_EGG = ITEMS.register("gold_mane_lion_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.GOLD_MANE_LION, 0xAA7627, 0xF0D889, new Item.Properties()));
    public static final RegistryObject<Item> RED_SUN_LION_SPAWN_EGG = ITEMS.register("red_sun_lion_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.RED_SUN_LION, 0x8D2013, 0xF0A234, new Item.Properties()));
    public static final RegistryObject<Item> THICK_HORN_RHINO_SPAWN_EGG = ITEMS.register("thick_horn_rhino_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.THICK_HORN_RHINO, 0x806135, 0xD9C99C, new Item.Properties()));
    public static final RegistryObject<Item> MOSS_BACK_BEAR_SPAWN_EGG = ITEMS.register("moss_back_bear_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.MOSS_BACK_BEAR, 0x53402E, 0x67752B, new Item.Properties()));
    public static final RegistryObject<Item> THUNDER_DEER_SPAWN_EGG = ITEMS.register("thunder_deer_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.THUNDER_DEER, 0x3A3B42, 0xE6C34C, new Item.Properties()));
    public static final RegistryObject<Item> ROCK_BACK_BOAR_SPAWN_EGG = ITEMS.register("rock_back_boar_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.ROCK_BACK_BOAR, 0x543B24, 0xBBA279, new Item.Properties()));
    public static final RegistryObject<Item> IRON_TOOTH_BADGER_SPAWN_EGG = ITEMS.register("iron_tooth_badger_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.IRON_TOOTH_BADGER, 0x262528, 0xBFC0B9, new Item.Properties()));
    static { com.longju.xiantu.talisman.TalismanItems.bootstrap(); }
    private static Item.Properties food(int stackSize) { return new Item.Properties().stacksTo(stackSize).food(new FoodProperties.Builder().nutrition(1).saturationMod(0.2f).alwaysEat().build()); }
    private static RegistryObject<Item> registerStone(SpiritStoneTier tier) {
        return ITEMS.register(tier.registryName(), () -> new SpiritStoneItem(new Item.Properties(), tier));
    }
    private static RegistryObject<Item> beastMaterial(String id, String nameKey, boolean storesElement) {
        return ITEMS.register(id, () -> new BeastMaterialItem(new Item.Properties().stacksTo(64), nameKey, storesElement));
    }
    public static Item getSpiritStone(SpiritStoneTier tier) {
        return switch (tier) {
            case FAN_LING_SHI -> FAN_LING_SHI.get(); case LING_JING -> LING_JING.get();
            case LING_YU -> LING_YU.get(); case XUAN_JING -> XUAN_JING.get();
            case DI_SUI -> DI_SUI.get(); case TIAN_JING -> TIAN_JING.get();
            case XIAN_YU -> XIAN_YU.get(); case DAO_SHI -> DAO_SHI.get();
        };
    }
    private ModItems() {}
}

