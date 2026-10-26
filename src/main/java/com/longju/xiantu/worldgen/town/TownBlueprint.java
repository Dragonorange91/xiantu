package com.longju.xiantu.worldgen.town;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import java.util.*;

/** Cached column index over the actual datapack template; no repeated 80k-block scan per chunk. */
final class TownBlueprint {
    record Cell(int y,BlockState state,CompoundTag data){}
    private static final Map<StructureTemplate,TownBlueprint> CACHE=new WeakHashMap<>();
    private final List<Cell>[] columns;
    @SuppressWarnings("unchecked") private TownBlueprint(StructureTemplate template){
        columns=(List<Cell>[])new List<?>[TownLayout.WIDTH*TownLayout.DEPTH];for(int i=0;i<columns.length;i++)columns[i]=new ArrayList<>();
        CompoundTag tag=template.save(new CompoundTag());var palette=tag.getList("palette",10);var states=new ArrayList<BlockState>();
        for(var value:palette)states.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),(CompoundTag)value));
        for(var value:tag.getList("blocks",10)){
            var cell=(CompoundTag)value;var p=cell.getList("pos",3);int x=p.getInt(0),y=p.getInt(1),z=p.getInt(2);
            if(x>=0&&x<TownLayout.WIDTH&&z>=0&&z<TownLayout.DEPTH)columns[z*TownLayout.WIDTH+x].add(new Cell(y,states.get(cell.getInt("state")),cell.contains("nbt")?cell.getCompound("nbt").copy():null));
        }
        for(var list:columns)list.sort(Comparator.comparingInt(Cell::y));
    }
    static synchronized TownBlueprint of(StructureTemplate template){return CACHE.computeIfAbsent(template,TownBlueprint::new);}
    List<Cell> column(int x,int z){return x>=0&&x<TownLayout.WIDTH&&z>=0&&z<TownLayout.DEPTH?columns[z*TownLayout.WIDTH+x]:List.of();}
}
