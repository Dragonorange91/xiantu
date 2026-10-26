package com.longju.xiantu.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Element and grade variants share one item id and use NBT-backed model data. */
public final class DemonCoreItem extends Item {
    public static final java.util.List<String> ELEMENTS = java.util.List.of("metal", "wood", "water", "fire", "earth", "thunder");
    public static final int MAX_GRADE = 9;

    public DemonCoreItem(Properties properties) { super(properties); }

    public static int grade(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("core_grade")
                ? Math.max(1, Math.min(MAX_GRADE, stack.getTag().getInt("core_grade"))) : 1;
    }

    public static String element(ItemStack stack) {
        String element = stack.hasTag() ? stack.getTag().getString("core_element") : "";
        return ELEMENTS.contains(element) ? element : "";
    }

    public static int modelData(ItemStack stack) {
        String element = element(stack);
        if (element.isEmpty()) return 0;
        return ELEMENTS.indexOf(element) * MAX_GRADE + grade(stack);
    }

    public static void setData(ItemStack stack, int grade, String element) {
        String safeElement = ELEMENTS.contains(element) ? element : "fire";
        int safeGrade = Math.max(1, Math.min(MAX_GRADE, grade));
        var tag = stack.getOrCreateTag();
        tag.putInt("core_grade", safeGrade);
        tag.putString("core_element", safeElement);
        tag.putInt("CustomModelData", ELEMENTS.indexOf(safeElement) * MAX_GRADE + safeGrade);
    }

    @Override public Component getName(ItemStack stack) {
        String element = element(stack);
        if (element.isEmpty()) return super.getName(stack);
        return Component.translatable("item.xiantu.demon_core.variant", grade(stack),
                Component.translatable("element.xiantu." + element));
    }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!element(stack).isEmpty()) return;
        tooltip.add(Component.translatable("tooltip.xiantu.core.grade", grade(stack)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.xiantu.core.alchemy").withStyle(ChatFormatting.GRAY));
    }
}
