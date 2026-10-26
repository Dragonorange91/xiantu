package com.longju.xiantu.trade;

/** Physical currency only; no fractional balance or skip-tier exchange. */
public final class TradeEconomyRules {
    public static int exchangeCost(int lowerTier){if(lowerTier<1||lowerTier>=8)throw new IllegalArgumentException("adjacent tier");return 4;}
    public static int facilityPrice(int tier){if(tier<1||tier>8)throw new IllegalArgumentException("tier");return tier==1?3:tier==2?4:tier==3?5:tier==4?6:tier==5?8:tier==6?10:tier==7?12:16;}
    public static long day(long gameTime){return Math.floorDiv(gameTime,24000L);}
    private TradeEconomyRules(){}
}
