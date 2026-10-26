package com.longju.xiantu.worldgen.town;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.*;

public final class TownWorldgen {
    public static final DeferredRegister<StructureType<?>> STRUCTURES=DeferredRegister.create(Registries.STRUCTURE_TYPE,"xiantu");
    public static final DeferredRegister<StructurePieceType> PIECES=DeferredRegister.create(Registries.STRUCTURE_PIECE,"xiantu");
    public static final RegistryObject<StructureType<CultivationTownStructure>> TOWN=STRUCTURES.register("cultivation_town",()->()->CultivationTownStructure.CODEC);
    public static final RegistryObject<StructurePieceType> TILE=PIECES.register("town_tile",()->TownTerrainPiece::new);
    public static final RegistryObject<StructureType<com.longju.xiantu.worldgen.RuinStructure>> RUIN=STRUCTURES.register("adventure_ruin",()->()->com.longju.xiantu.worldgen.RuinStructure.CODEC);
    public static final RegistryObject<StructurePieceType> RUIN_TILE=PIECES.register("ruin_tile",()->com.longju.xiantu.worldgen.RuinStructurePiece::new);
    private TownWorldgen(){}
}
