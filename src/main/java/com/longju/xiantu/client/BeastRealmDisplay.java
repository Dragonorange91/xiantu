package com.longju.xiantu.client;

import com.longju.xiantu.entity.BeastRealmRules;
import com.longju.xiantu.cultivation.RealmRegistry;
import com.longju.xiantu.network.BeastRealmPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class BeastRealmDisplay {
    private static final Map<UUID,BeastRealmPacket> DATA=new HashMap<>();
    public static void receive(BeastRealmPacket p){if(BeastRealmRules.valid(p.realm(),p.stage()))DATA.put(p.uuid(),p);}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){DATA.clear();}
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent e){if(e.getLevel().isClientSide)DATA.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void name(RenderNameTagEvent e){
        var mc=Minecraft.getInstance();var p=DATA.get(e.getEntity().getUUID());
        if(p==null||mc.player==null||mc.level==null||!mc.level.dimension().location().equals(p.dimension())||mc.player.distanceToSqr(e.getEntity())>400)return;
        var realm=RealmRegistry.get(p.realm(),p.stage());
        String element=switch(p.element()){case "metal"->"金";case "wood"->"木";case "water"->"水";case "fire"->"火";case "earth"->"土";case "thunder"->"雷";default->"";};
        var content=e.getContent().copy().append(Component.literal(" ["+(p.elite()?"精英·":"")+(element.isEmpty()?"":element+"·")+realm.majorName()+realm.minorName()+"]").withStyle(p.elite()?ChatFormatting.GOLD:ChatFormatting.AQUA));
        e.setContent(content);e.setResult(Event.Result.ALLOW);
    }
    private BeastRealmDisplay(){}
}
