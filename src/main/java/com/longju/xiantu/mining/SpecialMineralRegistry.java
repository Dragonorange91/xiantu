package com.longju.xiantu.mining;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.*;

/** Plain geological ingredients: no appraisal, cultivation rank, ingot or powder forms. */
public final class SpecialMineralRegistry {
    public record Mineral(String id, String oreId, String name, int minY, int maxY, double chance,
                          int minVein, int maxVein, String habitat) {}
    public static final List<Mineral> ALL = List.of(
            new Mineral("cinnabar", "cinnabar_ore", "朱砂", -32, 48, .60, 4, 4, "stone"),
            new Mineral("purple_pattern_crystal", "purple_pattern_crystal_ore", "紫纹晶", -16, 40, 0.1, 3, 5, "stone"),
            new Mineral("starlight_stone", "starlight_stone_ore", "星辉石", 48, 160, 0.06, 2, 3, "mountain"),
            new Mineral("chaos_crystal", "chaos_crystal_ore", "混沌晶", -56, -32, 0.01, 1, 2, "deep"),
            new Mineral("cold_soul_crystal", "cold_soul_crystal_ore", "寒魄晶", -48, 24, 0.08, 2, 3, "cold"),
            new Mineral("flame_heart_stone", "flame_heart_stone_ore", "炎心石", -48, 16, 0.04, 1, 2, "lava"),
            new Mineral("thunder_stone", "thunder_stone_ore", "雷鸣石", -24, 40, 0.08, 2, 3, "thunder"),
            new Mineral("lodestone_shard", "lodestone_ore", "玄磁石", -48, 24, 0.09, 3, 5, "deep"),
            new Mineral("moonlit_stone", "moonlit_stone_ore", "月辉石", 48, 144, 0.06, 2, 4, "mountain"),
            new Mineral("scoria_core", "scoria_core_ore", "炽曜石", 20, 100, 0.08, 2, 4, "nether"),
            new Mineral("void_crystal", "void_crystal_ore", "虚空晶", 0, 96, 0.03, 1, 2, "end"));
    public static final Map<String,RegistryObject<Block>> ORES = new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Item>> MATERIALS = new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Item>> ORE_ITEMS = new LinkedHashMap<>();
    public static void registerBlocks(DeferredRegister<Block> blocks) {
        for (var m:ALL) ORES.put(m.id(),blocks.register(m.oreId(),()->m.id().equals("cinnabar")?new com.longju.xiantu.talisman.CinnabarOreBlock():new Block(
                BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0F,3.0F).requiresCorrectToolForDrops())));
    }
    public static void registerItems(DeferredRegister<Item> items) {
        for (var m:ALL) MATERIALS.put(m.id(),items.register(m.id(),()->new Item(new Item.Properties().stacksTo(64))));
    }
    public static void registerBlockItems(DeferredRegister<Item> items) {
        for (var m:ALL) ORE_ITEMS.put(m.id(),items.register(m.oreId(),()->new BlockItem(ORES.get(m.id()).get(),new Item.Properties())));
    }
    private SpecialMineralRegistry() {}
}
