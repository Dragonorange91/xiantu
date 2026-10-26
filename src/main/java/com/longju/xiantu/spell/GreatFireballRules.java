package com.longju.xiantu.spell;

/** Tick-based, server-owned charge curve. No client-supplied size/damage/resource values. */
public final class GreatFireballRules {
    public static final int MIN_TICKS=6,MAX_TICKS=100,MAX_HOLD=200,HEARTBEAT_TIMEOUT=16;
    public static double progress(double ticks){return Math.max(0,Math.min(1,ticks/MAX_TICKS));}
    public static double radius(double ticks){return .38+3.12*Math.pow(progress(ticks),.85);}
    public static double lift(double radius){return Math.max(0,radius-1.25);}
    public static double damageMultiplier(double ticks){return .65+4.35*Math.pow(progress(ticks),1.35);}
    public static double costFraction(double ticks){return .25+.75*progress(ticks);}
    public static double blastRadius(double ticks){return 1.5+6.5*progress(ticks);}
    public static double speed(double ticks){return 1.15+.5*progress(ticks);}
    public static double falloff(double distance,double radius){return Math.max(.35,1-.65*Math.max(0,Math.min(1,distance/radius)));}
    private GreatFireballRules(){}
}
