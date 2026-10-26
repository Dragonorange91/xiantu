package com.longju.xiantu.mining;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MetalMaterialRegistry {
    public record Material(String id, String name, int tier, int veinSize, int minY, int maxY, double chancePercent) {}

    public static final List<Material> ALL = List.of(
            new Material("hanxing_iron", "寒星铁", 1, 8, -32, 64, 100),
            new Material("chisui_copper", "赤髓铜", 2, 6, -48, 48, 75),
            new Material("zixiao_copper", "紫霄铜", 3, 5, -64, 32, 40),
            new Material("jiuyou_iron", "九幽寒铁", 4, 4, -64, 16, 20),
            new Material("taiyi_gold", "太乙精金", 5, 3, -64, 0, 8),
            new Material("zifu_silver", "紫府仙银", 6, 3, -64, -24, 3),
            new Material("hunyuan_stone", "混沌元石", 7, 2, -64, -40, 1),
            new Material("taichu_shenjin", "太初神金", 8, 1, -64, -48, 0.5)
    );

    public static final Map<String, RegistryObject<Block>> ORES = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Block>> STORAGE_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> RAW_ORES = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> INGOTS = new LinkedHashMap<>();

    public static void registerBlocks(DeferredRegister<Block> blocks) {
        for (Material material : ALL) {
            Block host = material.tier() <= 2 ? Blocks.STONE : Blocks.DEEPSLATE;
            ORES.put(material.id(), blocks.register(material.id() + "_ore", () -> new Block(
                    BlockBehaviour.Properties.copy(host).strength(3.0F + material.tier() * 0.5F, 6.0F)
                            .requiresCorrectToolForDrops())));
            STORAGE_BLOCKS.put(material.id(), blocks.register(material.id() + "_block", () -> new Block(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F))));
        }
    }

    public static void registerItems(DeferredRegister<Item> items) {
        for (Material material : ALL) {
            RAW_ORES.put(material.id(), items.register(material.id() + "_raw", () -> new Item(new Item.Properties())));
            INGOTS.put(material.id(), items.register(material.id() + "_ingot", () -> new Item(new Item.Properties())));
        }
    }

    public static void registerBlockItems(DeferredRegister<Item> items) {
        for (Material material : ALL) {
            var ore = ORES.get(material.id());
            var storage = STORAGE_BLOCKS.get(material.id());
            items.register(material.id() + "_ore", () -> new net.minecraft.world.item.BlockItem(ore.get(), new Item.Properties()));
            items.register(material.id() + "_block", () -> new net.minecraft.world.item.BlockItem(storage.get(), new Item.Properties()));
        }
    }

    public static Material byOre(Block stateBlock) {
        for (Material material : ALL) {
            if (ORES.get(material.id()).get() == stateBlock) return material;
        }
        return null;
    }

    public static String requiredToolTag(Material material) {
        return switch (material.tier()) {
            case 1 -> "minecraft:needs_stone_tool";
            case 2 -> "minecraft:needs_iron_tool";
            case 3 -> "xiantu:needs_hanxing_tool";
            case 4, 5, 6 -> "xiantu:needs_jiuyou_tool";
            default -> "xiantu:needs_hundun_tool";
        };
    }

    private MetalMaterialRegistry() {}
}
