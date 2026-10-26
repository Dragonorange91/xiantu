package com.longju.xiantu.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.resources.ResourceLocation;
import com.longju.xiantu.refining.WorkstationLayout;

/** Shared nine-sliced jade/bronze skin. Geometry and item hit boxes remain menu-owned. */
public final class WorkstationGui {
    public static final int TEXT=0xffefdfb7,MUTED=0xffa9babc,ACCENT=0xffc4a76a;
    private static final ResourceLocation PANEL=new ResourceLocation("xiantu","textures/gui/cultivation/panel.png"),
        PAPER=new ResourceLocation("xiantu","textures/gui/cultivation/paper.png"),
        SLOT=new ResourceLocation("xiantu","textures/gui/cultivation/slot.png"),
        BAGUA=new ResourceLocation("xiantu","textures/gui/cultivation/bagua.png");
    private static void region(GuiGraphics g,ResourceLocation tex,int x,int y,int w,int h,int u,int v,int sw,int sh,int tw,int th){
        if(w>0&&h>0)g.blit(tex,x,y,w,h,(float)u,(float)v,sw,sh,tw,th);
    }
    public static void frame(GuiGraphics g,int x,int y,int w,int h,boolean paper){
        ResourceLocation tex=paper?PAPER:PANEL;int b=Math.min(12,Math.min(w,h)/3),s=48;
        region(g,tex,x,y,b,b,0,0,s,s,256,256);region(g,tex,x+w-b,y,b,b,256-s,0,s,s,256,256);
        region(g,tex,x,y+h-b,b,b,0,256-s,s,s,256,256);region(g,tex,x+w-b,y+h-b,b,b,256-s,256-s,s,s,256,256);
        region(g,tex,x+b,y,w-2*b,b,s,0,256-2*s,s,256,256);region(g,tex,x+b,y+h-b,w-2*b,b,s,256-s,256-2*s,s,256,256);
        region(g,tex,x,y+b,b,h-2*b,0,s,s,256-2*s,256,256);region(g,tex,x+w-b,y+b,b,h-2*b,256-s,s,s,256-2*s,256,256);
        region(g,tex,x+b,y+b,w-2*b,h-2*b,s,s,256-2*s,256-2*s,256,256);
    }
    public static void panel(GuiGraphics g,int x,int y,int w,int h){
        frame(g,x,y,w,h,false);
    }
    public static void background(GuiGraphics g,int x,int y,int w,int h){
        frame(g,x,y,w,h,false);
        g.fill(x+6,y+20,x+w-6,y+21,ACCENT);
        panel(g,x+6,y+44,152,h-50);panel(g,x+WorkstationLayout.PANEL_X,y+WorkstationLayout.PANEL_Y,w-174,WorkstationLayout.PANEL_HEIGHT);
        panel(g,x+168,y+218,w-174,42);
        panel(g,x+168,y+264,184,88);panel(g,x+356,y+264,w-362,88);
    }
    public static void slot(GuiGraphics g,int x,int y,Slot s){
        slotAt(g,x+s.x,y+s.y);
    }
    public static void slotAt(GuiGraphics g,int sx,int sy){region(g,SLOT,sx-1,sy-1,18,18,0,0,32,32,32,32);}
    public static void progress(GuiGraphics g,int x,int y,int w,int elapsed,int total){
        g.fill(x,y,x+w,y+3,0xff16272b);g.fill(x,y,x+w*Math.max(0,Math.min(elapsed,Math.max(1,total)))/Math.max(1,total),y+3,ACCENT);
    }
    public static void arrow(GuiGraphics g,int x,int y,int w){
        g.fill(x,y,x+w-3,y+2,ACCENT);
        for(int i=0;i<4;i++)g.fill(x+w-4+i,y-3+i,x+w-3+i,y+5-i,ACCENT);
    }
    public static void bagua(GuiGraphics g,int cx,int cy){
        region(g,BAGUA,cx-WorkstationLayout.BAGUA_SIZE/2,cy-WorkstationLayout.BAGUA_SIZE/2+WorkstationLayout.BAGUA_ART_Y_OFFSET,
            WorkstationLayout.BAGUA_SIZE,WorkstationLayout.BAGUA_SIZE,0,0,192,192,192,192);
    }
    private WorkstationGui(){}
}
