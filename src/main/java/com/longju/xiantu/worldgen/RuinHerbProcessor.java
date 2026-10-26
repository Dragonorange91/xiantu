package com.longju.xiantu.worldgen;
import com.mojang.serialization.Codec;
import com.longju.xiantu.medicine.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import java.util.*;
public final class RuinHerbProcessor extends StructureProcessor {
    public static final RuinHerbProcessor INSTANCE=new RuinHerbProcessor();
    public static final Codec<RuinHerbProcessor> CODEC=Codec.unit(()->INSTANCE);
    public static final int[] WEIGHTS={30,24,18,12,8,4,3,1};
    public static int tier(int roll){int sum=0;for(int i=0;i<8;i++){sum+=WEIGHTS[i];if(roll<sum)return i+1;}return 8;}
    @Override public StructureTemplate.StructureBlockInfo processBlock(LevelReader level,BlockPos origin,BlockPos pivot,
            StructureTemplate.StructureBlockInfo raw,StructureTemplate.StructureBlockInfo placed,StructurePlaceSettings settings){
        if(!(placed.state().getBlock() instanceof MedicinePlantBlock))return placed;
        // Columns are written bottom-up in both placement paths. A misplaced
        // marker must not leave a plant floating on irrigation water or paving.
        if(!placed.state().canSurvive(level,placed.pos()))
            return new StructureTemplate.StructureBlockInfo(placed.pos(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),null);
        var random=settings.getRandom(placed.pos());int tier=tier(random.nextInt(100));
        var candidates=MedicineCatalog.HERBS.stream().filter(d->d.tier()==tier && d.wildPlant()
            && MedicineRegistry.WILD_PLANTS.containsKey(d.registryId())).toList();
        if(candidates.isEmpty())throw new IllegalStateException("Missing ruin herb tier "+tier);
        var selected=candidates.get(random.nextInt(candidates.size()));
        return new StructureTemplate.StructureBlockInfo(placed.pos(),MedicineRegistry.WILD_PLANTS.get(selected.registryId()).get().defaultBlockState(),null);
    }
    @Override protected StructureProcessorType<?> getType(){return RuinBlueprints.HERBS.get();}
}
