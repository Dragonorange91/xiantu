package com.longju.xiantu.client;
import com.longju.xiantu.network.QitianAppearancePacket;
import com.longju.xiantu.entity.BeastResourceRules;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Render-only proxies: do not add entities to the level or run their AI. */
@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class QitianAppearance {
 private static final Map<UUID,String> FORMS=new HashMap<>();
 private static final Map<UUID,Mob> PROXIES=new HashMap<>();
 private static net.minecraft.client.multiplayer.ClientLevel level;
 public static void receive(QitianAppearancePacket p){
  var mc=Minecraft.getInstance();if(mc.level==null||!mc.level.dimension().location().equals(p.dimension()))return;
  if(level!=mc.level){clear();level=mc.level;}
  String form=BeastResourceRules.SPECIES.contains(p.species())?p.species():"";
  if(form.isEmpty()){FORMS.remove(p.owner());release(p.owner());return;}
  if(!form.equals(FORMS.put(p.owner(),form)))release(p.owner());
 }
 private static void release(UUID owner){var mob=PROXIES.remove(owner);if(mob!=null){mob.discard();net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new EntityLeaveLevelEvent(mob,mob.level()));}}
 public static void clear(){for(var id:new ArrayList<>(PROXIES.keySet()))release(id);FORMS.clear();level=null;}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(level!=mc.level){clear();level=mc.level;}if(level==null)return;
  for(var id:new ArrayList<>(FORMS.keySet())){var owner=level.getPlayerByUUID(id);if(owner==null||!owner.isAlive()){FORMS.remove(id);release(id);}}
 }
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();}
 @SubscribeEvent public static void render(RenderPlayerEvent.Pre e){
  Player player=e.getEntity();String species=FORMS.get(player.getUUID());if(species==null||!player.isAlive()||player.isSpectator())return;
  Mob proxy=PROXIES.get(player.getUUID());
  if(proxy==null){var type=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("xiantu",species));var entity=type==null?null:type.create(player.level());if(!(entity instanceof Mob mob))return;proxy=mob;proxy.setNoAi(true);proxy.setSilent(true);proxy.getPersistentData().putBoolean("xiantu_qitian_visual_proxy",true);PROXIES.put(player.getUUID(),proxy);}
  proxy.setPos(player.position());proxy.xo=player.xo;proxy.yo=player.yo;proxy.zo=player.zo;proxy.xOld=player.xOld;proxy.yOld=player.yOld;proxy.zOld=player.zOld;
  proxy.setYRot(player.getYRot());proxy.yRotO=player.yRotO;proxy.yBodyRot=player.yBodyRot;proxy.yBodyRotO=player.yBodyRotO;proxy.yHeadRot=player.yHeadRot;proxy.yHeadRotO=player.yHeadRotO;
  proxy.setXRot(player.getXRot());proxy.xRotO=player.xRotO;proxy.tickCount=player.tickCount;proxy.setOnGround(player.onGround());proxy.setDeltaMovement(player.getX()-player.xo,player.getY()-player.yo,player.getZ()-player.zo);
  // Reuse each species' accepted GLB scale, materials and movement animations.
  float yaw=net.minecraft.util.Mth.rotLerp(e.getPartialTick(),player.yBodyRotO,player.yBodyRot);
  Minecraft.getInstance().getEntityRenderDispatcher().render(proxy,0,0,0,yaw,e.getPartialTick(),e.getPoseStack(),e.getMultiBufferSource(),e.getPackedLight());e.setCanceled(true);
 }
 private QitianAppearance(){}
}
