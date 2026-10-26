package com.longju.xiantu.talisman;

/** Pure server rules: tier, tool, profession and quality are separate axes. */
public final class TalismanRules {
 public static final int[] XP={100,300,900,2700,8100,24300,72900,218700,656100};
 public static final double[] QUALITY={1,1,1.15,1.35,1.6,2};
 public static int realm(int tier){return tier>=7?tier:tier-1;}
 public static int level(int xp){int l=0;for(int cost:XP){if(xp<cost)break;xp-=cost;l++;}return l;}
 public static int energy(int tier){int n=50;for(int i=1;i<tier;i++)n*=3;return n;}
 public static int ticks(int tier){return (10+5*tier)*20;}
 public static int brushBonus(int brush,int tier){return switch(brush){case 1->tier<=3?4:0;case 2->tier<=3?14:tier<=6?10:0;case 3->tier<=3?26:tier<=6?22:18;default->0;};}
 public static int brushQuality(int brush,int tier){return switch(brush){case 1->tier<=3?50:0;case 2->tier<=3?150:tier<=6?100:0;case 3->tier<=3?300:tier<=6?250:200;default->0;};}
 public static int success(int complexity,int brush,int tier,int profession,int craft){return Math.max(5,Math.min(99,80-10*complexity+brushBonus(brush,tier)+profession+Math.max(-20,Math.min(6,craft))));}
 public static int[] quality(int profession,int brush,int tier,int material){return com.longju.xiantu.refining.RefiningRules.qualityWeights(profession,1,1,material,brushQuality(brush,tier));}
 public static int rebirthIndex(int recorded,int major){return Math.max(0,recorded-(major==0?1:3));}
 private TalismanRules(){}
}
