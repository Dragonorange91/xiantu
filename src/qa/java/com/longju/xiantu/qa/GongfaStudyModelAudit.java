package com.longju.xiantu.qa;

import com.longju.xiantu.client.GongfaStudyRenderer;
import com.longju.xiantu.gongfa.GongfaRegistry;
import com.longju.xiantu.network.GongfaStudyPacket;
import com.longju.xiantu.registry.ModEntities;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.UUID;

/** Actual bakery/override contract with a detached client owner; never changes the real player/world. */
@Mod.EventBusSubscriber(modid="xiantu_qa",value=Dist.CLIENT)
public final class GongfaStudyModelAudit {
 private static int checks;
 private static void check(boolean value,String reason){checks++;if(!value)throw new IllegalStateException(reason);}
 @SubscribeEvent public static void commands(RegisterClientCommandsEvent event){
  event.getDispatcher().register(Commands.literal("xiantustudymodelaudit").executes(c->audit()));
 }
 private static int audit(){
  var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return 0;
  checks=0;UUID id=UUID.randomUUID();
  LivingEntity owner=new RemotePlayer(mc.level,new GameProfile(id,"StudyModelQA")){
   @Override public boolean isUsingItem(){return true;}
   @Override public ItemStack getUseItem(){return getMainHandItem();}
  };
  Entity seat=ModEntities.FORMATION_SEAT.get().create(mc.level);
  try{
   check(owner.startRiding(seat,true),"detached owner seats without world insertion");
   for(var method:GongfaRegistry.ALL){
    check(com.longju.xiantu.client.StudyPageGeometry.ready(method.id()),"direct PNG/cuboid reading geometry loaded "+method.id());
    var stack=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("xiantu",method.id())));
    owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
    var model=mc.getModelManager().getModel(new ModelResourceLocation("xiantu",method.id(),"inventory"));
    check(model!=mc.getModelManager().getMissingModel(),"book bakery "+method.id());
    check(net.minecraftforge.client.extensions.common.IClientItemExtensions.of(stack).getCustomRenderer() instanceof GongfaStudyRenderer,"book renderer extension");
    GongfaStudyRenderer.receive(new GongfaStudyPacket(id,"",0));
    check(!model.getOverrides().resolve(model,stack,mc.level,owner,0).isCustomRenderer(),"closed without server heartbeat");
    GongfaStudyRenderer.receive(new GongfaStudyPacket(id,method.id(),45));
    var active=model.getOverrides().resolve(model,stack,mc.level,owner,0);
    check(active.isCustomRenderer(),"active study override "+method.id());
    check(active.applyTransform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,new PoseStack(),false).isCustomRenderer(),"first-person retains animated route");
    check(!active.applyTransform(ItemDisplayContext.GUI,new PoseStack(),false).isCustomRenderer(),"GUI remains static");
    for(String side:new String[]{"left","right"}){
     var part=mc.getModelManager().getModel(new ResourceLocation("xiantu","item/gongfa_study/"+method.id()+"_"+side));
     check(GongfaStudyRenderer.validPart(part,mc.getModelManager().getMissingModel()),"real loaded study wing, geometry and non-missing sprites "+side);
    }
    GongfaStudyRenderer.receive(new GongfaStudyPacket(id,"",0));
    check(!model.getOverrides().resolve(model,stack,mc.level,owner,0).isCustomRenderer(),"cancel returns closed model");
   }
   var leaf=mc.getModelManager().getModel(new ResourceLocation("xiantu","item/gongfa_study/template_leaf"));
   check(GongfaStudyRenderer.validPart(leaf,mc.getModelManager().getMissingModel()),"turning page bakery and non-missing sprites");
   String result="[Study model QA] PASS "+checks+" actual baked/override checks; synthetic owner, not physical held-use evidence";
   mc.player.sendSystemMessage(Component.literal(result));com.mojang.logging.LogUtils.getLogger().info(result);return 1;
  }catch(Exception|LinkageError error){com.mojang.logging.LogUtils.getLogger().error("[Study model QA] FAIL "+checks,error);mc.player.sendSystemMessage(Component.literal("[Study model QA] FAIL "+checks+": "+error));return 0;}
  finally{GongfaStudyRenderer.receive(new GongfaStudyPacket(id,"",0));owner.stopRiding();}
 }
 @Mod.EventBusSubscriber(modid="xiantu_qa",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
 public static final class Bakery {
  @SubscribeEvent public static void baked(ModelEvent.BakingCompleted event){
   var missing=event.getModelManager().getMissingModel();int wings=0;
   for(var method:GongfaRegistry.ALL){var m=event.getModels().get(new ModelResourceLocation("xiantu",method.id(),"inventory"));
    if(m==null||!m.getClass().getName().contains("GongfaStudyRenderer$Book")){
     com.mojang.logging.LogUtils.getLogger().error("[Study bakery QA] FAIL missing wrapper {}",method.id());return;
    }
    for(String side:new String[]{"left","right"}){
     var part=event.getModels().get(new ResourceLocation("xiantu","item/gongfa_study/"+method.id()+"_"+side));
     if(!GongfaStudyRenderer.validPart(part,missing)){com.mojang.logging.LogUtils.getLogger().error("[Study bakery QA] FAIL invalid wing {} {}",method.id(),side);return;}wings++;
    }
   }
   if(!GongfaStudyRenderer.validPart(event.getModels().get(new ResourceLocation("xiantu","item/gongfa_study/template_leaf")),missing)){com.mojang.logging.LogUtils.getLogger().error("[Study bakery QA] FAIL invalid turning leaf");return;}
   com.mojang.logging.LogUtils.getLogger().info("[Study bakery QA] PASS all 24 book wrappers, {} wings and turning page with real sprites after complete baking",wings);
  }
 }
 private GongfaStudyModelAudit(){}
}
