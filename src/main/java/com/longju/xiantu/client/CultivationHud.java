package com.longju.xiantu.client;

import com.longju.xiantu.cultivation.CultivationCapability;
import com.longju.xiantu.cultivation.RealmDefinition;
import com.longju.xiantu.cultivation.RealmRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = com.longju.xiantu.XiantuMod.MOD_ID)
public final class CultivationHud {
    private static final ResourceLocation[] REALM_ICONS = {
            icon("qi"), icon("foundation"), icon("golden_core"), icon("nascent_soul"),
            icon("spirit_transformation"), icon("void_refinement"), icon("body_integration"),
            icon("great_vehicle"), icon("tribulation")
    };

    private CultivationHud() {}
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        CultivationCapability.resetClient();
    }

    @SubscribeEvent
    public static void hideVanillaBars(RenderGuiOverlayEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!visible(minecraft)) return;
        var id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id()) || id.equals(VanillaGuiOverlay.FOOD_LEVEL.id())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!visible(minecraft)) return;
        var data = CultivationCapability.clientData();
        RealmDefinition realm = RealmRegistry.get(data.majorRealm(), data.stage());
        GuiGraphics graphics = event.getGuiGraphics();
        // Scale the whole composition, including type and badge, on narrow windows.
        float scale = Math.min(HudLayout.scale(event.getWindow()), (event.getWindow().getGuiScaledWidth()-8)/210F);
        graphics.pose().pushPose();
        graphics.pose().translate(4, 4, 0);
        graphics.pose().scale(scale, scale, 1);
        int x = 10;
        int y = 10;
        int iconSize = 32;
        drawRoundedRect(graphics, 0, 0, 210, 77, 0xC4121C1B, 5);
        graphics.fill(7, 5, 203, 6, 0x806B806A);
        graphics.fill(7, 71, 203, 72, 0x606B806A);
        graphics.fill(5, 7, 6, 16, 0xFFC3AC73);
        graphics.fill(204, 61, 205, 70, 0xFFC3AC73);
        drawRoundedRect(graphics, x - 3, y - 3, 38, 38, 0xBF697B64, 19);
        drawRoundedRect(graphics, x - 2, y - 2, 36, 36, 0xF01B2421, 18);
        if (data.cultivationUnlocked()) {
            ResourceLocation icon = REALM_ICONS[Math.max(0, Math.min(REALM_ICONS.length - 1, data.majorRealm()))];
            graphics.blit(icon, x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);
        } else {
            drawRoundedRect(graphics, x, y, iconSize, iconSize, 0xFF8A826E, 16);
            drawRoundedRect(graphics, x + 2, y + 2, 28, 28, 0xFF211D1A, 14);
            drawCenteredLabel(graphics, minecraft, "凡", x + iconSize / 2, y + 11, 0xFFF4E7CA);
        }
        drawCenteredLabel(graphics, minecraft, data.cultivationUnlocked() ? realm.majorName() : "凡人",
                x + iconSize / 2, 48, 0xFFE7D5A5);
        drawCenteredLabel(graphics, minecraft, data.cultivationUnlocked() ? realm.minorName() : "未入门",
                x + iconSize / 2, 60, 0xFFB9C3B5);

        int barX = 48;
        drawBar(graphics, minecraft, barX, 14, 158, 0xFFD85C55,
                "生命", minecraft.player.getHealth(), minecraft.player.getMaxHealth());
        String health = healthText(minecraft.player.getHealth()) + " / " + healthText(minecraft.player.getMaxHealth());
        int healthX = barX + 152 - minecraft.font.width(health);
        // Keep large late-game health values inside the bar without hiding its label.
        float healthScale = Math.min(1.0F, 110.0F / Math.max(1, minecraft.font.width(health)));
        graphics.pose().pushPose();
        graphics.pose().translate(healthScale < 1 ? barX + 152 - minecraft.font.width(health) * healthScale : healthX, 14, 0);
        graphics.pose().scale(healthScale, 1, 1);
        graphics.drawString(minecraft.font, health, 0, 0, 0xFFFFF4E8, false);
        graphics.pose().popPose();
        drawBar(graphics, minecraft, barX, 27, 140, 0xFF43BBC4,
                "灵力", data.power(), data.powerMax());
        drawBar(graphics, minecraft, barX, 40, 120, 0xFFE5BD56,
                "修为", data.cultivationUnlocked() ? data.progress() : 0, data.progressMax());
        drawBar(graphics, minecraft, barX, 53, 100, 0xFFE18B46,
                "饱食度", minecraft.player.getFoodData().getFoodLevel(), 20);
        graphics.pose().popPose();
    }

    private static void drawCenteredLabel(GuiGraphics graphics, Minecraft minecraft, String label,
                                          int centerX, int y, int color) {
        graphics.drawString(minecraft.font, label, centerX - minecraft.font.width(label) / 2, y, color, false);
    }

    private static boolean visible(Minecraft minecraft) {
        return minecraft.player != null && minecraft.screen == null
                && !minecraft.options.hideGui && !minecraft.player.isSpectator();
    }

    private static String healthText(float value) {
        long tenths = Math.round(Math.max(0.0D, value) * 10);
        return tenths % 10 == 0 ? Long.toString(tenths / 10) : (tenths / 10) + "." + (tenths % 10);
    }

    private static ResourceLocation icon(String name) {
        return new ResourceLocation("xiantu", "textures/gui/realm/round/" + name + ".png");
    }

    private static void drawBar(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width,
                                int color, String label, double value, double max) {
        int height = 10;
        drawRoundedRect(graphics, x, y, width, height, 0xFF788374, 5);
        drawRoundedRect(graphics, x + 1, y + 1, width - 2, height - 2, 0xFF222C29, 4);
        int filled = max <= 0 ? 0 : (int) Math.round((width - 2) * Math.max(0.0D, Math.min(1.0D, value / max)));
        if (filled > 0) {
            drawRoundedRect(graphics, x + 1, y + 1, filled, height - 2, color, 4);
            if (filled > 6) graphics.fill(x + 4, y + 2, x + filled - 1, y + 3, 0x50FFFFFF);
        }
        int labelY = y + (height - minecraft.font.lineHeight) / 2;
        graphics.drawString(minecraft.font, label, x + 7, labelY, 0xFFF8F0E2, false);
    }

    private static void drawRoundedRect(GuiGraphics graphics, int x, int y, int width, int height, int color, int radius) {
        if (width <= 0 || height <= 0) return;
        int corner = Math.min(radius, Math.min(width / 2, height / 2));
        // Unmanaged fill flushes every span; keep the identical geometry in one batch.
        graphics.drawManaged(() -> {
            for (int row = 0; row < height; row++) {
                int inset = 0;
                if (row < corner) {
                    double dy = corner - row - 0.5D;
                    inset = corner - (int) Math.floor(Math.sqrt(corner * corner - dy * dy));
                } else if (row >= height - corner) {
                    double dy = row - (height - corner) + 0.5D;
                    inset = corner - (int) Math.floor(Math.sqrt(corner * corner - dy * dy));
                }
                graphics.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
            }
        });
    }
}
