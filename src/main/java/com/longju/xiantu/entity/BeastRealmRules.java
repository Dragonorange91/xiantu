package com.longju.xiantu.entity;

/** Pure probability and attribute rules. Realm/stage indices match player cultivation. */
public final class BeastRealmRules {
    public static final double[][] WEIGHTS = {
        {64,24,8,2.8,.9,.25,.03,.015,.005},
        {40,30,18,8,3,.8,.12,.06,.02},
        {15,25,25,18,10,5,1.2,.6,.2}
    };
    private static final double[] HEALTH={36,70,140,300,650,1400,2100,3200,7500};
    private static final double[] ATTACK={3,4,6,9,13,18,21,25,34};
    public static int roll(double[] weights,double unitRoll) {
        double sum=0;for(double w:weights)sum+=w;
        double value=Math.max(0,Math.min(Math.nextDown(1.0),unitRoll))*sum;
        for(int i=0;i<weights.length;i++){value-=weights[i];if(value<0)return i;}
        return weights.length-1;
    }
    public static int stages(int realm){return realm==0?9:4;}
    /** Nearby natural spawns: 35% one realm below, 60% equal, 5% one above.
     * At the first/last realm, out-of-range weight merges into the boundary realm. */
    public static int nearPlayerRealm(int playerRealm,double unitRoll){
        int base=Math.max(0,Math.min(8,playerRealm));
        int offset=unitRoll<.35?-1:unitRoll<.95?0:1;
        return Math.max(0,Math.min(8,base+offset));
    }
    /** Keep every region's full realm range: 70% local player context, 30% region ecology. */
    public static int naturalRealm(int playerRealm,double[] regionWeights,double unitRoll){
        double unit=Math.max(0,Math.min(Math.nextDown(1.0),unitRoll));
        if(playerRealm<0)return roll(regionWeights,unit);
        return unit<.7?nearPlayerRealm(playerRealm,unit/.7):roll(regionWeights,(unit-.7)/.3);
    }
    /** Nine major realms use eight crafting tiers: Lianxu and Heti share tier six. */
    public static int coreTier(int realm){
        if(realm<0||realm>8)throw new IllegalArgumentException("realm");
        return realm<=5?realm+1:realm;
    }
    public static boolean valid(int realm,int stage){return realm>=0&&realm<9&&stage>=0&&stage<stages(realm);}
    public static double factor(int realm,int stage){if(!valid(realm,stage))throw new IllegalArgumentException("realm/stage");return 1+.2*stage/(stages(realm)-1);}
    public static double health(int realm,int stage,boolean elite){return HEALTH[realm]*factor(realm,stage)*(elite?1.8:1);}
    public static double attack(int realm,int stage,boolean elite){return ATTACK[realm]*factor(realm,stage)*(elite?1.15:1);}
    public static boolean timeAllowed(String time,long dayTime){
        long t=Math.floorMod(dayTime,24000);
        return switch(time){case "day"->t<12000;case "night"->t>=12000;
            case "twilight"->t<=1000||t>=23000||(t>=11000&&t<=13000);case "any"->true;
            default->false;};
    }
    private BeastRealmRules(){}
}
