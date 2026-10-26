package com.longju.xiantu.client;

import com.lowdragmc.photon.client.fx.*;
import com.longju.xiantu.network.BeastVfxPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

/** Bounded short-lived effects, keyed to real source identities and client level. */
final class BeastPhotonRuntime {
    private record Active(FXEffect effect,Entity source,long expires,String stage) {}
    private static final List<Active> ACTIVE=new ArrayList<>();
    private static final Set<String> WARNED=new HashSet<>();
    private static final Map<net.minecraft.resources.ResourceLocation,FX> PRELOADED=new HashMap<>();
    private static final Map<UUID,Entity> PROJECTILES=new HashMap<>();
    private static ClientLevel level;
    private BeastPhotonRuntime() {}
    static boolean receive(BeastVfxPacket p) {
        var mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null || !mc.level.dimension().location().equals(p.dimension()))return false;
        if(level!=mc.level){clear();level=mc.level;}
        Entity source=level.getEntity(p.entityId());
        if(source==null || !source.getUUID().equals(p.owner()))return false;
        String stage=p.effect().getPath().substring(p.effect().getPath().lastIndexOf('/')+1);
        if(stage.equals("cancel")){remove(source,"warn");remove(source,"trail");remove(source,"charge");return true;}
        if(!p.effect().getNamespace().equals("xiantu") || !p.effect().getPath().startsWith("beasts/")
                || p.ticks()<1 || p.ticks()>100 || !Float.isFinite(p.yaw())
                || !Double.isFinite(p.position().lengthSqr()) || mc.player.distanceToSqr(p.position())>64*64)return false;
        if(stage.equals("warn") || stage.equals("trail"))remove(source,stage);
        prune();
        if(ACTIVE.size()>=64){destroy(ACTIVE.remove(0));}
        try {
            // Resource I/O/deserialization and texture upload happen during reload,
            // never on a first attack. Missing production IDs are reported once.
            FX fx=PRELOADED.get(p.effect());
            if(fx==null)throw new IllegalStateException("Missing Photon FX "+p.effect());
            FXEffect effect;
            if(p.follow()) {
                effect=new EntityEffect(fx,level,source,EntityEffect.AutoRotate.NONE);
                var offset=stage.equals("warn")?net.minecraft.world.phys.Vec3.ZERO:p.position().subtract(source.position());
                effect.setOffset(new Vector3f((float)offset.x,(float)offset.y,(float)offset.z));
            } else {
                BlockPos block=BlockPos.containing(p.position());
                var fixed=new BlockEffect(fx,level,block);fixed.setCheckState(false);effect=fixed;
                effect.setOffset(new Vector3f((float)(p.position().x-block.getX()-.5),
                        (float)(p.position().y-block.getY()-.5),(float)(p.position().z-block.getZ()-.5)));
            }
            effect.setRotation(new Quaternionf().rotateY((float)Math.toRadians(-p.yaw())));
            effect.setAllowMulti(true);effect.setForcedDeath(true);effect.start();
            ACTIVE.add(new Active(effect,source,level.getGameTime()+p.ticks()+14,stage));
            return true;
        } catch(RuntimeException | LinkageError ex) {
            if(WARNED.add(p.effect().toString()))LogUtils.getLogger().error("[Xiantu Photon] Failed {}",p.effect(),ex);
            return false;
        }
    }
    private static void destroy(Active a) {
        PROJECTILES.remove(a.source.getUUID());
        if(a.effect.getRuntime()!=null)a.effect.getRuntime().destroy(true);
        if(a.effect instanceof EntityEffect e){var list=EntityEffect.CACHE.get(e.entity);if(list!=null){list.remove(e);if(list.isEmpty())EntityEffect.CACHE.remove(e.entity);}}
        if(a.effect instanceof BlockEffect b){var list=BlockEffect.CACHE.get(b.pos);if(list!=null){list.remove(b);if(list.isEmpty())BlockEffect.CACHE.remove(b.pos);}}
    }
    private static void remove(Entity source,String stage) {
        ACTIVE.removeIf(a->{if(a.source==source && a.stage.equals(stage)){destroy(a);return true;}return false;});
    }
    static void tick() {
        var current=Minecraft.getInstance().level;
        if(current!=level){clear();level=current;return;}
        if(current==null)return;
        PROJECTILES.values().removeIf(Entity::isRemoved);
        prune();
    }
    private static void prune(){
        if(level==null)return;
        ACTIVE.removeIf(a->{
            if(a.source.isRemoved() || (a.source instanceof LivingEntity l && !l.isAlive())
                    || level.getGameTime()>=a.expires || a.effect.getRuntime()==null || !a.effect.getRuntime().isAlive()){
                destroy(a);return true;
            }return false;
        });
    }
    static void projectile(Entity entity,String species) {
        if(PROJECTILES.containsKey(entity.getUUID()))return;
        boolean started=receive(new BeastVfxPacket(new net.minecraft.resources.ResourceLocation("xiantu","beasts/"+species+"/projectile"),
                entity.level().dimension().location(),entity.getId(),entity.getUUID(),entity.position(),
                (float)Math.toDegrees(Math.atan2(-entity.getDeltaMovement().x,entity.getDeltaMovement().z)),64,true));
        // A capacity/range/resource rejection must not hide this projectile for its whole life.
        if(started)PROJECTILES.put(entity.getUUID(),entity);
    }
    static FX preloaded(net.minecraft.resources.ResourceLocation id){return PRELOADED.get(id);}
    static void install(Map<net.minecraft.resources.ResourceLocation,FX> effects) {
        clear();PRELOADED.clear();PRELOADED.putAll(effects);
        LogUtils.getLogger().info("[Xiantu Photon] Preloaded {} native graphs before combat",effects.size());
    }
    static void clear(){ACTIVE.forEach(BeastPhotonRuntime::destroy);ACTIVE.clear();level=null;WARNED.clear();PROJECTILES.clear();}
}
