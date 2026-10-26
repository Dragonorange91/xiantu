package com.longju.xiantu.smelting;

import com.longju.xiantu.mining.MetalMaterialRegistry;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Closed raw-ore catalogue; neither ore blocks nor equipment can be smelted here. */
public final class SmeltingRecipes {
    public static final String KNOWLEDGE="facility/smelting_furnace";
    public record Recipe(String metal,int ticks,int power) {
        public ItemStack output(){return new ItemStack(MetalMaterialRegistry.INGOTS.get(metal).get());}
        public boolean matches(ItemStack s){return s.is(MetalMaterialRegistry.RAW_ORES.get(metal).get());}
    }
    public static final List<Recipe> ALL=List.of(
        new Recipe("zixiao_copper",200,25),new Recipe("jiuyou_iron",300,50),
        new Recipe("taiyi_gold",400,100),new Recipe("zifu_silver",500,200),
        new Recipe("hunyuan_stone",600,400),new Recipe("taichu_shenjin",700,800));
    public static Recipe find(ItemStack s){return ALL.stream().filter(r->r.matches(s)).findFirst().orElse(null);}
    public static Recipe find(String id){return ALL.stream().filter(r->r.metal().equals(id)).findFirst().orElse(null);}
    private SmeltingRecipes(){}
}
