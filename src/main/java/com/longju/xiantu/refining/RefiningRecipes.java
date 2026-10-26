package com.longju.xiantu.refining;
import com.google.gson.*;
import com.longju.xiantu.item.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class RefiningRecipes {
    public record Input(String item,int count,String label,int grade,String element,String hide){
        public boolean matches(ItemStack stack){
            if(stack.isEmpty()||!item.equals(String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()))))return false;
            if(grade>0&&(stack.getItem() instanceof DemonCoreItem?DemonCoreItem.grade(stack):BeastMaterialItem.grade(stack))!=grade)return false;
            if(!element.isEmpty()&&!element.equals(stack.getItem() instanceof DemonCoreItem?DemonCoreItem.element(stack):BeastMaterialItem.element(stack)))return false;
            return hide.isEmpty()||hide.equals(BeastHideItem.tier(stack));
        }
        public ItemStack example(){var s=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(item)),count);if(grade>0){if(s.getItem() instanceof DemonCoreItem)DemonCoreItem.setData(s,grade,element);else BeastMaterialItem.setData(s,grade,element);}if(!hide.isEmpty())s.getOrCreateTag().putString("hide_tier",hide);return s;}
    }
    public record Recipe(String id,String name,String output,int tier,String kind,String slot,String theme,int profession,int furnace,int energy,int seconds,List<Input> inputs){
        public ItemStack result(){return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(output)));}
    }
    /** Diagram metadata remains readable even when crafting is not yet enabled. */
    public record Diagram(String id,String name,int tier){}
    private record Catalogue(List<Recipe> recipes,Map<String,Diagram> diagrams,Map<String,Recipe> definitions){}
    private static final Catalogue CATALOGUE=load();
    public static final List<Recipe> ALL=CATALOGUE.recipes();
    public static Diagram diagram(String id){return CATALOGUE.diagrams().get(id);}
    private static final Map<String,Recipe> BY_ID=ALL.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Recipe::id,r->r));
    public static Recipe find(String id){return BY_ID.get(id);}
    public static Recipe definition(String id){return CATALOGUE.definitions().get(id);}
    private static final Map<String,Recipe> BASE_EQUIPMENT=CATALOGUE.definitions().values().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Recipe::output,r->r,(a,b)->a.tier()<=b.tier()?a:b));
    public static Recipe baseEquipment(String output){return BASE_EQUIPMENT.get(output);}
    public static List<Recipe> tier(int tier){return ALL.stream().filter(r->r.tier==tier).toList();}
    private static String str(JsonObject n,String key){return n.has(key)?n.get(key).getAsString():"";}
    private static int integer(JsonObject n,String key){return n.has(key)?n.get(key).getAsInt():0;}
    private static Catalogue load(){
        try(var in=RefiningRecipes.class.getResourceAsStream("/data/xiantu/refining/catalog.json")){
            if(in==null)throw new IllegalStateException("Missing refining catalogue");
            var root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();var out=new ArrayList<Recipe>();var diagrams=new LinkedHashMap<String,Diagram>();var definitions=new LinkedHashMap<String,Recipe>();
            for(var value:root.getAsJsonArray("recipes")){var r=value.getAsJsonObject();
                if(r.get("enabled").getAsBoolean()||str(r,"theme").equals("齐天"))diagrams.put(str(r,"id"),new Diagram(str(r,"id"),str(r,"name"),integer(r,"tier")));
                if(!r.get("enabled").getAsBoolean()&&!str(r,"theme").equals("齐天"))continue;var inputs=new ArrayList<Input>();
                for(var v:r.getAsJsonArray("ingredients")){var n=v.getAsJsonObject();inputs.add(new Input(str(n,"item"),integer(n,"count"),str(n,"label"),integer(n,"grade"),str(n,"element"),str(n,"hide")));}
                var recipe=new Recipe(str(r,"id"),str(r,"name"),str(r,"output"),integer(r,"tier"),str(r,"kind"),str(r,"slot"),str(r,"theme"),integer(r,"profession"),integer(r,"furnace"),integer(r,"energy"),integer(r,"seconds"),List.copyOf(inputs));definitions.put(recipe.id(),recipe);if(r.get("enabled").getAsBoolean())out.add(recipe);
            }return new Catalogue(List.copyOf(out),Map.copyOf(diagrams),Map.copyOf(definitions));
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    public static boolean isMaterial(ItemStack stack){
        String id=String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));
        return ALL.stream().anyMatch(r->r.inputs().stream().anyMatch(i->i.item().equals(id)));
    }
    public static boolean uniformQualities(List<ItemStack> slots){
        var qualities=new HashMap<Item,Integer>();
        for(var s:slots)if(!s.isEmpty()&&!(s.getItem() instanceof DemonCoreItem)){
            int q=com.longju.xiantu.material.MaterialQuality.get(s);var old=qualities.putIfAbsent(s.getItem(),q);if(old!=null&&old!=q)return false;
        }return true;
    }
    /** Allocate on copies first, so split stacks and failed validation cannot partly consume inputs. */
    public static int[] allocate(Recipe recipe,List<ItemStack> slots){
        if(!uniformQualities(slots))return null;
        int[] used=new int[slots.size()];
        for(var input:recipe.inputs){int left=input.count;for(int i=0;i<slots.size()&&left>0;i++)if(input.matches(slots.get(i))){int n=Math.min(left,slots.get(i).getCount()-used[i]);used[i]+=n;left-=n;}if(left>0)return null;}
        return used;
    }
    /** Only allocated inputs count: extra inventory stacks and neutral metals cannot lower or raise quality. */
    public static int materialQuality(List<ItemStack> slots,int[] allocated){
        if(allocated==null||allocated.length!=slots.size())throw new IllegalArgumentException("Missing input allocation");
        boolean found=false;int minimum=5;
        for(int i=0;i<slots.size();i++)if(allocated[i]>0&&!(slots.get(i).getItem() instanceof DemonCoreItem)&&com.longju.xiantu.material.MaterialQuality.eligible(slots.get(i))){
            found=true;int q=com.longju.xiantu.material.MaterialQuality.get(slots.get(i));if(q==0)return 0;minimum=Math.min(minimum,q);
        }
        return found?minimum:0;
    }
    private RefiningRecipes(){}
}
