package com.longju.xiantu.mixin;

import net.minecraft.world.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.*;

/** Virtual six-sided collision: no visible blocks, fluids/pistons cannot move it. */
@Mixin(Entity.class)
public abstract class InstanceCollisionMixin {
    @ModifyVariable(method="collideBoundingBox",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static List<VoxelShape> xiantu$walls(List<VoxelShape> original,Entity entity,Vec3 movement,AABB current,Level l,List<VoxelShape> ignored){
        if(!com.longju.xiantu.instance.InstanceService.isDimension(l.dimension().location()))return original;
        var box=current.expandTowards(movement);
        var b=l.getWorldBorder();double minX=b.getMinX(),maxX=b.getMaxX(),minZ=b.getMinZ(),maxZ=b.getMaxZ();
        var walls=new ArrayList<VoxelShape>();double pad=32;
        if(box.minX<minX)walls.add(Shapes.box(minX-pad,box.minY-pad,box.minZ-pad,minX,box.maxY+pad,box.maxZ+pad));
        if(box.maxX>maxX)walls.add(Shapes.box(maxX,box.minY-pad,box.minZ-pad,maxX+pad,box.maxY+pad,box.maxZ+pad));
        if(box.minZ<minZ)walls.add(Shapes.box(box.minX-pad,box.minY-pad,minZ-pad,box.maxX+pad,box.maxY+pad,minZ));
        if(box.maxZ>maxZ)walls.add(Shapes.box(box.minX-pad,box.minY-pad,maxZ,box.maxX+pad,box.maxY+pad,maxZ+pad));
        if(box.minY< -63)walls.add(Shapes.box(box.minX-pad,-95,box.minZ-pad,box.maxX+pad,-63,box.maxZ+pad));
        if(box.maxY>319)walls.add(Shapes.box(box.minX-pad,319,box.minZ-pad,box.maxX+pad,351,box.maxZ+pad));
        if(walls.isEmpty())return original;walls.addAll(original);return walls;
    }
}
