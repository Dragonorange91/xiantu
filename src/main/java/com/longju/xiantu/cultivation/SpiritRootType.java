package com.longju.xiantu.cultivation;

public enum SpiritRootType {
    METAL("金"), WOOD("木"), WATER("水"), FIRE("火"), EARTH("土");
    private final String displayName;
    SpiritRootType(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
}
