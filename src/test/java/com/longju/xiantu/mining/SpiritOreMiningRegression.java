package com.longju.xiantu.mining;

public final class SpiritOreMiningRegression {
    private static int checks;

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
        System.out.println("[Spirit ore PASS] " + label);
    }

    public static void main(String[] args) {
        require(SpiritOreMiningRules.experienceForOre(1) == 1
                        && SpiritOreMiningRules.experienceForOre(4) == 2
                        && SpiritOreMiningRules.experienceForOre(6) == 4
                        && SpiritOreMiningRules.experienceForOre(7) == 8
                        && SpiritOreMiningRules.experienceForOre(8) == 16,
                "Vein experience scales by ore tier");
        require(SpiritVeinProgress.levelForExperience(127) == 0
                        && SpiritVeinProgress.levelForExperience(128) == 1
                        && SpiritVeinProgress.levelForExperience(1_023) == 1
                        && SpiritVeinProgress.levelForExperience(1_024) == 2
                        && SpiritVeinProgress.levelForExperience(8_191) == 2
                        && SpiritVeinProgress.levelForExperience(8_192) == 3,
                "Vein level thresholds are inclusive and ordered");
        require(SpiritVeinProgress.nextThresholdForLevel(0) == 128
                        && SpiritVeinProgress.nextThresholdForLevel(1) == 1_024
                        && SpiritVeinProgress.nextThresholdForLevel(2) == 8_192
                        && SpiritVeinProgress.nextThresholdForLevel(3) == 8_192,
                "Vein display threshold matches the current level");
        require(SpiritVeinProgress.displayExperienceForLevel(20, 1) == 20
                        && SpiritVeinProgress.displayExperienceForLevel(10_000, 3) == 8_192,
                "Menu progress is bounded to short-safe level thresholds");
        var originalPlayerData = new net.minecraft.nbt.CompoundTag();
        originalPlayerData.putInt("xiantu_spirit_vein_xp", 1_024);
        originalPlayerData.putInt("xiantu_spirit_vein_level", 2);
        var respawnedPlayerData = new net.minecraft.nbt.CompoundTag();
        SpiritVeinProgress.copy(originalPlayerData, respawnedPlayerData);
        require(respawnedPlayerData.getInt("xiantu_spirit_vein_xp") == 1_024
                        && respawnedPlayerData.getInt("xiantu_spirit_vein_level") == 2,
                "Vein level and experience survive player cloning");
        originalPlayerData.putInt("xiantu_spirit_vein_xp", -1);
        originalPlayerData.putInt("xiantu_spirit_vein_level", 99);
        var sanitizedPlayerData = new net.minecraft.nbt.CompoundTag();
        SpiritVeinProgress.copy(originalPlayerData, sanitizedPlayerData);
        require(sanitizedPlayerData.getInt("xiantu_spirit_vein_xp") == 0
                        && sanitizedPlayerData.getInt("xiantu_spirit_vein_level") == 3,
                "Cloned vein data is clamped to valid values");
        require(Math.abs(SpiritOreMiningRules.bonusChance(1) - 0.15D) < 1e-9
                        && Math.abs(SpiritOreMiningRules.bonusChance(5) - 0.30D) < 1e-9
                        && SpiritOreMiningRules.bonusChance(99) == 0.30D,
                "Pickaxe quality bonus odds stay in range");
        require(SpiritOreMiningRules.rollBonusDrops(0, 5, () -> 0.0D) == 0,
                "Level zero performs no bonus rolls");
        require(SpiritOreMiningRules.rollBonusDrops(3, 5, () -> 0.29D) == 3,
                "Three successful quality-five rolls award three bonus stones");
        require(SpiritOreMiningRules.rollBonusDrops(3, 1, () -> 0.15D) == 0,
                "Roll equal to the chance threshold does not pass");
        require(SpiritOreMiningRules.rollBonusDrops(3, 1, new Sequence(0.14D, 0.15D, 0.10D)) == 2,
                "Each vein level roll uses the configured pickaxe-quality chance");
        require(SpiritOreMiningRules.rollBonusDrops(99, 99, () -> 0.0D) == 3,
                "Bonus rolls clamp to three levels and five qualities");
        System.out.println("Spirit ore mining regression: " + checks + " passed");
    }

    private static final class Sequence implements java.util.function.DoubleSupplier {
        private final double[] values;
        private int index;

        private Sequence(double... values) {
            this.values = values;
        }

        @Override
        public double getAsDouble() {
            return values[Math.min(index++, values.length - 1)];
        }
    }
}
