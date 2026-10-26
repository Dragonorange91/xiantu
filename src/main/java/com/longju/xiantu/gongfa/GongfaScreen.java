package com.longju.xiantu.gongfa;

import com.longju.xiantu.cultivation.CultivationCapability;
import com.longju.xiantu.cultivation.RealmRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class GongfaScreen extends com.longju.xiantu.client.FittedContainerScreen<GongfaMenu> {
    public GongfaScreen(GongfaMenu menu, Inventory inventory, Component title) {
        super(menu,inventory,title); imageWidth = 284; imageHeight = 216;
    }
    private void send(int button) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId,button);
    }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("确认修习"),b -> send(0)).bounds(leftPos+12,topPos+183,124,20).build());
        addRenderableWidget(Button.builder(Component.literal("暂不修习"),b -> send(1)).bounds(leftPos+148,topPos+183,124,20).build());
    }
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY) {
        com.longju.xiantu.client.WorkstationGui.frame(graphics,leftPos,topPos,imageWidth,imageHeight,false);
    }
    private void text(GuiGraphics graphics,String value,int y,int color) { graphics.drawString(font,value,12,y,color,false); }
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY) {
        var data = CultivationCapability.clientData();
        var method = GongfaRegistry.get(menu.methodId());
        if (method == null) return;
        boolean entrance = method.tierIndex() == 0, first = data.gongfa().active().isEmpty();
        text(graphics,entrance ? "开启修仙之路" : first ? "修习主功法" : "是否散功重修？",12,0xf3dca7);
        text(graphics,(entrance ? "" : method.tier()+" · ")+method.name(),34,0x95dde0);
        if (entrance) {
            text(graphics,"开启修炼资格，踏入练气一层",57,0xe7d5a5);
            text(graphics,"不占主功法槽，可修至练气九层",77,0xe7d5a5);
            text(graphics,"普通死亡仍保留入门资格",97,0xb9d69b);
        } else if (first) {
            text(graphics,"首次修习，不扣境界与阶段进度",57,0xb9d69b);
            text(graphics,"学成后立即作为当前主功法",77,0xe7d5a5);
            text(graphics,"功法从0层开始修炼",97,0xe7d5a5);
        } else {
            var old = GongfaRegistry.get(data.gongfa().active());
            var projected = RealmRegistry.get(GongfaService.previewRealm(data),GongfaService.previewStage(data));
            text(graphics,"当前："+(old == null ? "未知" : old.name()),57,0xe7d5a5);
            text(graphics,"散功后："+projected.majorName()+" · "+projected.minorName(),77,0xd58c78);
            text(graphics,"新旧功法层数清零，阶段进度清零",97,0xd58c78);
            text(graphics,"相关装备和法术重新检查修为要求",117,0xc9b997);
        }
        text(graphics,"确认后盘坐蒲团，长按右键10秒",139,0xe7d5a5);
        text(graphics,"中断不散功；功法书保留，可重复使用",159,0xb9d69b);
    }
    @Override protected void renderFitted(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        renderBackground(graphics); super.render(graphics,mouseX,mouseY,partial);
    }
}
