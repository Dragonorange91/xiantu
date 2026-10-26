package com.longju.xiantu.refining;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** A closed catalogue: the client can select an index, never invent an output or cost. */
public final class FacilityRecipes {
    public record Recipe(String id,String name,String output,int tier,int furnace,int realm,
                         boolean upgrade,List<RefiningRecipes.Input> inputs) {
        public String knowledge(){return "facility/"+(id.startsWith("array_rune_brick_")||id.startsWith("array_conduit_pillar_")?"array_structure_"+id.charAt(id.length()-1):id);}
        public int count(){return id.startsWith("array_")&&!id.startsWith("array_eye_")?4:1;}
        public RefiningRecipes.Recipe allocationRecipe(){
            return new RefiningRecipes.Recipe(id,name,output,tier,"facility","","",0,furnace,0,10,inputs);
        }
    }
    public static final List<Recipe> ALL=load();
    private static List<Recipe> load(){
        try(var in=FacilityRecipes.class.getResourceAsStream("/data/xiantu/refining/facilities.json")){
            if(in==null)throw new IllegalStateException("Missing facility catalogue");
            var out=new ArrayList<Recipe>();
            for(var v:JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonArray()){
                var n=v.getAsJsonObject();var inputs=new ArrayList<RefiningRecipes.Input>();
                for(var ingredient:n.getAsJsonArray("ingredients")){
                    var i=ingredient.getAsJsonObject();inputs.add(new RefiningRecipes.Input(i.get("item").getAsString(),i.get("count").getAsInt(),"",0,"",""));
                }
                out.add(new Recipe(n.get("id").getAsString(),n.get("name").getAsString(),n.get("output").getAsString(),n.get("tier").getAsInt(),n.get("furnace").getAsInt(),n.get("realm").getAsInt(),n.get("upgrade").getAsBoolean(),List.copyOf(inputs)));
            }
            if(out.stream().map(Recipe::id).distinct().count()!=out.size())throw new IllegalStateException("Duplicate facility recipe");
            return List.copyOf(out);
        }catch(Exception e){throw new IllegalStateException("Invalid facility catalogue",e);}
    }
    public static Recipe find(String id){return ALL.stream().filter(r->r.id().equals(id)).findFirst().orElse(null);}
    public static String label(String id){
        if(id.equals("refining_station_1"))return "一阶砺锋炉设施图";
        if(id.equals("smelting_furnace"))return "熔炼炉设施图";
        if(id.equals("forging_table"))return "锻打台设施图";
        if(id.equals("jiuyou_pickaxe"))return "九幽镐器方";
        if(id.equals("hundun_pickaxe"))return "混沌镐器方";
        if(id.matches("array_structure_[23]"))return id.charAt(id.length()-1)+"级聚灵阵结构工艺图";
        var r=find(id);return r==null?"":r.name()+"设施图";
    }
    public static List<String> blueprints(){var ids=new java.util.LinkedHashSet<String>();ids.add("refining_station_1");ids.add("forging_table");ids.add("smelting_furnace");ALL.forEach(r->ids.add(r.knowledge().substring(9)));return List.copyOf(ids);}
    public static boolean isMaterial(net.minecraft.world.item.ItemStack s){return ALL.stream().flatMap(r->r.inputs().stream()).anyMatch(i->i.matches(s));}
    private FacilityRecipes(){}
}
