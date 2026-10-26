package com.longju.xiantu.talisman;

import java.util.List;

/** Learning grants a tier band, independently of the player's crafting realm and brush. */
public final class TalismanTomes {
    public record Tome(String id,String name,int minimum,int maximum){
        public boolean covers(int tier){return tier>=minimum&&tier<=maximum;}
        public String description(){return "解锁"+minimum+"至"+maximum+"阶符纸、符笔及符箓配方";}
    }
    public static final List<Tome> ALL=List.of(
        new Tome("xuanmen_initial","玄门符箓·初解",1,3),
        new Tome("xuanmen_true","玄门符箓·真诠",4,6),
        new Tome("xuanmen_secret","玄门符箓·秘典",7,8));
    public static Tome find(String id){return ALL.stream().filter(t->t.id().equals(id)).findFirst().orElse(null);}
    public static Tome forTier(int tier){return ALL.stream().filter(t->t.covers(tier)).findFirst().orElseThrow(()->new IllegalArgumentException("Talisman tier "+tier));}
    private TalismanTomes(){}
}
