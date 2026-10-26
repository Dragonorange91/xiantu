package com.longju.xiantu.instance;

import com.longju.xiantu.registry.ModBlocks;
import com.longju.xiantu.medicine.*;
import com.longju.xiantu.entity.*;
import com.longju.xiantu.wood.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;

/** Stateless chunk-local flora/ores. Exploring a fresh chunk generates resources once, never rerolls mined chunks. */
public final class RealmEcology {
    public static void decorate(net.minecraft.world.level.WorldGenLevel level,ChunkAccess chunk,InstanceTerrain t,RealmLayout layout){
        var cp=chunk.getPos();var random=new Random(InstanceTerrain.mix(t.seed()^cp.toLong()));
        var p=new BlockPos.MutableBlockPos();
        // Thin eligible herbs by 25% without consuming the ore/resource random stream.
        var herbDensity=new Random(InstanceTerrain.mix(t.seed()^cp.toLong()^0x686572627468696eL));
        // Ordinary flora and trees come entirely from vanilla biome placed features.
        // Add only Xiantu resources, once at the normal FEATURES generation stage.
        for(int i=0;i<3+InstanceRules.Abundance.values()[t.abundance()].resources;i++){
            int x=cp.getMinBlockX()+random.nextInt(16),z=cp.getMinBlockZ()+random.nextInt(16);
            if(t.sanctuary(x,z)||!t.natural()&&layout.protectedColumn(x,z))continue;
            int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            int tier=InstanceRules.resourceTier(InstanceRules.Abundance.values()[t.abundance()],false,random);
            var herbs=MedicineCatalog.HERBS.stream().filter(v->v.tier()==tier&&MedicineRegistry.WILD_PLANTS.containsKey(v.registryId())).toList();
            if(!herbs.isEmpty()){
                var state=MedicineRegistry.WILD_PLANTS.get(herbs.get(random.nextInt(herbs.size())).registryId()).get().defaultBlockState();
                p.set(x,y,z);if(herbDensity.nextInt(4)!=0&&level.isEmptyBlock(p)&&state.canSurvive(level,p))level.setBlock(p,state,2);
            }
        }
        var ores=List.of(ModBlocks.FAN_LING_SHI_ORE,ModBlocks.LING_JING_ORE,ModBlocks.LING_YU_ORE,ModBlocks.XUAN_JING_ORE,ModBlocks.DI_SUI_ORE,ModBlocks.TIAN_JING_ORE,ModBlocks.XIAN_YU_ORE,ModBlocks.DAO_SHI_ORE);
        int veins=2*InstanceRules.Abundance.values()[t.abundance()].resources;
        for(int i=0;i<veins;i++){
            int x=cp.getMinBlockX()+2+random.nextInt(12),z=cp.getMinBlockZ()+2+random.nextInt(12),y=-56+random.nextInt(112);
            int tier=InstanceRules.resourceTier(InstanceRules.Abundance.values()[t.abundance()],false,random);
            var metal=com.longju.xiantu.mining.MetalMaterialRegistry.ALL.get(tier-1);
            for(int k=0;k<8;k++){
                p.set(x+random.nextInt(3)-1,y+random.nextInt(3)-1,z+random.nextInt(3)-1);
                var before=level.getBlockState(p);
                if(before.is(net.minecraft.tags.BlockTags.STONE_ORE_REPLACEABLES)||before.is(net.minecraft.tags.BlockTags.DEEPSLATE_ORE_REPLACEABLES))
                    level.setBlock(p,(k<4?ores.get(tier-1).get():com.longju.xiantu.mining.MetalMaterialRegistry.ORES.get(metal.id()).get()).defaultBlockState(),2);
            }
        }
    }
    public static void spiritWood(net.minecraft.world.level.WorldGenLevel level,ChunkAccess chunk,InstanceTerrain t,RealmLayout layout){
        var cp=chunk.getPos();var random=new Random(InstanceTerrain.mix(t.seed()^cp.toLong()^0x7370697269747764L));
        var abundance=InstanceRules.Abundance.values()[t.abundance()];
        // One possible tree per chunk, with bounded retries for obstructed terrain.
        // Woodland realms are somewhat richer; vanilla forest/tree generation stays intact.
        int chance=20+5*abundance.ordinal()+(t.theme()==0?10:0);
        if(random.nextInt(100)>=chance)return;
        int tier=InstanceRules.resourceTier(abundance,false,random);var type=SpiritWoodType.values()[tier-1];
        for(int attempt=0;attempt<4;attempt++){
            int x=cp.getMinBlockX()+6+random.nextInt(4),z=cp.getMinBlockZ()+6+random.nextInt(4);
            var base=new BlockPos(x,level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z),z);
            if(!level.getBlockState(base.below()).is(net.minecraft.tags.BlockTags.DIRT))continue;
            // Check the complete crown, not just the trunk. Never touch a neighboring
            // generation chunk, arrival sanctuary or a blueprint's reserved column.
            var geometry=SpiritTreeGrowth.layout(base,type);
            if(geometry.keySet().stream().anyMatch(at->at.getX()<cp.getMinBlockX()||at.getX()>cp.getMaxBlockX()
                    ||at.getZ()<cp.getMinBlockZ()||at.getZ()>cp.getMaxBlockZ()
                    ||t.sanctuary(at.getX(),at.getZ())||!t.natural()&&layout.protectedColumn(at.getX(),at.getZ())))continue;
            if(SpiritTreeGrowth.grow(level,base,type))return;
        }
    }
    public static void tick(ServerLevel level,InstanceLedger.Slot slot){
        var n=InstanceService.data(level.getServer()).session(slot);if(!n.getBoolean("Ready")||n.getBoolean("Frozen")||!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)||BeastEcology.profile()==null)return;
        var species=BeastEcology.profile().species().stream().filter(v->v.element().equals(n.getString("Theme"))&&!v.elite()&&Set.of("land","fly").contains(v.mode())).toList();if(species.isEmpty())return;
        var a=InstanceRules.Abundance.valueOf(n.getString("Abundance"));
        for(var player:level.players())if(player.isAlive()&&!player.isSpectator()&&Math.floorMod(level.getGameTime()+player.getUUID().getLeastSignificantBits(),140)==0){
            var random=level.random;double angle=random.nextDouble()*Math.PI*2,radius=32+random.nextInt(48);
            int x=(int)Math.floor(player.getX()+Math.cos(angle)*radius),z=(int)Math.floor(player.getZ()+Math.sin(angle)*radius);var column=new BlockPos(x,64,z);
            if(!level.hasChunkAt(column)||!InstanceService.inside(level,x,65,z)||Math.abs(x)<20&&Math.abs(z)<20)continue;
            var at=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column);if(level.players().stream().anyMatch(p->p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(at))<24*24))continue;
            var nearby=level.getEntitiesOfClass(Mob.class,new net.minecraft.world.phys.AABB(at).inflate(64),m->m.isAlive()&&BeastRealmService.isBeast(m));if(nearby.size()>=12)continue;
            var choice=species.get(random.nextInt(species.size()));if(!BeastNaturalSpawns.habitat(level,at,choice))continue;
            var mob=BeastNaturalSpawns.spawn(level,at,choice);if(mob!=null)mob.getPersistentData().putUUID("xiantu_realm_instance",slot.instance());
        }
    }
    private RealmEcology(){}
}
