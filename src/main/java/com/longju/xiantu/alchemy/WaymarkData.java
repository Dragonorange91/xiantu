package com.longju.xiantu.alchemy;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;
/** Ownership and world identity live in world data, independently of player inventory/capability. */
public final class WaymarkData extends SavedData {
    private UUID world=UUID.randomUUID();
    private CompoundTag owners=new CompoundTag();
    public static WaymarkData get(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(WaymarkData::load,WaymarkData::new,"xiantu_waymarks");}
    public UUID world(){return world;}
    private static WaymarkData load(CompoundTag t){var d=new WaymarkData();if(t.hasUUID("world"))d.world=t.getUUID("world");d.owners=t.getCompound("owners").copy();return d;}
    private String key(ServerLevel level,BlockPos pos){return level.dimension().location()+"/"+pos.asLong();}
    public boolean owned(ServerLevel level,BlockPos pos,UUID owner){String k=key(level,pos);return owners.hasUUID(k)&&owners.getUUID(k).equals(owner);}
    public void put(ServerLevel level,BlockPos pos,UUID owner){owners.putUUID(key(level,pos),owner);setDirty();}
    public void remove(ServerLevel level,BlockPos pos){owners.remove(key(level,pos));setDirty();}
    @Override public CompoundTag save(CompoundTag t){t.putUUID("world",world);t.put("owners",owners.copy());return t;}
}
