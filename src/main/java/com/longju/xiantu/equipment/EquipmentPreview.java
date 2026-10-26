package com.longju.xiantu.equipment;

import com.longju.xiantu.registry.ModItems;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;
import java.util.List;

/** Creative-only visual prototypes. Survival crafting and final cultivation effects are separate work. */
public final class EquipmentPreview {
    public static final RegistryObject<Item> SWORD = ModItems.ITEMS.register("preview_qingshuang_sword", () ->
            new com.longju.xiantu.refining.RefinedEquipment.Sword(Tiers.IRON, 1, -2.4F, new Item.Properties()) {
                @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
                    super.appendHoverText(stack, level, lines, flag);
                }
            });
    public static final RegistryObject<Item> HELMET = armor("helmet", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> CHESTPLATE = armor("chestplate", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> LEGGINGS = armor("leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> BOOTS = armor("boots", ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> JINGUBANG = ModItems.ITEMS.register("preview_jingubang", () -> new com.longju.xiantu.refining.RefinedEquipment.Sword(Tiers.IRON, 1, -2.4F, new Item.Properties()));
    public static final RegistryObject<Item> QITIAN_HELMET = qitian("helmet", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> QITIAN_CHESTPLATE = qitian("chestplate", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> QITIAN_LEGGINGS = qitian("leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> QITIAN_BOOTS = qitian("boots", ArmorItem.Type.BOOTS);
    private static RegistryObject<Item> qitian(String name, ArmorItem.Type type) {
        return ModItems.ITEMS.register("preview_qitian_" + name, () -> new PreviewArmorItem(type, "qitian_armor", "wukong_materials"));
    }
    private static RegistryObject<Item> armor(String name, ArmorItem.Type type) {
        return ModItems.ITEMS.register("preview_cangming_" + name, () -> new PreviewArmorItem(type));
    }
    public static void bootstrap() {}
    public static List<RegistryObject<Item>> items() { return List.of(SWORD, HELMET, CHESTPLATE, LEGGINGS, BOOTS,
            JINGUBANG, QITIAN_HELMET, QITIAN_CHESTPLATE, QITIAN_LEGGINGS, QITIAN_BOOTS); }
    private EquipmentPreview() {}
}
