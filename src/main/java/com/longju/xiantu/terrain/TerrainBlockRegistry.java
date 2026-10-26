package com.longju.xiantu.terrain;

import com.longju.xiantu.XiantuMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TerrainBlockRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, XiantuMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, XiantuMod.MOD_ID);
    public static final Map<String, RegistryObject<Block>> BLOCKS_BY_ID = new LinkedHashMap<>();

    static {
        register("thunder_rock", false);
        register("thunder_soil", true);
        registerPillar("lightning_scarred_log");
        register("meteor_rock", false);
        register("meteor_soil", true);
        registerFalling("star_dust_sand");
        register("impact_glass_rock", false);
        registerEmissive("meteor_glow_fissure", 4);
        register("meteorite_stone", false);
        registerFallingOre("ore_xingchen_sand", 1.2F);
        registerNaturalOre("ore_xingyun_sand", 5.0F);
        register("valley_streamstone", false);
        register("valley_fertile_soil", true);
    }

    private static void register(String id, boolean soil) {
        RegistryObject<Block> block = BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.copy(
                soil ? Blocks.DIRT : Blocks.STONE)));
        BLOCKS_BY_ID.put(id, block);
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static void registerPillar(String id) {
        RegistryObject<Block> block = BLOCKS.register(id, () -> new RotatedPillarBlock(
                BlockBehaviour.Properties.copy(Blocks.DARK_OAK_LOG)));
        BLOCKS_BY_ID.put(id, block);
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static void registerFalling(String id) {
        RegistryObject<Block> block = BLOCKS.register(id, () -> new FallingBlock(
                BlockBehaviour.Properties.copy(Blocks.SAND)));
        BLOCKS_BY_ID.put(id, block);
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static void registerFallingOre(String id, float hardness) {
        RegistryObject<Block> block = BLOCKS.register(id, () -> new NaturalFallingResourceOreBlock(
                BlockBehaviour.Properties.copy(Blocks.SAND)
                        .strength(hardness, 3.0F)
                        .requiresCorrectToolForDrops()));
        BLOCKS_BY_ID.put(id, block);
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static void registerNaturalOre(String id, float hardness) {
        RegistryObject<Block> block = BLOCKS.register(id, () -> new NaturalResourceOreBlock(
                BlockBehaviour.Properties.copy(Blocks.STONE)
                        .strength(hardness, 6.0F)
                        .requiresCorrectToolForDrops()));
        BLOCKS_BY_ID.put(id, block);
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static void registerEmissive(String id, int light) {
        RegistryObject<Block> block = BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)
                .lightLevel(state -> light)));
        BLOCKS_BY_ID.put(id, block);
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    private TerrainBlockRegistry() {}
}
