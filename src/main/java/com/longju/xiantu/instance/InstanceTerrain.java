package com.longju.xiantu.instance;

/** Immutable generation input, published before a session requests its first chunk. */
public record InstanceTerrain(long seed,int diameterChunks,int abundance) {
    public InstanceTerrain(long seed,int diameterChunks){this(seed,diameterChunks,0);}
    public InstanceTerrain {
        if(diameterChunks<16||diameterChunks>192)throw new IllegalArgumentException("Realm diameter");
        if(abundance<0||abundance>=InstanceRules.Abundance.values().length)throw new IllegalArgumentException("Abundance");
    }
    public static final String[] THEMES={"wood","fire","water","metal","earth","thunder"};
    public static final String[] NAMES={"苍木灵谷","赤岩古境","沧澜秘境","金石残界","玄岩洞天","雷泽遗境"};
    public int theme(){return (int)Math.floorMod(mix(seed),6);}
    public boolean natural(){return Math.floorMod(mix(seed^0x6e61747572616cL),100)<55;}
    public String name(){return (natural()?new String[]{"苍木灵谷","赤霞山野","沧澜泽国","金石群山","玄岩沃野","雷泽灵原"}:NAMES)[theme()];}
    public boolean sanctuary(int x,int z){return Math.abs(x)<=14&&Math.abs(z)<=14;}
    public int generationRadius(){return diameterChunks*8+32;}
    public boolean generates(int x,int z){return Math.abs((long)x)<=generationRadius()&&Math.abs((long)z)<=generationRadius();}
    public static long mix(long v){v=(v^(v>>>30))*0xbf58476d1ce4e5b9L;v=(v^(v>>>27))*0x94d049bb133111ebL;return v^(v>>>31);}
    public double noise(int x,int z,int scale,long salt){
        int a=Math.floorDiv(x,scale),b=Math.floorDiv(z,scale);double tx=smooth(Math.floorMod(x,scale)/(double)scale),tz=smooth(Math.floorMod(z,scale)/(double)scale);
        return lerp(lerp(sample(a,b,salt),sample(a+1,b,salt),tx),lerp(sample(a,b+1,salt),sample(a+1,b+1,salt),tx),tz);
    }
    private double sample(int x,int z,long salt){return (mix(seed^((long)x*341873128712L)^((long)z*132897987541L)^salt)>>>11)*0x1.0p-53*2-1;}
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}
    private static double smooth(double t){return t*t*(3-2*t);}
}
