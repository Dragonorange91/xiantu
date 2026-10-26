package com.longju.xiantu.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

/** Three strength bands, independent from appraisal and the old numbered material grades. */
public final class BeastHideItem extends Item {
    public BeastHideItem(Properties properties) { super(properties); }
    public static String tierForRealm(int realm) {
        return realm<0||realm>8 ? "" : realm<=1 ? "basic" : realm<=3 ? "intermediate" : "advanced";
    }
    public static String tier(ItemStack stack) {
        String value=stack.hasTag()?stack.getTag().getString("hide_tier"):"";
        return List.of("basic","intermediate","advanced").contains(value)?value:"";
    }
    public static ItemStack fromRealm(int realm,String element) {
        String tier=tierForRealm(realm);
        if(tier.isEmpty()||!DemonCoreItem.ELEMENTS.contains(element)) return ItemStack.EMPTY;
        var stack=new ItemStack(com.longju.xiantu.registry.ModItems.BEAST_HIDE.get());
        stack.getOrCreateTag().putString("hide_tier",tier);
        stack.getTag().putString("beast_element",element);
        return stack;
    }
    @Override public Component getName(ItemStack stack) {
        String tier=tier(stack);
        return tier.isEmpty()?super.getName(stack):Component.translatable("item.xiantu.beast_hide."+tier);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        String element=BeastMaterialItem.element(stack);
        if(!element.isEmpty()) lines.add(Component.translatable("tooltip.xiantu.beast_material.element",
                Component.translatable("element.xiantu."+element)));
    }
}
