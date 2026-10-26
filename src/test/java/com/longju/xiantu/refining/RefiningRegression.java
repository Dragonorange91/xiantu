package com.longju.xiantu.refining;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Economy and probability boundaries, independent of a running client. */
public final class RefiningRegression {
    private static int checks;
    private static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    public static void main(String[] args)throws Exception{
        check(ArmorBalance.protection(1,1)==0,"tier-one ordinary keeps diamond baseline");
        for(int tier=1;tier<=7;tier++)for(int quality=1;quality<=5;quality++){
            double ward=4*ArmorBalance.protection(tier,quality);
            check(ward>=0&&ward<=.320001,"bounded full-set spiritual protection");
            if(tier>1)check(ward>4*ArmorBalance.protection(tier-1,quality),"each rank improves capped armor defense");
            if(quality>1)check(ward>4*ArmorBalance.protection(tier,quality-1),"each quality improves capped armor defense");
        }
        int[][] baguaCenters={{0,-45},{32,-32},{44,0},{32,32},{0,45},{-32,32},{-44,0},{-32,-32}};
        for(int i=0;i<baguaCenters.length;i++){
            check(WorkstationLayout.materialX(i)+8-WorkstationLayout.BAGUA_X==baguaCenters[i][0],"material slot centered on bagua cell x "+i);
            check(WorkstationLayout.materialY(i)+8-WorkstationLayout.BAGUA_Y==baguaCenters[i][1],"material slot centered on bagua cell y "+i);
        }
        check(WorkstationLayout.WIDTH>=550&&WorkstationLayout.HEIGHT>=350,"expanded cultivation workstation canvas");
        check(RefiningRules.successPercent(0,1,1)==90,"eligible furnace retains base success");
        for(int required=2;required<=6;required++)for(int level=0;level<=9;level++)check(RefiningRules.successPercent(level,required-1,required)==0,"insufficient furnace never advertises success");
        check(RefiningRules.energy(1)==100&&RefiningRules.energy(7)==72900,"one energy rate without furnace-fire premium");
        for(int target=1;target<=8;target++){
            check(RefiningRules.fireQualityBonus(0,target)==0,"ordinary flame no bonus");
            for(int rank=1;rank<=6;rank++){
                int bonus=RefiningRules.fireQualityBonus(rank,target);
                check(bonus<=rank*150&&bonus>=0,"bounded fire bonus");
                if(target>rank)check(bonus<=RefiningRules.fireQualityBonus(rank,target-1)/4,"outclassed fire attenuates");
                for(int lv=0;lv<=9;lv++)for(int furnace=1;furnace<=6;furnace++)for(int quality=0;quality<=5;quality++){
                    int[] base=RefiningRules.qualityWeights(lv,furnace,quality,quality),with=RefiningRules.qualityWeights(lv,furnace,quality,quality,bonus);
                    check(Arrays.stream(with).sum()==10000&&Arrays.stream(with).allMatch(v->v>=0),"exact probability conservation");
                    check(with[0]<=base[0]&&with[4]>=base[4]&&with[4]<10000,"bonus improves odds without guaranteeing perfect");
                }
            }
        }
        check(RefiningRules.fireQualityBonus(1,5)==0,"low fire negligible on high equipment");
        var catalogue=JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/xiantu/refining/catalog.json"))).getAsJsonObject().getAsJsonArray("recipes");
        var ranges=new HashMap<String,List<Integer>>();int qingshuang=0,qitian=0;
        for(var entry:catalogue){var r=entry.getAsJsonObject();int tier=r.get("tier").getAsInt();var inputs=r.getAsJsonArray("ingredients");
            check(inputs.size()<=8,"fits manual slots");
            int sum=0;for(var value:inputs){int count=value.getAsJsonObject().get("count").getAsInt();check(count>0&&count<=64,"fits one normal input stack");sum+=count;}
            check(sum>inputs.size(),"not a recipe made of single units");
            check(r.get("requiresBlueprint").getAsBoolean()==(tier>=4),"fourth tier blueprint threshold");
            if(r.get("theme").getAsString().equals("齐天")){
                qitian++;check(tier==7&&r.get("profession").getAsInt()==9&&r.get("furnace").getAsInt()==5,"Qitian minimum furnace five, profession and item tier preserved");
                check(r.get("enabled").getAsBoolean()&&!r.has("disabledReason"),"Qitian crafting enabled with independent survival sources");
            }
            ranges.computeIfAbsent(r.get("output").getAsString(),k->new ArrayList<>()).add(tier);
            if(r.get("theme").getAsString().equals("青霜剑")){qingshuang++;check(tier<=3&&!r.get("requiresBlueprint").getAsBoolean(),"Qingshuang only low tiers");}
        }
        check(qingshuang==3,"Qingshuang three versions");
        check(qitian==5,"five independent Qitian equipment recipes");
        for(int tier=1;tier<=7;tier++){
            check(RefiningRules.blueprintRequired(tier)==(tier>=4),"tier four requires a diagram");
            check(RefiningProgress.blueprintKey("example_tier_"+tier).equals("example"),"one named equipment diagram across all tiers");
        }
        check(!RefiningProgress.blueprintKey("example_tier_4").equals(RefiningProgress.blueprintKey("other_tier_4")),"different equipment remain separate");
        var specials=Set.of("jingu_staff","fengchi_crown","suozi_chestplate","liujin_leggings","ousi_boots");
        for(String id:specials)check(RefiningRecipes.diagram(id)!=null&&RefiningRecipes.find(id)!=null,"Qitian diagram and manufacturing recipe both enabled");
        Path lootRoot=Path.of("src/main/resources/data/xiantu/loot_tables");int sourceCount=0;
        try(var paths=Files.walk(lootRoot)){
            for(var file:paths.filter(v->v.toString().endsWith(".json")).toList()){
                String json=Files.readString(file);if(specials.stream().noneMatch(json::contains))continue;
                check(file.getFileName().toString().equals("boss_monkey.json"),"Qitian diagrams only come from monkey Boss rewards");sourceCount++;
                var table=JsonParser.parseString(json).getAsJsonObject();var pool=table.getAsJsonArray("pools").asList().stream().map(JsonElement::getAsJsonObject).filter(v->v.has("name")&&v.get("name").getAsString().equals("xiantu_qitian_diagram")).findFirst().orElseThrow();
                check(pool.get("rolls").getAsInt()==1&&pool.getAsJsonArray("entries").size()==5,"one of five diagrams per Boss reward");
            }
        }check(sourceCount==1,"unique Qitian diagram source");
        for(var tiers:ranges.values())check(Collections.max(tiers)<=Math.min(7,Collections.min(tiers)+2),"named equipment ceiling");
        for(int tier=1;tier<=7;tier++){
            double total=0;
            for(String slot:List.of("helmet","chestplate","leggings","boots")){
                double value=ArmorStats.protection(tier,slot);total+=value;
                check(value>=switch(slot){case "helmet","boots"->3;case "chestplate"->8;default->6;},"every piece at least diamond");
                if(tier>1)check(value>=ArmorStats.protection(tier-1,slot),"armor progression never decreases");
            }
            check(tier!=1||total==20,"tier one complete armor equals diamond");
            check(ArmorStats.toughness(tier)>=2,"diamond toughness baseline");
            if(tier>1)check(ArmorStats.toughness(tier)>ArmorStats.toughness(tier-1),"toughness continues to grow at high tiers");
        }
        System.out.println("Refining regression PASS: "+checks);
    }
}
