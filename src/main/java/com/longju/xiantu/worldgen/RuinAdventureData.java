package com.longju.xiantu.worldgen;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Server-owned instance state. The template's frozen specification survives datapack changes. */
public final class RuinAdventureData extends SavedData {
    public static final class Instance {
        public final String id,blueprint; public final BlockPos origin; public final JsonObject spec;
        public final CompoundTag state; public final AABB bounds;
        Instance(String id,String blueprint,BlockPos origin,JsonObject spec,CompoundTag state){
            this.id=id;this.blueprint=blueprint;this.origin=origin;this.spec=spec;this.state=state;
            var s=spec.getAsJsonArray("size");bounds=new AABB(origin,origin.offset(s.get(0).getAsInt(),s.get(1).getAsInt(),s.get(2).getAsInt()));
        }
        public boolean boss(){return spec.has("bossPlan")&&!spec.get("bossPlan").isJsonNull();}
        public boolean defeated(){return state.getBoolean("Defeated");}
        public BlockPos absolute(JsonArray a){return origin.offset(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());}
        public AABB arena(){var a=spec.getAsJsonObject("bossPlan").getAsJsonArray("bounds");return new AABB(origin.getX()+a.get(0).getAsInt(),origin.getY()+a.get(1).getAsInt(),origin.getZ()+a.get(2).getAsInt(),origin.getX()+a.get(3).getAsInt()+1,origin.getY()+a.get(4).getAsInt()+1,origin.getZ()+a.get(5).getAsInt()+1);}
        public boolean waveDone(String wave){return state.getCompound("Waves").getCompound(wave).getBoolean("Done");}
        public boolean guardiansDone(){for(var e:spec.getAsJsonArray("encounters"))if(!waveDone(e.getAsJsonObject().get("id").getAsString()))return false;return true;}
    }
    private final Map<String,Instance> instances=new LinkedHashMap<>();
    private final Map<Long,List<Instance>> chunks=new HashMap<>();
    public static RuinAdventureData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(RuinAdventureData::load,RuinAdventureData::new,"xiantu_ruin_adventures");}
    public Instance find(String id){return instances.get(id);}
    public Collection<Instance> all(){return Collections.unmodifiableCollection(instances.values());}
    public List<Instance> at(BlockPos p){return chunks.getOrDefault(net.minecraft.world.level.ChunkPos.asLong(p.getX()>>4,p.getZ()>>4),List.of());}
    private void index(Instance i){for(int x=((int)i.bounds.minX)>>4;x<=((int)i.bounds.maxX-1)>>4;x++)for(int z=((int)i.bounds.minZ)>>4;z<=((int)i.bounds.maxZ-1)>>4;z++)chunks.computeIfAbsent(net.minecraft.world.level.ChunkPos.asLong(x,z),k->new ArrayList<>()).add(i);}
    public Instance ensure(String id,String blueprint,BlockPos origin,JsonObject spec){
        var old=instances.get(id);if(old!=null){if(!old.origin.equals(origin)||!old.blueprint.equals(blueprint))throw new IllegalArgumentException("Copied ruin identity");return old;}
        var i=new Instance(id,blueprint,origin.immutable(),spec.deepCopy(),new CompoundTag());instances.put(id,i);index(i);setDirty();return i;
    }
    private static RuinAdventureData load(CompoundTag tag){var d=new RuinAdventureData();for(var t:tag.getList("Instances",Tag.TAG_COMPOUND)){
        var n=(CompoundTag)t;var i=new Instance(n.getString("Id"),n.getString("Blueprint"),BlockPos.of(n.getLong("Origin")),JsonParser.parseString(n.getString("Spec")).getAsJsonObject(),n.getCompound("Progress"));d.instances.put(i.id,i);d.index(i);
    }return d;}
    @Override public CompoundTag save(CompoundTag tag){var list=new ListTag();for(var i:instances.values()){
        var n=new CompoundTag();n.putString("Id",i.id);n.putString("Blueprint",i.blueprint);n.putLong("Origin",i.origin.asLong());n.putString("Spec",i.spec.toString());n.put("Progress",i.state.copy());list.add(n);
    }tag.put("Instances",list);return tag;}
    public boolean claim(Instance i,String chest){var claims=i.state.getCompound("Claims");if(claims.getBoolean(chest))return false;claims.putBoolean(chest,true);i.state.put("Claims",claims);setDirty();return true;}
}
