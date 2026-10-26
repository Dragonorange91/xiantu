package com.longju.xiantu.worldgen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import java.util.*;
/** Index each template once; generation reads only this chunk's columns. */
public final class RuinTemplateIndex {
    public record Cell(int y,BlockState state,CompoundTag data){}
    private static final Map<StructureTemplate,RuinTemplateIndex>CACHE=new WeakHashMap<>();
    private final Map<Long,List<Cell>> columns=new HashMap<>();
    private RuinTemplateIndex(StructureTemplate template){
        var n=template.save(new CompoundTag());var palette=n.getList("palette",10);var states=new ArrayList<BlockState>();for(var p:palette)states.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),(CompoundTag)p));
        for(var t:n.getList("blocks",10)){var c=(CompoundTag)t;var p=c.getList("pos",3);columns.computeIfAbsent(net.minecraft.world.level.ChunkPos.asLong(p.getInt(0),p.getInt(2)),k->new ArrayList<>()).add(new Cell(p.getInt(1),states.get(c.getInt("state")),c.contains("nbt")?c.getCompound("nbt").copy():null));}
        for(var list:columns.values())list.sort(Comparator.comparingInt(Cell::y));
    }
    public static synchronized RuinTemplateIndex of(StructureTemplate template){return CACHE.computeIfAbsent(template,RuinTemplateIndex::new);}
    public List<Cell> column(int x,int z){return columns.getOrDefault(net.minecraft.world.level.ChunkPos.asLong(x,z),List.of());}
}
