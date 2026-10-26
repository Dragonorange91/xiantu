package com.longju.xiantu.client;

import com.longju.xiantu.entity.BossCombatSkills;
import com.longju.xiantu.entity.CompletionLandEntity;
import com.longju.xiantu.network.BeastVfxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** One-shot timeline edges, keyed by the server sequence. No geometry construction
 * per rendered frame, no warning footprint, and no client-authoritative damage. */
final class BossPhotonTimeline {
    private static final class State { int sequence,lastAge=-1; boolean counter; State(int sequence){this.sequence=sequence;} }
    private static final Map<UUID,State> STATES=new HashMap<>();
    private static ClientLevel level;
    private static Vec3 vector(CompoundTag n,String p){return new Vec3(n.getDouble(p+"X"),n.getDouble(p+"Y"),n.getDouble(p+"Z"));}
    static void tick() {
        var mc=Minecraft.getInstance();if(mc.level!=level){clear();level=mc.level;}
        if(level==null||mc.player==null)return;
        Set<UUID> seen=new HashSet<>();
        for(var e:level.entitiesForRendering()) {
            if(!(e instanceof CompletionLandEntity boss)||!boss.boss()||!boss.isAlive()||boss.distanceToSqr(mc.player)>64*64)continue;
            var n=boss.bossVisual();if(n.isEmpty()||n.getBoolean("cancelled"))continue;
            var skill=BossCombatSkills.find(boss.definitionId(),n.getString("skill"));
            if(skill==null)continue;
            int age=(int)(level.getGameTime()-n.getLong("start"));if(age<0||age>=skill.duration())continue;
            seen.add(boss.getUUID());int sequence=n.getInt("sequence");
            var state=STATES.get(boss.getUUID());
            if(state==null||state.sequence!=sequence){state=new State(sequence);STATES.put(boss.getUUID(),state);}
            Vec3 dir=vector(n,"direction"),origin=vector(n,"origin"),aim=vector(n,"aim");
            String root=boss.definitionId()+"/"+skill.id();
            // The new spear choreography has its own mesh VFX; do not overlay
            // the old generic spell effect or request an absent Photon resource.
            if(boss.definitionId().equals("meteor_general")&&Set.of("attack","blink_pierce","star_javelin","star_track").contains(skill.id())){state.lastAge=age;continue;}
            if(state.lastAge<0 && age<skill.windup())emit(boss,root+"/charge",boss.position(),dir,skill.windup()-age,true);
            if(state.lastAge<skill.windup() && age>=skill.windup()) {
                boolean atAim=Set.of("fall","fall_sequence","seal","pressure","links").contains(skill.shape());
                boolean follow=Set.of("charge","leap","melee","fist_combo","counter","guard").contains(skill.shape());
                emit(boss,root+"/release",atAim?aim:origin,dir,Math.min(100,skill.active()),follow);
            }
            if(skill.shape().equals("counter") && n.getBoolean("counterTriggered") && !state.counter) {
                emit(boss,root+"/impact",boss.position().add(dir.scale(1.4)).add(0,1.1,0),dir,12,false);
                state.counter=true;
            }
            state.lastAge=age;
        }
        STATES.keySet().retainAll(seen);
    }
    private static void emit(CompletionLandEntity boss,String path,Vec3 position,Vec3 dir,int ticks,boolean follow) {
        BeastPhotonRuntime.receive(new BeastVfxPacket(new ResourceLocation("xiantu","beasts/"+path),
            level.dimension().location(),boss.getId(),boss.getUUID(),position,
            (float)Math.toDegrees(Math.atan2(-dir.x,dir.z)),Math.max(1,ticks),follow));
    }
    static void clear(){STATES.clear();level=null;}
}
