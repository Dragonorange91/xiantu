package com.longju.xiantu.worldgen;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Coordinate identity and committed encounter ledger survive reload and block replacement. */
public final class RuinInstanceData extends SavedData {
    private final Set<String> placed=new HashSet<>(),triggered=new HashSet<>();
    public static RuinInstanceData get(ServerLevel l){return l.getDataStorage().computeIfAbsent(RuinInstanceData::load,RuinInstanceData::new,"xiantu_ruin_instances");}
    private static RuinInstanceData load(CompoundTag n){var d=new RuinInstanceData();for(Tag t:n.getList("Placed",Tag.TAG_STRING))d.placed.add(t.getAsString());for(Tag t:n.getList("Triggered",Tag.TAG_STRING))d.triggered.add(t.getAsString());return d;}
    @Override public CompoundTag save(CompoundTag n){ListTag p=new ListTag(),t=new ListTag();placed.forEach(s->p.add(StringTag.valueOf(s)));triggered.forEach(s->t.add(StringTag.valueOf(s)));n.put("Placed",p);n.put("Triggered",t);return n;}
    public boolean placed(String id){return placed.contains(id);}
    public void place(String id){placed.add(id);setDirty();}
    public boolean triggered(String id){return triggered.contains(id);}
    public boolean trigger(String id){boolean changed=triggered.add(id);if(changed)setDirty();return changed;}
}
