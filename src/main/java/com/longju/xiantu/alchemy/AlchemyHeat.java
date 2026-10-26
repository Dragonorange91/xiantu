package com.longju.xiantu.alchemy;
/** Integer control state: only active furnace ticks change temperature. */
public final class AlchemyHeat {
 public static final int GENTLE=0,STRONG=1;
 public static int target(int mode){return mode==STRONG?75:35;}
 public static int step(int temperature,int mode){return temperature+Integer.signum(target(mode)-temperature)*Math.min(3,Math.abs(target(mode)-temperature));}
 public static boolean inBand(int temperature,int mode){return Math.abs(temperature-target(mode))<=10;}
 public static boolean held(int matching,int duration){return matching*100>=duration*60;}
 private AlchemyHeat(){}
}
