package com.longju.xiantu.alchemy;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
public final class AlchemyCraftingRegression{
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        for(int mode=0;mode<=1;mode++)for(int initial=0;initial<=100;initial++){
            int temperature=initial;for(int t=0;t<40;t++)temperature=AlchemyHeat.step(temperature,mode);
            check(temperature==AlchemyHeat.target(mode),"heat converges without oscillation");
            check(AlchemyHeat.inBand(temperature,mode)&&!AlchemyHeat.inBand(temperature,1-mode),"explicit fire target");
        }
        check(!AlchemyHeat.held(59,100)&&AlchemyHeat.held(60,100),"mix fire must be held for 60 percent of stage");
        for(int rank=1;rank<=8;rank++){
            if(AlchemyRules.furnace(rank)>1)for(int lv=0;lv<=9;lv++)check(AlchemyRules.success("basic",lv,AlchemyRules.furnace(rank)-1,AlchemyRules.furnace(rank),0)==0,"insufficient furnace never advertises success");
            long spent=0;for(int t=0;t<AlchemyRules.ticks(rank);t++)spent+=AlchemyRules.debit(t,AlchemyRules.ticks(rank),AlchemyRules.energy(rank));
            check(spent==(long)AlchemyRules.energy(rank)*1000,"exact integer energy total "+rank);
            check(AlchemyRules.furnace(rank)>=1&&AlchemyRules.furnace(rank)<=6,"legal furnace");
            for(int stage=0;stage<6;stage++)check(AlchemyRules.stage(AlchemyRules.ticks(rank)*(AlchemyRules.BOUNDS[stage]+1)/100,AlchemyRules.ticks(rank))==stage,"stage boundaries");
        }
        check(AlchemyRules.energy(1)==50&&AlchemyRules.energy(8)==109350,"energy curve");
        check(AlchemyRules.success("basic",0,1,1,0)==9500&&AlchemyRules.success("breakthrough",0,1,1,0)==8500&&AlchemyRules.success("rare",0,1,1,0)==7500,"category odds");
        for(int lv=0;lv<=9;lv++)for(int furnace=1;furnace<=6;furnace++)for(int quality=0;quality<=5;quality++){
            int[] previous=null;for(int auxiliary=0;auxiliary<=600;auxiliary+=25){var weights=AlchemyRules.qualityWeights(lv,furnace,quality,quality,0,auxiliary);check(Arrays.stream(weights).sum()==20000,"half basis point conservation");check(Arrays.stream(weights).allMatch(v->v>=0),"nonnegative odds");check(weights[4]>0&&weights[4]<20000,"perfect neither forbidden nor guaranteed");if(previous!=null)check(weights[4]>=previous[4]&&weights[0]<=previous[0],"monotone auxiliary benefit");previous=weights;}
        }
        var entries=JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/xiantu/alchemy/recipes.json"))).getAsJsonArray();check(entries.size()>=48,"expanded formula catalogue");
        var formulaIds=new HashSet<String>();var products=new HashSet<String>();
        for(var recipe:AlchemyRecipes.ALL)check(recipe.knowledge().equals(recipe.rank()<=3?com.longju.xiantu.refining.CraftingTreatises.ALCHEMY_KEY:"alchemy/"+recipe.pill()),"same high-tier pill shares one diagram knowledge key");
        for(var v:entries){var r=v.getAsJsonObject();String id=r.get("id").getAsString(),product=r.has("pill")?r.get("pill").getAsString():id;check(formulaIds.add(id),"unique formula id");products.add(product);int rank=r.get("rank").getAsInt(),cores=0;var groups=new HashSet<String>();var inputs=r.getAsJsonArray("ingredients");check(inputs.size()<=8,"fits real material slots");
            for(var input:inputs){var i=input.getAsJsonObject();groups.add(i.get("group").getAsString());check(i.get("count").getAsInt()>0&&i.get("count").getAsInt()<=64,"valid stack quantity");if(!i.get("item").getAsString().equals("xiantu:demon_core"))check(i.get("count").getAsInt()>=2,"substantial herb and material quantities");if(i.get("item").getAsString().equals("xiantu:demon_core")){cores++;check(i.get("count").getAsInt()==1&&i.get("grade").getAsInt()==rank&&i.get("element").equals(r.get("element")),"one exact required core");}}
            check(cores==1&&groups.equals(Set.of("main","auxiliary","blend")),"declared primary/secondary/blend groups");
        }
        check(entries.size()>products.size(),"alternate recipes reuse pill effects without adding placeholder pills");
        var facilities=JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/xiantu/refining/facilities.json"))).getAsJsonArray();int furnaces=0,progression=0;var facilityIds=new HashSet<String>();
        for(var v:facilities){var r=v.getAsJsonObject();String id=r.get("id").getAsString();check(facilityIds.add(id),"unique facility id");int tier=r.get("tier").getAsInt();
            if(id.startsWith("array_")){progression++;check(r.get("furnace").getAsInt()==(tier==2?2:4),"formation crafting furnace gate");}
            else if(id.endsWith("_pickaxe")){progression++;check(r.get("ingredients").getAsJsonArray().size()==3,"three pickaxe material families");}
            else{furnaces++;check(r.get("furnace").getAsInt()==(r.get("upgrade").getAsBoolean()?tier-1:tier),"sequential refining and same-tier alchemy");}}
        check(furnaces==11&&progression==10,"5 upgrades + 6 alchemy furnaces + 10 progression recipes");
        System.out.println("Alchemy crafting regression PASS: "+checks);
    }
}
