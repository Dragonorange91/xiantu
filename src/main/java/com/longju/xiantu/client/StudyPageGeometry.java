package com.longju.xiantu.client;

import com.google.gson.*;
import com.longju.xiantu.gongfa.GongfaRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.*;

/** Existing Blockbench cuboids rendered with their PNGs, independent of the item-model bakery. */
public final class StudyPageGeometry {
 private record Face(ResourceLocation texture,float[][] points,float[] uv,float nx,float ny,float nz){}
 private static volatile Map<String,List<Face>> parts=Map.of();
 public static boolean ready(String method){return parts.containsKey(method+"_left")&&parts.containsKey(method+"_right")&&parts.containsKey("template_leaf");}
 public static void reload(ResourceManager manager){
  var loaded=new HashMap<String,List<Face>>();
  for(var method:GongfaRegistry.ALL)for(String side:List.of("left","right"))load(manager,method.id()+"_"+side,loaded);
  load(manager,"template_leaf",loaded);parts=Map.copyOf(loaded);
  com.mojang.logging.LogUtils.getLogger().info("[Xiantu study] Loaded {}/49 direct textured Blockbench page parts",parts.size());
 }
 private static JsonObject json(ResourceManager manager,String name)throws java.io.IOException{
  try(var reader=manager.openAsReader(new ResourceLocation("xiantu","models/item/gongfa_study/"+name+".json"))){return JsonParser.parseReader(reader).getAsJsonObject();}
 }
 private static void load(ResourceManager manager,String name,Map<String,List<Face>> loaded){
  try{
   var model=json(manager,name);var textures=new HashMap<String,String>();
   if(model.has("parent")&&model.get("parent").getAsString().startsWith("xiantu:item/gongfa_study/")){
    var parent=json(manager,model.get("parent").getAsString().substring("xiantu:item/gongfa_study/".length()));
    parent.getAsJsonObject("textures").entrySet().forEach(e->textures.put(e.getKey(),e.getValue().getAsString()));
    model.add("elements",parent.get("elements"));
   }
   model.getAsJsonObject("textures").entrySet().forEach(e->textures.put(e.getKey(),e.getValue().getAsString()));
   var faces=new ArrayList<Face>();
   for(var value:model.getAsJsonArray("elements")){
    var element=value.getAsJsonObject();if(element.has("rotation"))throw new IllegalArgumentException("Unexpected rotated study cuboid");
    var from=element.getAsJsonArray("from");var to=element.getAsJsonArray("to");
    float x0=from.get(0).getAsFloat()/16,y0=from.get(1).getAsFloat()/16,z0=from.get(2).getAsFloat()/16;
    float x1=to.get(0).getAsFloat()/16,y1=to.get(1).getAsFloat()/16,z1=to.get(2).getAsFloat()/16;
    for(var entry:element.getAsJsonObject("faces").entrySet()){
     var f=entry.getValue().getAsJsonObject();if(f.has("rotation")&&f.get("rotation").getAsInt()!=0)throw new IllegalArgumentException("Unexpected rotated UV");
     String ref=f.get("texture").getAsString();for(int n=0;ref.startsWith("#")&&n<16;n++)ref=Objects.requireNonNull(textures.get(ref.substring(1)));
     var id=new ResourceLocation(ref);var texture=new ResourceLocation(id.getNamespace(),"textures/"+id.getPath()+".png");
     if(manager.getResource(texture).isEmpty())throw new IllegalArgumentException("Missing study PNG "+texture);
     float[][] points;float nx=0,ny=0,nz=0;
     switch(entry.getKey()){
      case "north"->{points=new float[][]{{x1,y1,z0},{x1,y0,z0},{x0,y0,z0},{x0,y1,z0}};nz=-1;}
      case "south"->{points=new float[][]{{x0,y1,z1},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1}};nz=1;}
      case "west"->{points=new float[][]{{x0,y1,z0},{x0,y0,z0},{x0,y0,z1},{x0,y1,z1}};nx=-1;}
      case "east"->{points=new float[][]{{x1,y1,z1},{x1,y0,z1},{x1,y0,z0},{x1,y1,z0}};nx=1;}
      case "up"->{points=new float[][]{{x0,y1,z0},{x0,y1,z1},{x1,y1,z1},{x1,y1,z0}};ny=1;}
      case "down"->{points=new float[][]{{x0,y0,z1},{x0,y0,z0},{x1,y0,z0},{x1,y0,z1}};ny=-1;}
      default->throw new IllegalArgumentException("Study face "+entry.getKey());
     }
     var uv=f.getAsJsonArray("uv");faces.add(new Face(texture,points,new float[]{uv.get(0).getAsFloat()/16,uv.get(1).getAsFloat()/16,uv.get(2).getAsFloat()/16,uv.get(3).getAsFloat()/16},nx,ny,nz));
    }
   }
   loaded.put(name,List.copyOf(faces));
  }catch(Exception error){com.mojang.logging.LogUtils.getLogger().error("[Xiantu study] Direct page load failed {}",name,error);}
 }
 public static void draw(String name,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
  for(var face:parts.getOrDefault(name,List.of())){
   var consumer=buffers.getBuffer(RenderType.entityCutoutNoCull(face.texture));var p=poses.last();
   for(int i=0;i<4;i++){var v=face.points[i];float u=i<2?face.uv[0]:face.uv[2],uv=i==0||i==3?face.uv[1]:face.uv[3];
    consumer.vertex(p.pose(),v[0],v[1],v[2]).color(255,255,255,255).uv(u,uv).overlayCoords(overlay).uv2(light).normal(p.normal(),face.nx,face.ny,face.nz).endVertex();
   }
  }
 }
 private StudyPageGeometry(){}
}
