package com.longju.xiantu.refining;

/** Shared client/server slot coordinates. Slot indices and saved inventories do not change. */
public final class WorkstationLayout {
    private static final int[] DX={-8,24,36,24,-8,-40,-52,-40},DY={-53,-40,-8,24,37,24,-8,-40};
    public static int materialX(int index){return BAGUA_X+DX[index];}
    public static int materialY(int index){return BAGUA_Y+DY[index];}
    public static final int WIDTH=550,HEIGHT=358;
    public static final int BAGUA_X=256,BAGUA_Y=128,BAGUA_SIZE=148,BAGUA_ART_Y_OFFSET=3;
    public static final int STONE_X=186,FIRE_X=BAGUA_X-8,FIRE_Y=BAGUA_Y-8,FUEL_Y=288,OUTPUT_X=508,OUTPUT_Y=112;
    public static final int BLANK_X=244,BLANK_Y=112,INVENTORY_X=368,INVENTORY_Y=272,HOTBAR_Y=330;
    public static final int REQUIREMENT_X=186,REQUIREMENT_Y=238,REQUIREMENT_STEP=30;
    public static final int AUX_X=408,AUX_Y=112,PANEL_X=168,PANEL_Y=44,PANEL_HEIGHT=172;
    private WorkstationLayout(){}
}
