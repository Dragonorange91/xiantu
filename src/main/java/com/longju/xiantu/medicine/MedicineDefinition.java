package com.longju.xiantu.medicine;

public record MedicineDefinition(String registryId, String displayName, int tier, boolean plantable,
                                 boolean wildPlant, boolean seedItem) {
}
