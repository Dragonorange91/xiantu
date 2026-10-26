package com.longju.xiantu.material;

import net.minecraft.world.item.*;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import java.util.List;
import com.longju.xiantu.wood.*;

/** Zero means unexamined; five revealed grades never change on crafting or placement. */
public final class MaterialQuality {
    public static final String KEY="xiantu_material_quality";
    public static final String[] NAMES={"品质不明","凡品","下品","中品","上品","极品"};
    public static final String[] RANKS={"","一阶","二阶","三阶","四阶","五阶","六阶","七阶","八阶","九阶"};
    public static final TagKey<Item> MATERIALS=TagKey.create(Registries.ITEM,new ResourceLocation("xiantu","identifiable_materials"));
    private MaterialQuality(){}
    public static boolean excluded(ItemStack stack){
        if(stack.getItem() instanceof com.longju.xiantu.item.SpiritStoneItem||stack.getItem() instanceof com.longju.xiantu.item.DemonCoreItem)return true;
        var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if(id==null||!id.getNamespace().equals("xiantu"))return false;
        for(var metal:com.longju.xiantu.mining.MetalMaterialRegistry.ALL)
            for(String suffix:List.of("_raw","_ingot","_ore","_block"))if(id.getPath().equals(metal.id()+suffix))return true;
        return false;
    }
    public static int get(ItemStack stack){return !excluded(stack)&&stack.hasTag()?Math.max(0,Math.min(5,stack.getTag().getInt(KEY))):0;}
    public static ItemStack set(ItemStack stack,int grade){
        int q=excluded(stack)?0:Math.max(0,Math.min(5,grade));
        if(q==0){if(stack.hasTag()){stack.getTag().remove(KEY);if(stack.getTag().isEmpty())stack.setTag(null);}}
        else stack.getOrCreateTag().putInt(KEY,q);
        return stack;
    }
    public static boolean eligible(ItemStack stack){return !stack.isEmpty()&&!excluded(stack)&&stack.is(MATERIALS);}
    public static int rank(ItemStack stack){
        if(stack.getItem() instanceof WoodBlockItem w)return w.type.rank();
        if(stack.getItem() instanceof WoodMaterialItem w)return w.type.rank();
        return 0;
    }
    public static Component name(Component base,int rank,ItemStack stack,boolean material){
        Component result=Component.literal(RANKS[Math.max(0,Math.min(9,rank))]+"·").append(base);
        if(material&&get(stack)>0)result=result.copy().append("·"+NAMES[get(stack)]);
        return result;
    }
    public static void tooltip(ItemStack stack,List<Component> list){
        if(get(stack)==0)list.add(Component.literal("品质不明，需用鉴定灵台探明").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    public static int inherit(CraftingContainer grid){
        int lowest=5;boolean found=false;
        for(int i=0;i<grid.getContainerSize();i++){
            ItemStack s=grid.getItem(i);if(!eligible(s))continue;
            found=true;int q=get(s);if(q==0)return 0;lowest=Math.min(lowest,q);
        }
        return found?lowest:0;
    }
    public static int roll(RandomSource random){
        var weights=com.longju.xiantu.config.XiantuConfig.MATERIAL_QUALITY_WEIGHTS;
        double total=0;for(var weight:weights)total+=weight.get();
        if(total<=0)return 1;
        double pick=random.nextDouble()*total;
        for(int i=0;i<5;i++){pick-=weights.get(i).get();if(pick<0)return i+1;}
        return 5;
    }
    /** Explicit crafting probability hook; unexamined and ordinary material give no bonus. */
    public static double manufacturingBonus(ItemStack s){return !eligible(s)||get(s)<=1?0:(get(s)-1)*1.25;}
}
