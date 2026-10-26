package com.longju.xiantu.equipment;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.List;
import java.util.function.Consumer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class PreviewArmorItem extends ArmorItem {
    private final String modelName;
    private final String materialName;
    public PreviewArmorItem(Type type) { this(type,"cangming_zhenyue","cangming_materials"); }
    public PreviewArmorItem(Type type, String modelName, String materialName) {
        super(ArmorMaterials.IRON, type, new Item.Properties());
        this.modelName=modelName; this.materialName=materialName;
    }
    @Override public int getMaxDamage(ItemStack stack){return com.longju.xiantu.refining.RefinedEquipment.infinite(stack)?0:com.longju.xiantu.refining.RefinedEquipment.durability(stack,super.getMaxDamage(stack));}
    @Override public boolean canEquip(ItemStack stack,EquipmentSlot slot,Entity entity){return slot==getEquipmentSlot();}
    @Override public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "xiantu:textures/models/armor/" + materialName + ".png";
    }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        // Forge's Item constructor invokes this before subclass fields have been assigned.
        consumer.accept(new IClientItemExtensions() {
            private IClientItemExtensions delegate;
            @Override public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                    net.minecraft.world.entity.LivingEntity entity, ItemStack stack, EquipmentSlot slot,
                    net.minecraft.client.model.HumanoidModel<?> original) {
                if(delegate==null) com.longju.xiantu.client.equipment.CangmingArmorModel.install(value -> delegate=value, modelName);
                return delegate.getHumanoidArmorModel(entity,stack,slot,original);
            }
        });
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
    }
}
