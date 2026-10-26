package com.longju.xiantu.spell;
/** Travel and automatic recovery balance, expressed as fractions of real capacity. */
public final class SupportSpellRules {
 public static final int FLIGHT_XP_TICKS=200,HEAL_COOLDOWN=1200;
 public static double flightDrainRatio(int realm,boolean boost){return (realm<=2?1.0/300:1.0/420)*(boost?2:1);}
 public static double healRatio(int mastery){return .10+.025*Math.max(0,Math.min(9,mastery));}
 public static boolean heals(double health,double maximum){return health>0&&maximum>0&&health/maximum<=.30;}
 private SupportSpellRules(){}
}
