package com.longju.xiantu.item;

public enum SpiritStoneTier {
    FAN_LING_SHI("fan_ling_shi", 1, 100),
    LING_JING("ling_jing", 2, 310),
    LING_YU("ling_yu", 3, 670),
    XUAN_JING("xuan_jing", 4, 2_790),
    DI_SUI("di_sui", 5, 8_620),
    TIAN_JING("tian_jing", 6, 35_300),
    XIAN_YU("xian_yu", 7, 73_500),
    DAO_SHI("dao_shi", 8, 299_000);

    private final String registryName;
    private final int rank;
    private final long spiritualPower;

    SpiritStoneTier(String registryName, int rank, long spiritualPower) {
        this.registryName = registryName;
        this.rank = rank;
        this.spiritualPower = spiritualPower;
    }

    public String registryName() {
        return registryName;
    }

    public int rank() {
        return rank;
    }

    public int downcraftCount() {
        return rank == 1 ? 0 : (int) (spiritualPower / values()[rank - 2].spiritualPower);
    }

    public long spiritualPower() {
        return spiritualPower;
    }
}
