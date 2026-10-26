package com.longju.xiantu.entity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Authoring and server combat consume the same versioned, packaged timing contract. */
public final class BossCombatSkills {
    public record Skill(String id,String name,String shape,String motion,int windup,int active,int recovery,
                        int cooldown,double reach,double multiplier,boolean ultimate) {
        public int duration(){return windup+active+recovery;}
    }
    public static final Skill BASIC=new Skill("attack","普攻","melee","attack",14,12,40,66,3.6,1,false);
    public static final Skill SPEAR_BASIC=new Skill("attack","星枪突刺","spear_melee","thrust",18,12,40,70,4.8,1,false);
    private static final Map<String,List<Skill>> ALL=load();
    private static Map<String,List<Skill>> load(){
        var stream=BossCombatSkills.class.getResourceAsStream("/assets/xiantu/boss_combat.json");
        if(stream==null)throw new IllegalStateException("Missing Boss choreography contract");
        try(var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            Map<String,List<Skill>> map=new Gson().fromJson(reader,new TypeToken<Map<String,List<Skill>>>(){}.getType());
            for(var list:map.values())for(var s:list)
                if(s.recovery<40 || s.windup<20 || s.active<1 || s.duration()>200 || (s.ultimate && s.windup!=30))
                    throw new IllegalStateException("Invalid Boss timeline "+s.id);
            return Map.copyOf(map);
        }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
    }
    public static List<Skill> forBoss(String id){return ALL.getOrDefault(id,List.of());}
    public static Skill basic(String boss){return boss.equals("meteor_general")?SPEAR_BASIC:BASIC;}
    public static Skill find(String boss,String skill){return skill.equals("attack")?basic(boss):forBoss(boss).stream().filter(s->s.id.equals(skill)).findFirst().orElse(null);}
    public static boolean shapeHits(String shape,double along,double side,double distance,double radius,double progress,double height){
        if(height>3.5 || height< -2)return false;
        return switch(shape){
            case "melee" -> progress>=.45 && progress<=.80 && along>=0 && distance<=radius && Math.abs(side)<2;
            case "spear_melee","blink_thrust" -> progress>=.2 && progress<=.65 && along>=0 && along<=radius && Math.abs(side)<.75 && Math.abs(height)<2.2;
            case "fist_combo" -> along>=0 && distance<=radius && Math.abs(side)<1.3 && (progress*3%1)>=.30 && (progress*3%1)<=.60;
            case "counter" -> along>=0 && distance<=radius && Math.abs(side)<2;
            case "fan" -> along>0 && along<radius*progress && Math.abs(side)<Math.max(.65,along*.65);
            case "line","projectile" -> along>=radius*Math.max(0,progress-.14) && along<=radius*progress+.6 && Math.abs(side)<.65;
            case "lanes" -> along>=radius*Math.max(0,progress-.16) && along<=radius*progress+.6 && (Math.abs(side)<.45 || Math.abs(Math.abs(side)-2.6)<.45);
            case "ring","gap_ring" -> Math.abs(distance-radius*progress)<.7 && (!shape.equals("gap_ring") || along/Math.max(.001,distance)<.8);
            case "wave" -> Math.abs(distance-radius*progress)<.75 && height<.65;
            case "fall" -> distance<radius && progress>=.8;
            case "cross" -> Math.abs(along)<radius && Math.abs(side)<radius && Math.min(Math.abs(side-along),Math.abs(side+along))<.8 && progress>=.45;
            case "pressure" -> distance>1.3 && distance<radius*(1-.55*progress) && progress>.35;
            default -> false;
        };
    }
    private BossCombatSkills(){}
}
