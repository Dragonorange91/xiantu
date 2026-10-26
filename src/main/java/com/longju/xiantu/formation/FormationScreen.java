package com.longju.xiantu.formation;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FormationScreen extends com.longju.xiantu.client.FittedContainerScreen<FormationMenu> {
    private net.minecraft.client.gui.components.Button force, switchEffect;
    private int pendingRank;
    private boolean pendingSwitch;
    public FormationScreen(FormationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 238;
    }

    @Override protected void init() {
        super.init();
        force = addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("强行修炼"), b -> {
            int rank = menu.bonusRank();
            if (pendingRank != rank) { pendingRank = rank; b.setMessage(Component.literal("确认立即死亡")); return; }
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 100 + rank);
            pendingRank = 0;
        }).bounds(leftPos+10, topPos+127, 110, 20).build());
        switchEffect = addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("切换加成"), b -> {
            if (!pendingSwitch) { pendingSwitch = true; b.setMessage(Component.literal("确认舍弃剩余时长")); return; }
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
            pendingSwitch = false; b.setMessage(Component.literal("切换加成"));
        }).bounds(leftPos+128, topPos+127, 118, 20).build());
    }

    @Override protected void containerTick() {
        super.containerTick();
        force.active = menu.unsafe() && menu.structureValid() && menu.runtimeSeconds() > 0;
        switchEffect.active = menu.structureValid() && menu.runtimeSeconds() > 0 && menu.slots.get(1).hasItem();
        if (!force.active || pendingRank != menu.bonusRank()) { pendingRank = 0; force.setMessage(Component.literal("强行修炼")); }
    }
    private static String time(long seconds) { return seconds / 3600 + "时" + seconds % 3600 / 60 + "分" + seconds % 60 + "秒"; }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        com.longju.xiantu.client.WorkstationGui.frame(graphics,x,y,imageWidth,imageHeight,false);
        for (var slot : menu.slots) {
            int sx = x + slot.x, sy = y + slot.y;
            com.longju.xiantu.client.WorkstationGui.slotAt(graphics,sx,sy);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 7, 0xFFF1E7C8, false);
        graphics.drawString(font, "运行："+time(menu.runtimeSeconds()), 10, 25, 0xFFE5D8A6, false);
        graphics.drawString(font, "加成："+time(menu.bonusSeconds()), 139, 25, 0xFF9ED8D0, false);
        graphics.drawString(font, "供能（可放部分灵石）", 10, 39, 0xFFE5D8A6, false);
        graphics.drawString(font, "加成（只收完整灵石）", 130, 39, 0xFF9ED8D0, false);
        graphics.drawString(font, menu.tier()+"级阵 · "+menu.powerPerMinute()+"灵力/分钟 · "+(menu.structureValid()?"结构完整":"结构失效"), 10, 74, 0xFFE9DEC3, false);
        String effect = menu.bonusRank() == 0 ? "无灵石加成" : menu.bonusRank()+"阶效果 · +"+Math.round((FormationRules.bonusMultiplier(menu.tier(),menu.bonusRank())-1)*100)+"% · 安全境界："+com.longju.xiantu.cultivation.RealmRegistry.MAJOR_REALM_NAMES.get(FormationRules.minimumRealm(menu.bonusRank()));
        graphics.drawString(font, effect, 10, 88, 0xFF9ED8D0, false);
        graphics.drawString(font, menu.unsafe()?"危险：越级修炼将立即爆体死亡":"中央蒲团修炼；潜行或受伤停止", 10, 102, menu.unsafe()?0xFFFF9279:0xFF9DDD9A, false);
        graphics.drawString(font, "无人打坐也消耗；停阵时两项暂停", 10, 114, 0xFFE9DEC3, false);
    }

    @Override
    protected void renderFitted(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
