package com.longju.xiantu.mining;

import com.longju.xiantu.terrain.NaturalResourceOreBlock;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.*;
import java.util.*;

/** Tiered geological resources, distinct from the ten ungraded workshop additives. */
public final class AdvancedMinerals {
    public static final List<SpecialMineralRegistry.Mineral> NATURAL = List.of(
        new SpecialMineralRegistry.Mineral("hellfire_stone","hellfire_stone_ore","业火石",-48,16,.04,1,2,"lava"),
        new SpecialMineralRegistry.Mineral("reincarnation_crystal","reincarnation_crystal_ore","轮回晶",-64,16,.02,1,2,"stone"),
        new SpecialMineralRegistry.Mineral("tribulation_stone","tribulation_stone_ore","雷劫石",-24,16,.15,3,4,"thunder"),
        new SpecialMineralRegistry.Mineral("causality_stone","causality_stone_ore","因果石",-64,-32,.01,1,2,"stone"));
    public static final SpecialMineralRegistry.Mineral THUNDER_OUTCROP = new SpecialMineralRegistry.Mineral(
        "tribulation_stone","tribulation_stone_outcrop","雷劫石露头",-64,319,.01,1,2,"thunder");
    public static final Map<String,RegistryObject<Block>> ORES = new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Item>> MATERIALS = new LinkedHashMap<>(), ORE_ITEMS = new LinkedHashMap<>();
    public static void registerBlocks(DeferredRegister<Block> blocks) {
        for(var m:NATURAL) ORES.put(m.id(),blocks.register(m.oreId(),()->m.id().equals("causality_stone")?new Block(properties()):new NaturalResourceOreBlock(properties())));
    }
    private static BlockBehaviour.Properties properties(){return BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(3.5F,3F).requiresCorrectToolForDrops();}
    public static void registerItems(DeferredRegister<Item> items){
        for(String id:List.of("hellfire_stone","reincarnation_crystal","tribulation_stone","causality_stone"))
            MATERIALS.put(id,items.register(id,()->new Item(new Item.Properties())));
    }
    public static void registerBlockItems(DeferredRegister<Item> items){
        ORES.forEach((id,b)->ORE_ITEMS.put(id,items.register(id+"_ore",()->new BlockItem(b.get(),new Item.Properties()))));
    }
    private AdvancedMinerals(){}
}
