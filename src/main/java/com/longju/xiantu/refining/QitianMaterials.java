package com.longju.xiantu.refining;

import com.longju.xiantu.registry.ModItems;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.*;
import java.util.*;

/** Unique Qitian ingredients; their sources are finite ruin/event transactions. */
public final class QitianMaterials {
    public static final Map<String,RegistryObject<Item>> ITEMS=new LinkedHashMap<>();
    public static RegistryObject<Block> ORE;
    public static void blocks(DeferredRegister<Block> blocks){
        ORE=blocks.register("spirit_gold_essence_ore",()->new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_GOLD_ORE).strength(7,6).requiresCorrectToolForDrops()));
    }
    public static void items(DeferredRegister<Item> items){
        for(String id:List.of("jingu_fragment","spirit_gold_essence","qitian_battle_soul","purple_gold_monkey_hide","nine_heaven_cloud_silk"))
            ITEMS.put(id,items.register(id,()->new Item(new Item.Properties())));
    }
    public static void blockItems(DeferredRegister<Item> items){items.register("spirit_gold_essence_ore",()->new BlockItem(ORE.get(),new Item.Properties()));}
    public static ItemStack stack(String id,int count){return new ItemStack(ITEMS.get(id).get(),count);}
    private QitianMaterials(){}
}
