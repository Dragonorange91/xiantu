package com.longju.xiantu.spell;
/** Charge balance boundaries are gameplay contracts, independent of presentation. */
public final class GreatFireballRegression {
 private static int count;
 private static void check(boolean ok,String why){count++;if(!ok)throw new AssertionError(why);}
 public static void main(String[] args){
  check(SpellRules.realm(3)==2,"tier three requires Golden Core");
  check(GreatFireballRules.MIN_TICKS==6&&GreatFireballRules.MAX_TICKS==100,"tap guard and five second full charge");
  check(GreatFireballRules.MAX_HOLD>GreatFireballRules.MAX_TICKS,"full charge can be held before safety release");
  check(GreatFireballRules.radius(100)==3.5,"full fireball is seven blocks wide");
  check(GreatFireballRules.damageMultiplier(100)==5,"full damage multiplier");
  check(GreatFireballRules.costFraction(0)==.25&&GreatFireballRules.costFraction(100)==1,"initial to full payment");
  check(GreatFireballRules.blastRadius(100)==8,"bounded splash");
  for(int tick=1;tick<=100;tick++){
   check(GreatFireballRules.radius(tick)>GreatFireballRules.radius(tick-1),"visible growth each tick");
   check(GreatFireballRules.damageMultiplier(tick)>GreatFireballRules.damageMultiplier(tick-1),"damage growth each tick");
   check(GreatFireballRules.costFraction(tick)>GreatFireballRules.costFraction(tick-1),"resource growth each tick");
   double r=GreatFireballRules.radius(tick);
   check(1.62+GreatFireballRules.lift(r)-r*.8>.25,"level aim collision sphere clears ground");
   long due=(long)Math.ceil(20000*GreatFireballRules.costFraction(tick));
   check(due<=20000,"no overpayment past full cost");
  }
  for(int tick: new int[]{-100,0,100,200,10000})check(GreatFireballRules.progress(tick)>=0&&GreatFireballRules.progress(tick)<=1,"progress cap");
  check(GreatFireballRules.radius(200)==GreatFireballRules.radius(100),"hold cannot overgrow");
  check(GreatFireballRules.falloff(0,8)==1&&GreatFireballRules.falloff(100,8)==.35,"radial falloff bounded");
  long paid=5000,total=5000;for(int tick=0;tick<=200;tick++){long due=(long)Math.ceil(20000*GreatFireballRules.costFraction(tick));long delta=Math.max(0,due-paid);paid+=delta;total+=delta;}
  check(total==20000,"incremental payment neither duplicates nor exceeds full charge");
  System.out.println("Great Fireball PASS "+count+" assertions");
 }
}
