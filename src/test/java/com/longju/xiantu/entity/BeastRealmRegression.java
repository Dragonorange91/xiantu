package com.longju.xiantu.entity;

import com.google.gson.JsonParser;
import java.nio.file.*;
import java.util.*;

public final class BeastRealmRegression {
    private static int checks;
    private static void require(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    public static void main(String[] args)throws Exception{
        int stages=0;
        for(int r=0;r<9;r++)for(int s=0;s<BeastRealmRules.stages(r);s++){
            stages++;require(BeastRealmRules.valid(r,s),"valid stage");
            require(BeastRealmRules.health(r,s,true)>BeastRealmRules.health(r,s,false),"elite health");
            require(BeastRealmRules.attack(r,s,true)>BeastRealmRules.attack(r,s,false),"elite attack");
            if(s>0)require(BeastRealmRules.health(r,s,false)>BeastRealmRules.health(r,s-1,false),"stage progression");
        }
        require(stages==41,"41 stages");require(!BeastRealmRules.valid(-1,0)&&!BeastRealmRules.valid(9,0)&&!BeastRealmRules.valid(1,4),"invalid bounds");
        for(int player=-1;player<9;player++){
            int base=Math.max(0,player);int[] counts=new int[9];
            for(int i=0;i<10000;i++){
                int rolled=BeastRealmRules.nearPlayerRealm(player,(i+.5)/10000.0);
                require(rolled>=Math.max(0,base-1)&&rolled<=Math.min(8,base+1),"nearby spawn range "+player);
                counts[rolled]++;
            }
            int[] expected=new int[9];expected[Math.max(0,base-1)]+=3500;expected[base]+=6000;expected[Math.min(8,base+1)]+=500;
            require(Arrays.equals(counts,expected),"35/60/5 with merged boundary weights "+player);
        }
        require(BeastRealmRules.nearPlayerRealm(3,.34999)==2&&BeastRealmRules.nearPlayerRealm(3,.35)==3
            &&BeastRealmRules.nearPlayerRealm(3,.94999)==3&&BeastRealmRules.nearPlayerRealm(3,.95)==4,"Yuanying threshold edges");
        for(var weights:BeastRealmRules.WEIGHTS)for(int player=-1;player<9;player++){
            int[] counts=new int[9];final int samples=200000;
            for(int i=0;i<samples;i++)counts[BeastRealmRules.naturalRealm(player,weights,(i+.5)/samples)]++;
            double[] expected=new double[9];
            for(int realm=0;realm<9;realm++)expected[realm]=samples*weights[realm]/100*(player<0?1:.3);
            if(player>=0){expected[Math.max(0,player-1)]+=samples*.7*.35;expected[player]+=samples*.7*.60;expected[Math.min(8,player+1)]+=samples*.7*.05;}
            for(int realm=0;realm<9;realm++){
                require(counts[realm]>0,"all realms stay reachable for every region/player");
                require(Math.abs(counts[realm]-expected[realm])<=1.0001,"70/30 ecology/player blend "+player+"/"+realm);
            }
        }
        int[] coreTiers={1,2,3,4,5,6,6,7,8};
        for(int r=0;r<9;r++)require(BeastRealmRules.coreTier(r)==coreTiers[r],"realm to crafting core tier "+r);
        require(Math.abs(BeastRealmRules.health(8,3,true)-16200)<.001,"uncapped highest elite health");
        for(var weights:BeastRealmRules.WEIGHTS){
            require(Math.abs(Arrays.stream(weights).sum()-100)<.000001,"100 percent");
            double cumulative=0;
            for(int r=0;r<9;r++){require(BeastRealmRules.roll(weights,(cumulative+weights[r]/2)/100)==r,"every rare realm reachable");cumulative+=weights[r];}
            require(BeastRealmRules.roll(weights,0)==0&&BeastRealmRules.roll(weights,1)==8,"roll endpoints");
        }
        require(BeastRealmRules.timeAllowed("day",0)&&!BeastRealmRules.timeAllowed("day",12000),"day edge");
        require(BeastRealmRules.timeAllowed("night",12000)&&!BeastRealmRules.timeAllowed("night",24000),"night wrap");
        require(BeastRealmRules.timeAllowed("twilight",-1)&&BeastRealmRules.timeAllowed("twilight",12000)&&!BeastRealmRules.timeAllowed("twilight",6000),"twilight wrap");
        var root=JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/xiantu/beast_ecology/overworld.json"))).getAsJsonObject();
        var p=BeastEcology.parse(root);require(p.species().size()==30,"thirty beasts");
        require(p.species().stream().filter(BeastEcology.Species::elite).count()==6,"six elites");
        require(p.species().stream().map(BeastEcology.Species::element).distinct().count()==6,"six elements");
        for(int i=0;i<3;i++)require(Arrays.equals(p.weights().get(i),BeastRealmRules.WEIGHTS[i]),"data and rules agree");
        var duplicate=root.deepCopy();duplicate.getAsJsonArray("species").add(duplicate.getAsJsonArray("species").get(0));
        try{BeastEcology.parse(duplicate);throw new AssertionError("duplicate accepted");}catch(com.google.gson.JsonParseException expected){checks++;}
        System.out.println("Beast realm/ecology PASS "+checks);
    }
}
