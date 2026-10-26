package com.longju.xiantu.entity;

import com.longju.xiantu.XiantuMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** One bounded attempt per player per ten seconds, not thirty independent vanilla spawn entries. */
@Mod.EventBusSubscriber(modid=XiantuMod.MOD_ID)
public final class BeastNaturalSpawns {
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.level instanceof ServerLevel level)||level.dimension()!=Level.OVERWORLD
            ||!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)||BeastEcology.profile()==null)return;
        for(var player:level.players())if(player.isAlive()&&!player.isSpectator()
            &&Math.floorMod(level.getGameTime()+player.getUUID().getLeastSignificantBits(),200)==0)attempt(level,player);
    }
    public static Mob attempt(ServerLevel level,ServerPlayer player){
        var random=level.random;double angle=random.nextDouble()*Math.PI*2;
        double radius=Math.sqrt(32*32+random.nextDouble()*(96*96-32*32));
        int x=net.minecraft.util.Mth.floor(player.getX()+Math.cos(angle)*radius),z=net.minecraft.util.Mth.floor(player.getZ()+Math.sin(angle)*radius);
        var column=new BlockPos(x,player.getBlockY(),z);
        if(!level.hasChunkAt(column))return null;
        int surface=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
        int y=surface;int probe=random.nextInt(5);
        if(probe==0){
            y=net.minecraft.util.Mth.clamp(player.getBlockY()+random.nextInt(49)-24,level.getMinBuildHeight()+1,level.getMaxBuildHeight()-3);
            // A short floor probe within this single candidate column; no second spawn attempt.
            for(int n=0;n<8&&y>level.getMinBuildHeight()+1&&!level.getBlockState(new BlockPos(x,y-1,z)).isSolid();n++)y--;
        }else if(probe==1&&level.getFluidState(new BlockPos(x,surface-1,z)).is(FluidTags.WATER))y=surface-2-random.nextInt(10);
        var pos=new BlockPos(x,y,z);
        if(!allowedLocation(level,pos)||BeastSpawnProtection.nearBed(level,pos)||BeastSpawnProtection.inTown(level,pos))return null;
        var biome=level.getBiome(pos);
        var eligible=BeastEcology.profile().species().stream().filter(s->s.matches(biome)&&BeastRealmRules.timeAllowed(s.time(),level.getDayTime()))
            .filter(s->habitat(level,pos,s)).toList();
        if(eligible.isEmpty())return null;
        var elements=eligible.stream().map(BeastEcology.Species::element).distinct().sorted().toList();
        String element=elements.get(random.nextInt(elements.size()));var same=eligible.stream().filter(s->s.element().equals(element)).toList();
        int roll=random.nextInt(same.stream().mapToInt(s->s.elite()?3:10).sum());BeastEcology.Species chosen=same.get(0);
        for(var s:same){roll-=s.elite()?3:10;if(roll<0){chosen=s;break;}}
        if(!belowCap(level,pos,chosen.elite()))return null;
        return spawn(level,pos,chosen);
    }
    public static boolean allowedLocation(ServerLevel level,BlockPos pos){
        if(level.dimension()!=Level.OVERWORLD||!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos))return false;
        boolean nearby=false;var center=Vec3.atBottomCenterOf(pos);
        for(var p:level.players())if(p.isAlive()&&!p.isSpectator()){double d=p.distanceToSqr(center);if(d<32*32)return false;if(d<=96*96)nearby=true;}
        return nearby;
    }
    public static boolean belowCap(ServerLevel level,BlockPos pos,boolean elite){
        int count=0,elites=0;
        for(var mob:level.getEntitiesOfClass(Mob.class,new AABB(pos).inflate(64),m->m.isAlive()&&BeastRealmService.isBeast(m)))
            if(mob.distanceToSqr(Vec3.atCenterOf(pos))<=64*64){count++;if(BeastRealmService.elite(mob))elites++;}
        return count<8&&(!elite||elites<1);
    }
    public static boolean habitat(ServerLevel level,BlockPos pos,BeastEcology.Species s){
        var fluid=level.getFluidState(pos);boolean water=fluid.is(FluidTags.WATER);
        if(s.mode().equals("water"))return water&&level.getFluidState(pos.above()).is(FluidTags.WATER)&&level.getFluidState(pos.below()).is(FluidTags.WATER);
        if(!fluid.isEmpty()||!level.getBlockState(pos).getCollisionShape(level,pos).isEmpty())return false;
        boolean floor=level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP);
        if(!floor)return false;
        if(s.mode().equals("fly")&&!level.canSeeSky(pos)&&!s.id().equals("purple_lightning_bat"))return false;
        if(s.mode().equals("cave")&&level.canSeeSky(pos)&&!level.getBiome(pos).is(new ResourceLocation("minecraft:mushroom_fields")))return false;
        if(!s.daylight()&&level.getBrightness(LightLayer.BLOCK,pos)>7)return false;
        if(s.shore()){
            boolean near=false;for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
                var p=pos.offset(dx,-1,dz);if(level.hasChunkAt(p)&&level.getFluidState(p).is(FluidTags.WATER))near=true;
            }if(!near)return false;
        }
        // The thunder lion belongs to the core, the marten to the edge.
        if(s.id().equals("thunder_prison_lion")&&BeastEcology.tier(level,pos,"thunder")!=2)return false;
        if(s.id().equals("electric_marten")&&BeastEcology.tier(level,pos,"thunder")!=1)return false;
        return true;
    }
    /** Caller must have checked ecology, budgets and protection. Uses real Forge finalization/join hooks. */
    public static Mob spawn(ServerLevel level,BlockPos pos,BeastEcology.Species species){
        var type=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(XiantuMod.MOD_ID,species.id()));
        if(type==null||!(type.create(level) instanceof Mob mob))return null;
        double lift=species.mode().equals("fly")?2:0;
        mob.moveTo(pos.getX()+.5,pos.getY()+lift,pos.getZ()+.5,level.random.nextFloat()*360,0);
        // Entire bounding box must be loaded and clear, including flying/wide models.
        var b=mob.getBoundingBox();
        if(!level.hasChunksAt(new BlockPos((int)Math.floor(b.minX),(int)Math.floor(b.minY),(int)Math.floor(b.minZ)),new BlockPos((int)Math.ceil(b.maxX),(int)Math.ceil(b.maxY),(int)Math.ceil(b.maxZ)))
            ||!level.noCollision(mob)||(!species.mode().equals("water")&&level.containsAnyLiquid(b))){mob.discard();return null;}
        ForgeEventFactory.onFinalizeSpawn(mob,level,level.getCurrentDifficultyAt(pos),MobSpawnType.NATURAL,null,null);
        if(mob.isSpawnCancelled()){mob.discard();return null;}
        return level.addFreshEntity(mob)?mob:null;
    }
    private BeastNaturalSpawns(){}
}
