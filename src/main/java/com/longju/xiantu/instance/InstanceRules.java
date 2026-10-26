package com.longju.xiantu.instance;

import java.util.random.RandomGenerator;

/** Chapter 16 rules; realm indices use RealmRegistry's zero-based major realms. */
public final class InstanceRules {
    public static final int REALM_SLOTS=16,CAVE_SLOTS=256,CAVE_BLOCKS=192,CAVE_BUFFER=16;
    public static final long EARLY_RETURN_TICKS=5*60*20;
    public enum Abundance {
        BARREN("荒",20,1,0,1,4,20,30),NORMAL("常",30,2,0,1,8,25,40),
        RICH("丰",25,3,1,4,12,30,50),FLOURISHING("盛",18,4,2,5,20,35,60),
        ABUNDANT("极盛",7,5,3,8,32,40,70);
        public final String label;
        public final int weight,resources,minRealm,maxRealm,capacity,minMinutes,maxMinutes;
        Abundance(String label,int weight,int resources,int minRealm,int maxRealm,int capacity,int minMinutes,int maxMinutes){
            this.label=label;this.weight=weight;this.resources=resources;this.minRealm=minRealm;this.maxRealm=maxRealm;
            this.capacity=capacity;this.minMinutes=minMinutes;this.maxMinutes=maxMinutes;
        }
    }
    public enum Size {
        COMPACT(40,16,48),STANDARD(40,32,96),LARGE(20,64,192);
        public final int weight,minChunks,maxChunks;
        Size(int weight,int minChunks,int maxChunks){this.weight=weight;this.minChunks=minChunks;this.maxChunks=maxChunks;}
    }
    public record Parameters(Abundance abundance,Size size,int diameterChunks,int minutes,boolean caveToken,boolean soulCrystal){}
    public static Abundance abundance(int roll){
        if(roll<0||roll>=100)throw new IllegalArgumentException("Abundance roll");
        for(var a:Abundance.values()){if(roll<a.weight)return a;roll-=a.weight;}throw new AssertionError();
    }
    public static Size size(int roll){
        if(roll<0||roll>=100)throw new IllegalArgumentException("Size roll");
        for(var s:Size.values()){if(roll<s.weight)return s;roll-=s.weight;}throw new AssertionError();
    }
    public static Parameters draw(RandomGenerator random){
        var a=abundance(random.nextInt(100));var s=size(random.nextInt(100));
        int midpoint=(a.minMinutes+a.maxMinutes)/2;
        int low=s==Size.LARGE?midpoint:a.minMinutes,high=s==Size.COMPACT?midpoint:a.maxMinutes;
        return new Parameters(a,s,random.nextInt(s.minChunks,s.maxChunks+1),random.nextInt(low,high+1),
                random.nextInt(100)<5,a.maxRealm>=3&&random.nextInt(100)<10);
    }
    public static boolean eligible(Parameters p,boolean unlocked,int realm){return unlocked&&realm>=p.abundance.minRealm&&realm<=p.abundance.maxRealm;}
    public static final long REALM_INTERVAL_MILLIS=2*60*60*1000L;
    /** Persisted wall clock: sleeping/tick rate cannot skip or lengthen the real-time interval. */
    public static long nextRealmAt(long closedAtMillis){return Math.addExact(closedAtMillis,REALM_INTERVAL_MILLIS);}
    public static int guardianRealm(Abundance a,boolean high,RandomGenerator r){int pick=r.nextInt(a.minRealm,a.maxRealm+1);return high?Math.max(pick,r.nextInt(a.minRealm,a.maxRealm+1)):pick;}
    public static int resourceTier(Abundance a,boolean high,RandomGenerator r){return Math.min(8,guardianRealm(a,high,r)+1);}
    private InstanceRules(){}
}
