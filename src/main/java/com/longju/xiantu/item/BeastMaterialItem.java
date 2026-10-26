package com.longju.xiantu.item;

import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Shared tier and five-element metadata for materials obtained from beasts. */
public final class BeastMaterialItem extends Item {
    public static final int MAX_GRADE = 9;
    private static final List<String> ELEMENTS = List.of("metal", "wood", "water", "fire", "earth", "thunder");
    private static final Map<String, String> ELEMENT_NAMES = Map.of(
            "metal", "金", "wood", "木", "water", "水", "fire", "火", "earth", "土", "thunder", "雷");
    private final String nameKey;
    private final boolean storesElement;

    public BeastMaterialItem(Properties properties, String nameKey, boolean storesElement) {
        super(properties);
        this.nameKey = nameKey;
        this.storesElement = storesElement;
    }

    public static int grade(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("beast_grade")
                ? Math.max(1, Math.min(MAX_GRADE, stack.getTag().getInt("beast_grade"))) : 0;
    }

    public static String element(ItemStack stack) {
        String element = stack.hasTag() ? stack.getTag().getString("beast_element") : "";
        return ELEMENTS.contains(element) ? element : "";
    }

    public static ItemStack setData(ItemStack stack, int grade, String element) {
        stack.getOrCreateTag().putInt("beast_grade", Math.max(1, Math.min(MAX_GRADE, grade)));
        if (stack.getItem() instanceof BeastMaterialItem item && item.storesElement && ELEMENTS.contains(element))
            stack.getTag().putString("beast_element", element);
        else stack.removeTagKey("beast_element");
        return stack;
    }

    @Override public Component getName(ItemStack stack) {
        int grade = grade(stack);
        if (grade == 0) return Component.translatable(nameKey);
        return Component.translatable("item.xiantu.beast_material.tiered",
                Component.translatable("tier.xiantu." + grade), Component.translatable(nameKey));
    }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!storesElement) return;
        String element = element(stack);
        if (!element.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.xiantu.beast_material.element",
                    ELEMENT_NAMES.get(element)).withStyle(ChatFormatting.GRAY));
        }
    }
}
