package com.longju.xiantu.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Keep a larger logical layout usable in small windows without changing the user's GUI setting. */
public abstract class FittedContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private float fit=1;
    protected FittedContainerScreen(T menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @Override protected void init(){
        int realWidth=minecraft.getWindow().getGuiScaledWidth(),realHeight=minecraft.getWindow().getGuiScaledHeight();
        fit=Math.min(1F,Math.min(realWidth/(float)(imageWidth+4),realHeight/(float)(imageHeight+4)));
        width=Math.round(realWidth/fit);height=Math.round(realHeight/fit);super.init();
    }
    @Override public final void render(GuiGraphics g,int mx,int my,float dt){
        g.pose().pushPose();g.pose().scale(fit,fit,1);
        try{renderFitted(g,Math.round(mx/fit),Math.round(my/fit),dt);}finally{g.pose().popPose();}
    }
    protected void renderFitted(GuiGraphics g,int mx,int my,float dt){super.render(g,mx,my,dt);}
    @Override public boolean mouseClicked(double x,double y,int b){return clickFitted(x/fit,y/fit,b);}
    protected boolean clickFitted(double x,double y,int b){return super.mouseClicked(x,y,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return releaseFitted(x/fit,y/fit,b);}
    protected boolean releaseFitted(double x,double y,int b){return super.mouseReleased(x,y,b);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return dragFitted(x/fit,y/fit,b,dx/fit,dy/fit);}
    protected boolean dragFitted(double x,double y,int b,double dx,double dy){return super.mouseDragged(x,y,b,dx,dy);}
    @Override public final boolean mouseScrolled(double x,double y,double d){return scrollFitted(x/fit,y/fit,d);}
    protected boolean scrollFitted(double x,double y,double d){return super.mouseScrolled(x,y,d);}
}
