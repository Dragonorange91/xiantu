package com.longju.xiantu.refining;

/** Pure rules shared by server validation and the workstation preview. */
public final class RefiningRules {
    public static final int BLUEPRINT_TIER=4;
    private static final int[] XP={100,300,600,1200,2400,4000,6500,9000,12000};
    private static final int[][] SHIFTS=buildShifts();
    /** Prefix allocations make every extra basis point monotone, unlike largest-remainder rounding. */
    private static int[][] buildShifts(){
        int[][] result=new int[10][];
        for(int lv=0;lv<10;lv++){
            int[] weights={2200+100*lv,700+150*lv,90+60*lv,10+90*lv},allocated=new int[4];
            int max=7000-400*lv;result[lv]=new int[(max+1)*4];
            for(int n=1;n<=max;n++){
                int best=0;for(int i=1;i<4;i++)if((long)weights[i]*(allocated[best]+1)>(long)weights[best]*(allocated[i]+1))best=i;
                allocated[best]++;System.arraycopy(allocated,0,result[lv],n*4,4);
            }
        }return result;
    }
    public static final String[] NAMES={"未入门","砺锋","淬火","百炼","通灵","器心","器魂","器道","器仙","器祖"};
    public static int level(int total){int level=0;for(int cost:XP){if(total<cost)break;total-=cost;level++;}return level;}
    public static int progress(int total){for(int cost:XP){if(total<cost)return Math.max(0,total);total-=cost;}return 0;}
    public static int next(int level){return level>=9?0:XP[Math.max(0,level)];}
    public static int award(int level,int tier){return level-tier>=3?Math.max(1,tier*10/4):tier*10;}
    public static boolean blueprintRequired(int tier){return tier>=BLUEPRINT_TIER;}
    public static int requiredLevel(int tier){return Math.max(0,tier-1);}
    public static int energy(int tier){int value=100;for(int i=1;i<tier;i++)value*=3;return value;}
    public static int fireQualityBonus(int rank,int targetTier){
        if(rank<=0)return 0;int value=Math.min(6,rank)*150;
        for(int i=rank;i<targetTier;i++)value/=4;return value;
    }
    public static int successPercent(int level,int furnace,int required){return furnace<required?0:Math.min(100,90+level+Math.min(4,2*Math.max(0,furnace-required)));}
    /** Hundredths of percentage points; all transfer comes from the ordinary grade. */
    public static int[] qualityWeights(int level,int furnace,int furnaceQuality,int materialQuality){
        return qualityWeights(level,furnace,furnaceQuality,materialQuality,0);
    }
    public static int[] qualityWeights(int level,int furnace,int furnaceQuality,int materialQuality,int fireBonus){
        int l=Math.max(0,Math.min(9,level));
        int[] weights={7000-400*l,2200+100*l,700+150*l,90+60*l,10+90*l};
        int transfer=Math.min(weights[0],Math.max(0,furnace-1)*150+Math.max(0,furnaceQuality-1)*150+Math.max(0,materialQuality-1)*125+Math.max(0,Math.min(900,fireBonus)));
        weights[0]-=transfer;
        for(int i=1;i<5;i++)weights[i]+=SHIFTS[l][transfer*4+i-1];
        return weights;
    }
    public static int rollQuality(int roll,int[] weights){int cursor=roll;for(int i=0;i<weights.length;i++){cursor-=weights[i];if(cursor<0)return i+1;}return 5;}
    private RefiningRules(){}
}
