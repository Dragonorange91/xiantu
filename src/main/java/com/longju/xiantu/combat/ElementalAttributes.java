package com.longju.xiantu.combat;

import com.longju.xiantu.XiantuMod;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = XiantuMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ElementalAttributes {
    public static final DeferredRegister<Attribute> REGISTRY = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, XiantuMod.MOD_ID);
    public static final Map<Element, RegistryObject<Attribute>> RESISTANCE = new EnumMap<>(Element.class);
    public static final Map<Element, RegistryObject<Attribute>> PENETRATION = new EnumMap<>(Element.class);
    static {
        for (Element element : Element.values()) if (element.elemental()) {
            RESISTANCE.put(element, REGISTRY.register(element.id() + "_resistance", () ->
                    new RangedAttribute("attribute.xiantu." + element.id() + "_resistance", 0, -.25, .80).setSyncable(true)));
            PENETRATION.put(element, REGISTRY.register(element.id() + "_penetration", () ->
                    new RangedAttribute("attribute.xiantu." + element.id() + "_penetration", 0, 0, 1).setSyncable(true)));
        }
    }
    @SubscribeEvent public static void modify(EntityAttributeModificationEvent event) {
        for (var type : event.getTypes()) for (Element element : RESISTANCE.keySet()) {
            if (!event.has(type, RESISTANCE.get(element).get())) event.add(type, RESISTANCE.get(element).get());
            if (!event.has(type, PENETRATION.get(element).get())) event.add(type, PENETRATION.get(element).get());
        }
    }
    public static double resistance(LivingEntity entity, Element element) { return value(entity, RESISTANCE.get(element)); }
    public static double penetration(LivingEntity entity, Element element) { return value(entity, PENETRATION.get(element)); }
    private static double value(LivingEntity entity, RegistryObject<Attribute> attribute) {
        return entity == null || attribute == null || !entity.getAttributes().hasAttribute(attribute.get())
                ? 0 : entity.getAttributeValue(attribute.get());
    }
    private ElementalAttributes() {}
}
