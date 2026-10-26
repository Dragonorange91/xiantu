package com.longju.xiantu.terrain;

import com.longju.xiantu.XiantuMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TerrainPlantRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, XiantuMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, XiantuMod.MOD_ID);
    public static final Map<String, RegistryObject<Block>> PLANTS = new LinkedHashMap<>();

    static {
        register("thunder_sedge", "thunder_rock", 1);
        registerStormBamboo();
        register("meteor_ash_grass", "star_dust_sand", 0);
        register("valley_jade_fern", "valley_fertile_soil", 0);
    }

    private static void register(String id, String soilId, int light) {
        RegistryObject<Block> plant = BLOCKS.register(id, () -> new TerrainPlantBlock(
                BlockBehaviour.Properties.copy(Blocks.GRASS).noCollission().instabreak()
                        .lightLevel(state -> light), soilId));
        PLANTS.put(id, plant);
        ITEMS.register(id, () -> new BlockItem(plant.get(), new Item.Properties()));
    }

    private static void registerStormBamboo() {
        RegistryObject<Block> plant = BLOCKS.register("storm_bamboo", () -> new StormBambooBlock(
                BlockBehaviour.Properties.copy(Blocks.BAMBOO_SAPLING).noCollission().instabreak(), "thunder_rock"));
        PLANTS.put("storm_bamboo", plant);
        ITEMS.register("storm_bamboo", () -> new BlockItem(plant.get(), new Item.Properties()));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    private TerrainPlantRegistry() {}
}
