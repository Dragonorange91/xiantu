package com.longju.xiantu.qa;

import com.longju.xiantu.alchemy.MedicineService;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.item.PillItem;
import com.longju.xiantu.registry.ModItems;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Executes actual registered pill methods on a disposable test-world player. */
@Mod("xiantu_qa")
@Mod.EventBusSubscriber(modid = "xiantu_qa", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MedicineIntegration {
    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        WorkstationAudit.commands(event);
        ChunkAuraAudit.commands(event);
        CultivationCombatAudit.commands(event);
        HabitatDecorationAudit.commands(event);
        RuinGallery.commands(event);
        GalleryRevision.commands(event);
        event.getDispatcher().register(Commands.literal("xiantuqa")
                .then(Commands.literal("beast_photon").requires(source -> source.hasPermission(2))
                    .then(Commands.argument("species",com.mojang.brigadier.arguments.StringArgumentType.word())
                        .then(Commands.argument("scenario",com.mojang.brigadier.arguments.StringArgumentType.word())
                            .executes(context -> BeastPhotonIntegration.start(context.getSource(),
                                com.mojang.brigadier.arguments.StringArgumentType.getString(context,"species"),
                                com.mojang.brigadier.arguments.StringArgumentType.getString(context,"scenario"))))))
                .then(Commands.literal("elements")
                    .executes(context -> ElementalIntegration.run(context.getSource().getPlayerOrException())))
                .then(Commands.literal("physique").executes(context -> PhysiqueIntegration.run(context.getSource().getPlayerOrException())))
                .then(Commands.literal("gongfa").executes(context -> GongfaIntegration.run(context.getSource().getPlayerOrException())))
                .then(Commands.literal("gongfa_gui").executes(context -> GongfaIntegration.gui(context.getSource().getPlayerOrException())))
                .then(Commands.literal("gongfa_gui_restore").executes(context -> GongfaIntegration.restoreGui(context.getSource().getPlayerOrException())))
                .then(Commands.literal("material_appraisal").executes(context -> MaterialAppraisalIntegration.run(context.getSource().getPlayerOrException())))
                .then(Commands.literal("material_appraisal_gui").executes(context -> MaterialAppraisalIntegration.gui(context.getSource().getPlayerOrException())))
                .then(Commands.literal("spirit_wood").executes(context -> SpiritWoodIntegration.run(context.getSource().getPlayerOrException())))
                .then(Commands.literal("spirit_wood_gui").executes(context -> SpiritWoodIntegration.gui(context.getSource().getPlayerOrException())))
                .then(Commands.literal("spirit_wood_gallery").executes(context -> SpiritWoodIntegration.gallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("spirit_wood_art_gallery").executes(context -> SpiritWoodIntegration.artGallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_materials").executes(context -> BuildingIntegration.run(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_water_channel_gallery").executes(context -> WaterChannelIntegration.gallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_jade_cabinet_gallery").executes(context -> JadeCabinetIntegration.gallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_stone_chest_gallery").executes(context -> StoneChestIntegration.gallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_steles_gallery").executes(context -> BuildingIntegration.stelesGallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_cracked_gallery").executes(context -> BuildingIntegration.crackedGallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_mossy_gallery").executes(context -> BuildingIntegration.mossyGallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_finished_steps_gallery").executes(context -> BuildingIntegration.finishedStepsGallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_jade_table_gallery").executes(context -> BuildingIntegration.jadeTableGallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_gallery").executes(context -> BuildingIntegration.gallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_openings_gallery").executes(context -> BuildingIntegration.openingsGallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_windows_gallery").executes(context -> BuildingIntegration.windowsGallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_roofs_gallery").executes(context -> BuildingIntegration.roofsGallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_lamps_gallery").executes(context -> BuildingIntegration.lampsGallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_plaques_gallery").executes(context -> BuildingIntegration.plaquesGallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_beams_gallery").executes(context -> BuildingIntegration.beamsGallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("building_eaves_gallery").executes(context -> BuildingIntegration.eavesGallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("medicine").executes(context -> run(context.getSource().getPlayerOrException())))
                .then(Commands.literal("formation").executes(context -> FormationIntegration.run(context.getSource().getPlayerOrException())))
                .then(Commands.literal("formation_balance").executes(context -> FormationBalanceIntegration.run(context.getSource().getPlayerOrException())))
                .then(Commands.literal("formation_balance_gallery").executes(context -> FormationBalanceIntegration.gallery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("realm_gui").executes(context -> RealmIntegration.openHighRealmStatus(context.getSource().getPlayerOrException())))
                .then(Commands.literal("realm_attributes").executes(context -> RealmIntegration.verifyAttributes(context.getSource().getPlayerOrException())))
                .then(Commands.literal("breakthrough").executes(context -> RealmIntegration.verifyBreakthrough(context.getSource().getPlayerOrException())))
                .then(Commands.literal("inner_demon").executes(context -> InnerDemonIntegration.run(context.getSource().getPlayerOrException())))
                .then(Commands.literal("tribulation").executes(context -> ThunderTribulationIntegration.run(context.getSource().getPlayerOrException())))
                .then(Commands.literal("realm_reset").executes(context -> RealmIntegration.reset(context.getSource().getPlayerOrException())))
                .then(Commands.literal("ore_tiers").executes(context -> OreMiningIntegration.run(context.getSource().getPlayerOrException())))
                .then(Commands.literal("ore_mining").executes(context -> OreMiningIntegration.runSurvivalMining(context.getSource().getPlayerOrException())))
                .then(Commands.literal("ore_worldgen").executes(context -> OreMiningIntegration.scanWorldgen(context.getSource().getPlayerOrException())))
                .then(Commands.literal("ore_small_features").executes(context -> SmallOreIntegration.run(context.getSource().getPlayerOrException())))
                .then(Commands.literal("spirit_stone_absorption").executes(context -> SpiritStoneIntegration.runAbsorption(context.getSource().getPlayerOrException())))
                .then(Commands.literal("spirit_stone_recipes").executes(context -> SpiritStoneIntegration.verifyDowncraftRecipes(context.getSource().getPlayerOrException())))
                .then(Commands.literal("spirit_stone_prepare").executes(context -> SpiritStoneIntegration.prepareManualAbsorption(context.getSource().getPlayerOrException())))
                .then(Commands.literal("spirit_stone_hit_test").executes(context -> SpiritStoneIntegration.scheduleManualDamage(context.getSource().getPlayerOrException())))
                .then(Commands.literal("spirit_stone_restore").executes(context -> SpiritStoneIntegration.restoreManualAbsorption(context.getSource().getPlayerOrException()))));
    }
    private static void check(ServerPlayer player, boolean ok, String label) {
        String line = "[Medicine QA " + (ok ? "PASS" : "FAIL") + "] " + label;
        player.sendSystemMessage(Component.literal(line));
        if (!ok) throw new IllegalStateException(line);
    }
    private static void eat(ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        ((PillItem) stack.getItem()).finishUsingItem(stack, player.level(), player);
    }
    private static int run(ServerPlayer player) { return PillsAudit.run(player); }
}
