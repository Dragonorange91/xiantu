package com.longju.xiantu.worldgen;

/** Numeric rules isolated from entity creation for regression tests. */
public final class RuinBossRules {
    public static double healthScale(int players){return 1+1.7*(Math.max(1,Math.min(6,players))-1);}
    public static double attackScale(int players){return new double[]{1,1.06,1.11,1.15,1.18,1.20}[Math.max(1,Math.min(6,players))-1];}
    public static float soften(float amount,double singlePlayerHealth){double threshold=singlePlayerHealth*.03;return (float)(amount<=threshold?amount:threshold+(amount-threshold)*.25);}
    private RuinBossRules(){}
}
