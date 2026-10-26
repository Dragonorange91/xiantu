package com.longju.xiantu.material;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;
import java.util.List;
public final class MaterialBlockItem extends BlockItem {
    public MaterialBlockItem(Block block,Properties properties){super(block,properties);}
    @Override public Component getName(ItemStack s){return MaterialQuality.name(super.getName(s),MaterialQuality.rank(s),s,true);}
    @Override public void appendHoverText(ItemStack s,Level l,List<Component> text,TooltipFlag flag){super.appendHoverText(s,l,text,flag);MaterialQuality.tooltip(s,text);}
}
