package com.longju.xiantu.client;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.*;

/** Static Blockbench geometry baked once at reload; no world mesh instance or per-frame IO. */
final class RealmGateMesh {
    private record Face(float[][] v,float[] n,float[] uv,ResourceLocation texture,boolean emissive){}
    private static List<Face> faces=List.of();
    static void reload(ResourceManager manager){
        List<Face> result=new ArrayList<>();
        try(var r=manager.openAsReader(new ResourceLocation("xiantu","effects/realm_astral_gate.json"))){
            for(var raw:JsonParser.parseReader(r).getAsJsonArray()){
                var o=raw.getAsJsonObject();float[][] vertices=new float[4][];
                for(int i=0;i<4;i++)vertices[i]=array(o.getAsJsonArray("vertices").get(i).getAsJsonArray());
                result.add(new Face(vertices,array(o.getAsJsonArray("normal")),array(o.getAsJsonArray("uv")),new ResourceLocation("xiantu","textures/block/"+o.get("texture").getAsString()+".png"),o.get("emissive").getAsBoolean()));
            }
        }catch(Exception e){throw new IllegalStateException("Invalid Blockbench portal export",e);}
        faces=List.copyOf(result);
    }
    private static float[] array(JsonArray a){float[] v=new float[a.size()];for(int i=0;i<v.length;i++)v[i]=a.get(i).getAsFloat();return v;}
    static void draw(PoseStack stack,MultiBufferSource buffers,int light,boolean active){
        stack.pushPose();stack.translate(.5,0,.5);
        for(var f:faces){var v=buffers.getBuffer(RenderType.entityCutoutNoCull(f.texture));
            for(int i=0;i<4;i++)v.vertex(stack.last().pose(),f.v[i][0],f.v[i][1],f.v[i][2])
                .color(1f,1f,1f,1f).uv(f.uv[i==0||i==3?0:2],f.uv[i<2?3:1])
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(active&&f.emissive?15728880:light)
                .normal(stack.last().normal(),f.n[0],f.n[1],f.n[2]).endVertex();
        }stack.popPose();
    }
    private RealmGateMesh(){}
}
