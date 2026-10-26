package com.longju.xiantu.talisman;

/** Tier boundaries must not allow an introductory tome to unlock high-tier variants. */
public final class TalismanTomesRegression {
    private static int assertions;
    private static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        String[] names={"玄门符箓·初解","玄门符箓·真诠","玄门符箓·秘典"};
        check(TalismanTomes.ALL.size()==3,"exactly three tomes");
        for(int i=0;i<3;i++){check(TalismanTomes.ALL.get(i).name().equals(names[i]),"accepted book name");check(TalismanTomes.ALL.get(i).description().contains("符纸、符笔及符箓配方"),"book includes all corresponding crafts");}
        for(int tier=1;tier<=8;tier++){
            int band=tier<=3?0:tier<=6?1:2;
            var expected=TalismanTomes.ALL.get(band);
            check(TalismanTomes.forTier(tier)==expected,"correct book for tier "+tier);
            check(TalismanTomes.find(expected.id())==expected,"knowledge ID round trip");
            for(int other=0;other<3;other++)check(TalismanTomes.ALL.get(other).covers(tier)==(other==band),"independent learning bands");
        }
        check(TalismanTomes.forTier(3)==TalismanTomes.ALL.get(0),"fixed tier-three rebirth uses introductory tome");
        for(int invalid:new int[]{-1,0,9,100}){
            boolean rejected=false;
            try{TalismanTomes.forTier(invalid);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"reject invalid tier "+invalid);
        }
        check(TalismanTomes.find("fireball")==null,"individual-symbol books removed");
        System.out.println("Talisman tomes: "+assertions+" assertions PASS");
    }
}
