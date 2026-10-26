package com.longju.xiantu.cultivation;

public record RealmDefinition(int index, int majorIndex, int stage, String majorName,
                              String minorName, long progressCap, long powerCap) {
    public double progressCoefficient() {
        return progressCap;
    }
}
