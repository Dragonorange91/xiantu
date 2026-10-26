package com.longju.xiantu.combat;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.cultivation.CultivationCapability;
import com.longju.xiantu.cultivation.SpiritRootType;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = XiantuMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ElementalCombat {
    public static final Map<Element, TagKey<EntityType<?>>> AFFINITIES = new EnumMap<>(Element.class);
    public static final Map<Element, TagKey<Item>> WEAPONS = new EnumMap<>(Element.class);
    static {
        for (Element element : Element.values()) if (element.elemental()) {
            var id = new ResourceLocation(XiantuMod.MOD_ID, "elemental_affinity/" + element.id());
            AFFINITIES.put(element, TagKey.create(Registries.ENTITY_TYPE, id));
            WEAPONS.put(element, TagKey.create(Registries.ITEM,
                    new ResourceLocation(XiantuMod.MOD_ID, "elemental_weapons/" + element.id())));
        }
    }
    public static Set<Element> affinities(LivingEntity entity) {
        var result = EnumSet.noneOf(Element.class);
        if (entity instanceof Player player) {
            player.getCapability(CultivationCapability.DATA).ifPresent(data -> {
                for (SpiritRootType root : SpiritRootType.values()) if (data.hasSpiritRoot(root))
                    result.add(Element.valueOf(root.name()));
            });
        } else for (var entry : AFFINITIES.entrySet()) if (entity.getType().is(entry.getValue())) result.add(entry.getKey());
        return result;
    }
    /** Ambiguous weapons do not silently select the most favorable element. */
    public static Element weaponElement(ItemStack stack) {
        if(com.longju.xiantu.refining.WeaponPassives.weapon(com.longju.xiantu.refining.RefinedEquipment.recipe(stack)))
            return com.longju.xiantu.refining.WeaponPassives.element(stack);
        Element selected = Element.PHYSICAL;
        for (var entry : WEAPONS.entrySet()) if (stack.is(entry.getValue())) {
            if (selected.elemental()) return Element.PHYSICAL;
            selected = entry.getKey();
        }
        return selected;
    }
    public static Element attackElement(DamageSource source) {
        // An explicit PHYSICAL override suppresses even a beast's innate elemental attack.
        if (source instanceof ElementalDamageSource explicit) return explicit.element();
        if (!(source.getEntity() instanceof LivingEntity attacker)) return Element.PHYSICAL;
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == attacker && attacker instanceof Player)
            return weaponElement(attacker.getMainHandItem());
        if (attacker instanceof Player || !(source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_PROJECTILE) || source.is(DamageTypes.THROWN))) return Element.PHYSICAL;
        Set<Element> elements = affinities(attacker);
        return elements.size() == 1 ? elements.iterator().next() : Element.PHYSICAL;
    }
    /** One mutation of the existing hit; never generate a second damage/kill event. */
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void hurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || !ElementalConfig.ENABLED.get() || event.getAmount() <= 0) return;
        Element attack = attackElement(event.getSource());
        if (!attack.elemental()) return;
        LivingEntity owner = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
        event.setAmount(ElementalRules.damage(event.getAmount(), attack, affinities(event.getEntity()),
                ElementalAttributes.resistance(event.getEntity(), attack), event.getEntity() instanceof Player defender ? com.longju.xiantu.refining.EquipmentConditions.penetration(defender,attack,ElementalAttributes.resistance(defender,attack),ElementalAttributes.penetration(owner,attack)) : ElementalAttributes.penetration(owner, attack),
                ElementalConfig.ADVANTAGE.get(), ElementalConfig.REVERSE.get(),
                event.getSource() instanceof ElementalDamageSource explicit ? explicit.elementalFraction() : 1));
    }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        Element element = weaponElement(event.getItemStack());
        if (element.elemental()) event.getToolTip().add(Component.translatable("tooltip.xiantu.elemental_attack",
                Component.translatable("element.xiantu." + element.id())).withStyle(ChatFormatting.AQUA));
    }
    private ElementalCombat() {}
}
