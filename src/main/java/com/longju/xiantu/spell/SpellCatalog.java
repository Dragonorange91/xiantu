package com.longju.xiantu.spell;
import com.longju.xiantu.combat.Element;
import java.util.*;

/** Authoritative gameplay definitions. Presentation is deliberately independent. */
public final class SpellCatalog {
 public record Spell(String id,String name,int tier,Element element,String type,double damage,double range,int windup,int cooldown,int duration,double cost,boolean stationary){
  public int realm(){return SpellRules.realm(tier);}
  public boolean active(){return !type.equals("reaction")&&!type.equals("passive");}
 }
 public static final List<Spell> ALL=List.of(
  new Spell("fire_bolt","火球术",1,Element.FIRE,"projectile",6,20,6,40,0,.03,false),
  new Spell("thunder_strike","雷引术",3,Element.THUNDER,"target",12,24,16,100,0,.07,true),
  new Spell("great_fireball","大火球术",3,Element.FIRE,"charged",28,56,100,160,0,.20,false),
  new Spell("rejuvenation","回春术",1,Element.WOOD,"passive",0,0,0,1200,0,.08,false),
  new Spell("sword_flight","御剑术",2,Element.PHYSICAL,"mount",0,0,12,0,0,.03,false));
 public static boolean enabled(Spell s){return s!=null&&ALL.contains(s);}
 public static Spell find(String id){return ALL.stream().filter(s->s.id().equals(id)).findFirst().orElse(null);}
 private SpellCatalog(){}
}
