package com.longju.xiantu.refining;

/** Per-piece spiritual protection beyond vanilla's capped armor attributes. */
public final class ArmorBalance {
    public static double protection(int tier,int quality){
        if(tier<1||tier>7||quality<1||quality>5)throw new IllegalArgumentException("Armor rank/quality");
        // Four matching pieces: 0..24% by rank, plus 0..8% by quality.
        // Tier-one ordinary retains its exact vanilla diamond baseline.
        return ((tier-1)*.04+(quality-1)*.02)/4;
    }
    private ArmorBalance(){}
}
