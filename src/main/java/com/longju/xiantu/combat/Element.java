package com.longju.xiantu.combat;

/** Stable combat identities; thunder is deliberately outside the five-element cycle. */
public enum Element {
    PHYSICAL("physical", "物理"), METAL("metal", "金"), WOOD("wood", "木"),
    WATER("water", "水"), FIRE("fire", "火"), EARTH("earth", "土"), THUNDER("thunder", "雷");

    private final String id, displayName;
    Element(String id, String displayName) { this.id = id; this.displayName = displayName; }
    public String id() { return id; }
    public String displayName() { return displayName; }
    public boolean elemental() { return this != PHYSICAL; }
    public boolean overcomes(Element other) {
        return switch (this) {
            case METAL -> other == WOOD;
            case WOOD -> other == EARTH;
            case EARTH -> other == WATER;
            case WATER -> other == FIRE;
            case FIRE -> other == METAL;
            default -> false;
        };
    }
}
