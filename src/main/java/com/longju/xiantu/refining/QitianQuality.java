package com.longju.xiantu.refining;

/** Quality changes potency, never skill access or costs. */
public final class QitianQuality {
    public static double scale(int quality){return switch(Math.max(1,Math.min(5,quality))){case 2->1.15;case 3->1.35;case 4->1.6;case 5->2;default->1;};}
    private QitianQuality(){}
}
