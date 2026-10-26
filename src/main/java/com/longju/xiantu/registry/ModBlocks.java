package com.longju.xiantu.registry;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.building.RoofRidgeBlock;
import com.longju.xiantu.building.PlaqueBlock;
import com.longju.xiantu.building.PillarTrimBlock;
import com.longju.xiantu.building.StoneBeamBlock;
import com.longju.xiantu.building.ConnectedStoneBeamBlock;
import com.longju.xiantu.building.StonePillarBlock;
import com.longju.xiantu.building.StoneEavesBlock;
import com.longju.xiantu.building.BrokenPillarBlock;
import com.longju.xiantu.building.RuinRubbleBlock;
import com.longju.xiantu.building.StoneLampBlock;
import com.longju.xiantu.building.StoneSteleBlock;
import com.longju.xiantu.building.JadeTableBlock;
import com.longju.xiantu.building.StoneChestBlock;
import com.longju.xiantu.building.JadeCabinetBlock;
import com.longju.xiantu.building.StoneWaterChannelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import com.longju.xiantu.formation.FormationCoreBlock;
import com.longju.xiantu.formation.FormationPartBlock;
import com.longju.xiantu.formation.MeditationCushionBlock;
import com.longju.xiantu.mining.SpiritOreBlock;
import com.longju.xiantu.item.SpiritStoneTier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, XiantuMod.MOD_ID);
    public static final RegistryObject<Block> QUNYOU_FIGURINE = BLOCKS.register("qunyou_figurine", () -> new com.longju.xiantu.building.QunyouFigurineBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(.6F).noOcclusion()));
    public static final RegistryObject<Block> SMELTING_FURNACE = BLOCKS.register("smelting_furnace", () -> new com.longju.xiantu.smelting.SmeltingBlock(BlockBehaviour.Properties.copy(Blocks.FURNACE).lightLevel(state -> 0).strength(3.5f)));
    public static final RegistryObject<Block> FORMATION_CORE = BLOCKS.register("formation_core", () -> new FormationCoreBlock(BlockBehaviour.Properties.copy(Blocks.ENCHANTING_TABLE).strength(3.0f).lightLevel(state -> state.getValue(FormationCoreBlock.ACTIVE) ? 8 : 0)));
    public static final RegistryObject<Block> ARRAY_BASE_1 = BLOCKS.register("array_base_1", () -> new FormationPartBlock(BlockBehaviour.Properties.copy(Blocks.CUT_COPPER).strength(3.0f), FormationPartBlock.Part.BASE));
    public static final RegistryObject<Block> ARRAY_RUNE_1 = BLOCKS.register("array_rune_brick_1", () -> new FormationPartBlock(BlockBehaviour.Properties.copy(Blocks.CUT_COPPER).strength(3.0f).lightLevel(state -> 2), FormationPartBlock.Part.RUNE));
    public static final RegistryObject<Block> ARRAY_PILLAR_1 = BLOCKS.register("array_conduit_pillar_1", () -> new FormationPartBlock(BlockBehaviour.Properties.copy(Blocks.CUT_COPPER).strength(3.0f).noOcclusion(), FormationPartBlock.Part.PILLAR));
    public static final RegistryObject<Block> FORMATION_CORE_2 = BLOCKS.register("array_eye_2", () -> new FormationCoreBlock(BlockBehaviour.Properties.copy(Blocks.ENCHANTING_TABLE).strength(3.0f).lightLevel(state -> state.getValue(FormationCoreBlock.ACTIVE) ? 10 : 0), 2));
    public static final RegistryObject<Block> ARRAY_BASE_2 = BLOCKS.register("array_base_2", () -> new FormationPartBlock(BlockBehaviour.Properties.copy(Blocks.CUT_COPPER).strength(3.5f), FormationPartBlock.Part.BASE, 2));
    public static final RegistryObject<Block> ARRAY_RUNE_2 = BLOCKS.register("array_rune_brick_2", () -> new FormationPartBlock(BlockBehaviour.Properties.copy(Blocks.CUT_COPPER).strength(3.5f).lightLevel(state -> 5), FormationPartBlock.Part.RUNE, 2));
    public static final RegistryObject<Block> ARRAY_PILLAR_2 = BLOCKS.register("array_conduit_pillar_2", () -> new FormationPartBlock(BlockBehaviour.Properties.copy(Blocks.CUT_COPPER).strength(3.5f).noOcclusion(), FormationPartBlock.Part.PILLAR, 2));
    static { com.longju.xiantu.refining.QitianMaterials.blocks(BLOCKS); }
    public static final RegistryObject<Block> FORMATION_CORE_3 = BLOCKS.register("array_eye_3", () -> new FormationCoreBlock(BlockBehaviour.Properties.copy(Blocks.ENCHANTING_TABLE).strength(3.0f).lightLevel(state -> state.getValue(FormationCoreBlock.ACTIVE) ? 12 : 0), 3));
    public static final RegistryObject<Block> ARRAY_BASE_3 = BLOCKS.register("array_base_3", () -> new FormationPartBlock(BlockBehaviour.Properties.copy(Blocks.CUT_COPPER).strength(4.0f), FormationPartBlock.Part.BASE, 3));
    public static final RegistryObject<Block> ARRAY_RUNE_3 = BLOCKS.register("array_rune_brick_3", () -> new FormationPartBlock(BlockBehaviour.Properties.copy(Blocks.CUT_COPPER).strength(4.0f).lightLevel(state -> 8), FormationPartBlock.Part.RUNE, 3));
    public static final RegistryObject<Block> ARRAY_PILLAR_3 = BLOCKS.register("array_conduit_pillar_3", () -> new FormationPartBlock(BlockBehaviour.Properties.copy(Blocks.CUT_COPPER).strength(4.0f).noOcclusion(), FormationPartBlock.Part.PILLAR, 3));
    public static final RegistryObject<Block> MEDITATION_CUSHION = BLOCKS.register("meditation_cushion", () -> new MeditationCushionBlock(BlockBehaviour.Properties.copy(Blocks.WHITE_WOOL).strength(0.5f).noOcclusion()));
    public static final RegistryObject<Block> FAN_LING_SHI_ORE = registerOre(SpiritStoneTier.FAN_LING_SHI, Blocks.STONE, 3.0F);
    public static final RegistryObject<Block> LING_JING_ORE = registerOre(SpiritStoneTier.LING_JING, Blocks.STONE, 3.5F);
    public static final RegistryObject<Block> LING_YU_ORE = registerOre(SpiritStoneTier.LING_YU, Blocks.DEEPSLATE, 4.0F);
    public static final RegistryObject<Block> XUAN_JING_ORE = registerOre(SpiritStoneTier.XUAN_JING, Blocks.DEEPSLATE, 4.5F);
    public static final RegistryObject<Block> DI_SUI_ORE = registerOre(SpiritStoneTier.DI_SUI, Blocks.DEEPSLATE, 5.0F);
    public static final RegistryObject<Block> TIAN_JING_ORE = registerOre(SpiritStoneTier.TIAN_JING, Blocks.DEEPSLATE, 5.5F);
    public static final RegistryObject<Block> XIAN_YU_ORE = registerOre(SpiritStoneTier.XIAN_YU, Blocks.DEEPSLATE, 6.0F);
    public static final RegistryObject<Block> DAO_SHI_ORE = registerOre(SpiritStoneTier.DAO_SHI, Blocks.DEEPSLATE, 7.0F);
    static { com.longju.xiantu.mining.MetalMaterialRegistry.registerBlocks(BLOCKS); com.longju.xiantu.mining.SpecialMineralRegistry.registerBlocks(BLOCKS); com.longju.xiantu.mining.AdvancedMinerals.registerBlocks(BLOCKS); }
    public static final java.util.Map<String, RegistryObject<Block>> METAL_ORES =
            java.util.Collections.unmodifiableMap(com.longju.xiantu.mining.MetalMaterialRegistry.ORES);
    public static final java.util.Map<String, RegistryObject<Block>> METAL_STORAGE_BLOCKS =
            java.util.Collections.unmodifiableMap(com.longju.xiantu.mining.MetalMaterialRegistry.STORAGE_BLOCKS);

    public static final RegistryObject<Block> AZURE_BRICK = registerBuildingBlock("azure_brick", Blocks.STONE_BRICKS, 2.5F);
    public static final java.util.Map<String, RegistryObject<Block>> TOWN_MATERIALS =
            com.longju.xiantu.building.TownMaterialRegistry.register(BLOCKS);
    public static final RegistryObject<Block> TOWN_RED_WOOD = registerBuildingBlock("town_red_wood", Blocks.OAK_PLANKS, 2.0F);
    public static final RegistryObject<Block> TOWN_GRAY_BRICK = registerBuildingBlock("town_gray_brick", Blocks.STONE_BRICKS, 2.0F);
    public static final RegistryObject<Block> TOWN_PAVING_LAMP = BLOCKS.register("town_paving_lamp", () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS).strength(2.5F).requiresCorrectToolForDrops().lightLevel(state -> 15)));
    public static final RegistryObject<Block> TOWN_CYAN_ROOF = registerBuildingBlock("town_cyan_roof", Blocks.PRISMARINE_BRICKS, 2.5F);
    // Shaped town-roof pieces: these keep the cyan roof palette while providing
    // real stepped eaves and a ridge instead of another full cube.
    public static final RegistryObject<Block> TOWN_CYAN_ROOF_STAIRS = BLOCKS.register("town_cyan_roof_stairs", () -> new StairBlock(() -> TOWN_CYAN_ROOF.get().defaultBlockState(), buildingProperties(Blocks.PRISMARINE_BRICKS, 2.5F)));
    public static final RegistryObject<Block> TOWN_CYAN_ROOF_TILES = BLOCKS.register("town_cyan_roof_tiles", () -> new SlabBlock(buildingProperties(Blocks.PRISMARINE_BRICKS, 2.5F)));
    public static final RegistryObject<Block> TOWN_CYAN_EAVES = registerEaves("town_cyan_eaves", 2.5F, false);
    public static final RegistryObject<Block> TOWN_CYAN_EAVES_CORNER = registerEaves("town_cyan_eaves_corner", 2.5F, true);
    public static final RegistryObject<Block> TOWN_CYAN_ROOF_RIDGE = registerRoofRidge("town_cyan_roof_ridge", 2.5F);
    public static final RegistryObject<Block> TOWN_CARVED_PANEL = registerBuildingBlock("town_carved_panel", Blocks.OAK_PLANKS, 2.0F);
    public static final RegistryObject<Block> AZURE_BRICK_SMOOTH = registerBuildingBlock("azure_brick_smooth", Blocks.STONE_BRICKS, 2.5F);
    public static final RegistryObject<Block> AZURE_BRICK_CARVED = registerBuildingBlock("azure_brick_carved", Blocks.STONE_BRICKS, 2.5F);
    public static final RegistryObject<Block> AZURE_BRICK_PILLAR = registerPillar("azure_brick_pillar", Blocks.STONE_BRICKS, 2.5F);
    public static final RegistryObject<Block> AZURE_BRICK_STAIRS = BLOCKS.register("azure_brick_stairs", () -> new StairBlock(() -> AZURE_BRICK.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 2.5F)));
    public static final RegistryObject<Block> AZURE_BRICK_SLAB = BLOCKS.register("azure_brick_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 2.5F)));
    public static final RegistryObject<Block> AZURE_BRICK_WALL = BLOCKS.register("azure_brick_wall", () -> new WallBlock(buildingProperties(Blocks.STONE_BRICKS, 2.5F)));

    public static final RegistryObject<Block> BLACK_ROCK = registerBuildingBlock("black_rock", Blocks.BASALT, 4.0F);
    public static final RegistryObject<Block> BLACK_ROCK_PILLAR = registerPillar("black_rock_pillar", Blocks.BASALT, 4.0F);
    public static final RegistryObject<Block> BLACK_ROCK_STAIRS = BLOCKS.register("black_rock_stairs", () -> new StairBlock(() -> BLACK_ROCK.get().defaultBlockState(), buildingProperties(Blocks.BASALT, 4.0F)));
    public static final RegistryObject<Block> BLACK_ROCK_SLAB = BLOCKS.register("black_rock_slab", () -> new SlabBlock(buildingProperties(Blocks.BASALT, 4.0F)));
    public static final RegistryObject<Block> BLACK_ROCK_WALL = BLOCKS.register("black_rock_wall", () -> new WallBlock(buildingProperties(Blocks.BASALT, 4.0F)));

    public static final RegistryObject<Block> CLOUD_JADE = registerBuildingBlock("cloud_jade", Blocks.CALCITE, 3.0F);
    public static final RegistryObject<Block> CLOUD_JADE_PILLAR = registerPillar("cloud_jade_pillar", Blocks.CALCITE, 3.0F);
    public static final RegistryObject<Block> CLOUD_JADE_STAIRS = BLOCKS.register("cloud_jade_stairs", () -> new StairBlock(() -> CLOUD_JADE.get().defaultBlockState(), buildingProperties(Blocks.CALCITE, 3.0F)));
    public static final RegistryObject<Block> CLOUD_JADE_SLAB = BLOCKS.register("cloud_jade_slab", () -> new SlabBlock(buildingProperties(Blocks.CALCITE, 3.0F)));
    public static final RegistryObject<Block> CLOUD_JADE_WALL = BLOCKS.register("cloud_jade_wall", () -> new WallBlock(buildingProperties(Blocks.CALCITE, 3.0F)));

    public static final RegistryObject<Block> SPRING_STONE = registerBuildingBlock("spring_stone", Blocks.PRISMARINE, 3.0F);
    public static final RegistryObject<Block> SPRING_STONE_PILLAR = registerPillar("spring_stone_pillar", Blocks.PRISMARINE, 3.0F);
    public static final RegistryObject<Block> SPRING_STONE_STAIRS = BLOCKS.register("spring_stone_stairs", () -> new StairBlock(() -> SPRING_STONE.get().defaultBlockState(), buildingProperties(Blocks.PRISMARINE, 3.0F)));
    public static final RegistryObject<Block> SPRING_STONE_SLAB = BLOCKS.register("spring_stone_slab", () -> new SlabBlock(buildingProperties(Blocks.PRISMARINE, 3.0F)));
    public static final RegistryObject<Block> SPRING_STONE_WALL = BLOCKS.register("spring_stone_wall", () -> new WallBlock(buildingProperties(Blocks.PRISMARINE, 3.0F)));

    public static final RegistryObject<Block> CLOUD_FLOATING_STONE = registerBuildingBlock("cloud_floating_stone", Blocks.CALCITE, 3.5F);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_PILLAR = registerPillar("cloud_floating_stone_pillar", Blocks.CALCITE, 3.5F);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_STAIRS = BLOCKS.register("cloud_floating_stone_stairs", () -> new StairBlock(() -> CLOUD_FLOATING_STONE.get().defaultBlockState(), buildingProperties(Blocks.CALCITE, 3.5F)));
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_SLAB = BLOCKS.register("cloud_floating_stone_slab", () -> new SlabBlock(buildingProperties(Blocks.CALCITE, 3.5F)));
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_WALL = BLOCKS.register("cloud_floating_stone_wall", () -> new WallBlock(buildingProperties(Blocks.CALCITE, 3.5F)));

    public static final RegistryObject<Block> AZURE_BRICK_RAILING = registerRailing("azure_brick_railing", Blocks.STONE_BRICKS, 2.5F);
    public static final RegistryObject<Block> BLACK_ROCK_SMOOTH = registerBuildingBlock("black_rock_smooth", Blocks.BASALT, 4.0F);
    public static final RegistryObject<Block> BLACK_ROCK_CARVED = registerBuildingBlock("black_rock_carved", Blocks.BASALT, 4.0F);
    public static final RegistryObject<Block> BLACK_ROCK_RAILING = registerRailing("black_rock_railing", Blocks.BASALT, 4.0F);
    public static final RegistryObject<Block> CLOUD_JADE_SMOOTH = registerBuildingBlock("cloud_jade_smooth", Blocks.CALCITE, 3.0F);
    public static final RegistryObject<Block> CLOUD_JADE_CARVED = registerBuildingBlock("cloud_jade_carved", Blocks.CALCITE, 3.0F);
    public static final RegistryObject<Block> CLOUD_JADE_RAILING = registerRailing("cloud_jade_railing", Blocks.CALCITE, 3.0F);
    public static final RegistryObject<Block> SPRING_STONE_SMOOTH = registerBuildingBlock("spring_stone_smooth", Blocks.PRISMARINE, 3.0F);
    public static final RegistryObject<Block> SPRING_STONE_CARVED = registerBuildingBlock("spring_stone_carved", Blocks.PRISMARINE, 3.0F);
    public static final RegistryObject<Block> SPRING_STONE_RAILING = registerRailing("spring_stone_railing", Blocks.PRISMARINE, 3.0F);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_SMOOTH = registerBuildingBlock("cloud_floating_stone_smooth", Blocks.CALCITE, 3.5F);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_CARVED = registerBuildingBlock("cloud_floating_stone_carved", Blocks.CALCITE, 3.5F);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_RAILING = registerRailing("cloud_floating_stone_railing", Blocks.CALCITE, 3.5F);

    // Hand-operable, non-wooden doors: vanilla redstone and iron-door sounds, stone footsteps.
    private static final BlockSetType STONE_OPENINGS = BlockSetType.register(new BlockSetType(
            "xiantu:stone_openings", true, SoundType.STONE,
            BlockSetType.IRON.doorClose(), BlockSetType.IRON.doorOpen(),
            BlockSetType.IRON.trapdoorClose(), BlockSetType.IRON.trapdoorOpen(),
            BlockSetType.STONE.pressurePlateClickOff(), BlockSetType.STONE.pressurePlateClickOn(),
            BlockSetType.STONE.buttonClickOff(), BlockSetType.STONE.buttonClickOn()));
    public static final RegistryObject<Block> AZURE_BRICK_DOOR = BLOCKS.register("azure_brick_door", () -> new DoorBlock(buildingProperties(Blocks.STONE_BRICKS, 2.5F).noOcclusion(), STONE_OPENINGS));
    public static final RegistryObject<Block> AZURE_BRICK_TRAPDOOR = BLOCKS.register("azure_brick_trapdoor", () -> new TrapDoorBlock(buildingProperties(Blocks.STONE_BRICKS, 2.5F).noOcclusion(), STONE_OPENINGS));
    public static final RegistryObject<Block> AZURE_BRICK_WINDOW = registerWindow("azure_brick_window", 2.5F);
    public static final RegistryObject<Block> BLACK_ROCK_DOOR = BLOCKS.register("black_rock_door", () -> new DoorBlock(buildingProperties(Blocks.BASALT, 4.0F).noOcclusion(), STONE_OPENINGS));
    public static final RegistryObject<Block> BLACK_ROCK_TRAPDOOR = BLOCKS.register("black_rock_trapdoor", () -> new TrapDoorBlock(buildingProperties(Blocks.BASALT, 4.0F).noOcclusion(), STONE_OPENINGS));
    public static final RegistryObject<Block> BLACK_ROCK_WINDOW = registerWindow("black_rock_window", 4.0F);
    public static final RegistryObject<Block> CLOUD_JADE_DOOR = BLOCKS.register("cloud_jade_door", () -> new DoorBlock(buildingProperties(Blocks.CALCITE, 3.0F).noOcclusion(), STONE_OPENINGS));
    public static final RegistryObject<Block> CLOUD_JADE_TRAPDOOR = BLOCKS.register("cloud_jade_trapdoor", () -> new TrapDoorBlock(buildingProperties(Blocks.CALCITE, 3.0F).noOcclusion(), STONE_OPENINGS));
    public static final RegistryObject<Block> CLOUD_JADE_WINDOW = registerWindow("cloud_jade_window", 3.0F);
    public static final RegistryObject<Block> SPRING_STONE_DOOR = BLOCKS.register("spring_stone_door", () -> new DoorBlock(buildingProperties(Blocks.PRISMARINE, 3.0F).noOcclusion(), STONE_OPENINGS));
    public static final RegistryObject<Block> SPRING_STONE_TRAPDOOR = BLOCKS.register("spring_stone_trapdoor", () -> new TrapDoorBlock(buildingProperties(Blocks.PRISMARINE, 3.0F).noOcclusion(), STONE_OPENINGS));
    public static final RegistryObject<Block> SPRING_STONE_WINDOW = registerWindow("spring_stone_window", 3.0F);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_DOOR = BLOCKS.register("cloud_floating_stone_door", () -> new DoorBlock(buildingProperties(Blocks.CALCITE, 3.5F).noOcclusion(), STONE_OPENINGS));
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_TRAPDOOR = BLOCKS.register("cloud_floating_stone_trapdoor", () -> new TrapDoorBlock(buildingProperties(Blocks.CALCITE, 3.5F).noOcclusion(), STONE_OPENINGS));
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_WINDOW = registerWindow("cloud_floating_stone_window", 3.5F);

    public static final RegistryObject<Block> AZURE_BRICK_ROOF_TILES = registerRoofTiles("azure_brick_roof_tiles", 2.5F);
    public static final RegistryObject<Block> AZURE_BRICK_ROOF_STAIRS = registerRoofStairs("azure_brick_roof_stairs", AZURE_BRICK, 2.5F);
    public static final RegistryObject<Block> AZURE_BRICK_ROOF_RIDGE = registerRoofRidge("azure_brick_roof_ridge", 2.5F);
    public static final RegistryObject<Block> BLACK_ROCK_ROOF_TILES = registerRoofTiles("black_rock_roof_tiles", 4.0F);
    public static final RegistryObject<Block> BLACK_ROCK_ROOF_STAIRS = registerRoofStairs("black_rock_roof_stairs", BLACK_ROCK, 4.0F);
    public static final RegistryObject<Block> BLACK_ROCK_ROOF_RIDGE = registerRoofRidge("black_rock_roof_ridge", 4.0F);
    public static final RegistryObject<Block> CLOUD_JADE_ROOF_TILES = registerRoofTiles("cloud_jade_roof_tiles", 3.0F);
    public static final RegistryObject<Block> CLOUD_JADE_ROOF_STAIRS = registerRoofStairs("cloud_jade_roof_stairs", CLOUD_JADE, 3.0F);
    public static final RegistryObject<Block> CLOUD_JADE_ROOF_RIDGE = registerRoofRidge("cloud_jade_roof_ridge", 3.0F);
    public static final RegistryObject<Block> SPRING_STONE_ROOF_TILES = registerRoofTiles("spring_stone_roof_tiles", 3.0F);
    public static final RegistryObject<Block> SPRING_STONE_ROOF_STAIRS = registerRoofStairs("spring_stone_roof_stairs", SPRING_STONE, 3.0F);
    public static final RegistryObject<Block> SPRING_STONE_ROOF_RIDGE = registerRoofRidge("spring_stone_roof_ridge", 3.0F);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_ROOF_TILES = registerRoofTiles("cloud_floating_stone_roof_tiles", 3.5F);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_ROOF_STAIRS = registerRoofStairs("cloud_floating_stone_roof_stairs", CLOUD_FLOATING_STONE, 3.5F);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_ROOF_RIDGE = registerRoofRidge("cloud_floating_stone_roof_ridge", 3.5F);

    public static final RegistryObject<Block> AZURE_BRICK_LAMP_STAND = registerStoneLamp("azure_brick_lamp_stand", 2.5F, false);
    public static final RegistryObject<Block> AZURE_BRICK_STONE_LAMP = registerStoneLamp("azure_brick_stone_lamp", 2.5F, true);
    public static final RegistryObject<Block> BLACK_ROCK_LAMP_STAND = registerStoneLamp("black_rock_lamp_stand", 4.0F, false);
    public static final RegistryObject<Block> BLACK_ROCK_STONE_LAMP = registerStoneLamp("black_rock_stone_lamp", 4.0F, true);
    public static final RegistryObject<Block> CLOUD_JADE_LAMP_STAND = registerStoneLamp("cloud_jade_lamp_stand", 3.0F, false);
    public static final RegistryObject<Block> CLOUD_JADE_STONE_LAMP = registerStoneLamp("cloud_jade_stone_lamp", 3.0F, true);
    public static final RegistryObject<Block> SPRING_STONE_LAMP_STAND = registerStoneLamp("spring_stone_lamp_stand", 3.0F, false);
    public static final RegistryObject<Block> SPRING_STONE_STONE_LAMP = registerStoneLamp("spring_stone_stone_lamp", 3.0F, true);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_LAMP_STAND = registerStoneLamp("cloud_floating_stone_lamp_stand", 3.5F, false);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_STONE_LAMP = registerStoneLamp("cloud_floating_stone_stone_lamp", 3.5F, true);

    public static final RegistryObject<Block> WANBAOGE_PLAQUE = shopPlaque("wanbaoge_plaque");
    public static final RegistryObject<Block> FORGING_SHOP_PLAQUE = shopPlaque("forging_shop_plaque");
    public static final RegistryObject<Block> ALCHEMY_PAVILION_PLAQUE = shopPlaque("alchemy_pavilion_plaque");
    public static final RegistryObject<Block> TALISMAN_SHOP_PLAQUE = shopPlaque("talisman_shop_plaque");

    private static RegistryObject<Block> shopPlaque(String id) {
        return BLOCKS.register(id, () -> new com.longju.xiantu.building.PlaqueBlock.Large(
                BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.0F).noOcclusion()));
    }

    public static final RegistryObject<Block> WOODEN_PLAQUE = BLOCKS.register("wooden_plaque",
            () -> new PlaqueBlock(BlockBehaviour.Properties.copy(Blocks.DARK_OAK_PLANKS).strength(2.0F).noOcclusion()));
    public static final RegistryObject<Block> WOODEN_PLAQUE_MEDIUM = BLOCKS.register("wooden_plaque_medium",
            () -> new PlaqueBlock.Medium(BlockBehaviour.Properties.copy(Blocks.DARK_OAK_PLANKS).strength(2.0F).noOcclusion()));
    public static final RegistryObject<Block> WOODEN_PLAQUE_LARGE = BLOCKS.register("wooden_plaque_large",
            () -> new PlaqueBlock.Large(BlockBehaviour.Properties.copy(Blocks.DARK_OAK_PLANKS).strength(2.0F).noOcclusion()));
    public static final RegistryObject<Block> WOODEN_PLAQUE_VERTICAL = BLOCKS.register("wooden_plaque_vertical",
            () -> new PlaqueBlock.Vertical(BlockBehaviour.Properties.copy(Blocks.DARK_OAK_PLANKS).strength(2.0F).noOcclusion()));

    public static final RegistryObject<Block> AZURE_BRICK_PILLAR_BASE = registerPillarTrim("azure_brick_pillar_base", 2.5F, false);
    public static final RegistryObject<Block> AZURE_BRICK_PILLAR_CAPITAL = registerPillarTrim("azure_brick_pillar_capital", 2.5F, true);
    public static final RegistryObject<Block> BLACK_ROCK_PILLAR_BASE = registerPillarTrim("black_rock_pillar_base", 4.0F, false);
    public static final RegistryObject<Block> BLACK_ROCK_PILLAR_CAPITAL = registerPillarTrim("black_rock_pillar_capital", 4.0F, true);
    public static final RegistryObject<Block> CLOUD_JADE_PILLAR_BASE = registerPillarTrim("cloud_jade_pillar_base", 3.0F, false);
    public static final RegistryObject<Block> CLOUD_JADE_PILLAR_CAPITAL = registerPillarTrim("cloud_jade_pillar_capital", 3.0F, true);
    public static final RegistryObject<Block> SPRING_STONE_PILLAR_BASE = registerPillarTrim("spring_stone_pillar_base", 3.0F, false);
    public static final RegistryObject<Block> SPRING_STONE_PILLAR_CAPITAL = registerPillarTrim("spring_stone_pillar_capital", 3.0F, true);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_PILLAR_BASE = registerPillarTrim("cloud_floating_stone_pillar_base", 3.5F, false);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_PILLAR_CAPITAL = registerPillarTrim("cloud_floating_stone_pillar_capital", 3.5F, true);

    public static final RegistryObject<Block> AZURE_BRICK_BEAM = registerStoneBeam("azure_brick_beam", 2.5F, false);
    public static final RegistryObject<Block> AZURE_BRICK_BRACKET = registerStoneBeam("azure_brick_bracket", 2.5F, true);
    public static final RegistryObject<Block> BLACK_ROCK_BEAM = registerStoneBeam("black_rock_beam", 4.0F, false);
    public static final RegistryObject<Block> BLACK_ROCK_BRACKET = registerStoneBeam("black_rock_bracket", 4.0F, true);
    public static final RegistryObject<Block> CLOUD_JADE_BEAM = registerStoneBeam("cloud_jade_beam", 3.0F, false);
    public static final RegistryObject<Block> CLOUD_JADE_BRACKET = registerStoneBeam("cloud_jade_bracket", 3.0F, true);
    public static final RegistryObject<Block> SPRING_STONE_BEAM = registerStoneBeam("spring_stone_beam", 3.0F, false);
    public static final RegistryObject<Block> SPRING_STONE_BRACKET = registerStoneBeam("spring_stone_bracket", 3.0F, true);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_BEAM = registerStoneBeam("cloud_floating_stone_beam", 3.5F, false);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_BRACKET = registerStoneBeam("cloud_floating_stone_bracket", 3.5F, true);

    public static final RegistryObject<Block> AZURE_BRICK_EAVES = registerEaves("azure_brick_eaves", 2.5F, false);
    public static final RegistryObject<Block> AZURE_BRICK_EAVES_CORNER = registerEaves("azure_brick_eaves_corner", 2.5F, true);
    public static final RegistryObject<Block> BLACK_ROCK_EAVES = registerEaves("black_rock_eaves", 4.0F, false);
    public static final RegistryObject<Block> BLACK_ROCK_EAVES_CORNER = registerEaves("black_rock_eaves_corner", 4.0F, true);
    public static final RegistryObject<Block> CLOUD_JADE_EAVES = registerEaves("cloud_jade_eaves", 3.0F, false);
    public static final RegistryObject<Block> CLOUD_JADE_EAVES_CORNER = registerEaves("cloud_jade_eaves_corner", 3.0F, true);
    public static final RegistryObject<Block> SPRING_STONE_EAVES = registerEaves("spring_stone_eaves", 3.0F, false);
    public static final RegistryObject<Block> SPRING_STONE_EAVES_CORNER = registerEaves("spring_stone_eaves_corner", 3.0F, true);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_EAVES = registerEaves("cloud_floating_stone_eaves", 3.5F, false);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_EAVES_CORNER = registerEaves("cloud_floating_stone_eaves_corner", 3.5F, true);

    public static final RegistryObject<Block> AZURE_BRICK_STELE = BLOCKS.register("azure_brick_stele", () -> new StoneSteleBlock(buildingProperties(Blocks.STONE_BRICKS, 2.5F).noOcclusion()));
    public static final RegistryObject<Block> BLACK_ROCK_STELE = BLOCKS.register("black_rock_stele", () -> new StoneSteleBlock(buildingProperties(Blocks.STONE_BRICKS, 4.0F).noOcclusion()));
    public static final RegistryObject<Block> CLOUD_JADE_STELE = BLOCKS.register("cloud_jade_stele", () -> new StoneSteleBlock(buildingProperties(Blocks.STONE_BRICKS, 3.0F).noOcclusion()));
    public static final RegistryObject<Block> SPRING_STONE_STELE = BLOCKS.register("spring_stone_stele", () -> new StoneSteleBlock(buildingProperties(Blocks.STONE_BRICKS, 3.0F).noOcclusion()));
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_STELE = BLOCKS.register("cloud_floating_stone_stele", () -> new StoneSteleBlock(buildingProperties(Blocks.STONE_BRICKS, 3.5F).noOcclusion()));

    public static final RegistryObject<Block> AZURE_BRICK_CRACKED = registerBuildingBlock("azure_brick_cracked", Blocks.STONE_BRICKS, 2.5F);
    public static final RegistryObject<Block> AZURE_BRICK_CRACKED_STAIRS = BLOCKS.register("azure_brick_cracked_stairs", () -> new StairBlock(() -> AZURE_BRICK_CRACKED.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 2.5F)));
    public static final RegistryObject<Block> AZURE_BRICK_CRACKED_SLAB = BLOCKS.register("azure_brick_cracked_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 2.5F)));
    public static final RegistryObject<Block> BLACK_ROCK_CRACKED = registerBuildingBlock("black_rock_cracked", Blocks.BASALT, 4.0F);
    public static final RegistryObject<Block> BLACK_ROCK_CRACKED_STAIRS = BLOCKS.register("black_rock_cracked_stairs", () -> new StairBlock(() -> BLACK_ROCK_CRACKED.get().defaultBlockState(), buildingProperties(Blocks.BASALT, 4.0F)));
    public static final RegistryObject<Block> BLACK_ROCK_CRACKED_SLAB = BLOCKS.register("black_rock_cracked_slab", () -> new SlabBlock(buildingProperties(Blocks.BASALT, 4.0F)));
    public static final RegistryObject<Block> CLOUD_JADE_CRACKED = registerBuildingBlock("cloud_jade_cracked", Blocks.STONE_BRICKS, 3.0F);
    public static final RegistryObject<Block> CLOUD_JADE_CRACKED_STAIRS = BLOCKS.register("cloud_jade_cracked_stairs", () -> new StairBlock(() -> CLOUD_JADE_CRACKED.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> CLOUD_JADE_CRACKED_SLAB = BLOCKS.register("cloud_jade_cracked_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> SPRING_STONE_CRACKED = registerBuildingBlock("spring_stone_cracked", Blocks.STONE_BRICKS, 3.0F);
    public static final RegistryObject<Block> SPRING_STONE_CRACKED_STAIRS = BLOCKS.register("spring_stone_cracked_stairs", () -> new StairBlock(() -> SPRING_STONE_CRACKED.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> SPRING_STONE_CRACKED_SLAB = BLOCKS.register("spring_stone_cracked_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_CRACKED = registerBuildingBlock("cloud_floating_stone_cracked", Blocks.STONE_BRICKS, 3.5F);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_CRACKED_STAIRS = BLOCKS.register("cloud_floating_stone_cracked_stairs", () -> new StairBlock(() -> CLOUD_FLOATING_STONE_CRACKED.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 3.5F)));
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_CRACKED_SLAB = BLOCKS.register("cloud_floating_stone_cracked_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 3.5F)));

    public static final RegistryObject<Block> AZURE_BRICK_MOSSY = registerBuildingBlock("azure_brick_mossy", Blocks.STONE_BRICKS, 2.5F);
    public static final RegistryObject<Block> AZURE_BRICK_MOSSY_STAIRS = BLOCKS.register("azure_brick_mossy_stairs", () -> new StairBlock(() -> AZURE_BRICK_MOSSY.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 2.5F)));
    public static final RegistryObject<Block> AZURE_BRICK_MOSSY_SLAB = BLOCKS.register("azure_brick_mossy_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 2.5F)));
    public static final RegistryObject<Block> AZURE_BRICK_MOSSY_WALL = BLOCKS.register("azure_brick_mossy_wall", () -> new WallBlock(buildingProperties(Blocks.STONE_BRICKS, 2.5F)));
    public static final RegistryObject<Block> BLACK_ROCK_MOSSY = registerBuildingBlock("black_rock_mossy", Blocks.BASALT, 4.0F);
    public static final RegistryObject<Block> BLACK_ROCK_MOSSY_STAIRS = BLOCKS.register("black_rock_mossy_stairs", () -> new StairBlock(() -> BLACK_ROCK_MOSSY.get().defaultBlockState(), buildingProperties(Blocks.BASALT, 4.0F)));
    public static final RegistryObject<Block> BLACK_ROCK_MOSSY_SLAB = BLOCKS.register("black_rock_mossy_slab", () -> new SlabBlock(buildingProperties(Blocks.BASALT, 4.0F)));
    public static final RegistryObject<Block> BLACK_ROCK_MOSSY_WALL = BLOCKS.register("black_rock_mossy_wall", () -> new WallBlock(buildingProperties(Blocks.BASALT, 4.0F)));
    public static final RegistryObject<Block> CLOUD_JADE_MOSSY = registerBuildingBlock("cloud_jade_mossy", Blocks.STONE_BRICKS, 3.0F);
    public static final RegistryObject<Block> CLOUD_JADE_MOSSY_STAIRS = BLOCKS.register("cloud_jade_mossy_stairs", () -> new StairBlock(() -> CLOUD_JADE_MOSSY.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> CLOUD_JADE_MOSSY_SLAB = BLOCKS.register("cloud_jade_mossy_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> CLOUD_JADE_MOSSY_WALL = BLOCKS.register("cloud_jade_mossy_wall", () -> new WallBlock(buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> SPRING_STONE_MOSSY = registerBuildingBlock("spring_stone_mossy", Blocks.STONE_BRICKS, 3.0F);
    public static final RegistryObject<Block> SPRING_STONE_MOSSY_STAIRS = BLOCKS.register("spring_stone_mossy_stairs", () -> new StairBlock(() -> SPRING_STONE_MOSSY.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> SPRING_STONE_MOSSY_SLAB = BLOCKS.register("spring_stone_mossy_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> SPRING_STONE_MOSSY_WALL = BLOCKS.register("spring_stone_mossy_wall", () -> new WallBlock(buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_MOSSY = registerBuildingBlock("cloud_floating_stone_mossy", Blocks.STONE_BRICKS, 3.5F);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_MOSSY_STAIRS = BLOCKS.register("cloud_floating_stone_mossy_stairs", () -> new StairBlock(() -> CLOUD_FLOATING_STONE_MOSSY.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 3.5F)));
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_MOSSY_SLAB = BLOCKS.register("cloud_floating_stone_mossy_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 3.5F)));
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_MOSSY_WALL = BLOCKS.register("cloud_floating_stone_mossy_wall", () -> new WallBlock(buildingProperties(Blocks.STONE_BRICKS, 3.5F)));

    public static final RegistryObject<Block> AZURE_BRICK_SMOOTH_STAIRS = BLOCKS.register("azure_brick_smooth_stairs", () -> new StairBlock(() -> AZURE_BRICK_SMOOTH.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 2.5F)));
    public static final RegistryObject<Block> AZURE_BRICK_SMOOTH_SLAB = BLOCKS.register("azure_brick_smooth_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 2.5F)));
    public static final RegistryObject<Block> AZURE_BRICK_CARVED_STAIRS = BLOCKS.register("azure_brick_carved_stairs", () -> new StairBlock(() -> AZURE_BRICK_CARVED.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 2.5F)));
    public static final RegistryObject<Block> AZURE_BRICK_CARVED_SLAB = BLOCKS.register("azure_brick_carved_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 2.5F)));
    public static final RegistryObject<Block> BLACK_ROCK_SMOOTH_STAIRS = BLOCKS.register("black_rock_smooth_stairs", () -> new StairBlock(() -> BLACK_ROCK_SMOOTH.get().defaultBlockState(), buildingProperties(Blocks.BASALT, 4.0F)));
    public static final RegistryObject<Block> BLACK_ROCK_SMOOTH_SLAB = BLOCKS.register("black_rock_smooth_slab", () -> new SlabBlock(buildingProperties(Blocks.BASALT, 4.0F)));
    public static final RegistryObject<Block> BLACK_ROCK_CARVED_STAIRS = BLOCKS.register("black_rock_carved_stairs", () -> new StairBlock(() -> BLACK_ROCK_CARVED.get().defaultBlockState(), buildingProperties(Blocks.BASALT, 4.0F)));
    public static final RegistryObject<Block> BLACK_ROCK_CARVED_SLAB = BLOCKS.register("black_rock_carved_slab", () -> new SlabBlock(buildingProperties(Blocks.BASALT, 4.0F)));
    public static final RegistryObject<Block> CLOUD_JADE_SMOOTH_STAIRS = BLOCKS.register("cloud_jade_smooth_stairs", () -> new StairBlock(() -> CLOUD_JADE_SMOOTH.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> CLOUD_JADE_SMOOTH_SLAB = BLOCKS.register("cloud_jade_smooth_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> CLOUD_JADE_CARVED_STAIRS = BLOCKS.register("cloud_jade_carved_stairs", () -> new StairBlock(() -> CLOUD_JADE_CARVED.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> CLOUD_JADE_CARVED_SLAB = BLOCKS.register("cloud_jade_carved_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> SPRING_STONE_SMOOTH_STAIRS = BLOCKS.register("spring_stone_smooth_stairs", () -> new StairBlock(() -> SPRING_STONE_SMOOTH.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> SPRING_STONE_SMOOTH_SLAB = BLOCKS.register("spring_stone_smooth_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> SPRING_STONE_CARVED_STAIRS = BLOCKS.register("spring_stone_carved_stairs", () -> new StairBlock(() -> SPRING_STONE_CARVED.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> SPRING_STONE_CARVED_SLAB = BLOCKS.register("spring_stone_carved_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 3.0F)));
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_SMOOTH_STAIRS = BLOCKS.register("cloud_floating_stone_smooth_stairs", () -> new StairBlock(() -> CLOUD_FLOATING_STONE_SMOOTH.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 3.5F)));
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_SMOOTH_SLAB = BLOCKS.register("cloud_floating_stone_smooth_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 3.5F)));
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_CARVED_STAIRS = BLOCKS.register("cloud_floating_stone_carved_stairs", () -> new StairBlock(() -> CLOUD_FLOATING_STONE_CARVED.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, 3.5F)));
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_CARVED_SLAB = BLOCKS.register("cloud_floating_stone_carved_slab", () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, 3.5F)));

    public static final RegistryObject<Block> CLOUD_JADE_TABLE = BLOCKS.register("cloud_jade_table", () -> new JadeTableBlock(buildingProperties(Blocks.STONE_BRICKS, 3.0F).noOcclusion()));
    public static final RegistryObject<Block> SPRING_STONE_WATER_CHANNEL = BLOCKS.register("spring_stone_water_channel", () -> new StoneWaterChannelBlock(buildingProperties(Blocks.STONE_BRICKS, 3.0F).noOcclusion()));
    public static final RegistryObject<Block> CLOUD_JADE_CABINET = BLOCKS.register("cloud_jade_cabinet", () -> new JadeCabinetBlock(buildingProperties(Blocks.STONE_BRICKS, 3.0F).noOcclusion()));
    public static final RegistryObject<Block> RUIN_ENCOUNTER = BLOCKS.register("ruin_encounter_seal", com.longju.xiantu.worldgen.RuinEncounterBlock::new);
    public static final RegistryObject<Block> RUIN_REWARD_CHEST = BLOCKS.register("ruin_reward_chest", () -> new com.longju.xiantu.building.RewardChestBlock(buildingProperties(Blocks.BASALT, 4.0F).noOcclusion()));
    public static final RegistryObject<Block> BLACK_ROCK_CHEST = BLOCKS.register("black_rock_chest", () -> new StoneChestBlock(buildingProperties(Blocks.BASALT, 4.0F).noOcclusion()));

    private static RegistryObject<Block> registerEaves(String name, float strength, boolean corner) {
        return BLOCKS.register(name, () -> new StoneEavesBlock(buildingProperties(Blocks.STONE_BRICKS, strength).noOcclusion(), corner));
    }

    private static RegistryObject<Block> registerStoneBeam(String name, float strength, boolean bracket) {
        return BLOCKS.register(name, () -> bracket
                ? new StoneBeamBlock(buildingProperties(Blocks.STONE_BRICKS, strength).noOcclusion(), true)
                : new ConnectedStoneBeamBlock(buildingProperties(Blocks.STONE_BRICKS, strength).noOcclusion()));
    }

    private static RegistryObject<Block> registerPillarTrim(String name, float strength, boolean capital) {
        return BLOCKS.register(name, () -> new PillarTrimBlock(buildingProperties(Blocks.STONE_BRICKS, strength).noOcclusion(), capital));
    }

    public static final RegistryObject<Block> AZURE_BRICK_BROKEN_PILLAR = registerBrokenPillar("azure_brick_broken_pillar", 2.5F);
    public static final RegistryObject<Block> BLACK_ROCK_BROKEN_PILLAR = registerBrokenPillar("black_rock_broken_pillar", 4.0F);
    public static final RegistryObject<Block> CLOUD_JADE_BROKEN_PILLAR = registerBrokenPillar("cloud_jade_broken_pillar", 3.0F);
    public static final RegistryObject<Block> SPRING_STONE_BROKEN_PILLAR = registerBrokenPillar("spring_stone_broken_pillar", 3.0F);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_BROKEN_PILLAR = registerBrokenPillar("cloud_floating_stone_broken_pillar", 3.5F);

    public static final RegistryObject<Block> AZURE_BRICK_RUBBLE = registerRubble("azure_brick_rubble", 2.5F);
    public static final RegistryObject<Block> BLACK_ROCK_RUBBLE = registerRubble("black_rock_rubble", 4.0F);
    public static final RegistryObject<Block> CLOUD_JADE_RUBBLE = registerRubble("cloud_jade_rubble", 3.0F);
    public static final RegistryObject<Block> SPRING_STONE_RUBBLE = registerRubble("spring_stone_rubble", 3.0F);
    public static final RegistryObject<Block> CLOUD_FLOATING_STONE_RUBBLE = registerRubble("cloud_floating_stone_rubble", 3.5F);

    private static RegistryObject<Block> registerRubble(String name, float strength) {
        return BLOCKS.register(name, () -> new RuinRubbleBlock(buildingProperties(Blocks.STONE_BRICKS, strength).noOcclusion()));
    }

    private static RegistryObject<Block> registerBrokenPillar(String name, float strength) {
        return BLOCKS.register(name, () -> new BrokenPillarBlock(buildingProperties(Blocks.STONE_BRICKS, strength).noOcclusion()));
    }

    private static RegistryObject<Block> registerStoneLamp(String name, float strength, boolean lamp) {
        return BLOCKS.register(name, () -> new StoneLampBlock(buildingProperties(Blocks.STONE_BRICKS, strength)
                .noOcclusion().lightLevel(state -> lamp ? 15 : 0), lamp));
    }

    private static RegistryObject<Block> registerRoofTiles(String name, float strength) {
        return BLOCKS.register(name, () -> new SlabBlock(buildingProperties(Blocks.STONE_BRICKS, strength)));
    }

    private static RegistryObject<Block> registerRoofStairs(String name, RegistryObject<Block> base, float strength) {
        return BLOCKS.register(name, () -> new StairBlock(() -> base.get().defaultBlockState(), buildingProperties(Blocks.STONE_BRICKS, strength)));
    }

    private static RegistryObject<Block> registerRoofRidge(String name, float strength) {
        return BLOCKS.register(name, () -> new RoofRidgeBlock(buildingProperties(Blocks.STONE_BRICKS, strength).noOcclusion()));
    }

    private static RegistryObject<Block> registerWindow(String name, float strength) {
        return BLOCKS.register(name, () -> new IronBarsBlock(
                buildingProperties(Blocks.GLASS_PANE, strength).sound(SoundType.STONE).noOcclusion()));
    }

    private static RegistryObject<Block> registerRailing(String name, Block material, float strength) {
        return BLOCKS.register(name, () -> new FenceBlock(buildingProperties(material, strength).noOcclusion()));
    }

    private static BlockBehaviour.Properties buildingProperties(Block material, float strength) {
        return BlockBehaviour.Properties.copy(material).strength(strength, 6.0F);
    }

    private static RegistryObject<Block> registerBuildingBlock(String name, Block material, float strength) {
        return BLOCKS.register(name, () -> new Block(buildingProperties(material, strength)));
    }

    private static RegistryObject<Block> registerPillar(String name, Block material, float strength) {
        return BLOCKS.register(name, () -> new StonePillarBlock(buildingProperties(material, strength).noOcclusion()));
    }

    private static RegistryObject<Block> registerOre(SpiritStoneTier tier, Block host, float strength) {
        return BLOCKS.register(tier.registryName() + "_ore", () -> new SpiritOreBlock(
                BlockBehaviour.Properties.copy(host).strength(strength, 6.0F), tier));
    }
    public static final RegistryObject<net.minecraft.world.level.block.Block> APPRAISAL = BLOCKS.register("appraisal_altar", () -> new com.longju.xiantu.material.AppraisalBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.STONE).strength(3).noOcclusion()));
    public static final RegistryObject<Block> EQUIPMENT_REPAIR=BLOCKS.register("equipment_repair_table",com.longju.xiantu.refining.EquipmentRepairBlock::new);
    private ModBlocks() {}
}
