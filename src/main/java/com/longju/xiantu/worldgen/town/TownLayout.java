package com.longju.xiantu.worldgen.town;

import java.util.List;
/** Generated from the compact town-layout.json; regenerate with sync_compact.py. */
public final class TownLayout {
    public static final int WIDTH=145, DEPTH=133, CENTER_X=72, CENTER_Z=66, ORIGIN_OFFSET=64;
    public record Merchant(String profession,int x,int y,int z,float yaw) {}
    public static final List<Merchant> MERCHANTS=List.of(
        new Merchant("wanbao_merchant",32,2,24,0),
        new Merchant("smith_merchant",117,2,22,0),
        new Merchant("alchemy_merchant",32,2,108,180),
        new Merchant("talisman_merchant",109,2,110,180),
        new Merchant("town_guide",66,1,72,0));
    private TownLayout() {}
}
