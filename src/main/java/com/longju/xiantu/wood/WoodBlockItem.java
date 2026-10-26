package com.longju.xiantu.wood;
import com.longju.xiantu.material.MaterialQuality;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;
import java.util.List;
public final class WoodBlockItem extends BlockItem {
    public final SpiritWoodType type;
    public final boolean material;
    public WoodBlockItem(Block b,SpiritWoodType type,boolean material){super(b,new Item.Properties());this.type=type;this.material=material;}
    /** Processed construction pieces have material names, without cultivation tiers. */
    public boolean isBuildingMaterial(){
        return getBlock() instanceof SpiritPlanksBlock || (!material
                && !(getBlock() instanceof SpiritSaplingBlock)
                && !(getBlock() instanceof net.minecraft.world.level.block.LeavesBlock));
    }
    @Override public Component getName(ItemStack s){return isBuildingMaterial()?super.getName(s):MaterialQuality.name(super.getName(s),type.rank(),s,material);}
    @Override public void appendHoverText(ItemStack s,Level l,List<Component> text,TooltipFlag flag){
        if(material){MaterialQuality.tooltip(s,text);text.add(Component.literal("采伐/加工境界："+com.longju.xiantu.cultivation.RealmRegistry.MAJOR_REALM_NAMES.get(type.realm)).withStyle(net.minecraft.ChatFormatting.GRAY));}
        if(getBlock() instanceof SpiritSaplingBlock){text.add(Component.literal("自然成长："+type.minutes+"分钟（仅加载区块时计时）").withStyle(net.minecraft.ChatFormatting.GRAY));text.add(Component.literal("拆下重种会清空生长进度和催熟投入").withStyle(net.minecraft.ChatFormatting.GRAY));}
    }
    @Override public int getBurnTime(ItemStack stack,net.minecraft.world.item.crafting.RecipeType<?> recipeType){return 0;}
}
