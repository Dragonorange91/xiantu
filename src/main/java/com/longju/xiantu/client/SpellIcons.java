package com.longju.xiantu.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

/** Client presentation only; learning scrolls retain their own item artwork. */
public final class SpellIcons {
    private static final Map<String,ResourceLocation> ICONS=Map.of(
        "fire_bolt",new ResourceLocation("xiantu","textures/gui/spells/fire_bolt.png"),
        "thunder_strike",new ResourceLocation("xiantu","textures/gui/spells/thunder_strike.png"),
        "sword_flight",new ResourceLocation("xiantu","textures/gui/spells/sword_flight.png"),
        "great_fireball",new ResourceLocation("xiantu","textures/gui/spells/great_fireball.png"),
        "rejuvenation",new ResourceLocation("xiantu","textures/gui/spells/rejuvenation.png"));
    public static ResourceLocation texture(String spell){return ICONS.get(spell);}
    public static void draw(GuiGraphics graphics,String spell,int x,int y,int size){
        var icon=texture(spell);
        if(icon!=null)graphics.blit(icon,x,y,size,size,0,0,64,64,64,64);
    }
    private SpellIcons(){}
}
