package com.longju.xiantu.worldgen;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
public final class RuinIntegrationRegression {
    private static int checks;
    private static void require(boolean ok,String detail){checks++;if(!ok)throw new AssertionError(detail);}
    public static void main(String[]args)throws Exception{
        Path data=Path.of("src/main/resources/data/xiantu");int count=0,natural=0,bosses=0;
        Set<String> all=new HashSet<>();
        try(var files=Files.list(data.resolve("ruin_blueprints"))){for(var file:files.filter(p->p.toString().endsWith(".json")).toList()){
            var b=JsonParser.parseString(Files.readString(file)).getAsJsonObject();count++;String id=b.get("id").getAsString();all.add(id);
            require(b.get("schema").getAsInt()==2,id+" schema");
            require(Files.exists(data.resolve("structures/ruins/"+id+".nbt")),id+" NBT");
            Set<String> waves=new HashSet<>(),boxes=new HashSet<>();
            for(var v:b.getAsJsonArray("encounters")){var e=v.getAsJsonObject();require(waves.add(e.get("id").getAsString()),id+" duplicate wave");require(e.get("triggerRadius").getAsInt()==32,id+" approach radius");}
            for(var v:b.getAsJsonArray("markers")){var m=v.getAsJsonObject();if(!m.get("type").getAsString().equals("reward"))continue;
                require(boxes.add(m.get("id").getAsString()),id+" duplicate chest");String loot=m.get("loot").getAsString();require(Files.exists(data.resolve("loot_tables/"+loot.substring(7)+".json")),id+" missing pool");
            }
            require(boxes.size()>=2,id+" multiple rewards");
            if(b.has("bossPlan")&&!b.get("bossPlan").isJsonNull()){bosses++;String entity=b.getAsJsonObject("bossPlan").get("entity").getAsString();require(Set.of("xiantu:meteor_general","xiantu:spirit_ape_king","xiantu:five_element_warden").contains(entity),"archived boss must stay disabled");
                String loot=b.getAsJsonObject("bossPlan").get("loot").getAsString();require(Files.exists(data.resolve("loot_tables/"+loot.substring(7)+".json")),id+" boss material table");
                var pool=JsonParser.parseString(Files.readString(data.resolve("loot_tables/"+loot.substring(7)+".json"))).getAsJsonObject();
                require(!pool.toString().contains("preview_"),"appearance samples never become survival rewards");
                var rolls=pool.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonObject("rolls");require(rolls.get("min").getAsInt()>=4,"boss guaranteed material groups");
            }
            if(b.getAsJsonObject("placement").get("natural").getAsBoolean()){natural++;require(Files.exists(data.resolve("worldgen/structure/"+id+".json")),id+" natural registry");}
        }}
        require(count==20,"20 templates");require(natural==20,"20 natural variants");require(bosses==3,"3 active bosses");
        for(String retired:List.of("thunder_cloud_altar","five_elements_prison","flower_fruit_mountain","starfall_altar")){
            require(!all.contains(retired),"retired ruin has no blueprint: "+retired);
            require(!Files.exists(data.resolve("worldgen/structure/"+retired+".json"))&&!Files.exists(data.resolve("structures/ruins/"+retired+".nbt")),"retired ruin cannot generate or be placed: "+retired);
        }
        require(Math.abs(RuinBossRules.healthScale(6)-9.5)<1e-8,"six-player health");require(RuinBossRules.healthScale(7)==RuinBossRules.healthScale(6),"no extra scaling after six");
        require(RuinBossRules.attackScale(4)==1.15,"additive attack");require(RuinBossRules.soften(20,1000)==20,"small hits unchanged");require(RuinBossRules.soften(70,1000)==40,"excess reduced to quarter");
        try(var files=Files.list(data.resolve("worldgen/structure_set"))){for(var f:files.filter(p->p.getFileName().toString().startsWith("ruins_")).toList()){
            var s=JsonParser.parseString(Files.readString(f)).getAsJsonObject();var p=s.getAsJsonObject("placement");require(p.get("spacing").getAsInt()>p.get("separation").getAsInt(),"valid spread");
            for(var v:s.getAsJsonArray("structures"))require(all.contains(v.getAsJsonObject().get("structure").getAsString().substring(7)),"set references template");
        }}
        System.out.println("Ruin integration: "+checks+" assertions; "+count+" templates, "+natural+" natural structures");
    }
}
