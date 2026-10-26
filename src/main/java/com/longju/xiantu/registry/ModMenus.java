package com.longju.xiantu.registry;

import com.longju.xiantu.XiantuMod;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.extensions.IForgeMenuType;
import com.longju.xiantu.formation.FormationMenu;
import com.longju.xiantu.cultivation.CultivationStatusMenu;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, XiantuMod.MOD_ID);
    public static final RegistryObject<MenuType<com.longju.xiantu.instance.InstanceMenu>> INSTANCES=MENUS.register("instances",()->IForgeMenuType.create(com.longju.xiantu.instance.InstanceMenu::new));
    public static final RegistryObject<MenuType<com.longju.xiantu.smelting.SmeltingMenu>> SMELTING = MENUS.register("smelting_furnace", () -> IForgeMenuType.create(com.longju.xiantu.smelting.SmeltingMenu::new));
    public static final RegistryObject<MenuType<FormationMenu>> FORMATION_MENU = MENUS.register("formation", () -> IForgeMenuType.create(FormationMenu::new));
    public static final RegistryObject<MenuType<CultivationStatusMenu>> CULTIVATION_STATUS_MENU = MENUS.register("cultivation_status", () -> IForgeMenuType.create(CultivationStatusMenu::new));
    public static final RegistryObject<MenuType<com.longju.xiantu.material.AppraisalMenu>> APPRAISAL = MENUS.register("appraisal_altar", () -> IForgeMenuType.create(com.longju.xiantu.material.AppraisalMenu::new));
    public static final RegistryObject<MenuType<com.longju.xiantu.trade.TownMerchantMenu>> TOWN_MERCHANT = MENUS.register("town_merchant", () -> IForgeMenuType.create(com.longju.xiantu.trade.TownMerchantMenu::new));
    public static final RegistryObject<MenuType<com.longju.xiantu.equipment.AccessoryMenu>> ACCESSORIES = MENUS.register("accessories", () -> IForgeMenuType.create(com.longju.xiantu.equipment.AccessoryMenu::new));
    public static final RegistryObject<MenuType<com.longju.xiantu.refining.RefiningMenu>> REFINING = MENUS.register("refining", () -> IForgeMenuType.create(com.longju.xiantu.refining.RefiningMenu::new));
    public static final RegistryObject<MenuType<com.longju.xiantu.alchemy.PillChoiceMenu>> PILL_CHOICE = MENUS.register("pill_choice", () -> IForgeMenuType.create(com.longju.xiantu.alchemy.PillChoiceMenu::new));
    public static final RegistryObject<MenuType<com.longju.xiantu.alchemy.AlchemyMenu>> ALCHEMY = MENUS.register("alchemy", () -> IForgeMenuType.create(com.longju.xiantu.alchemy.AlchemyMenu::new));
    public static final RegistryObject<MenuType<com.longju.xiantu.spell.SpellMenu>> SPELLS = MENUS.register("spells", () -> IForgeMenuType.create(com.longju.xiantu.spell.SpellMenu::new));
    public static final RegistryObject<MenuType<com.longju.xiantu.refining.EquipmentRepairMenu>> EQUIPMENT_REPAIR=MENUS.register("equipment_repair",()->IForgeMenuType.create(com.longju.xiantu.refining.EquipmentRepairMenu::new));
    public static final RegistryObject<MenuType<com.longju.xiantu.refining.EquipmentSkillMenu>> EQUIPMENT_SKILLS=MENUS.register("equipment_skills",()->IForgeMenuType.create(com.longju.xiantu.refining.EquipmentSkillMenu::new));
    public static final RegistryObject<MenuType<com.longju.xiantu.talisman.TalismanMenu>> TALISMAN=MENUS.register("talisman_table",()->IForgeMenuType.create(com.longju.xiantu.talisman.TalismanMenu::new));
    private ModMenus() {}
    public static final RegistryObject<MenuType<com.longju.xiantu.refining.BlueprintLearningMenu>> BLUEPRINT_LEARNING = MENUS.register("blueprint_learning", () -> IForgeMenuType.create(com.longju.xiantu.refining.BlueprintLearningMenu::new));
    public static final RegistryObject<MenuType<com.longju.xiantu.gongfa.GongfaMenu>> GONGFA_MENU = MENUS.register("gongfa", () -> IForgeMenuType.create(com.longju.xiantu.gongfa.GongfaMenu::new));
}
