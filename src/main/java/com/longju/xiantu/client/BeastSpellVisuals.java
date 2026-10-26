package com.longju.xiantu.client;

import com.longju.xiantu.entity.CompletionLandEntity;
import com.longju.xiantu.network.BeastVfxPacket;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;

/** Packet-bound ordinary ability subjects. Immutable event parameters, no particle history per render. */
final class BeastSpellVisuals {
    private record Active(Entity owner,String species,String stage,Vec3 point,Vec3 offset,Vec3 dir,long start,int ticks,boolean follow){}
    private static final List<Active> ACTIVE=new ArrayList<>();
    private static ClientLevel level;
    static void receive(BeastVfxPacket p){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        if(level!=mc.level){clear();level=mc.level;}
        if(!p.dimension().equals(level.dimension().location())||!p.effect().getNamespace().equals("xiantu")||!p.effect().getPath().startsWith("beasts/"))return;
        Entity source=level.getEntity(p.entityId());if(source==null||!source.getUUID().equals(p.owner())||source instanceof CompletionLandEntity b&&b.boss())return;
        String[] parts=p.effect().getPath().split("/");if(parts.length!=3)return;String stage=parts[2];
        if(stage.equals("cancel")){ACTIVE.removeIf(a->a.owner==source);return;}
        if(p.ticks()<1||p.ticks()>100||!Float.isFinite(p.yaw())||!Double.isFinite(p.position().lengthSqr())||mc.player.distanceToSqr(p.position())>64*64)return;
        if(!List.of("warn","trail","impact","projectile","flee").contains(stage))return;
        ACTIVE.removeIf(a->a.owner==source&&a.stage.equals(stage));if(ACTIVE.size()>=64)return;
        double yaw=Math.toRadians(p.yaw());ACTIVE.add(new Active(source,parts[1],stage,p.position(),p.position().subtract(source.position()),new Vec3(-Math.sin(yaw),0,Math.cos(yaw)),level.getGameTime(),p.ticks(),p.follow()));
    }
    static void tick(){
        if(level!=Minecraft.getInstance().level){clear();return;}if(level==null)return;
        ACTIVE.removeIf(a->a.owner.isRemoved()||a.owner instanceof LivingEntity l&&!l.isAlive()||level.getGameTime()-a.start>a.ticks+12);
    }
    static void render(VertexConsumer v,Matrix4f m,Vec3 camera,float partial){
        if(level!=Minecraft.getInstance().level||level==null)return;
        for(Active a:ACTIVE){Vec3 point=a.follow?a.owner.getPosition(partial).add(a.offset):a.point;if(point.distanceToSqr(camera)>64*64)continue;
            double age=level.getGameTime()+partial-a.start;
            BossSpellGeometry.beast(v,m,camera,a.species,a.stage,point,a.dir,age,a.ticks);
        }
    }
    static void clear(){ACTIVE.clear();level=null;}
}
