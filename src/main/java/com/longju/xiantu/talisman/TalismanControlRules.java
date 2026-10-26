package com.longju.xiantu.talisman;

/** Ranks are one-based for targets; talisman realm mapping also handles the combined sixth tier. */
public final class TalismanControlRules {
    public static boolean allowed(int targetRank,int tier,int previous){return previous<2&&targetRank-(TalismanRules.realm(tier)+1)<2;}
    public static double duration(int targetRank,int tier,boolean elite,int previous){
        if(!allowed(targetRank,tier,previous))return 0;
        return (targetRank>TalismanRules.realm(tier)+1?.5:1)*(elite?.5:1)*(previous==1?.5:1);
    }
    public static int strokeStart(int total,int strokes,int index){return total*15/100+(total*85/100-total*15/100)*index/strokes;}
    public static int strokeEnd(int total,int strokes,int index){return strokeStart(total,strokes,index+1);}
    private TalismanControlRules(){}
}
