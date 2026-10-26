package com.longju.xiantu.medicine;

import com.longju.xiantu.XiantuMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MedicineRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, XiantuMod.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, XiantuMod.MOD_ID);
    public static final Map<String, RegistryObject<Item>> HERB_ITEMS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> SEED_ITEMS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Block>> WILD_PLANTS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Block>> CROPS = new LinkedHashMap<>();
    public static final List<RegistryObject<Block>> SOILS = new java.util.ArrayList<>();
    public static final List<RegistryObject<Item>> SOIL_ITEMS = new java.util.ArrayList<>();
    public static final List<RegistryObject<Block>> FARMLAND_BLOCKS = new java.util.ArrayList<>();
    public static final List<RegistryObject<Item>> FARMLAND_ITEMS = new java.util.ArrayList<>();

    static {
        for (MedicineDefinition definition : MedicineCatalog.HERBS) {
            HERB_ITEMS.put(definition.registryId(), ITEMS.register(definition.registryId(),
                    () -> new Item(new Item.Properties().stacksTo(64))));
            if (definition.wildPlant()) {
                WILD_PLANTS.put(definition.registryId(), BLOCKS.register(definition.registryId() + "_wild",
                        () -> new MedicinePlantBlock(BlockBehaviour.Properties.copy(Blocks.GRASS)
                                .offsetType(BlockBehaviour.OffsetType.NONE).noCollission().instabreak(), definition)));
            }
            if (definition.plantable()) {
                CROPS.put(definition.registryId(), BLOCKS.register(definition.registryId() + "_crop",
                        () -> new MedicineCropBlock(BlockBehaviour.Properties.copy(Blocks.WHEAT)
                                .noCollission().randomTicks().instabreak(), definition)));
                SEED_ITEMS.put(definition.registryId(), ITEMS.register(definition.registryId() + "_seed",
                        () -> new MedicineSeedItem(new Item.Properties().stacksTo(64), definition)));
            }
        }

        for (int tier = 0; tier <= 6; tier++) {
            final int soilTier = tier;
            RegistryObject<Block> soil = BLOCKS.register("spirit_soil_" + tier,
                    () -> new MedicineSoilBlock(BlockBehaviour.Properties.copy(Blocks.DIRT)
                            .strength(0.8F).sound(SoundType.GRAVEL), soilTier));
            SOILS.add(soil);
            SOIL_ITEMS.add(ITEMS.register("spirit_soil_" + tier,
                    () -> new BlockItem(soil.get(), new Item.Properties())));

            RegistryObject<Block> farmland = BLOCKS.register("spirit_farmland_" + tier,
                    () -> new MedicineFarmlandBlock(BlockBehaviour.Properties.copy(Blocks.FARMLAND)
                            .strength(0.6F).sound(SoundType.GRAVEL).noOcclusion(), soilTier));
            FARMLAND_BLOCKS.add(farmland);
            FARMLAND_ITEMS.add(ITEMS.register("spirit_farmland_" + tier,
                    () -> new BlockItem(farmland.get(), new Item.Properties())));
        }
    }

    public static Item herb(String registryId) { return HERB_ITEMS.get(registryId).get(); }
    public static Item seed(String registryId) { return SEED_ITEMS.get(registryId).get(); }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    private MedicineRegistry() {}
}
