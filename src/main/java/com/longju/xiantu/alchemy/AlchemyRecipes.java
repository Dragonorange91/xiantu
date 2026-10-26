package com.longju.xiantu.alchemy;
import com.google.gson.*;
import com.longju.xiantu.refining.*;
import com.longju.xiantu.item.DemonCoreItem;
import net.minecraft.world.item.ItemStack;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class AlchemyRecipes {
    public record Recipe(String id,String name,String pill,int rank,String element,String kind,List<RefiningRecipes.Input> inputs,List<String> groups,int warmFire,int blendFire){
        public String knowledge(){return rank<=3?CraftingTreatises.ALCHEMY_KEY:"alchemy/"+pill;}
        public RefiningRecipes.Recipe allocationRecipe(){return new RefiningRecipes.Recipe(id,name,"xiantu:"+id,rank,"alchemy","","",Math.max(0,rank-1),AlchemyRules.furnace(rank),AlchemyRules.energy(rank),AlchemyRules.ticks(rank)/20,inputs);}
        public ItemStack preview(){return PillCatalog.stack(pill,1);}
    }
    public static final List<Recipe> ALL=load();
    public static Recipe find(String id){return ALL.stream().filter(r->r.id().equals(id)).findFirst().orElse(null);}
    private static List<Recipe> load(){
        try(var in=AlchemyRecipes.class.getResourceAsStream("/data/xiantu/alchemy/recipes.json")){
            if(in==null)throw new IllegalStateException("Missing alchemy recipes");var out=new ArrayList<Recipe>();
            for(var v:JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonArray()){
                var n=v.getAsJsonObject();var inputs=new ArrayList<RefiningRecipes.Input>();var groups=new ArrayList<String>();
                for(var value:n.getAsJsonArray("ingredients")){
                    var i=value.getAsJsonObject();String group=i.get("group").getAsString();
                    if(!List.of("main","auxiliary","blend").contains(group))throw new IllegalStateException("Missing explicit herb group");groups.add(group);
                    if(i.get("count").getAsInt()<1||i.get("count").getAsInt()>64)throw new IllegalStateException("Invalid ingredient count");
                    inputs.add(new RefiningRecipes.Input(i.get("item").getAsString(),i.get("count").getAsInt(),"",i.has("grade")?i.get("grade").getAsInt():0,i.has("element")?i.get("element").getAsString():"",""));
                }
                if(inputs.isEmpty()||inputs.size()>8)throw new IllegalStateException("Recipe exceeds material slots");
                var heat=n.getAsJsonObject("heat");int warm=heat.get("warm").getAsInt(),blend=heat.get("blend").getAsInt();
                if((warm!=0&&warm!=1)||(blend!=0&&blend!=1))throw new IllegalStateException("Invalid explicit heat recipe");
                String id=n.get("id").getAsString(),pill=n.has("pill")?n.get("pill").getAsString():id;
                PillCatalog.get(pill);
                out.add(new Recipe(id,n.get("name").getAsString(),pill,n.get("rank").getAsInt(),n.get("element").getAsString(),n.get("kind").getAsString(),List.copyOf(inputs),List.copyOf(groups),warm,blend));
            }
            if(out.size()<48||out.stream().map(Recipe::id).distinct().count()!=out.size())throw new IllegalStateException("Expected at least 48 distinct recipes");return List.copyOf(out);
        }catch(Exception e){throw new IllegalStateException("Invalid alchemy recipes",e);}
    }
    public static boolean isMaterial(ItemStack s){return ALL.stream().flatMap(r->r.inputs().stream()).anyMatch(i->i.matches(s));}
    public record Auxiliary(int count,int success,int qualityHalf){}
    public static Auxiliary auxiliary(Recipe r,net.minecraft.world.Container slots){
        int count=0,success=0,quality=0;
        for(int i=0;i<slots.getContainerSize();i++){
            var s=slots.getItem(i);if(s.isEmpty())continue;
            if(!(s.getItem() instanceof DemonCoreItem)||!DemonCoreItem.element(s).equals(r.element()))return null;
            int grade=DemonCoreItem.grade(s);count+=s.getCount();if(count>8)return null;
            success+=s.getCount()*AlchemyRules.auxiliarySuccess(grade,r.rank());quality+=s.getCount()*AlchemyRules.auxiliaryQualityHalf(grade,r.rank());
        }return new Auxiliary(count,Math.min(1200,success),Math.min(600,quality));
    }
    private AlchemyRecipes(){}
}
