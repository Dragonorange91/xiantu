package com.longju.xiantu.registry;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.formation.FormationCoreBlockEntity;
import com.longju.xiantu.building.StoneChestBlockEntity;
import com.longju.xiantu.building.JadeCabinetBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, XiantuMod.MOD_ID);
    public static final RegistryObject<BlockEntityType<com.longju.xiantu.building.QunyouFigurineEntity>> QUNYOU_FIGURINE = BLOCK_ENTITIES.register("qunyou_figurine", () -> BlockEntityType.Builder.of(com.longju.xiantu.building.QunyouFigurineEntity::new, ModBlocks.QUNYOU_FIGURINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<FormationCoreBlockEntity>> FORMATION_CORE =
            BLOCK_ENTITIES.register("formation_core", () ->
                    BlockEntityType.Builder.of(FormationCoreBlockEntity::new,
                            ModBlocks.FORMATION_CORE.get(), ModBlocks.FORMATION_CORE_2.get(), ModBlocks.FORMATION_CORE_3.get()).build(null));

    public static final RegistryObject<BlockEntityType<StoneChestBlockEntity>> BLACK_ROCK_CHEST =
            BLOCK_ENTITIES.register("black_rock_chest", () ->
                    BlockEntityType.Builder.of(StoneChestBlockEntity::new, ModBlocks.BLACK_ROCK_CHEST.get()).build(null));

    public static final RegistryObject<BlockEntityType<JadeCabinetBlockEntity>> CLOUD_JADE_CABINET =
            BLOCK_ENTITIES.register("cloud_jade_cabinet", () ->
                    BlockEntityType.Builder.of(JadeCabinetBlockEntity::new, ModBlocks.CLOUD_JADE_CABINET.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.longju.xiantu.material.AppraisalBlockEntity>> APPRAISAL = BLOCK_ENTITIES.register("appraisal_altar", () -> BlockEntityType.Builder.of(com.longju.xiantu.material.AppraisalBlockEntity::new,ModBlocks.APPRAISAL.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.longju.xiantu.building.RewardChestBlockEntity>> RUIN_REWARD_CHEST = BLOCK_ENTITIES.register("ruin_reward_chest", () -> BlockEntityType.Builder.of(com.longju.xiantu.building.RewardChestBlockEntity::new, ModBlocks.RUIN_REWARD_CHEST.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.longju.xiantu.worldgen.RuinEncounterBlockEntity>> RUIN_ENCOUNTER = BLOCK_ENTITIES.register("ruin_encounter_seal", () -> BlockEntityType.Builder.of(com.longju.xiantu.worldgen.RuinEncounterBlockEntity::new, ModBlocks.RUIN_ENCOUNTER.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.longju.xiantu.refining.RefiningBlockEntity>> REFINING = BLOCK_ENTITIES.register("refining", () -> BlockEntityType.Builder.of(com.longju.xiantu.refining.RefiningBlockEntity::new,
        com.longju.xiantu.workstation.WorkstationRegistry.EQUIPMENT.entrySet().stream().filter(e -> com.longju.xiantu.refining.RefiningBlockEntity.FURNACES.contains(e.getKey()) || com.longju.xiantu.refining.RefiningBlockEntity.ALCHEMY.contains(e.getKey()) || e.getKey().equals("artisan_forging_table")).map(e -> e.getValue().get()).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final RegistryObject<BlockEntityType<com.longju.xiantu.refining.WorkstationPartBlockEntity>> REFINING_PART = BLOCK_ENTITIES.register("refining_part", () -> BlockEntityType.Builder.of(com.longju.xiantu.refining.WorkstationPartBlockEntity::new,
        com.longju.xiantu.workstation.WorkstationRegistry.EQUIPMENT.entrySet().stream().filter(e -> com.longju.xiantu.refining.RefiningBlockEntity.FURNACES.contains(e.getKey()) || com.longju.xiantu.refining.RefiningBlockEntity.ALCHEMY.contains(e.getKey()) || e.getKey().equals("artisan_forging_table")).map(e -> e.getValue().get()).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final RegistryObject<BlockEntityType<com.longju.xiantu.smelting.SmeltingBlockEntity>> SMELTING = BLOCK_ENTITIES.register("smelting_furnace", () -> BlockEntityType.Builder.of(com.longju.xiantu.smelting.SmeltingBlockEntity::new, ModBlocks.SMELTING_FURNACE.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.longju.xiantu.talisman.TalismanTableEntity>> TALISMAN=BLOCK_ENTITIES.register("talisman_table",()->BlockEntityType.Builder.of(com.longju.xiantu.talisman.TalismanTableEntity::new,com.longju.xiantu.workstation.WorkstationRegistry.EQUIPMENT.get("talisman_writing_table").get()).build(null));
    private ModBlockEntities() {
    }
}
