package com.longju.xiantu.client;

import com.longju.xiantu.network.SkillVfxPacket;
import com.longju.xiantu.refining.WeaponActives;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Server-confirmed, depth-tested three-dimensional cast paths, independent of optional Photon. */
@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class WeaponSkillRenderer {
    private record Style(String shape,int color,double reach,double width){}
    private static final Map<String,Style> STYLES=Map.ofEntries(
        Map.entry("shadow_sever",new Style("triple",0xB1D9FF,6,.08)),
        Map.entry("radiant_return",new Style("lotus",0xF8E6B0,1.15,.055)),
        Map.entry("dragon_pierce",new Style("spear",0x61E3C3,8,.12)),
        Map.entry("forged_guard",new Style("cross",0xFFAA65,1.25,.10)),
        Map.entry("star_shatter",new Style("fan",0xC7B5FF,4,.11)),
        Map.entry("chaos_suppress",new Style("vortex",0xBD91FA,3.5,.10)),
        Map.entry("godslayer_pierce",new Style("spear",0xFF697A,6,.13)),
        Map.entry("dao_corrosion",new Style("spear",0xAA74EB,7,.10)),
        Map.entry("celestial_guard",new Style("hexagon",0x8EBBFF,1.2,.065)),
        Map.entry("spirit_sweep",new Style("fan",0x93F4C4,4.5,.065)),
        Map.entry("divine_reveal",new Style("spear",0xFFF0BB,8,.10)),
        Map.entry("great_dao_quake",new Style("shock",0xF7D27F,4,.09)),
        Map.entry("heaven_split",new Style("cleave",0xFFB16B,3,.14)),
        Map.entry("sun_pierce",new Style("spear",0xFFE18A,3,.14)));
    private record Cast(UUID owner,Vec3 origin,Vec3 forward,Style style,long start,boolean charge){}
    private static final Map<UUID,Cast> CASTS=new LinkedHashMap<>();
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new BufferBuilder(65536));
    private static ClientLevel world;
    private static boolean world(){var l=Minecraft.getInstance().level;if(l!=world){clear();world=l;}return l!=null;}
    static void clear(){CASTS.clear();world=null;}
    public static void receive(SkillVfxPacket p){
        if(!world()||!world.dimension().location().equals(p.dimension())||!Double.isFinite(p.position().lengthSqr())||!Double.isFinite(p.direction().lengthSqr()))return;
        var owner=world.getEntity(p.entityId());if(owner==null||!owner.getUUID().equals(p.owner()))return;
        var style=STYLES.get(p.skill());boolean contact=p.skill().startsWith("weapon_contact_");
        if(style==null&&!contact)return;
        if(p.phase().equals("CANCEL")||p.phase().equals("END")){CASTS.remove(p.transaction());SkillCastAnimations.end(p.owner());return;}
        boolean charge=p.phase().equals("WINDUP"),impact=style!=null&&p.phase().equals("HIT");
        if(impact)style=new Style("impact",style.color,.7,style.width*.5);
        if(contact){if(!p.phase().equals("HIT"))return;int color=switch(p.skill().substring(15)){case "water"->0x8EDBFF;case "fire"->0xFFA45C;case "thunder"->0xC7ABFF;case "wood"->0x9AF4C6;case "earth"->0xEDC58E;default->0xF5E4B5;};style=new Style("contact",color,.65,.035);}
        else if(!charge&&!impact&&!p.phase().equals("RELEASE"))return;
        if(CASTS.size()>=32){var i=CASTS.keySet().iterator();i.next();i.remove();}
        var direction=p.direction().lengthSqr()<1e-8?new Vec3(0,0,1):p.direction().normalize();
        // A contact flash must not overwrite the ongoing release path of the same cast.
        var visualKey=impact?UUID.nameUUIDFromBytes((p.transaction()+":impact:"+p.position()).getBytes(java.nio.charset.StandardCharsets.UTF_8)):p.transaction();
        CASTS.put(visualKey,new Cast(p.owner(),p.position(),direction,style,world.getGameTime(),charge));
        if(charge){var mc=Minecraft.getInstance();if(mc.player!=null&&mc.player.getUUID().equals(p.owner())){
            var recipe=com.longju.xiantu.refining.RefinedEquipment.recipe(mc.player.getMainHandItem());
            if(recipe!=null&&WeaponActives.skillId(recipe.theme()).equals(p.skill())){
                mc.player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
                // A click released before the acknowledgement must not leave a ghost charge.
                if(!mc.options.keyUse.isDown()&&mc.gameMode!=null)mc.gameMode.releaseUsingItem(mc.player);
            }
        }}
        if(!contact&&!impact){if(charge)SkillCastAnimations.begin(p.owner(),p.skill());else SkillCastAnimations.pulse(p.owner(),p.skill());}
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();}
    @SubscribeEvent public static void draw(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||!world()||CASTS.isEmpty())return;
        double time=world.getGameTime()+event.getPartialTick();var camera=event.getCamera().getPosition();var matrix=event.getPoseStack().last().pose();
        CASTS.values().removeIf(c->world.getPlayerByUUID(c.owner)==null||time-c.start>(c.charge?200:24));

        for(var c:CASTS.values()){
            var player=world.getPlayerByUUID(c.owner);if(player==null||player.distanceToSqr(camera)>64*64)continue;
            double age=time-c.start,fade=c.charge?.42:Math.sin(Math.PI*Math.min(1,(age+1)/24));
            if(fade<=0)continue;Vec3 f=c.forward,side=new Vec3(-f.z,0,f.x).normalize();if(side.lengthSqr()<1e-8)side=new Vec3(1,0,0);var up=f.cross(side).normalize();
            var origin=Set.of("contact","impact").contains(c.style.shape)?c.origin:c.charge||Set.of("lotus","cross","hexagon").contains(c.style.shape)?player.getPosition(event.getPartialTick()).add(0,1,0):c.origin.add(0,1.05,0);
            String shape=c.charge?"charge":c.style.shape;double reach=c.style.reach;
            WeaponSkillArt.draw(BUFFERS,matrix,camera,shape,origin,f,side,up,age,reach,c.style.color);
        }BUFFERS.endBatch();
    }
    private WeaponSkillRenderer(){}
}
