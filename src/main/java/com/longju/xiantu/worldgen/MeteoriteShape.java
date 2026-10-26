package com.longju.xiantu.worldgen;

import java.util.ArrayList;
import java.util.List;

public enum MeteoriteShape {
    SMALL(new int[]{0, 1, 2, 1, 0}, 1, 1),
    MEDIUM(new int[]{0, 2, 2, 3, 2, 2, 0}, 4, 3),
    LARGE(new int[]{0, 2, 3, 3, 4, 3, 3, 2, 0}, 8, 6);

    private final int[] layerRadii;
    private final int starDustCount;
    private final int starOreCount;

    MeteoriteShape(int[] layerRadii, int starDustCount, int starOreCount) {
        this.layerRadii = layerRadii.clone();
        this.starDustCount = starDustCount;
        this.starOreCount = starOreCount;
    }

    public int height() {
        return layerRadii.length;
    }

    public int maxWidth() {
        int maxRadius = 0;
        for (int radius : layerRadii) maxRadius = Math.max(maxRadius, radius);
        return maxRadius * 2 + 1;
    }

    public int starDustCount() {
        return starDustCount;
    }

    public int starOreCount() {
        return starOreCount;
    }

    public static MeteoriteShape fromRoll(int roll) {
        if (roll < 0 || roll >= 10) throw new IllegalArgumentException("roll must be between 0 and 9");
        return roll < 3 ? SMALL : roll < 8 ? MEDIUM : LARGE;
    }

    public List<Cell> cells() {
        List<Cell> cells = new ArrayList<>();
        for (int y = 0; y < layerRadii.length; y++) {
            int radius = layerRadii[y];
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        cells.add(new Cell(x, y, z));
                    }
                }
            }
        }
        return cells;
    }

    public List<Cell> interiorCells() {
        List<Cell> interior = new ArrayList<>();
        for (Cell cell : cells()) {
            if (contains(cell.x + 1, cell.y, cell.z) && contains(cell.x - 1, cell.y, cell.z)
                    && contains(cell.x, cell.y + 1, cell.z) && contains(cell.x, cell.y - 1, cell.z)
                    && contains(cell.x, cell.y, cell.z + 1) && contains(cell.x, cell.y, cell.z - 1)) {
                interior.add(cell);
            }
        }
        return interior;
    }

    private boolean contains(int x, int y, int z) {
        if (y < 0 || y >= layerRadii.length) return false;
        int radius = layerRadii[y];
        return x * x + z * z <= radius * radius;
    }

    public record Cell(int x, int y, int z) {}
}
