package com.longju.xiantu.registry;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.item.DemonCoreItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, XiantuMod.MOD_ID);
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.xiantu.main"))
            .icon(() -> new ItemStack(ModItems.SPIRIT_STONE.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.FAN_LING_SHI.get());
                output.accept(ModItems.LING_JING.get());
                output.accept(ModItems.LING_YU.get());
                output.accept(ModItems.XUAN_JING.get());
                output.accept(ModItems.DI_SUI.get());
                output.accept(ModItems.TIAN_JING.get());
                output.accept(ModItems.XIAN_YU.get());
                output.accept(ModItems.DAO_SHI.get());
                output.accept(ModItems.INNER_DEMON_CRYSTAL.get());
                output.accept(ModItems.CULTIVATION_MANUAL.get());
                output.accept(ModItems.CULTIVATION_PROBE.get());
                output.accept(ModItems.REINCARNATION_STONE.get());
                output.accept(ModItems.GUIDING_STONE.get());
                output.accept(com.longju.xiantu.instance.InstanceRegistry.TOKEN.get());
                output.accept(com.longju.xiantu.instance.InstanceRegistry.CHILD.get());
                output.accept(com.longju.xiantu.instance.InstanceRegistry.REALM_TEST.get());
                output.accept(ModBlockItems.EQUIPMENT_REPAIR.get());
                output.accept(ModItems.YAN_TU_JUE.get());
                output.accept(ModItems.XUAN_SHUI_JUE.get());
                output.accept(ModItems.CHI_YAN_JUE.get());
                output.accept(ModItems.QING_MU_JUE.get());
                output.accept(ModItems.YOU_SHUI_ZHEN_JING.get());
                output.accept(ModItems.QING_SHAN_ZHEN_DI.get());
                output.accept(ModItems.FEN_CHI_CHI_HUO.get());
                output.accept(ModItems.BAI_CAO_ZHEN_JING.get());
                output.accept(ModItems.JIN_GUANG_XUAN_GONG.get());
                ModItems.ADDITIONAL_GONGFA.values().forEach(method->output.accept(method.get()));
                com.longju.xiantu.spell.SpellCatalog.ALL.stream().filter(com.longju.xiantu.spell.SpellCatalog::enabled).forEach(s->output.accept(com.longju.xiantu.spell.SpellScrolls.stack(s.id())));
                ModItems.COMPLETION_EGGS.values().forEach(egg -> output.accept(egg.get()));
                output.accept(ModItems.YANWEI_FOX_SPAWN_EGG.get());
                output.accept(ModItems.CRACKED_BLADE_WOLF_SPAWN_EGG.get());
                output.accept(ModItems.CRAG_ROCK_SPIDER_SPAWN_EGG.get());
                output.accept(ModItems.COLD_FANG_WOLF_SPAWN_EGG.get());
                output.accept(ModItems.THUNDER_FEATHER_RAVEN_SPAWN_EGG.get());
                output.accept(ModItems.VINE_DEER_SPAWN_EGG.get());
                output.accept(ModItems.THUNDER_DEER_SPAWN_EGG.get());
                output.accept(ModItems.IRON_TOOTH_BADGER_SPAWN_EGG.get());
                output.accept(ModItems.ROCK_BACK_BOAR_SPAWN_EGG.get());
                output.accept(ModItems.MOSS_BACK_BEAR_SPAWN_EGG.get());
                output.accept(ModItems.MOUNTAIN_BEAR_SPAWN_EGG.get());
                output.accept(ModItems.THICK_HORN_RHINO_SPAWN_EGG.get());
                output.accept(ModItems.GOLD_MANE_LION_SPAWN_EGG.get());
                output.accept(ModItems.RED_SUN_LION_SPAWN_EGG.get());
                output.accept(ModBlockItems.APPRAISAL.get());
                com.longju.xiantu.workstation.WorkstationRegistry.EQUIPMENT_ITEMS.forEach((id,item) -> {if (!id.contains("alchemy") && !id.contains("refining") && !id.equals("artisan_forging_table")) output.accept(item.get());});
                output.accept(ModBlockItems.FORMATION_CORE.get());
                output.accept(ModBlockItems.ARRAY_BASE_1.get());
                output.accept(ModBlockItems.ARRAY_RUNE_1.get());
                output.accept(ModBlockItems.ARRAY_PILLAR_1.get());
                output.accept(ModBlockItems.MEDITATION_CUSHION.get());
                output.accept(ModBlockItems.FORMATION_CORE_2.get());
                output.accept(ModBlockItems.ARRAY_BASE_2.get());
                output.accept(ModBlockItems.ARRAY_RUNE_2.get());
                output.accept(ModBlockItems.ARRAY_PILLAR_2.get());
                output.accept(ModBlockItems.FORMATION_CORE_3.get());
                output.accept(ModBlockItems.ARRAY_BASE_3.get());
                output.accept(ModBlockItems.ARRAY_RUNE_3.get());
                output.accept(ModBlockItems.ARRAY_PILLAR_3.get());
                output.accept(ModBlockItems.FAN_LING_SHI_ORE.get());
                output.accept(ModBlockItems.LING_JING_ORE.get());
                output.accept(ModBlockItems.LING_YU_ORE.get());
                output.accept(ModBlockItems.XUAN_JING_ORE.get());
                output.accept(ModBlockItems.DI_SUI_ORE.get());
                output.accept(ModBlockItems.TIAN_JING_ORE.get());
                output.accept(ModBlockItems.XIAN_YU_ORE.get());
                output.accept(ModBlockItems.DAO_SHI_ORE.get());
                com.longju.xiantu.mining.SpecialMineralRegistry.MATERIALS.values().forEach(item -> output.accept(item.get()));
                com.longju.xiantu.mining.AdvancedMinerals.MATERIALS.values().forEach(item -> output.accept(item.get()));
                com.longju.xiantu.mining.SpecialMineralRegistry.ORE_ITEMS.values().forEach(item -> output.accept(item.get()));
                com.longju.xiantu.mining.AdvancedMinerals.ORE_ITEMS.values().forEach(item -> output.accept(item.get()));
                output.accept(com.longju.xiantu.refining.QitianMaterials.ORE.get().asItem());
                for (var material : com.longju.xiantu.mining.MetalMaterialRegistry.ALL) {
                    output.accept(ModItems.METAL_RAW_ORES.get(material.id()).get());
                    output.accept(ModItems.METAL_INGOTS.get(material.id()).get());
                    output.accept(ModBlockItems.METAL_ORES.get(material.id()).get());
                    output.accept(ModBlockItems.METAL_STORAGE_BLOCKS.get(material.id()).get());
                }
            }).build());
    public static final RegistryObject<CreativeModeTab> BUILDING = TABS.register("building", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.xiantu.building"))
            .icon(() -> new ItemStack(ModBlockItems.AZURE_BRICK.get()))
            .withTabsBefore(MAIN.getId())
            .displayItems((parameters, output) -> {
                for (var type : com.longju.xiantu.wood.SpiritWoodType.values())
                    for (String part : com.longju.xiantu.wood.SpiritWoodRegistry.BUILDING)
                        output.accept(com.longju.xiantu.wood.SpiritWoodRegistry.item(type,part));
                output.accept(ModBlockItems.TOWN_RED_WOOD.get());
                ModBlockItems.TOWN_MATERIALS.values().forEach(item -> output.accept(item.get()));
                output.accept(ModBlockItems.WANBAOGE_PLAQUE.get());
                output.accept(ModBlockItems.QUNYOU_FIGURINE.get());
                output.accept(ModBlockItems.FORGING_SHOP_PLAQUE.get());
                output.accept(ModBlockItems.ALCHEMY_PAVILION_PLAQUE.get());
                output.accept(ModBlockItems.TALISMAN_SHOP_PLAQUE.get());
                output.accept(ModBlockItems.TOWN_GRAY_BRICK.get());
                output.accept(ModBlockItems.TOWN_PAVING_LAMP.get());
                output.accept(ModBlockItems.TOWN_CYAN_ROOF.get());
                output.accept(ModBlockItems.TOWN_CYAN_ROOF_STAIRS.get());
                output.accept(ModBlockItems.TOWN_CYAN_ROOF_TILES.get());
                output.accept(ModBlockItems.TOWN_CYAN_EAVES.get());
                output.accept(ModBlockItems.TOWN_CYAN_EAVES_CORNER.get());
                output.accept(ModBlockItems.TOWN_CYAN_ROOF_RIDGE.get());
                output.accept(ModBlockItems.TOWN_CARVED_PANEL.get());
                output.accept(com.longju.xiantu.instance.InstanceDecor.LANTERN.get().asItem());
                output.accept(com.longju.xiantu.instance.InstanceDecor.RELIQUARY.get().asItem());
                output.accept(com.longju.xiantu.instance.InstanceDecor.PILLAR.get().asItem());
                // AZURE_BRICK
                output.accept(ModBlockItems.RUIN_REWARD_CHEST.get());
                output.accept(ModBlockItems.AZURE_BRICK.get());
                output.accept(ModBlockItems.AZURE_BRICK_SMOOTH.get());
                output.accept(ModBlockItems.AZURE_BRICK_SMOOTH_STAIRS.get());
                output.accept(ModBlockItems.AZURE_BRICK_SMOOTH_SLAB.get());
                output.accept(ModBlockItems.AZURE_BRICK_CARVED.get());
                output.accept(ModBlockItems.AZURE_BRICK_CARVED_STAIRS.get());
                output.accept(ModBlockItems.AZURE_BRICK_CARVED_SLAB.get());
                output.accept(ModBlockItems.AZURE_BRICK_PILLAR.get());
                output.accept(ModBlockItems.AZURE_BRICK_STAIRS.get());
                output.accept(ModBlockItems.AZURE_BRICK_SLAB.get());
                output.accept(ModBlockItems.AZURE_BRICK_WALL.get());
                output.accept(ModBlockItems.AZURE_BRICK_RAILING.get());
                output.accept(ModBlockItems.AZURE_BRICK_DOOR.get());
                output.accept(ModBlockItems.AZURE_BRICK_TRAPDOOR.get());
                output.accept(ModBlockItems.AZURE_BRICK_WINDOW.get());
                output.accept(ModBlockItems.AZURE_BRICK_ROOF_TILES.get());
                output.accept(ModBlockItems.AZURE_BRICK_ROOF_STAIRS.get());
                output.accept(ModBlockItems.AZURE_BRICK_ROOF_RIDGE.get());
                output.accept(ModBlockItems.AZURE_BRICK_LAMP_STAND.get());
                output.accept(ModBlockItems.AZURE_BRICK_STONE_LAMP.get());
                output.accept(ModBlockItems.AZURE_BRICK_BEAM.get());
                output.accept(ModBlockItems.AZURE_BRICK_BRACKET.get());
                output.accept(ModBlockItems.AZURE_BRICK_EAVES.get());
                output.accept(ModBlockItems.AZURE_BRICK_EAVES_CORNER.get());
                output.accept(ModBlockItems.AZURE_BRICK_PILLAR_BASE.get());
                output.accept(ModBlockItems.AZURE_BRICK_PILLAR_CAPITAL.get());
                output.accept(ModBlockItems.AZURE_BRICK_BROKEN_PILLAR.get());
                output.accept(ModBlockItems.AZURE_BRICK_RUBBLE.get());
                output.accept(ModBlockItems.AZURE_BRICK_STELE.get());
                output.accept(ModBlockItems.AZURE_BRICK_CRACKED.get());
                output.accept(ModBlockItems.AZURE_BRICK_CRACKED_STAIRS.get());
                output.accept(ModBlockItems.AZURE_BRICK_CRACKED_SLAB.get());
                output.accept(ModBlockItems.AZURE_BRICK_MOSSY.get());
                output.accept(ModBlockItems.AZURE_BRICK_MOSSY_STAIRS.get());
                output.accept(ModBlockItems.AZURE_BRICK_MOSSY_SLAB.get());
                output.accept(ModBlockItems.AZURE_BRICK_MOSSY_WALL.get());
                // BLACK_ROCK
                output.accept(ModBlockItems.BLACK_ROCK.get());
                output.accept(ModBlockItems.BLACK_ROCK_PILLAR.get());
                output.accept(ModBlockItems.BLACK_ROCK_STAIRS.get());
                output.accept(ModBlockItems.BLACK_ROCK_SLAB.get());
                output.accept(ModBlockItems.BLACK_ROCK_WALL.get());
                output.accept(ModBlockItems.BLACK_ROCK_SMOOTH.get());
                output.accept(ModBlockItems.BLACK_ROCK_SMOOTH_STAIRS.get());
                output.accept(ModBlockItems.BLACK_ROCK_SMOOTH_SLAB.get());
                output.accept(ModBlockItems.BLACK_ROCK_CARVED.get());
                output.accept(ModBlockItems.BLACK_ROCK_CARVED_STAIRS.get());
                output.accept(ModBlockItems.BLACK_ROCK_CARVED_SLAB.get());
                output.accept(ModBlockItems.BLACK_ROCK_RAILING.get());
                output.accept(ModBlockItems.BLACK_ROCK_DOOR.get());
                output.accept(ModBlockItems.BLACK_ROCK_TRAPDOOR.get());
                output.accept(ModBlockItems.BLACK_ROCK_WINDOW.get());
                output.accept(ModBlockItems.BLACK_ROCK_ROOF_TILES.get());
                output.accept(ModBlockItems.BLACK_ROCK_ROOF_STAIRS.get());
                output.accept(ModBlockItems.BLACK_ROCK_ROOF_RIDGE.get());
                output.accept(ModBlockItems.BLACK_ROCK_LAMP_STAND.get());
                output.accept(ModBlockItems.BLACK_ROCK_STONE_LAMP.get());
                output.accept(ModBlockItems.BLACK_ROCK_BEAM.get());
                output.accept(ModBlockItems.BLACK_ROCK_BRACKET.get());
                output.accept(ModBlockItems.BLACK_ROCK_EAVES.get());
                output.accept(ModBlockItems.BLACK_ROCK_EAVES_CORNER.get());
                output.accept(ModBlockItems.BLACK_ROCK_PILLAR_BASE.get());
                output.accept(ModBlockItems.BLACK_ROCK_PILLAR_CAPITAL.get());
                output.accept(ModBlockItems.BLACK_ROCK_BROKEN_PILLAR.get());
                output.accept(ModBlockItems.BLACK_ROCK_RUBBLE.get());
                output.accept(ModBlockItems.BLACK_ROCK_STELE.get());
                output.accept(ModBlockItems.BLACK_ROCK_CHEST.get());
                output.accept(ModBlockItems.BLACK_ROCK_CRACKED.get());
                output.accept(ModBlockItems.BLACK_ROCK_CRACKED_STAIRS.get());
                output.accept(ModBlockItems.BLACK_ROCK_CRACKED_SLAB.get());
                output.accept(ModBlockItems.BLACK_ROCK_MOSSY.get());
                output.accept(ModBlockItems.BLACK_ROCK_MOSSY_STAIRS.get());
                output.accept(ModBlockItems.BLACK_ROCK_MOSSY_SLAB.get());
                output.accept(ModBlockItems.BLACK_ROCK_MOSSY_WALL.get());
                // CLOUD_JADE
                output.accept(ModBlockItems.CLOUD_JADE.get());
                output.accept(ModBlockItems.CLOUD_JADE_PILLAR.get());
                output.accept(ModBlockItems.CLOUD_JADE_STAIRS.get());
                output.accept(ModBlockItems.CLOUD_JADE_SLAB.get());
                output.accept(ModBlockItems.CLOUD_JADE_WALL.get());
                output.accept(ModBlockItems.CLOUD_JADE_SMOOTH.get());
                output.accept(ModBlockItems.CLOUD_JADE_SMOOTH_STAIRS.get());
                output.accept(ModBlockItems.CLOUD_JADE_SMOOTH_SLAB.get());
                output.accept(ModBlockItems.CLOUD_JADE_CARVED.get());
                output.accept(ModBlockItems.CLOUD_JADE_CARVED_STAIRS.get());
                output.accept(ModBlockItems.CLOUD_JADE_CARVED_SLAB.get());
                output.accept(ModBlockItems.CLOUD_JADE_RAILING.get());
                output.accept(ModBlockItems.CLOUD_JADE_DOOR.get());
                output.accept(ModBlockItems.CLOUD_JADE_TRAPDOOR.get());
                output.accept(ModBlockItems.CLOUD_JADE_WINDOW.get());
                output.accept(ModBlockItems.CLOUD_JADE_ROOF_TILES.get());
                output.accept(ModBlockItems.CLOUD_JADE_ROOF_STAIRS.get());
                output.accept(ModBlockItems.CLOUD_JADE_ROOF_RIDGE.get());
                output.accept(ModBlockItems.CLOUD_JADE_LAMP_STAND.get());
                output.accept(ModBlockItems.CLOUD_JADE_STONE_LAMP.get());
                output.accept(ModBlockItems.CLOUD_JADE_BEAM.get());
                output.accept(ModBlockItems.CLOUD_JADE_BRACKET.get());
                output.accept(ModBlockItems.CLOUD_JADE_EAVES.get());
                output.accept(ModBlockItems.CLOUD_JADE_EAVES_CORNER.get());
                output.accept(ModBlockItems.CLOUD_JADE_PILLAR_BASE.get());
                output.accept(ModBlockItems.CLOUD_JADE_PILLAR_CAPITAL.get());
                output.accept(ModBlockItems.CLOUD_JADE_BROKEN_PILLAR.get());
                output.accept(ModBlockItems.CLOUD_JADE_RUBBLE.get());
                output.accept(ModBlockItems.CLOUD_JADE_STELE.get());
                output.accept(ModBlockItems.CLOUD_JADE_TABLE.get());
                output.accept(ModBlockItems.CLOUD_JADE_CABINET.get());
                output.accept(ModBlockItems.CLOUD_JADE_CRACKED.get());
                output.accept(ModBlockItems.CLOUD_JADE_CRACKED_STAIRS.get());
                output.accept(ModBlockItems.CLOUD_JADE_CRACKED_SLAB.get());
                output.accept(ModBlockItems.CLOUD_JADE_MOSSY.get());
                output.accept(ModBlockItems.CLOUD_JADE_MOSSY_STAIRS.get());
                output.accept(ModBlockItems.CLOUD_JADE_MOSSY_SLAB.get());
                output.accept(ModBlockItems.CLOUD_JADE_MOSSY_WALL.get());
                // SPRING_STONE
                output.accept(ModBlockItems.SPRING_STONE.get());
                output.accept(ModBlockItems.SPRING_STONE_WATER_CHANNEL.get());
                output.accept(ModBlockItems.SPRING_STONE_PILLAR.get());
                output.accept(ModBlockItems.SPRING_STONE_STAIRS.get());
                output.accept(ModBlockItems.SPRING_STONE_SLAB.get());
                output.accept(ModBlockItems.SPRING_STONE_WALL.get());
                output.accept(ModBlockItems.SPRING_STONE_SMOOTH.get());
                output.accept(ModBlockItems.SPRING_STONE_SMOOTH_STAIRS.get());
                output.accept(ModBlockItems.SPRING_STONE_SMOOTH_SLAB.get());
                output.accept(ModBlockItems.SPRING_STONE_CARVED.get());
                output.accept(ModBlockItems.SPRING_STONE_CARVED_STAIRS.get());
                output.accept(ModBlockItems.SPRING_STONE_CARVED_SLAB.get());
                output.accept(ModBlockItems.SPRING_STONE_RAILING.get());
                output.accept(ModBlockItems.SPRING_STONE_DOOR.get());
                output.accept(ModBlockItems.SPRING_STONE_TRAPDOOR.get());
                output.accept(ModBlockItems.SPRING_STONE_WINDOW.get());
                output.accept(ModBlockItems.SPRING_STONE_ROOF_TILES.get());
                output.accept(ModBlockItems.SPRING_STONE_ROOF_STAIRS.get());
                output.accept(ModBlockItems.SPRING_STONE_ROOF_RIDGE.get());
                output.accept(ModBlockItems.SPRING_STONE_LAMP_STAND.get());
                output.accept(ModBlockItems.SPRING_STONE_STONE_LAMP.get());
                output.accept(ModBlockItems.SPRING_STONE_BEAM.get());
                output.accept(ModBlockItems.SPRING_STONE_BRACKET.get());
                output.accept(ModBlockItems.SPRING_STONE_EAVES.get());
                output.accept(ModBlockItems.SPRING_STONE_EAVES_CORNER.get());
                output.accept(ModBlockItems.SPRING_STONE_PILLAR_BASE.get());
                output.accept(ModBlockItems.SPRING_STONE_PILLAR_CAPITAL.get());
                output.accept(ModBlockItems.SPRING_STONE_BROKEN_PILLAR.get());
                output.accept(ModBlockItems.SPRING_STONE_RUBBLE.get());
                output.accept(ModBlockItems.SPRING_STONE_STELE.get());
                output.accept(ModBlockItems.SPRING_STONE_CRACKED.get());
                output.accept(ModBlockItems.SPRING_STONE_CRACKED_STAIRS.get());
                output.accept(ModBlockItems.SPRING_STONE_CRACKED_SLAB.get());
                output.accept(ModBlockItems.SPRING_STONE_MOSSY.get());
                output.accept(ModBlockItems.SPRING_STONE_MOSSY_STAIRS.get());
                output.accept(ModBlockItems.SPRING_STONE_MOSSY_SLAB.get());
                output.accept(ModBlockItems.SPRING_STONE_MOSSY_WALL.get());
                // CLOUD_FLOATING_STONE
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_PILLAR.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_STAIRS.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_SLAB.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_WALL.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_SMOOTH.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_SMOOTH_STAIRS.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_SMOOTH_SLAB.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_CARVED.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_CARVED_STAIRS.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_CARVED_SLAB.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_RAILING.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_DOOR.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_TRAPDOOR.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_WINDOW.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_ROOF_TILES.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_ROOF_STAIRS.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_ROOF_RIDGE.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_LAMP_STAND.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_STONE_LAMP.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_BEAM.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_BRACKET.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_EAVES.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_EAVES_CORNER.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_PILLAR_BASE.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_PILLAR_CAPITAL.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_BROKEN_PILLAR.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_RUBBLE.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_STELE.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_CRACKED.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_CRACKED_STAIRS.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_CRACKED_SLAB.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_MOSSY.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_MOSSY_STAIRS.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_MOSSY_SLAB.get());
                output.accept(ModBlockItems.CLOUD_FLOATING_STONE_MOSSY_WALL.get());
                // WOODEN_PLAQUE
                output.accept(ModBlockItems.WOODEN_PLAQUE.get());
                output.accept(ModBlockItems.WOODEN_PLAQUE_MEDIUM.get());
                output.accept(ModBlockItems.WOODEN_PLAQUE_LARGE.get());
                output.accept(ModBlockItems.WOODEN_PLAQUE_VERTICAL.get());
            }).build());
    public static final RegistryObject<CreativeModeTab> BEAST_MATERIALS = TABS.register("beast_materials", () ->
            CreativeModeTab.builder().title(Component.translatable("itemGroup.xiantu.beast_materials"))
                    .icon(() -> new ItemStack(ModItems.BEAST_BLOOD_SAC.get()))
                    .displayItems((parameters, output) -> {
                        for (String element : DemonCoreItem.ELEMENTS) {
                            for (int grade = 1; grade <= DemonCoreItem.MAX_GRADE; grade++) {
                                ItemStack core = new ItemStack(ModItems.DEMON_CORE.get());
                                DemonCoreItem.setData(core, grade, element);
                                output.accept(core);
                            }
                        }
                        output.accept(ModItems.BEAST_BONE.get());
                        output.accept(ModItems.BEAST_FANG.get());
                        output.accept(ModItems.BEAST_CLAW.get());
                        output.accept(ModItems.BEAST_SINEW.get());
                        output.accept(ModItems.BEAST_SCALE.get());
                        output.accept(ModItems.BEAST_HORN.get());
                        output.accept(ModItems.BEAST_VENOM_SAC.get());
                        output.accept(ModItems.BEAST_SILK.get());
                        for(int realm:new int[]{0,2,4}) for(String element:com.longju.xiantu.item.DemonCoreItem.ELEMENTS)
                            output.accept(com.longju.xiantu.item.BeastHideItem.fromRealm(realm,element));
                        output.accept(ModItems.BEAST_FEATHER.get());
                        output.accept(ModItems.BEAST_BLOOD_SAC.get());
                        output.accept(ModItems.BEAST_CARAPACE.get());
                        output.accept(ModItems.WOLF_HAIR.get());
                        output.accept(ModItems.FOX_HAIR.get());
                        output.accept(ModItems.VULTURE_FEATHER.get());
                        output.accept(ModItems.LION_MANE.get());
                        output.accept(ModItems.FALCON_FEATHER.get());
                        output.accept(ModItems.THUNDER_CROW_FEATHER.get());
                    }).build());
    public static final RegistryObject<CreativeModeTab> WOOD=TABS.register("spirit_wood",()->CreativeModeTab.builder()
        .title(Component.literal("仙途·灵木")).icon(()->new ItemStack(com.longju.xiantu.wood.SpiritWoodRegistry.item(com.longju.xiantu.wood.SpiritWoodType.QING_WEN_ELM,"sapling")))
        .displayItems((parameters,output)->{for(var type:com.longju.xiantu.wood.SpiritWoodType.values())for(String part:new String[]{"log","stripped_log","planks","leaves","sapling","stick","long_shaft","fiber"})output.accept(com.longju.xiantu.wood.SpiritWoodRegistry.item(type,part));}).build());
    public static final RegistryObject<CreativeModeTab> MEDICINE=TABS.register("medicine",()->CreativeModeTab.builder()
        .title(Component.literal("仙途·灵药")).icon(()->new ItemStack(com.longju.xiantu.medicine.MedicineRegistry.herb("herb_0058")))
        .displayItems((parameters,output)->{
            com.longju.xiantu.medicine.MedicineRegistry.HERB_ITEMS.values().forEach(item->output.accept(item.get()));
            com.longju.xiantu.medicine.MedicineRegistry.SEED_ITEMS.values().forEach(item->output.accept(item.get()));
            com.longju.xiantu.medicine.MedicineRegistry.SOIL_ITEMS.forEach(item->output.accept(item.get()));
            com.longju.xiantu.medicine.MedicineRegistry.FARMLAND_ITEMS.forEach(item->output.accept(item.get()));
        }).build());
    public static final RegistryObject<CreativeModeTab> TERRAIN=TABS.register("terrain",()->CreativeModeTab.builder()
        .title(Component.literal("仙途·地形")).icon(()->new ItemStack(com.longju.xiantu.terrain.TerrainBlockRegistry.BLOCKS_BY_ID.get("thunder_rock").get()))
        .displayItems((parameters,output)->{
            com.longju.xiantu.terrain.TerrainBlockRegistry.BLOCKS_BY_ID.values().forEach(block->output.accept(block.get()));
            com.longju.xiantu.terrain.TerrainPlantRegistry.PLANTS.values().forEach(block->output.accept(block.get()));
        }).build());
    public static final RegistryObject<CreativeModeTab> REFINING=TABS.register("refining",()->CreativeModeTab.builder()
        .title(Component.literal("仙途·炼器")).icon(()->new ItemStack(ModItems.HANXING_PICKAXE.get()))
        .displayItems((parameters,output)->{
                output.accept(ModItems.HANXING_PICKAXE.get());
                output.accept(ModItems.JIUYOU_PICKAXE.get());
                output.accept(ModItems.HUNDUN_PICKAXE.get());
                output.accept(com.longju.xiantu.refining.RefiningItems.SCRAP.get());
                output.accept(com.longju.xiantu.refining.CraftingTreatises.REFINING.get());
                com.longju.xiantu.refining.QitianMaterials.ITEMS.values().forEach(v->output.accept(v.get()));
                com.longju.xiantu.refining.FacilityRecipes.blueprints().forEach(id->output.accept(com.longju.xiantu.refining.RefiningItems.facilityDiagram(id)));
                output.accept(com.longju.xiantu.refining.SpiritFire.COMMON.get());
                com.longju.xiantu.refining.SpiritFire.WONDROUS.forEach(f->output.accept(f.get()));
                var diagrams=new java.util.HashSet<String>();
                for(var recipe:com.longju.xiantu.refining.RefiningRecipes.ALL)if(com.longju.xiantu.refining.RefiningRules.blueprintRequired(recipe.tier()) && diagrams.add(com.longju.xiantu.refining.RefiningProgress.blueprintKey(recipe.id()))){
                    var diagram=new ItemStack(com.longju.xiantu.refining.RefiningItems.BLUEPRINT.get());diagram.getOrCreateTag().putString("Recipe",recipe.id());output.accept(diagram);
                }
                com.longju.xiantu.equipment.EquipmentPreview.items().forEach(item -> output.accept(item.get()));
                com.longju.xiantu.equipment.EquipmentModelBatch.items().forEach(item -> output.accept(item.get()));
            output.accept(ModBlockItems.SMELTING_FURNACE.get());
            com.longju.xiantu.workstation.WorkstationRegistry.EQUIPMENT_ITEMS.forEach((id,item)->{if(id.contains("refining") || id.equals("artisan_forging_table"))output.accept(item.get());});
        }).build());
    public static final RegistryObject<CreativeModeTab> PILLS=TABS.register("pills",()->CreativeModeTab.builder()
        .title(Component.literal("仙途·丹药")).icon(()->new ItemStack(ModItems.BASIC_PILL.get()))
        .displayItems((parameters,output)->{
                for(var pill:com.longju.xiantu.alchemy.PillCatalog.ALL)for(int q=1;q<=5;q++)output.accept(com.longju.xiantu.alchemy.PillCatalog.stack(pill.id(),q));
                output.accept(com.longju.xiantu.alchemy.AlchemyItems.FAILED.get());
                output.accept(com.longju.xiantu.refining.CraftingTreatises.ALCHEMY.get());
                for(var recipe:com.longju.xiantu.alchemy.AlchemyRecipes.ALL)if(recipe.rank()>=4)output.accept(com.longju.xiantu.refining.RefiningItems.alchemyDiagram(recipe.id()));
                output.accept(com.longju.xiantu.alchemy.PillWaymark.BLOCK.get());
            com.longju.xiantu.workstation.WorkstationRegistry.EQUIPMENT_ITEMS.forEach((id,item)->{if(id.contains("alchemy"))output.accept(item.get());});
        }).build());
    public static final RegistryObject<CreativeModeTab> TALISMAN=TABS.register("talisman",()->CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.xiantu.talisman"))
        .icon(()->com.longju.xiantu.talisman.TalismanItems.symbol("fireball",1,1))
        .displayItems((parameters,output)->{
            output.accept(com.longju.xiantu.talisman.TalismanItems.CINNABAR.get());
            output.accept(com.longju.xiantu.mining.SpecialMineralRegistry.ORE_ITEMS.get("cinnabar").get());
            output.accept(com.longju.xiantu.talisman.TalismanItems.SPACE_SAND.get());
            com.longju.xiantu.talisman.TalismanItems.BRUSHES.forEach(item->output.accept(item.get()));
            output.accept(com.longju.xiantu.talisman.TalismanItems.BROKEN_BRUSH.get());
            output.accept(com.longju.xiantu.talisman.TalismanItems.RESIDUE.get());
            for(var tome:com.longju.xiantu.talisman.TalismanTomes.ALL)output.accept(com.longju.xiantu.talisman.TalismanItems.blueprint(tome.id()));
            output.accept(com.longju.xiantu.workstation.WorkstationRegistry.EQUIPMENT_ITEMS.get("talisman_writing_table").get());
            for(int tier=1;tier<=8;tier++)
                output.accept(com.longju.xiantu.talisman.TalismanItems.paper(tier,1,1));
            for(var definition:com.longju.xiantu.talisman.TalismanCatalog.ALL){
                for(int tier=definition.minimum();tier<=(definition.id().equals("rebirth")?3:8);tier++)
                    output.accept(com.longju.xiantu.talisman.TalismanItems.symbol(definition.id(),tier,1));
            }
        }).build());
    private ModCreativeTabs() {}
}

