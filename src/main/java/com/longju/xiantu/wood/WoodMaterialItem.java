package com.longju.xiantu.wood;
import com.longju.xiantu.material.MaterialQuality;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import java.util.List;
public class WoodMaterialItem extends Item {
    public final SpiritWoodType type;
    public WoodMaterialItem(SpiritWoodType type){super(new Item.Properties());this.type=type;}
    @Override public Component getName(ItemStack s){return MaterialQuality.name(super.getName(s),type.rank(),s,true);}
    @Override public void appendHoverText(ItemStack s,Level l,List<Component> text,TooltipFlag flag){MaterialQuality.tooltip(s,text);text.add(Component.literal("加工境界："+com.longju.xiantu.cultivation.RealmRegistry.MAJOR_REALM_NAMES.get(type.realm)).withStyle(net.minecraft.ChatFormatting.GRAY));}
    @Override public int getBurnTime(ItemStack stack,net.minecraft.world.item.crafting.RecipeType<?> recipeType){return 0;}
}
