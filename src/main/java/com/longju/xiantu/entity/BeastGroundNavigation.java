package com.longju.xiantu.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;

/** Keeps vanilla path following, including collision and stuck checks. */
final class BeastGroundNavigation extends GroundPathNavigation {
    BeastGroundNavigation(Mob mob,Level level){super(mob,level);}
    @Override protected void followThePath(){
        super.followThePath();
        // For widths such as .95F, (width+1)/2 rounds upward while width/2
        // rounds downward. Vanilla can reject a reached node by ~3e-8 blocks,
        // although MoveControl's actual next position is exactly under the mob.
        // Consume only that already-reached node; keep all other vanilla policy.
        if(path!=null&&!path.isDone()&&mob.position().distanceToSqr(path.getNextEntityPos(mob))<1.0e-6)
            path.advance();
    }
}
