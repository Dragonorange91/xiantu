package com.longju.xiantu.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.function.BooleanSupplier;

/** A keyboard-accessible item choice with a persistent selection outline. */
public final class ItemChoiceButton extends Button {
    private ItemStack icon=ItemStack.EMPTY;
    private net.minecraft.resources.ResourceLocation texture;
    private BooleanSupplier selected=()->false;
    public ItemChoiceButton(int x,int y,OnPress press){super(x,y,24,24,Component.empty(),press,DEFAULT_NARRATION);}
    public void choice(ItemStack stack,Component description,BooleanSupplier chosen){
        icon=stack.copy();icon.setCount(1);texture=null;setMessage(description);selected=chosen;
    }
    public void choice(net.minecraft.resources.ResourceLocation sprite,Component description,BooleanSupplier chosen){
        icon=ItemStack.EMPTY;texture=sprite;setMessage(description);selected=chosen;
    }
    @Override public void renderWidget(GuiGraphics g,int mx,int my,float dt){
        int x=getX(),y=getY();boolean chosen=selected.getAsBoolean();
        g.fill(x,y,x+width,y+height,chosen?WorkstationGui.ACCENT:isHoveredOrFocused()?0xffa9babc:0xff506668);
        g.fill(x+1,y+1,x+width-1,y+height-1,active?0xff1c3035:0xff253439);
        if(texture!=null)g.blit(texture,x+2,y+2,20,20,0,0,64,64,64,64);
        else if(!icon.isEmpty())g.renderItem(icon,x+4,y+4);
        if(!active)g.fill(x+1,y+1,x+width-1,y+height-1,0x66303b3e);
    }
    /** Draw in the same coordinate space as its screen, including fitted small-window layouts. */
    public void renderChoiceTooltip(GuiGraphics g,Font font,int mx,int my){
        if(visible&&isMouseOver(mx,my))g.renderTooltip(font,getMessage(),mx,my);
    }
}
