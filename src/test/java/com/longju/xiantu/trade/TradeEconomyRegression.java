package com.longju.xiantu.trade;

import com.longju.xiantu.item.SpiritStoneTier;

public final class TradeEconomyRegression {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        for(long day=0;day<1000;day++){
            check(TradeEconomyRules.day(day*24000)==day,"start inclusive");
            check(TradeEconomyRules.day(day*24000+23999)==day,"last tick same quota");
            check(TradeEconomyRules.day(day*24000+24000)==day+1,"next day fresh quota");
        }
        for(int tier=1;tier<8;tier++){
            int cost=TradeEconomyRules.exchangeCost(tier);
            check(cost==4,"all adjacent exchanges cost four");
            check(SpiritStoneTier.values()[tier].downcraftCount()<=cost,"adjacent round trip never multiplies stones");
            if(tier>1)check(TradeEconomyRules.facilityPrice(tier)>TradeEconomyRules.facilityPrice(tier-1),"facilities increase with tier");
        }
        for(int tier:new int[]{-1,0,8,9}){boolean denied=false;try{TradeEconomyRules.exchangeCost(tier);}catch(IllegalArgumentException e){denied=true;}check(denied,"outside adjacent exchange range denied");}
        System.out.println("PASS trade economy: "+checks+" day-boundary, adjacent exchange and round-trip checks");
    }
}
