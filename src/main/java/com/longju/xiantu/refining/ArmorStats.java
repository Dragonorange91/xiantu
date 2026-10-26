package com.longju.xiantu.refining;

/** Unscaled per-piece values. Tier one matches a full vanilla diamond set. */
public final class ArmorStats {
    private static final int[] OUTER={3,3,4,4,5,5,6};
    private static final int[] CHEST={8,9,9,10,10,11,11};
    private static final int[] LEGS={6,7,7,8,8,9,9};
    public static double protection(int tier,String slot){
        if(tier<1||tier>7)throw new IllegalArgumentException("Armor tier "+tier);
        return switch(slot){case "helmet","boots"->OUTER[tier-1];case "chestplate"->CHEST[tier-1];case "leggings"->LEGS[tier-1];default->throw new IllegalArgumentException("Armor slot "+slot);};
    }
    public static double toughness(int tier){
        if(tier<1||tier>7)throw new IllegalArgumentException("Armor tier "+tier);
        return 2+(tier-1)/3.0;
    }
    private ArmorStats(){}
}
