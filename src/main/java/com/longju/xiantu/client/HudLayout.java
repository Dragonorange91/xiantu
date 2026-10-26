package com.longju.xiantu.client;

/** Preserve compact-window sizing, then grow gradually for desktop/high-DPI windows. */
public final class HudLayout {
    public static int bossWidth(int width,int height,int panelRight) {
        double response=Math.max(1,Math.min(width/1280.,height/720.));
        int target=(int)Math.round(Math.min(1540,560*Math.pow(response,.68)));
        return Math.min(target,Math.max(200,width-2*(panelRight+12)));
    }
    public static float scale(com.mojang.blaze3d.platform.Window window) {
        double response = Math.min(window.getWidth() / 854.0, window.getHeight() / 480.0);
        double compact = Math.max(.8, Math.min(1.25, response * 1.1));
        // The old 1.25px cap made a 4K HUD almost the same size as an 854px HUD.
        // Grow above 1280x720, slower than the window, independently of GUI scale jumps.
        double growth = Math.pow(Math.max(1, response / 1.5), .75);
        double pixels = Math.min(3.5, compact * growth);
        return (float)(pixels / window.getGuiScale());
    }
    private HudLayout() {}
}
