package com.longju.xiantu.alchemy;
import com.longju.xiantu.refining.RefiningRules;
/** Integer accounting and one independent roll each for success, quality and extra yield. */
public final class AlchemyRules {
    public static final String[] NAMES={"未入门","引火","凝焰","化丹","通玄","丹心","丹魂","丹道","丹仙","丹祖"};
    private static final int[] SECONDS={10,20,40,60,90,120,180,240};
    public static int energy(int rank){return RefiningRules.energy(rank)/2;}
    public static int ticks(int rank){return SECONDS[rank-1]*20;}
    public static int furnace(int rank){return Math.max(1,Math.min(6,rank-1));}
    public static int realm(int rank){return rank>=7?rank:rank-1;}
    public static int success(String kind,int profession,int furnace,int required,int auxiliaryBonus){if(furnace<required)return 0;int base=switch(kind){case "basic"->9500;case "breakthrough"->8500;default->7500;};return Math.min(10000,base+profession*100+Math.min(400,Math.max(0,furnace-required)*200)+Math.min(1200,auxiliaryBonus));}
    public static int auxiliarySuccess(int core,int recipe){return core<recipe?100:core==recipe?150:200;}
    /** Half basis points preserve the exact 0.375 percentage point term. */
    public static int auxiliaryQualityHalf(int core,int recipe){return core<recipe?50:core==recipe?75:100;}
    public static long debit(int elapsed,int total,int energy){return (long)(elapsed+1)*energy*1000/total-(long)elapsed*energy*1000/total;}
    public static int stage(int elapsed,int total){int percent=elapsed*100/Math.max(1,total);return percent<15?0:percent<30?1:percent<45?2:percent<60?3:percent<80?4:5;}
    public static final int[] BOUNDS={0,15,30,45,60,80,100};
    public static int quality(int roll,int[] weights){return RefiningRules.rollQuality(roll,weights);}
    public static int[] qualityWeights(int level,int furnace,int fq,int material,int fire,int auxiliaryHalf){
        int[] weights=RefiningRules.qualityWeights(level,furnace,fq,material,fire);
        for(int i=0;i<weights.length;i++)weights[i]*=2;
        int transfer=Math.min(weights[0],Math.max(0,Math.min(600,auxiliaryHalf)));weights[0]-=transfer;
        int[] base=weights.clone(),added=new int[5];
        for(int unit=0;unit<transfer;unit++){
            int best=1;for(int i=2;i<5;i++)if((long)base[i]*(added[best]+1)>(long)base[best]*(added[i]+1))best=i;
            added[best]++;
        }
        for(int i=1;i<5;i++)weights[i]+=added[i];return weights;
    }
    private AlchemyRules(){}
}
