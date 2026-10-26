package com.longju.xiantu.spell;
public final class SpellRulesRegression {
 private static int count;
 private static void check(boolean value,String text){count++;if(!value)throw new AssertionError(text);}
 public static void main(String[] args){
  check(java.util.Arrays.stream(SpellRules.EXPERIENCE).sum()==890,"mastery total");
  for(int tier=1;tier<=8;tier++)for(int level=0;level<=9;level++){
   double maximum=100*Math.pow(3,tier-1),ratio=.03;long cost=SpellRules.cost(maximum,ratio,level);
   check(cost>0,"cost stays positive");check(cost<=SpellRules.cost(maximum,ratio,Math.max(0,level-1)),"mastery monotonically saves power");
  }
  check(SpellRules.cost(100,.2,9)==19100,"nine mastery 95.5 percent");check(SpellRules.cost(.001,.0001,9)==1,"one milli minimum");
  check(SpellRules.realmMultiplier(1,9)==.3,"realm resistance maximum");check(SpellRules.realmMultiplier(9,1)==1.75,"realm attack cap");
  check(SpellRules.softCap(100,20)==40,"excess soft cap");check(SpellRules.softCap(10,20)==10,"under threshold unchanged");
  check(SpellRules.controlTicks(40,0,0)==40,"ordinary full control");check(SpellRules.controlTicks(40,2,0)==15,"two-realm shorter control plus resistance");
  check(SpellRules.controlTicks(40,9,0)==0,"high realm rejects control");
  check(SpellRules.damage(6,0,0,0,0)==6,"first fireball retains base damage");
  check(SpellRules.damage(12,2,2,0,0)==12,"thunder retains unlock base damage");
  check(Math.abs(SpellRules.damage(6,0,0,0,9)-11.4)<1e-9,"mastery nine adds ninety percent");
  check(SpellRules.masteryMultiplier(-10)==1&&SpellRules.masteryMultiplier(100)==1.9,"bounded mastery");
  for(int minimum=0;minimum<9;minimum++)for(int mastery=0;mastery<=9;mastery++){
   double previous=0;
   for(int realm=minimum;realm<9;realm++)for(int stage=0;stage<com.longju.xiantu.cultivation.RealmRegistry.STAGE_COUNTS[realm];stage++){
    double damage=SpellRules.damage(6,minimum,realm,stage,mastery);
    check(damage>previous,"each cultivation stage grows absolute spell damage");previous=damage;
    if(mastery>0)check(damage>SpellRules.damage(6,minimum,realm,stage,mastery-1),"each mastery grows damage");
   }
  }
  check(Math.abs(SpellRules.damage(6,0,8,3,9)-632.32)<1e-8,"early spell remains relevant at final realm");
  check(SpellRules.damage(6,0,0,0,0)*2.2<SpellRules.damage(6,0,8,3,0),"absolute realm growth is outside bonus cap");
  System.out.println("Spell rules PASS "+count+" assertions");
 }
}
