package com.longju.xiantu.refining;

import com.longju.xiantu.spell.SpellService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

/** Ground dash ignores camera pitch, checks every intermediate body box and the final footing. */
public final class QitianMovement {
    public static Vec3 direction(ServerPlayer p) {
        var v=p.getLookAngle().multiply(1,0,1);
        return v.lengthSqr()<1e-8?new Vec3(-Math.sin(Math.toRadians(p.getYRot())),0,Math.cos(Math.toRadians(p.getYRot()))):v.normalize();
    }
    public static Vec3 destination(ServerPlayer p, Vec3 direction) {
        var dir=direction.multiply(1,0,1).normalize();
        var end=SpellService.safeDestination(p,16,dir);var delta=end.subtract(p.position());
        for(double length=delta.length();length>=2;length-=.25){
            var at=p.position().add(dir.scale(length));var pos=BlockPos.containing(at);
            if(!p.serverLevel().hasChunkAt(pos.below())||!p.serverLevel().getWorldBorder().isWithinBounds(pos))continue;
            if(!p.serverLevel().noCollision(p,p.getBoundingBox().move(at.subtract(p.position()))))continue;
            if(p.serverLevel().getBlockState(pos.below()).getCollisionShape(p.serverLevel(),pos.below()).isEmpty())continue;
            if(!p.serverLevel().getFluidState(pos).isEmpty()||p.serverLevel().getBlockState(pos).is(net.minecraft.tags.BlockTags.FIRE))continue;
            return at;
        }
        return p.position();
    }
    /** Future timestamps from a different world's clock never lock this ability permanently. */
    public static boolean recentCombat(long last,long now) {return last>0&&last<=now&&now-last<100;}
    public static boolean combat(ServerPlayer p){return recentCombat(p.getPersistentData().getLong("xiantu_equipment_last_combat"),SpellService.clock());}
    private QitianMovement(){}
}
