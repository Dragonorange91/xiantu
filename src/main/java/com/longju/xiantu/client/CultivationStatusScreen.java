package com.longju.xiantu.client;

import com.longju.xiantu.cultivation.CultivationStatusMenu;
import com.longju.xiantu.cultivation.RealmDefinition;
import com.longju.xiantu.cultivation.RealmRegistry;
import com.longju.xiantu.cultivation.SpiritRootType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

public final class CultivationStatusScreen extends FittedContainerScreen<CultivationStatusMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("xiantu", "textures/gui/cultivation_status.png");
    private Button breakthrough;
    private Button detox;

    public CultivationStatusScreen(CultivationStatusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 400;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("突破丹"), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1)).bounds(leftPos+151,topPos+375,48,14).build());
        detox=addRenderableWidget(Button.builder(Component.literal("调息排毒"), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,2)).bounds(leftPos+17,topPos+375,72,14).build());
        breakthrough = addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
        }).bounds(leftPos + 112, topPos + 375, 32, 14).build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        detox.setMessage(Component.literal(menu.meditationPaused() ? "继续修炼" : "调息排毒"));
        int x = leftPos;
        int y = topPos;
        graphics.blit(BACKGROUND, x, y, 0, 0, imageWidth, imageHeight, 256, 400);
        drawBar(graphics, x + 15, y + 52, 226, menu.progress(), menu.progressMax(), 0xFFD6A94E);
        drawBar(graphics, x + 15, y + 74, 226, menu.power(), menu.powerMax(), 0xFF44B8C2);
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, int width, double value, double max, int color) {
        graphics.fill(x, y, x + width, y + 5, 0xFF171411);
        int filled = max <= 0 ? 0 : (int) Math.round(width * Math.min(1.0D, value / max));
        if (filled > 0) graphics.fill(x, y, x + filled, y + 5, color);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        RealmDefinition realm = RealmRegistry.get(menu.majorRealm(), menu.stage());
        Component title = Component.translatable("gui.xiantu.cultivation.title");
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 8, 0xFFF3DCA7, false);
        graphics.drawString(font, Component.literal(com.longju.xiantu.cultivation.CultivationCapability.clientData().cultivationUnlocked() ? realm.majorName() + " · " + realm.minorName() : "凡人 · 尚未开启修仙"), 14, 28, 0xFFF2E8D5, false);
        graphics.drawString(font, Component.translatable("gui.xiantu.cultivation.progress",
                format(menu.progress()), format(menu.progressMax())), 14, 40, 0xFFE9CB80, false);
        graphics.drawString(font, Component.translatable("gui.xiantu.cultivation.power",
                format(menu.power()), format(menu.powerMax())), 14, 62, 0xFF95DDE0, false);
        graphics.drawString(font, Component.translatable("gui.xiantu.cultivation.roots", roots()), 15, 94, 0xFFB9D69B, false);
        graphics.drawString(font, Component.translatable("gui.xiantu.cultivation.comprehension", menu.comprehension()), 15, 109, 0xFFD6C6A1, false);
        graphics.drawString(font, Component.literal("灵脉 " + menu.veinLevel() + "级 · " + menu.veinExperience() + "/" + menu.veinNextThreshold()), 15, 124, 0xFF8FC8D0, false);
        graphics.drawString(font, Component.literal("心魔 " + format(menu.innerDemon())), 139, 94, 0xFFD58C78, false);
        if (RealmRegistry.isFinalStage(menu.majorRealm(), menu.stage())) {
            graphics.drawString(font, Component.literal("修炼上限"), 139, 113, 0xFFC9B997, false);
        } else {
            graphics.drawString(font, Component.literal("成功率 " + Math.round(menu.breakthroughChance() * 100.0D) + "%"), 139, 113, 0xFFD6C6A1, false);
        }
        var data = com.longju.xiantu.cultivation.CultivationCapability.clientData();
        var method = com.longju.xiantu.gongfa.GongfaRegistry.get(data.gongfa().active());
        boolean effective = com.longju.xiantu.gongfa.GongfaService.active(data) != null;
        int y = 158;
        y = drawWrapped(graphics, "体质 · " + data.physique().displayName, 15, y, 226, 0xFFF3DCA7, 2);
        for (String line : data.physique().description()) {
            y = drawWrapped(graphics, line, 15, y, 226, 0xFF95DDE0, 2);
        }
        y += 3;
        y = drawWrapped(graphics, "主功法 · " + (method == null ? "未装备" : method.tier() + "·" + method.name()), 15, y, 226, 0xFFF3DCA7, 1);
        var progress = data.gongfa().activeProgress();
        int layer = data.gongfa().activeLevel();
        String experience = progress == null ? "层数：—" : "层数："+layer+" / 9  "+(layer == 9 ? "已满层" : "经验："+progress.experience()+" / "+com.longju.xiantu.gongfa.GongfaRules.nextExperience(layer));
        y = drawWrapped(graphics, experience, 15, y, 226, 0xFFE7D5A5, 1);
        y = drawWrapped(graphics, String.format(Locale.ROOT,"自身修炼 ×%.2f · 灵根 ×%.2f",
                com.longju.xiantu.cultivation.CultivationEfficiency.personal(data),
                com.longju.xiantu.formation.FormationRules.rootMultiplier(data.spiritRootCount())),15,y,226,0xFF95DDE0,1);
        y = drawWrapped(graphics,String.format(Locale.ROOT,"灵力容量 +%.1f%% · 基础回复 +%.1f%%",effective ? method.powerBonus(layer)*100 : 0,effective ? method.recoveryBonus(layer)*100 : 0),15,y,226,0xFF95DDE0,2);
        y = drawWrapped(graphics,String.format(Locale.ROOT,"攻击 +%.0f%% · 生命 +%.0f%% · 护甲 +%.1f%s",
                effective ? method.attackBonus(layer)*100 : 0,effective ? method.healthBonus(layer)*100 : 0,
                effective ? method.armorBonus(layer) : 0,method != null && !effective ? "（暂停）" : ""),15,y,226,0xFFB9D69B,2);
        if (method != null) {
            drawWrapped(graphics, "被动 · " + (effective ? method.buff().nameText() + " · " + method.buff().format(method.buffBonus(layer)) : "暂停中"), 15, y, 226, 0xFFB9D69B, 2);
        }
        var medicine=data.medicine();
        graphics.drawString(font,String.format(Locale.ROOT,"丹毒 %.2f · 打坐修为 ×%.2f",medicine.toxicity(),medicine.cultivationMultiplier()),15,310,0xFFD6C6A1,false);
        graphics.drawString(font,"停药120秒后自然代谢，蒲团调息排毒更快",15,327,0xFFAA9A7B,false);
    }

    private int drawWrapped(GuiGraphics graphics, String text, int x, int y, int width, int color, int maxLines) {
        var lines = font.split(Component.literal(text), width);
        int count = Math.min(lines.size(), maxLines);
        for (int i = 0; i < count; i++) {
            graphics.drawString(font, lines.get(i), x, y, color);
            y += 12;
        }
        return y;
    }

    private Component roots() {
        StringBuilder value = new StringBuilder();
        for (SpiritRootType root : SpiritRootType.values()) {
            if ((menu.rootMask() & (1 << root.ordinal())) == 0) continue;
            value.append(root.displayName());
        }
        return Component.literal(value.isEmpty() ? "无" : value.toString());
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    protected void renderFitted(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        breakthrough.active = menu.breakthroughAvailable();
        breakthrough.setMessage(Component.literal("破境"));
        super.renderFitted(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
