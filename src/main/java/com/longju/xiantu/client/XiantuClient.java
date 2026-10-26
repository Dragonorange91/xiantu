package com.longju.xiantu.client;

import com.longju.xiantu.XiantuMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import com.longju.xiantu.registry.ModMenus;
import com.longju.xiantu.registry.ModEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;
import com.longju.xiantu.formation.FormationScreen;
import com.longju.xiantu.cultivation.CultivationStatusMenu;
import com.longju.xiantu.medicine.MedicineRegistry;
import com.longju.xiantu.terrain.TerrainPlantRegistry;

@Mod.EventBusSubscriber(modid = XiantuMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class XiantuClient {
    @SubscribeEvent
    public static void registerReloadListeners(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)AuthoredWeaponClips::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)RealmGateMesh::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->SpiritSwordRenderer.clear());
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)StudyPageGeometry::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                manager -> com.longju.xiantu.client.equipment.CangmingArmorModel.clear());
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                manager -> net.minecraft.client.Minecraft.getInstance().execute(BeastPhotonClient::clear));
        event.registerReloadListener(GltfItemRenderer.instance().reloadListener());
        event.registerReloadListener(YanweiFoxRenderer.reloadListener());
        event.registerReloadListener(CrackedBladeWolfRenderer.reloadListener());
        event.registerReloadListener(CragRockSpiderRenderer.reloadListener());
        event.registerReloadListener(SkinnedBeastRenderer.reloadListener());
        event.registerReloadListener(TownMerchantRenderer.reloadListener());
    }
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            com.longju.xiantu.alchemy.PillCatalog.ITEMS.values().forEach(item -> net.minecraft.client.renderer.item.ItemProperties.register(item.get(),
                new net.minecraft.resources.ResourceLocation("xiantu","pill_quality"), (stack,level,entity,seed) -> com.longju.xiantu.alchemy.PillCatalog.quality(stack)));
            MenuScreens.register(ModMenus.PILL_CHOICE.get(), PillChoiceScreen::new);
            net.minecraft.client.renderer.item.ItemProperties.register(
                    com.longju.xiantu.registry.ModItems.DEMON_CORE.get(),
                    new net.minecraft.resources.ResourceLocation(XiantuMod.MOD_ID, "core_model"),
                    (stack, level, entity, seed) -> com.longju.xiantu.item.DemonCoreItem.modelData(stack));
            MedicineRegistry.WILD_PLANTS.values().forEach(block ->
                    ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutout()));
            MedicineRegistry.CROPS.values().forEach(block ->
                    ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutout()));
            TerrainPlantRegistry.PLANTS.values().forEach(block ->
                    ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutout()));
        });
        event.enqueueWork(() -> MenuScreens.register(ModMenus.TOWN_MERCHANT.get(), TownMerchantScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.INSTANCES.get(), com.longju.xiantu.instance.InstanceScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.ACCESSORIES.get(), AccessoryScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.GONGFA_MENU.get(), com.longju.xiantu.gongfa.GongfaScreen::new));
        event.enqueueWork(() -> MenuScreens.register(com.longju.xiantu.wood.SpiritWoodRegistry.MENU.get(), com.longju.xiantu.wood.SaplingGrowthScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.TALISMAN.get(), com.longju.xiantu.talisman.TalismanScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.SMELTING.get(), com.longju.xiantu.smelting.SmeltingScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.REFINING.get(), com.longju.xiantu.refining.RefiningScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.BLUEPRINT_LEARNING.get(), com.longju.xiantu.refining.BlueprintLearningScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.ALCHEMY.get(), com.longju.xiantu.alchemy.AlchemyScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.SPELLS.get(), SpellScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.EQUIPMENT_REPAIR.get(), EquipmentRepairScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.EQUIPMENT_SKILLS.get(), EquipmentSkillScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.APPRAISAL.get(), com.longju.xiantu.material.AppraisalScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.FORMATION_MENU.get(), FormationScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.CULTIVATION_STATUS_MENU.get(), CultivationStatusScreen::new));
    }
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
    }
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.longju.xiantu.instance.InstanceRegistry.PORTAL_ENTITY.get(),RealmPortalRenderer::new);
        event.registerEntityRenderer(ModEntities.MERCHANTS.get("town_guide").get(), ctx -> new TownMerchantRenderer(ctx,"town_guide"));
        event.registerEntityRenderer(ModEntities.MERCHANTS.get("wanbao_merchant").get(), ctx -> new TownMerchantRenderer(ctx,"shen_wanchou"));
        event.registerEntityRenderer(ModEntities.MERCHANTS.get("smith_merchant").get(), ctx -> new TownMerchantRenderer(ctx,"wen_tieyi"));
        event.registerEntityRenderer(ModEntities.MERCHANTS.get("alchemy_merchant").get(), ctx -> new TownMerchantRenderer(ctx,"su_wenzhi"));
        event.registerEntityRenderer(ModEntities.MERCHANTS.get("talisman_merchant").get(), ctx -> new TownMerchantRenderer(ctx,"lu_xuanwen"));
        event.registerEntityRenderer(ModEntities.FOXFIRE.get(), PhotonProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.YANWEI_FOX.get(), YanweiFoxRenderer::new);
        event.registerEntityRenderer(ModEntities.CRACKED_BLADE_WOLF.get(), CrackedBladeWolfRenderer::new);
        event.registerEntityRenderer(ModEntities.CRAG_ROCK_SPIDER.get(), CragRockSpiderRenderer::new);
        event.registerEntityRenderer(ModEntities.COLD_FANG_WOLF.get(), context ->
                new SkinnedBeastRenderer<>(context, "han_ya_lang", 2.175F, 0.0F, .48F, beast ->
                        beast.isAttacking() ? "attack" : beast.isRunning() ? "run" : beast.isAlerted() ? "alert"
                                : beast.getDeltaMovement().horizontalDistanceSqr() > .0004 ? "walk" : "idle"));
        event.registerEntityRenderer(ModEntities.THUNDER_FEATHER_RAVEN.get(), context ->
                new SkinnedBeastRenderer<>(context, "ting_yu_ya", 1.5F, 0.0F, .35F,
                        com.longju.xiantu.entity.ThunderFeatherRavenEntity::animationState));
        event.registerEntityRenderer(ModEntities.VINE_DEER.get(), context ->
                new SkinnedBeastRenderer<>(context, "qing_teng_lu", 2.203F, 0.0F, .6F,
                        com.longju.xiantu.entity.VineDeerEntity::animationState));
        event.registerEntityRenderer(ModEntities.THUNDER_DEER.get(), context ->
                new SkinnedBeastRenderer<>(context, "lei_jiao_lu", 2.413F, 0.0F, .6F,
                        com.longju.xiantu.entity.ThunderDeerEntity::animationState));
        event.registerEntityRenderer(ModEntities.ROCK_BACK_BOAR.get(), context ->
                new SkinnedBeastRenderer<>(context, "yan_bei_ye_zhu", 2.772F, 0.0F, .65F,
                        com.longju.xiantu.entity.RockBackBoarEntity::animationState));
        event.registerEntityRenderer(ModEntities.IRON_TOOTH_BADGER.get(), context ->
                new SkinnedBeastRenderer<>(context, "tie_ya_huan", 2.422F, 0.0F, .55F,
                        com.longju.xiantu.entity.IronToothBadgerEntity::animationState));
        event.registerEntityRenderer(ModEntities.MOSS_BACK_BEAR.get(), context ->
                new SkinnedBeastRenderer<>(context, "tai_bei_xiong", 2.966F, 0.0F, .8F,
                        com.longju.xiantu.entity.MossBackBearEntity::animationState));
        event.registerEntityRenderer(ModEntities.MOUNTAIN_BEAR.get(), context ->
                new SkinnedBeastRenderer<>(context, "zhen_yue_xiong", 4.27F, 0.0F, .9F,
                        com.longju.xiantu.entity.MountainBearEntity::animationState));
        event.registerBlockEntityRenderer(com.longju.xiantu.registry.ModBlockEntities.BLACK_ROCK_CHEST.get(), StoneChestRenderer::new);
        event.registerBlockEntityRenderer(com.longju.xiantu.registry.ModBlockEntities.QUNYOU_FIGURINE.get(), QunyouFigurineRenderer::new);
        event.registerBlockEntityRenderer(com.longju.xiantu.registry.ModBlockEntities.RUIN_REWARD_CHEST.get(), RewardChestRenderer::new);
        event.registerEntityRenderer(ModEntities.THICK_HORN_RHINO.get(), context ->
                new SkinnedBeastRenderer<>(context, "hou_jiao_xi", 3.809F, 0.0F, .85F,
                        com.longju.xiantu.entity.ThickHornRhinoEntity::animationState));
        event.registerEntityRenderer(ModEntities.GOLD_MANE_LION.get(), context ->
                new SkinnedBeastRenderer<>(context, "jin_zong_shi", 3.012F, 0.0F, .75F,
                        com.longju.xiantu.entity.GoldManeLionEntity::animationState));
        event.registerEntityRenderer(ModEntities.RED_SUN_LION.get(), context ->
                new SkinnedBeastRenderer<>(context, "chi_yao_shi", 3.020F, 0.0F, .75F,
                        com.longju.xiantu.entity.RedSunLionEntity::animationState));
        event.registerEntityRenderer(ModEntities.COMPLETION_BOLT.get(), PhotonProjectileRenderer::new);
        for (var d : com.longju.xiantu.entity.CompletionBestiary.ALL) {
            event.registerEntityRenderer(ModEntities.COMPLETION.get(d.id()).get(), context ->
                    new SkinnedBeastRenderer<>(context,d.model(),d.scale(),0F,d.width()*.45F,
                            mob -> ((com.longju.xiantu.entity.CompletionAnimated)mob).animationState()));
        }
        event.registerEntityRenderer(ModEntities.FORMATION_SEAT.get(), FormationSeatRenderer::new);
        event.registerEntityRenderer(ModEntities.SPIRIT_SWORD.get(), SpiritSwordRenderer::new);
        event.registerEntityRenderer(ModEntities.INNER_DEMON.get(), InnerDemonRenderer::new);
        event.registerEntityRenderer(ModEntities.PILL_DECOY.get(), PillDecoyRenderer::new);
    }
    @SubscribeEvent
    public static void registerAdditionalModels(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
        GongfaStudyRenderer.Models.additional(event);
        event.register(new net.minecraft.resources.ResourceLocation("xiantu","item/spirit_flying_sword"));
        for(String part:new String[]{"base","lid"}) for(String type:new String[]{"single","left","right"}) event.register(new net.minecraft.resources.ResourceLocation("xiantu","block/ruin_reward_chest_"+part+"_"+type));
        for (String type : new String[]{"single", "left", "right"})
            for (String part : new String[]{"base", "lid"})
                event.register(new net.minecraft.resources.ResourceLocation("xiantu", "block/black_rock_chest_"+part+"_"+type));
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void wrapStudyModels(net.minecraftforge.client.event.ModelEvent.ModifyBakingResult event) {
        GongfaStudyRenderer.Models.baked(event);
    }
    @SubscribeEvent
    public static void verifyStudyModels(net.minecraftforge.client.event.ModelEvent.BakingCompleted event) {
        GongfaStudyRenderer.Models.completed(event);
    }
    private XiantuClient() {}
}
