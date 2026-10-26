package com.longju.xiantu.spell;
/** Capacity-independent travel budget and passive healing balance boundaries. */
public final class SupportSpellRegression {
 private static int count;
 private static void check(boolean ok,String why){count++;if(!ok)throw new AssertionError(why);}
 public static void main(String[] args){
  for(int realm:new int[]{1,2})for(double capacity:new double[]{10,100,2500,1000000}){
   double ordinary=capacity*SupportSpellRules.flightDrainRatio(realm,false);
   double boost=capacity*SupportSpellRules.flightDrainRatio(realm,true);
   check(Math.abs(capacity/ordinary-300)<1e-8,"five minutes independent of mana capacity");
   check(Math.abs(capacity/boost-150)<1e-8,"boost halves endurance");
   // Include the existing 3% summon debit in a real full-mana trip.
   check(capacity*.97/ordinary>=290&&capacity*.97/ordinary<=292,"actual trip remains about five minutes");
   double mixed=ordinary*120+boost*60;
   check(Math.abs(mixed/capacity-.8)<1e-9,"mixed travel cannot gain free sprint time");
  }
  check(SupportSpellRules.FLIGHT_XP_TICKS==200,"mastery from ten seconds actual travel");
  check(SupportSpellRules.HEAL_COOLDOWN==1200,"passive has sixty second recovery");
  check(!SupportSpellRules.heals(0,100),"cannot resurrect");
  check(!SupportSpellRules.heals(-1,100)&&!SupportSpellRules.heals(1,0),"invalid or lethal health rejected");
  check(SupportSpellRules.heals(30,100)&&SupportSpellRules.heals(1,100),"low-health threshold inclusive");
  check(!SupportSpellRules.heals(30.001,100),"healthy player cannot farm passive");
  check(Math.abs(SupportSpellRules.healRatio(0)-.10)<1e-9,"initial ten percent maximum health");
  for(int level=1;level<=9;level++)check(SupportSpellRules.healRatio(level)>SupportSpellRules.healRatio(level-1),"mastery improves healing");
  check(SupportSpellRules.healRatio(-1)==.1&&SupportSpellRules.healRatio(100)==.325,"bounded healing");
  System.out.println("Support Spells PASS "+count+" assertions");
 }
}
