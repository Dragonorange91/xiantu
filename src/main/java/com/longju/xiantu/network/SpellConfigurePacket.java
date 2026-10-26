package com.longju.xiantu.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.*;
import java.util.function.Supplier;
public record SpellConfigurePacket(int menu,int revision,List<String> slots){
 public static void encode(SpellConfigurePacket p,FriendlyByteBuf b){b.writeInt(p.menu);b.writeInt(p.revision);for(String id:p.slots)b.writeUtf(id,64);}
 public static SpellConfigurePacket decode(FriendlyByteBuf b){int menu=b.readInt(),rev=b.readInt();var ids=new ArrayList<String>();for(int i=0;i<com.longju.xiantu.spell.SpellData.TOTAL_SLOTS;i++)ids.add(b.readUtf(64));return new SpellConfigurePacket(menu,rev,List.copyOf(ids));}
 public static void handle(SpellConfigurePacket m,Supplier<NetworkEvent.Context> c){var p=c.get().getSender();if(p!=null&&p.containerMenu instanceof com.longju.xiantu.spell.SpellMenu&&p.containerMenu.containerId==m.menu&&p.containerMenu.stillValid(p)){
  boolean ok=m.slots.size()==com.longju.xiantu.spell.SpellData.TOTAL_SLOTS;for(String id:m.slots)if(!id.isEmpty()&&!com.longju.xiantu.spell.SpellService.denial(p,com.longju.xiantu.spell.SpellCatalog.find(id),true).isEmpty())ok=false;
  if(ok)ok=com.longju.xiantu.spell.SpellService.data(p).configure(m.revision,m.slots);if(!ok)p.sendSystemMessage(Component.literal("法术配置未保存：资格或数据已变化，请重新打开"));com.longju.xiantu.spell.SpellService.sync(p);
 }c.get().setPacketHandled(true);}
}
