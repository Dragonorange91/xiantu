package com.longju.xiantu.registry;

import com.longju.xiantu.XiantuMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, XiantuMod.MOD_ID);
    public static final RegistryObject<Item> QUNYOU_FIGURINE = ITEMS.register("qunyou_figurine", () -> new com.longju.xiantu.item.QunyouFigurineItem(ModBlocks.QUNYOU_FIGURINE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SMELTING_FURNACE = ITEMS.register("smelting_furnace", () -> new BlockItem(ModBlocks.SMELTING_FURNACE.get(), new Item.Properties()));
    public static final RegistryObject<Item> FORMATION_CORE = ITEMS.register("formation_core", () -> new BlockItem(ModBlocks.FORMATION_CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARRAY_BASE_1 = ITEMS.register("array_base_1", () -> new BlockItem(ModBlocks.ARRAY_BASE_1.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARRAY_RUNE_1 = ITEMS.register("array_rune_brick_1", () -> new BlockItem(ModBlocks.ARRAY_RUNE_1.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARRAY_PILLAR_1 = ITEMS.register("array_conduit_pillar_1", () -> new BlockItem(ModBlocks.ARRAY_PILLAR_1.get(), new Item.Properties()));
    public static final RegistryObject<Item> FORMATION_CORE_2 = ITEMS.register("array_eye_2", () -> new BlockItem(ModBlocks.FORMATION_CORE_2.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARRAY_BASE_2 = ITEMS.register("array_base_2", () -> new BlockItem(ModBlocks.ARRAY_BASE_2.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARRAY_RUNE_2 = ITEMS.register("array_rune_brick_2", () -> new BlockItem(ModBlocks.ARRAY_RUNE_2.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARRAY_PILLAR_2 = ITEMS.register("array_conduit_pillar_2", () -> new BlockItem(ModBlocks.ARRAY_PILLAR_2.get(), new Item.Properties()));
    public static final RegistryObject<Item> FORMATION_CORE_3 = ITEMS.register("array_eye_3", () -> new BlockItem(ModBlocks.FORMATION_CORE_3.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARRAY_BASE_3 = ITEMS.register("array_base_3", () -> new BlockItem(ModBlocks.ARRAY_BASE_3.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARRAY_RUNE_3 = ITEMS.register("array_rune_brick_3", () -> new BlockItem(ModBlocks.ARRAY_RUNE_3.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARRAY_PILLAR_3 = ITEMS.register("array_conduit_pillar_3", () -> new BlockItem(ModBlocks.ARRAY_PILLAR_3.get(), new Item.Properties()));
    public static final RegistryObject<Item> MEDITATION_CUSHION = ITEMS.register("meditation_cushion", () -> new BlockItem(ModBlocks.MEDITATION_CUSHION.get(), new Item.Properties()));
    public static final RegistryObject<Item> FAN_LING_SHI_ORE = ore("fan_ling_shi_ore", ModBlocks.FAN_LING_SHI_ORE);
    public static final RegistryObject<Item> LING_JING_ORE = ore("ling_jing_ore", ModBlocks.LING_JING_ORE);
    public static final RegistryObject<Item> LING_YU_ORE = ore("ling_yu_ore", ModBlocks.LING_YU_ORE);
    public static final RegistryObject<Item> XUAN_JING_ORE = ore("xuan_jing_ore", ModBlocks.XUAN_JING_ORE);
    public static final RegistryObject<Item> DI_SUI_ORE = ore("di_sui_ore", ModBlocks.DI_SUI_ORE);
    public static final RegistryObject<Item> TIAN_JING_ORE = ore("tian_jing_ore", ModBlocks.TIAN_JING_ORE);
    public static final RegistryObject<Item> XIAN_YU_ORE = ore("xian_yu_ore", ModBlocks.XIAN_YU_ORE);
    public static final RegistryObject<Item> DAO_SHI_ORE = ore("dao_shi_ore", ModBlocks.DAO_SHI_ORE);
    static { com.longju.xiantu.mining.SpecialMineralRegistry.registerBlockItems(ITEMS); com.longju.xiantu.mining.AdvancedMinerals.registerBlockItems(ITEMS); com.longju.xiantu.refining.QitianMaterials.blockItems(ITEMS); }
    public static final java.util.Map<String, RegistryObject<Item>> METAL_ORES = registerMetalBlockItems(false);
    public static final java.util.Map<String, RegistryObject<Item>> METAL_STORAGE_BLOCKS = registerMetalBlockItems(true);

    private static java.util.Map<String, RegistryObject<Item>> registerMetalBlockItems(boolean storage) {
        var result = new java.util.LinkedHashMap<String, RegistryObject<Item>>();
        for (var material : com.longju.xiantu.mining.MetalMaterialRegistry.ALL) {
            String id = material.id() + (storage ? "_block" : "_ore");
            var block = storage ? ModBlocks.METAL_STORAGE_BLOCKS.get(material.id()) : ModBlocks.METAL_ORES.get(material.id());
            result.put(material.id(), ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties())));
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    public static final RegistryObject<Item> AZURE_BRICK = building("azure_brick", ModBlocks.AZURE_BRICK);
    public static final RegistryObject<Item> AZURE_BRICK_SMOOTH = building("azure_brick_smooth", ModBlocks.AZURE_BRICK_SMOOTH);
    public static final RegistryObject<Item> AZURE_BRICK_CARVED = building("azure_brick_carved", ModBlocks.AZURE_BRICK_CARVED);
    public static final RegistryObject<Item> AZURE_BRICK_PILLAR = building("azure_brick_pillar", ModBlocks.AZURE_BRICK_PILLAR);
    public static final RegistryObject<Item> AZURE_BRICK_STAIRS = building("azure_brick_stairs", ModBlocks.AZURE_BRICK_STAIRS);
    public static final RegistryObject<Item> AZURE_BRICK_SLAB = building("azure_brick_slab", ModBlocks.AZURE_BRICK_SLAB);
    public static final RegistryObject<Item> AZURE_BRICK_WALL = building("azure_brick_wall", ModBlocks.AZURE_BRICK_WALL);

    public static final RegistryObject<Item> BLACK_ROCK = building("black_rock", ModBlocks.BLACK_ROCK);
    public static final RegistryObject<Item> BLACK_ROCK_PILLAR = building("black_rock_pillar", ModBlocks.BLACK_ROCK_PILLAR);
    public static final RegistryObject<Item> BLACK_ROCK_STAIRS = building("black_rock_stairs", ModBlocks.BLACK_ROCK_STAIRS);
    public static final RegistryObject<Item> BLACK_ROCK_SLAB = building("black_rock_slab", ModBlocks.BLACK_ROCK_SLAB);
    public static final RegistryObject<Item> BLACK_ROCK_WALL = building("black_rock_wall", ModBlocks.BLACK_ROCK_WALL);

    public static final RegistryObject<Item> CLOUD_JADE = building("cloud_jade", ModBlocks.CLOUD_JADE);
    public static final RegistryObject<Item> CLOUD_JADE_PILLAR = building("cloud_jade_pillar", ModBlocks.CLOUD_JADE_PILLAR);
    public static final RegistryObject<Item> CLOUD_JADE_STAIRS = building("cloud_jade_stairs", ModBlocks.CLOUD_JADE_STAIRS);
    public static final RegistryObject<Item> CLOUD_JADE_SLAB = building("cloud_jade_slab", ModBlocks.CLOUD_JADE_SLAB);
    public static final RegistryObject<Item> CLOUD_JADE_WALL = building("cloud_jade_wall", ModBlocks.CLOUD_JADE_WALL);

    public static final RegistryObject<Item> SPRING_STONE = building("spring_stone", ModBlocks.SPRING_STONE);
    public static final RegistryObject<Item> SPRING_STONE_PILLAR = building("spring_stone_pillar", ModBlocks.SPRING_STONE_PILLAR);
    public static final RegistryObject<Item> SPRING_STONE_STAIRS = building("spring_stone_stairs", ModBlocks.SPRING_STONE_STAIRS);
    public static final RegistryObject<Item> SPRING_STONE_SLAB = building("spring_stone_slab", ModBlocks.SPRING_STONE_SLAB);
    public static final RegistryObject<Item> SPRING_STONE_WALL = building("spring_stone_wall", ModBlocks.SPRING_STONE_WALL);

    public static final RegistryObject<Item> CLOUD_FLOATING_STONE = building("cloud_floating_stone", ModBlocks.CLOUD_FLOATING_STONE);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_PILLAR = building("cloud_floating_stone_pillar", ModBlocks.CLOUD_FLOATING_STONE_PILLAR);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_STAIRS = building("cloud_floating_stone_stairs", ModBlocks.CLOUD_FLOATING_STONE_STAIRS);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_SLAB = building("cloud_floating_stone_slab", ModBlocks.CLOUD_FLOATING_STONE_SLAB);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_WALL = building("cloud_floating_stone_wall", ModBlocks.CLOUD_FLOATING_STONE_WALL);

    public static final RegistryObject<Item> AZURE_BRICK_RAILING = building("azure_brick_railing", ModBlocks.AZURE_BRICK_RAILING);
    public static final RegistryObject<Item> BLACK_ROCK_SMOOTH = building("black_rock_smooth", ModBlocks.BLACK_ROCK_SMOOTH);
    public static final RegistryObject<Item> BLACK_ROCK_CARVED = building("black_rock_carved", ModBlocks.BLACK_ROCK_CARVED);
    public static final RegistryObject<Item> BLACK_ROCK_RAILING = building("black_rock_railing", ModBlocks.BLACK_ROCK_RAILING);
    public static final RegistryObject<Item> CLOUD_JADE_SMOOTH = building("cloud_jade_smooth", ModBlocks.CLOUD_JADE_SMOOTH);
    public static final RegistryObject<Item> CLOUD_JADE_CARVED = building("cloud_jade_carved", ModBlocks.CLOUD_JADE_CARVED);
    public static final RegistryObject<Item> CLOUD_JADE_RAILING = building("cloud_jade_railing", ModBlocks.CLOUD_JADE_RAILING);
    public static final RegistryObject<Item> SPRING_STONE_SMOOTH = building("spring_stone_smooth", ModBlocks.SPRING_STONE_SMOOTH);
    public static final RegistryObject<Item> SPRING_STONE_CARVED = building("spring_stone_carved", ModBlocks.SPRING_STONE_CARVED);
    public static final RegistryObject<Item> SPRING_STONE_RAILING = building("spring_stone_railing", ModBlocks.SPRING_STONE_RAILING);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_SMOOTH = building("cloud_floating_stone_smooth", ModBlocks.CLOUD_FLOATING_STONE_SMOOTH);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_CARVED = building("cloud_floating_stone_carved", ModBlocks.CLOUD_FLOATING_STONE_CARVED);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_RAILING = building("cloud_floating_stone_railing", ModBlocks.CLOUD_FLOATING_STONE_RAILING);

    private static RegistryObject<Item> building(String name, RegistryObject<net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static RegistryObject<Item> ore(String name, RegistryObject<net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
    public static final RegistryObject<Item> AZURE_BRICK_DOOR = ITEMS.register("azure_brick_door", () -> new DoubleHighBlockItem(ModBlocks.AZURE_BRICK_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> AZURE_BRICK_TRAPDOOR = building("azure_brick_trapdoor", ModBlocks.AZURE_BRICK_TRAPDOOR);
    public static final RegistryObject<Item> AZURE_BRICK_WINDOW = building("azure_brick_window", ModBlocks.AZURE_BRICK_WINDOW);
    public static final RegistryObject<Item> BLACK_ROCK_DOOR = ITEMS.register("black_rock_door", () -> new DoubleHighBlockItem(ModBlocks.BLACK_ROCK_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLACK_ROCK_TRAPDOOR = building("black_rock_trapdoor", ModBlocks.BLACK_ROCK_TRAPDOOR);
    public static final RegistryObject<Item> BLACK_ROCK_WINDOW = building("black_rock_window", ModBlocks.BLACK_ROCK_WINDOW);
    public static final RegistryObject<Item> CLOUD_JADE_DOOR = ITEMS.register("cloud_jade_door", () -> new DoubleHighBlockItem(ModBlocks.CLOUD_JADE_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> CLOUD_JADE_TRAPDOOR = building("cloud_jade_trapdoor", ModBlocks.CLOUD_JADE_TRAPDOOR);
    public static final RegistryObject<Item> CLOUD_JADE_WINDOW = building("cloud_jade_window", ModBlocks.CLOUD_JADE_WINDOW);
    public static final RegistryObject<Item> SPRING_STONE_DOOR = ITEMS.register("spring_stone_door", () -> new DoubleHighBlockItem(ModBlocks.SPRING_STONE_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPRING_STONE_TRAPDOOR = building("spring_stone_trapdoor", ModBlocks.SPRING_STONE_TRAPDOOR);
    public static final RegistryObject<Item> SPRING_STONE_WINDOW = building("spring_stone_window", ModBlocks.SPRING_STONE_WINDOW);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_DOOR = ITEMS.register("cloud_floating_stone_door", () -> new DoubleHighBlockItem(ModBlocks.CLOUD_FLOATING_STONE_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_TRAPDOOR = building("cloud_floating_stone_trapdoor", ModBlocks.CLOUD_FLOATING_STONE_TRAPDOOR);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_WINDOW = building("cloud_floating_stone_window", ModBlocks.CLOUD_FLOATING_STONE_WINDOW);

    public static final RegistryObject<Item> AZURE_BRICK_ROOF_TILES = building("azure_brick_roof_tiles", ModBlocks.AZURE_BRICK_ROOF_TILES);
    public static final RegistryObject<Item> AZURE_BRICK_ROOF_STAIRS = building("azure_brick_roof_stairs", ModBlocks.AZURE_BRICK_ROOF_STAIRS);
    public static final RegistryObject<Item> AZURE_BRICK_ROOF_RIDGE = building("azure_brick_roof_ridge", ModBlocks.AZURE_BRICK_ROOF_RIDGE);
    public static final RegistryObject<Item> BLACK_ROCK_ROOF_TILES = building("black_rock_roof_tiles", ModBlocks.BLACK_ROCK_ROOF_TILES);
    public static final RegistryObject<Item> BLACK_ROCK_ROOF_STAIRS = building("black_rock_roof_stairs", ModBlocks.BLACK_ROCK_ROOF_STAIRS);
    public static final RegistryObject<Item> BLACK_ROCK_ROOF_RIDGE = building("black_rock_roof_ridge", ModBlocks.BLACK_ROCK_ROOF_RIDGE);
    public static final RegistryObject<Item> CLOUD_JADE_ROOF_TILES = building("cloud_jade_roof_tiles", ModBlocks.CLOUD_JADE_ROOF_TILES);
    public static final RegistryObject<Item> CLOUD_JADE_ROOF_STAIRS = building("cloud_jade_roof_stairs", ModBlocks.CLOUD_JADE_ROOF_STAIRS);
    public static final RegistryObject<Item> CLOUD_JADE_ROOF_RIDGE = building("cloud_jade_roof_ridge", ModBlocks.CLOUD_JADE_ROOF_RIDGE);
    public static final RegistryObject<Item> SPRING_STONE_ROOF_TILES = building("spring_stone_roof_tiles", ModBlocks.SPRING_STONE_ROOF_TILES);
    public static final RegistryObject<Item> SPRING_STONE_ROOF_STAIRS = building("spring_stone_roof_stairs", ModBlocks.SPRING_STONE_ROOF_STAIRS);
    public static final RegistryObject<Item> SPRING_STONE_ROOF_RIDGE = building("spring_stone_roof_ridge", ModBlocks.SPRING_STONE_ROOF_RIDGE);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_ROOF_TILES = building("cloud_floating_stone_roof_tiles", ModBlocks.CLOUD_FLOATING_STONE_ROOF_TILES);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_ROOF_STAIRS = building("cloud_floating_stone_roof_stairs", ModBlocks.CLOUD_FLOATING_STONE_ROOF_STAIRS);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_ROOF_RIDGE = building("cloud_floating_stone_roof_ridge", ModBlocks.CLOUD_FLOATING_STONE_ROOF_RIDGE);

    public static final RegistryObject<Item> AZURE_BRICK_LAMP_STAND = building("azure_brick_lamp_stand", ModBlocks.AZURE_BRICK_LAMP_STAND);
    public static final RegistryObject<Item> AZURE_BRICK_STONE_LAMP = building("azure_brick_stone_lamp", ModBlocks.AZURE_BRICK_STONE_LAMP);
    public static final RegistryObject<Item> BLACK_ROCK_LAMP_STAND = building("black_rock_lamp_stand", ModBlocks.BLACK_ROCK_LAMP_STAND);
    public static final RegistryObject<Item> BLACK_ROCK_STONE_LAMP = building("black_rock_stone_lamp", ModBlocks.BLACK_ROCK_STONE_LAMP);
    public static final RegistryObject<Item> CLOUD_JADE_LAMP_STAND = building("cloud_jade_lamp_stand", ModBlocks.CLOUD_JADE_LAMP_STAND);
    public static final RegistryObject<Item> CLOUD_JADE_STONE_LAMP = building("cloud_jade_stone_lamp", ModBlocks.CLOUD_JADE_STONE_LAMP);
    public static final RegistryObject<Item> SPRING_STONE_LAMP_STAND = building("spring_stone_lamp_stand", ModBlocks.SPRING_STONE_LAMP_STAND);
    public static final RegistryObject<Item> SPRING_STONE_STONE_LAMP = building("spring_stone_stone_lamp", ModBlocks.SPRING_STONE_STONE_LAMP);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_LAMP_STAND = building("cloud_floating_stone_lamp_stand", ModBlocks.CLOUD_FLOATING_STONE_LAMP_STAND);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_STONE_LAMP = building("cloud_floating_stone_stone_lamp", ModBlocks.CLOUD_FLOATING_STONE_STONE_LAMP);

    public static final RegistryObject<Item> WANBAOGE_PLAQUE = building("wanbaoge_plaque", ModBlocks.WANBAOGE_PLAQUE);
    public static final RegistryObject<Item> FORGING_SHOP_PLAQUE = building("forging_shop_plaque", ModBlocks.FORGING_SHOP_PLAQUE);
    public static final RegistryObject<Item> ALCHEMY_PAVILION_PLAQUE = building("alchemy_pavilion_plaque", ModBlocks.ALCHEMY_PAVILION_PLAQUE);
    public static final RegistryObject<Item> TALISMAN_SHOP_PLAQUE = building("talisman_shop_plaque", ModBlocks.TALISMAN_SHOP_PLAQUE);
    public static final RegistryObject<Item> WOODEN_PLAQUE = building("wooden_plaque", ModBlocks.WOODEN_PLAQUE);
    public static final RegistryObject<Item> WOODEN_PLAQUE_MEDIUM = building("wooden_plaque_medium", ModBlocks.WOODEN_PLAQUE_MEDIUM);
    public static final RegistryObject<Item> WOODEN_PLAQUE_LARGE = building("wooden_plaque_large", ModBlocks.WOODEN_PLAQUE_LARGE);
    public static final RegistryObject<Item> WOODEN_PLAQUE_VERTICAL = building("wooden_plaque_vertical", ModBlocks.WOODEN_PLAQUE_VERTICAL);
    public static final RegistryObject<Item> AZURE_BRICK_PILLAR_BASE = building("azure_brick_pillar_base", ModBlocks.AZURE_BRICK_PILLAR_BASE);
    public static final RegistryObject<Item> AZURE_BRICK_PILLAR_CAPITAL = building("azure_brick_pillar_capital", ModBlocks.AZURE_BRICK_PILLAR_CAPITAL);
    public static final RegistryObject<Item> BLACK_ROCK_PILLAR_BASE = building("black_rock_pillar_base", ModBlocks.BLACK_ROCK_PILLAR_BASE);
    public static final RegistryObject<Item> BLACK_ROCK_PILLAR_CAPITAL = building("black_rock_pillar_capital", ModBlocks.BLACK_ROCK_PILLAR_CAPITAL);
    public static final RegistryObject<Item> CLOUD_JADE_PILLAR_BASE = building("cloud_jade_pillar_base", ModBlocks.CLOUD_JADE_PILLAR_BASE);
    public static final RegistryObject<Item> CLOUD_JADE_PILLAR_CAPITAL = building("cloud_jade_pillar_capital", ModBlocks.CLOUD_JADE_PILLAR_CAPITAL);
    public static final RegistryObject<Item> SPRING_STONE_PILLAR_BASE = building("spring_stone_pillar_base", ModBlocks.SPRING_STONE_PILLAR_BASE);
    public static final RegistryObject<Item> SPRING_STONE_PILLAR_CAPITAL = building("spring_stone_pillar_capital", ModBlocks.SPRING_STONE_PILLAR_CAPITAL);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_PILLAR_BASE = building("cloud_floating_stone_pillar_base", ModBlocks.CLOUD_FLOATING_STONE_PILLAR_BASE);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_PILLAR_CAPITAL = building("cloud_floating_stone_pillar_capital", ModBlocks.CLOUD_FLOATING_STONE_PILLAR_CAPITAL);
    public static final RegistryObject<Item> AZURE_BRICK_BROKEN_PILLAR = building("azure_brick_broken_pillar", ModBlocks.AZURE_BRICK_BROKEN_PILLAR);
    public static final RegistryObject<Item> BLACK_ROCK_BROKEN_PILLAR = building("black_rock_broken_pillar", ModBlocks.BLACK_ROCK_BROKEN_PILLAR);
    public static final RegistryObject<Item> CLOUD_JADE_BROKEN_PILLAR = building("cloud_jade_broken_pillar", ModBlocks.CLOUD_JADE_BROKEN_PILLAR);
    public static final RegistryObject<Item> SPRING_STONE_BROKEN_PILLAR = building("spring_stone_broken_pillar", ModBlocks.SPRING_STONE_BROKEN_PILLAR);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_BROKEN_PILLAR = building("cloud_floating_stone_broken_pillar", ModBlocks.CLOUD_FLOATING_STONE_BROKEN_PILLAR);
    public static final RegistryObject<Item> AZURE_BRICK_RUBBLE = building("azure_brick_rubble", ModBlocks.AZURE_BRICK_RUBBLE);
    public static final RegistryObject<Item> BLACK_ROCK_RUBBLE = building("black_rock_rubble", ModBlocks.BLACK_ROCK_RUBBLE);
    public static final RegistryObject<Item> CLOUD_JADE_RUBBLE = building("cloud_jade_rubble", ModBlocks.CLOUD_JADE_RUBBLE);
    public static final RegistryObject<Item> SPRING_STONE_RUBBLE = building("spring_stone_rubble", ModBlocks.SPRING_STONE_RUBBLE);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_RUBBLE = building("cloud_floating_stone_rubble", ModBlocks.CLOUD_FLOATING_STONE_RUBBLE);

    public static final RegistryObject<Item> AZURE_BRICK_BEAM = building("azure_brick_beam", ModBlocks.AZURE_BRICK_BEAM);
    public static final RegistryObject<Item> AZURE_BRICK_BRACKET = building("azure_brick_bracket", ModBlocks.AZURE_BRICK_BRACKET);
    public static final RegistryObject<Item> BLACK_ROCK_BEAM = building("black_rock_beam", ModBlocks.BLACK_ROCK_BEAM);
    public static final RegistryObject<Item> BLACK_ROCK_BRACKET = building("black_rock_bracket", ModBlocks.BLACK_ROCK_BRACKET);
    public static final RegistryObject<Item> CLOUD_JADE_BEAM = building("cloud_jade_beam", ModBlocks.CLOUD_JADE_BEAM);
    public static final RegistryObject<Item> CLOUD_JADE_BRACKET = building("cloud_jade_bracket", ModBlocks.CLOUD_JADE_BRACKET);
    public static final RegistryObject<Item> SPRING_STONE_BEAM = building("spring_stone_beam", ModBlocks.SPRING_STONE_BEAM);
    public static final RegistryObject<Item> SPRING_STONE_BRACKET = building("spring_stone_bracket", ModBlocks.SPRING_STONE_BRACKET);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_BEAM = building("cloud_floating_stone_beam", ModBlocks.CLOUD_FLOATING_STONE_BEAM);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_BRACKET = building("cloud_floating_stone_bracket", ModBlocks.CLOUD_FLOATING_STONE_BRACKET);

    public static final RegistryObject<Item> AZURE_BRICK_EAVES = building("azure_brick_eaves", ModBlocks.AZURE_BRICK_EAVES);
    public static final RegistryObject<Item> AZURE_BRICK_EAVES_CORNER = building("azure_brick_eaves_corner", ModBlocks.AZURE_BRICK_EAVES_CORNER);
    public static final RegistryObject<Item> BLACK_ROCK_EAVES = building("black_rock_eaves", ModBlocks.BLACK_ROCK_EAVES);
    public static final RegistryObject<Item> BLACK_ROCK_EAVES_CORNER = building("black_rock_eaves_corner", ModBlocks.BLACK_ROCK_EAVES_CORNER);
    public static final RegistryObject<Item> CLOUD_JADE_EAVES = building("cloud_jade_eaves", ModBlocks.CLOUD_JADE_EAVES);
    public static final RegistryObject<Item> CLOUD_JADE_EAVES_CORNER = building("cloud_jade_eaves_corner", ModBlocks.CLOUD_JADE_EAVES_CORNER);
    public static final RegistryObject<Item> SPRING_STONE_EAVES = building("spring_stone_eaves", ModBlocks.SPRING_STONE_EAVES);
    public static final RegistryObject<Item> SPRING_STONE_EAVES_CORNER = building("spring_stone_eaves_corner", ModBlocks.SPRING_STONE_EAVES_CORNER);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_EAVES = building("cloud_floating_stone_eaves", ModBlocks.CLOUD_FLOATING_STONE_EAVES);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_EAVES_CORNER = building("cloud_floating_stone_eaves_corner", ModBlocks.CLOUD_FLOATING_STONE_EAVES_CORNER);

    public static final RegistryObject<Item> AZURE_BRICK_STELE = building("azure_brick_stele", ModBlocks.AZURE_BRICK_STELE);
    public static final RegistryObject<Item> BLACK_ROCK_STELE = building("black_rock_stele", ModBlocks.BLACK_ROCK_STELE);
    public static final RegistryObject<Item> CLOUD_JADE_STELE = building("cloud_jade_stele", ModBlocks.CLOUD_JADE_STELE);
    public static final RegistryObject<Item> SPRING_STONE_STELE = building("spring_stone_stele", ModBlocks.SPRING_STONE_STELE);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_STELE = building("cloud_floating_stone_stele", ModBlocks.CLOUD_FLOATING_STONE_STELE);

    public static final RegistryObject<Item> AZURE_BRICK_CRACKED = building("azure_brick_cracked", ModBlocks.AZURE_BRICK_CRACKED);
    public static final RegistryObject<Item> AZURE_BRICK_CRACKED_STAIRS = building("azure_brick_cracked_stairs", ModBlocks.AZURE_BRICK_CRACKED_STAIRS);
    public static final RegistryObject<Item> AZURE_BRICK_CRACKED_SLAB = building("azure_brick_cracked_slab", ModBlocks.AZURE_BRICK_CRACKED_SLAB);
    public static final RegistryObject<Item> BLACK_ROCK_CRACKED = building("black_rock_cracked", ModBlocks.BLACK_ROCK_CRACKED);
    public static final RegistryObject<Item> BLACK_ROCK_CRACKED_STAIRS = building("black_rock_cracked_stairs", ModBlocks.BLACK_ROCK_CRACKED_STAIRS);
    public static final RegistryObject<Item> BLACK_ROCK_CRACKED_SLAB = building("black_rock_cracked_slab", ModBlocks.BLACK_ROCK_CRACKED_SLAB);
    public static final RegistryObject<Item> CLOUD_JADE_CRACKED = building("cloud_jade_cracked", ModBlocks.CLOUD_JADE_CRACKED);
    public static final RegistryObject<Item> CLOUD_JADE_CRACKED_STAIRS = building("cloud_jade_cracked_stairs", ModBlocks.CLOUD_JADE_CRACKED_STAIRS);
    public static final RegistryObject<Item> CLOUD_JADE_CRACKED_SLAB = building("cloud_jade_cracked_slab", ModBlocks.CLOUD_JADE_CRACKED_SLAB);
    public static final RegistryObject<Item> SPRING_STONE_CRACKED = building("spring_stone_cracked", ModBlocks.SPRING_STONE_CRACKED);
    public static final RegistryObject<Item> SPRING_STONE_CRACKED_STAIRS = building("spring_stone_cracked_stairs", ModBlocks.SPRING_STONE_CRACKED_STAIRS);
    public static final RegistryObject<Item> SPRING_STONE_CRACKED_SLAB = building("spring_stone_cracked_slab", ModBlocks.SPRING_STONE_CRACKED_SLAB);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_CRACKED = building("cloud_floating_stone_cracked", ModBlocks.CLOUD_FLOATING_STONE_CRACKED);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_CRACKED_STAIRS = building("cloud_floating_stone_cracked_stairs", ModBlocks.CLOUD_FLOATING_STONE_CRACKED_STAIRS);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_CRACKED_SLAB = building("cloud_floating_stone_cracked_slab", ModBlocks.CLOUD_FLOATING_STONE_CRACKED_SLAB);

    public static final RegistryObject<Item> AZURE_BRICK_MOSSY = building("azure_brick_mossy", ModBlocks.AZURE_BRICK_MOSSY);
    public static final RegistryObject<Item> AZURE_BRICK_MOSSY_STAIRS = building("azure_brick_mossy_stairs", ModBlocks.AZURE_BRICK_MOSSY_STAIRS);
    public static final RegistryObject<Item> AZURE_BRICK_MOSSY_SLAB = building("azure_brick_mossy_slab", ModBlocks.AZURE_BRICK_MOSSY_SLAB);
    public static final RegistryObject<Item> AZURE_BRICK_MOSSY_WALL = building("azure_brick_mossy_wall", ModBlocks.AZURE_BRICK_MOSSY_WALL);
    public static final RegistryObject<Item> BLACK_ROCK_MOSSY = building("black_rock_mossy", ModBlocks.BLACK_ROCK_MOSSY);
    public static final RegistryObject<Item> BLACK_ROCK_MOSSY_STAIRS = building("black_rock_mossy_stairs", ModBlocks.BLACK_ROCK_MOSSY_STAIRS);
    public static final RegistryObject<Item> BLACK_ROCK_MOSSY_SLAB = building("black_rock_mossy_slab", ModBlocks.BLACK_ROCK_MOSSY_SLAB);
    public static final RegistryObject<Item> BLACK_ROCK_MOSSY_WALL = building("black_rock_mossy_wall", ModBlocks.BLACK_ROCK_MOSSY_WALL);
    public static final RegistryObject<Item> CLOUD_JADE_MOSSY = building("cloud_jade_mossy", ModBlocks.CLOUD_JADE_MOSSY);
    public static final RegistryObject<Item> CLOUD_JADE_MOSSY_STAIRS = building("cloud_jade_mossy_stairs", ModBlocks.CLOUD_JADE_MOSSY_STAIRS);
    public static final RegistryObject<Item> CLOUD_JADE_MOSSY_SLAB = building("cloud_jade_mossy_slab", ModBlocks.CLOUD_JADE_MOSSY_SLAB);
    public static final RegistryObject<Item> CLOUD_JADE_MOSSY_WALL = building("cloud_jade_mossy_wall", ModBlocks.CLOUD_JADE_MOSSY_WALL);
    public static final RegistryObject<Item> SPRING_STONE_MOSSY = building("spring_stone_mossy", ModBlocks.SPRING_STONE_MOSSY);
    public static final RegistryObject<Item> SPRING_STONE_MOSSY_STAIRS = building("spring_stone_mossy_stairs", ModBlocks.SPRING_STONE_MOSSY_STAIRS);
    public static final RegistryObject<Item> SPRING_STONE_MOSSY_SLAB = building("spring_stone_mossy_slab", ModBlocks.SPRING_STONE_MOSSY_SLAB);
    public static final RegistryObject<Item> SPRING_STONE_MOSSY_WALL = building("spring_stone_mossy_wall", ModBlocks.SPRING_STONE_MOSSY_WALL);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_MOSSY = building("cloud_floating_stone_mossy", ModBlocks.CLOUD_FLOATING_STONE_MOSSY);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_MOSSY_STAIRS = building("cloud_floating_stone_mossy_stairs", ModBlocks.CLOUD_FLOATING_STONE_MOSSY_STAIRS);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_MOSSY_SLAB = building("cloud_floating_stone_mossy_slab", ModBlocks.CLOUD_FLOATING_STONE_MOSSY_SLAB);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_MOSSY_WALL = building("cloud_floating_stone_mossy_wall", ModBlocks.CLOUD_FLOATING_STONE_MOSSY_WALL);

    public static final RegistryObject<Item> AZURE_BRICK_SMOOTH_STAIRS = building("azure_brick_smooth_stairs", ModBlocks.AZURE_BRICK_SMOOTH_STAIRS);
    public static final RegistryObject<Item> AZURE_BRICK_SMOOTH_SLAB = building("azure_brick_smooth_slab", ModBlocks.AZURE_BRICK_SMOOTH_SLAB);
    public static final RegistryObject<Item> AZURE_BRICK_CARVED_STAIRS = building("azure_brick_carved_stairs", ModBlocks.AZURE_BRICK_CARVED_STAIRS);
    public static final RegistryObject<Item> AZURE_BRICK_CARVED_SLAB = building("azure_brick_carved_slab", ModBlocks.AZURE_BRICK_CARVED_SLAB);
    public static final RegistryObject<Item> BLACK_ROCK_SMOOTH_STAIRS = building("black_rock_smooth_stairs", ModBlocks.BLACK_ROCK_SMOOTH_STAIRS);
    public static final RegistryObject<Item> BLACK_ROCK_SMOOTH_SLAB = building("black_rock_smooth_slab", ModBlocks.BLACK_ROCK_SMOOTH_SLAB);
    public static final RegistryObject<Item> BLACK_ROCK_CARVED_STAIRS = building("black_rock_carved_stairs", ModBlocks.BLACK_ROCK_CARVED_STAIRS);
    public static final RegistryObject<Item> BLACK_ROCK_CARVED_SLAB = building("black_rock_carved_slab", ModBlocks.BLACK_ROCK_CARVED_SLAB);
    public static final RegistryObject<Item> CLOUD_JADE_SMOOTH_STAIRS = building("cloud_jade_smooth_stairs", ModBlocks.CLOUD_JADE_SMOOTH_STAIRS);
    public static final RegistryObject<Item> CLOUD_JADE_SMOOTH_SLAB = building("cloud_jade_smooth_slab", ModBlocks.CLOUD_JADE_SMOOTH_SLAB);
    public static final RegistryObject<Item> CLOUD_JADE_CARVED_STAIRS = building("cloud_jade_carved_stairs", ModBlocks.CLOUD_JADE_CARVED_STAIRS);
    public static final RegistryObject<Item> CLOUD_JADE_CARVED_SLAB = building("cloud_jade_carved_slab", ModBlocks.CLOUD_JADE_CARVED_SLAB);
    public static final RegistryObject<Item> SPRING_STONE_SMOOTH_STAIRS = building("spring_stone_smooth_stairs", ModBlocks.SPRING_STONE_SMOOTH_STAIRS);
    public static final RegistryObject<Item> SPRING_STONE_SMOOTH_SLAB = building("spring_stone_smooth_slab", ModBlocks.SPRING_STONE_SMOOTH_SLAB);
    public static final RegistryObject<Item> SPRING_STONE_CARVED_STAIRS = building("spring_stone_carved_stairs", ModBlocks.SPRING_STONE_CARVED_STAIRS);
    public static final RegistryObject<Item> SPRING_STONE_CARVED_SLAB = building("spring_stone_carved_slab", ModBlocks.SPRING_STONE_CARVED_SLAB);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_SMOOTH_STAIRS = building("cloud_floating_stone_smooth_stairs", ModBlocks.CLOUD_FLOATING_STONE_SMOOTH_STAIRS);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_SMOOTH_SLAB = building("cloud_floating_stone_smooth_slab", ModBlocks.CLOUD_FLOATING_STONE_SMOOTH_SLAB);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_CARVED_STAIRS = building("cloud_floating_stone_carved_stairs", ModBlocks.CLOUD_FLOATING_STONE_CARVED_STAIRS);
    public static final RegistryObject<Item> CLOUD_FLOATING_STONE_CARVED_SLAB = building("cloud_floating_stone_carved_slab", ModBlocks.CLOUD_FLOATING_STONE_CARVED_SLAB);

    public static final RegistryObject<Item> CLOUD_JADE_TABLE = building("cloud_jade_table", ModBlocks.CLOUD_JADE_TABLE);
    public static final RegistryObject<Item> SPRING_STONE_WATER_CHANNEL = building("spring_stone_water_channel", ModBlocks.SPRING_STONE_WATER_CHANNEL);
    public static final RegistryObject<Item> CLOUD_JADE_CABINET = building("cloud_jade_cabinet", ModBlocks.CLOUD_JADE_CABINET);
    public static final RegistryObject<Item> RUIN_REWARD_CHEST = building("ruin_reward_chest", ModBlocks.RUIN_REWARD_CHEST);
    public static final RegistryObject<Item> BLACK_ROCK_CHEST = building("black_rock_chest", ModBlocks.BLACK_ROCK_CHEST);
    public static final RegistryObject<Item> TOWN_RED_WOOD = building("town_red_wood", ModBlocks.TOWN_RED_WOOD);
    public static final RegistryObject<Item> TOWN_GRAY_BRICK = building("town_gray_brick", ModBlocks.TOWN_GRAY_BRICK);
    public static final RegistryObject<Item> TOWN_PAVING_LAMP = building("town_paving_lamp", ModBlocks.TOWN_PAVING_LAMP);
    public static final RegistryObject<Item> TOWN_CYAN_ROOF = building("town_cyan_roof", ModBlocks.TOWN_CYAN_ROOF);
    public static final RegistryObject<Item> TOWN_CYAN_ROOF_STAIRS = building("town_cyan_roof_stairs", ModBlocks.TOWN_CYAN_ROOF_STAIRS);
    public static final RegistryObject<Item> TOWN_CYAN_ROOF_TILES = building("town_cyan_roof_tiles", ModBlocks.TOWN_CYAN_ROOF_TILES);
    public static final RegistryObject<Item> TOWN_CYAN_EAVES = building("town_cyan_eaves", ModBlocks.TOWN_CYAN_EAVES);
    public static final RegistryObject<Item> TOWN_CYAN_EAVES_CORNER = building("town_cyan_eaves_corner", ModBlocks.TOWN_CYAN_EAVES_CORNER);
    public static final RegistryObject<Item> TOWN_CYAN_ROOF_RIDGE = building("town_cyan_roof_ridge", ModBlocks.TOWN_CYAN_ROOF_RIDGE);
    public static final RegistryObject<Item> TOWN_CARVED_PANEL = building("town_carved_panel", ModBlocks.TOWN_CARVED_PANEL);

    public static final RegistryObject<Item> APPRAISAL = ITEMS.register("appraisal_altar", () -> new BlockItem(ModBlocks.APPRAISAL.get(),new Item.Properties()));
    public static final java.util.Map<String, RegistryObject<Item>> TOWN_MATERIALS = registerTownMaterials();
    private static java.util.Map<String, RegistryObject<Item>> registerTownMaterials() {
        var result = new java.util.LinkedHashMap<String, RegistryObject<Item>>();
        ModBlocks.TOWN_MATERIALS.forEach((id, block) -> result.put(id,
                ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()))));
        return java.util.Collections.unmodifiableMap(result);
    }
    public static final RegistryObject<Item> EQUIPMENT_REPAIR=ITEMS.register("equipment_repair_table",()->new net.minecraft.world.item.BlockItem(ModBlocks.EQUIPMENT_REPAIR.get(),new Item.Properties()));
    private ModBlockItems() {}
}
