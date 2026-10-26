package com.longju.xiantu.spell;

/** Shared fixed-point mastery and resource rules; no random cast failure. */
public final class SpellRules {
 public static final int[] EXPERIENCE={20,30,40,60,80,100,140,180,240};
 public static long cost(double maximum,double ratio,int mastery){return Math.max(1,(long)Math.ceil(maximum*1000*ratio*(1-.005*Math.max(0,Math.min(9,mastery)))));}
 /** Absolute cultivation growth is separate from the capped method/equipment bonus zone. */
 public static double masteryMultiplier(int mastery){return 1+.1*Math.max(0,Math.min(9,mastery));}
 public static double cultivationMultiplier(int realm,int stage,int minimumRealm){
  return Math.max(1,com.longju.xiantu.cultivation.RealmRegistry.baseAttackExact(realm,stage)
    /com.longju.xiantu.cultivation.RealmRegistry.baseAttackExact(minimumRealm,0));
 }
 public static double damage(double base,int minimumRealm,int realm,int stage,int mastery){
  return Math.max(0,base)*cultivationMultiplier(realm,stage,minimumRealm)*masteryMultiplier(mastery);
 }
 public static int experience(int tier){return tier<=2?1:tier<=4?2:tier<=6?4:8;}
 public static int realm(int tier){return tier>=7?tier:tier-1;}
 public static double realmMultiplier(int attacker,int defender){return Math.max(.6,Math.min(1.75,1+.15*(attacker-defender)))*(1-Math.min(.5,.1*Math.max(0,defender-attacker)));}
 public static double softCap(double amount,double threshold){return amount<=threshold?amount:threshold+(amount-threshold)*.25;}
 public static int controlTicks(int base,int realmGap,int resistance){double score=100-resistance-12*Math.max(0,realmGap);return score<0?0:(int)Math.floor(base*(realmGap>=2?.5:1)*(score<50?.5:score<100?.75:1));}
 private SpellRules(){}
}
