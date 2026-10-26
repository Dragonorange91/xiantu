package com.longju.xiantu.entity;

import com.google.gson.*;
import com.longju.xiantu.XiantuMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Server datapack configuration, atomically validated before use; bosses are excluded. */
@Mod.EventBusSubscriber(modid=XiantuMod.MOD_ID)
public final class BeastEcology extends SimpleJsonResourceReloadListener {
    public record Species(String id,String element,boolean elite,String time,String mode,
                          boolean daylight,boolean shore,List<String> biomes) {
        public boolean matches(Holder<Biome> biome){return biomes.stream().anyMatch(s->matchesSelector(biome,s));}
    }
    public record Profile(List<Species> species,List<double[]> weights,Map<String,List<String>> featured,Map<String,List<String>> rare){}
    private static volatile Profile current;
    public BeastEcology(){super(new Gson(),"beast_ecology");}
    @SubscribeEvent public static void reload(AddReloadListenerEvent e){e.addListener(new BeastEcology());}
    public static Profile profile(){return current;}
    public static Species species(String id){var p=current;return p==null?null:p.species.stream().filter(s->s.id.equals(id)).findFirst().orElse(null);}
    public static boolean matchesSelector(Holder<Biome> biome,String s){return s.startsWith("#")
        ?biome.is(TagKey.create(Registries.BIOME,new ResourceLocation(s.substring(1)))):biome.is(new ResourceLocation(s));}
    public static int tier(ServerLevel level,BlockPos pos,String element){
        var p=current;if(p==null)return 0;var biome=level.getBiome(pos);
        if(element.equals("thunder")&&biome.is(new ResourceLocation("xiantu:thunder_wastes"))){
            // Only loaded biome samples: edge or unknown neighbors never upgrade to core.
            for(var q:List.of(pos.offset(32,0,0),pos.offset(-32,0,0),pos.offset(0,0,32),pos.offset(0,0,-32)))
                if(!level.hasChunkAt(q)||!level.getBiome(q).is(new ResourceLocation("xiantu:thunder_wastes")))return 1;
            return 2;
        }
        if(p.rare.getOrDefault(element,List.of()).stream().anyMatch(s->matchesSelector(biome,s)))return 2;
        return p.featured.getOrDefault(element,List.of()).stream().anyMatch(s->matchesSelector(biome,s))?1:0;
    }
    @Override protected void apply(Map<ResourceLocation,JsonElement> json,ResourceManager rm,ProfilerFiller profiler){
        var root=json.get(new ResourceLocation("xiantu:overworld"));
        if(root==null)throw new JsonParseException("Missing xiantu:beast_ecology/overworld.json");
        current=parse(root.getAsJsonObject());
    }
    public static Profile parse(JsonObject root){
        List<Species> species=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(var entry:root.getAsJsonArray("species")){
            var o=entry.getAsJsonObject();String id=o.get("id").getAsString();
            if(!BeastResourceRules.SPECIES.contains(id)||!seen.add(id))throw new JsonParseException("Unknown/duplicate beast "+id);
            String time=o.get("time").getAsString(),mode=o.get("mode").getAsString(),element=o.get("element").getAsString();
            if(!Set.of("day","night","twilight","any").contains(time)||!Set.of("land","cave","fly","water").contains(mode)
                ||!Set.of("metal","wood","water","fire","earth","thunder").contains(element))throw new JsonParseException("Invalid ecology "+id);
            var biomes=strings(o.getAsJsonArray("biomes"));if(biomes.isEmpty())throw new JsonParseException("Empty habitat "+id);
            biomes.forEach(s->new ResourceLocation(s.startsWith("#")?s.substring(1):s));
            species.add(new Species(id,element,o.get("elite").getAsBoolean(),time,mode,o.get("daylight").getAsBoolean(),o.get("shore").getAsBoolean(),biomes));
        }
        if(!seen.equals(BeastResourceRules.SPECIES))throw new JsonParseException("Ecology must define all 30 beasts");
        List<double[]> weights=new ArrayList<>();for(var a:root.getAsJsonArray("realm_weights")){
            if(a.getAsJsonArray().size()!=9)throw new JsonParseException("Need nine realms");double[] w=new double[9];double sum=0;
            for(int i=0;i<9;i++){w[i]=a.getAsJsonArray().get(i).getAsDouble();if(!Double.isFinite(w[i])||w[i]<=0)throw new JsonParseException("Invalid realm weight");sum+=w[i];}
            if(Math.abs(sum-100)>.0001)throw new JsonParseException("Realm weights must total 100");weights.add(w);
        }
        if(weights.size()!=3)throw new JsonParseException("Need normal/featured/rare weights");
        return new Profile(List.copyOf(species),List.copyOf(weights),selectors(root.getAsJsonObject("featured")),selectors(root.getAsJsonObject("rare")));
    }
    private static List<String> strings(JsonArray a){List<String> out=new ArrayList<>();for(var v:a)out.add(v.getAsString());return List.copyOf(out);}
    private static Map<String,List<String>> selectors(JsonObject o){var out=new HashMap<String,List<String>>();o.entrySet().forEach(e->{var values=strings(e.getValue().getAsJsonArray());values.forEach(s->new ResourceLocation(s.startsWith("#")?s.substring(1):s));out.put(e.getKey(),values);});return Map.copyOf(out);}
}
