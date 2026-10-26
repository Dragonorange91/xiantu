package com.longju.xiantu.qa;

import com.longju.xiantu.registry.ModEffects;
import com.longju.xiantu.entity.FoxfireProjectile;
import com.longju.xiantu.entity.BeastResourceRules;
import com.longju.xiantu.entity.CompletionAnimated;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import java.util.*;

/** Real goal/packet regression in the explicitly selected disposable flat world. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class BeastPhotonIntegration {
    private static final Set<String> IDS=BeastResourceRules.SPECIES;
    private static final Set<String> PASSIVE=Set.of("vine_deer","thunder_deer","iron_tooth_badger","molten_turtle","sand_armadillo","tide_pattern_ray");
    private static ServerLevel level;
    private static Mob beast;
    private static Pig target;
    private static String species,scenario;
    private static int age,projectiles,hits,skillHits;
    private static float previous;
    private static boolean snared,repositioned;
    private static double moved;
    private static final Map<BlockPos,net.minecraft.world.level.block.state.BlockState> RESTORE=new HashMap<>();
    private static final List<String> SAMPLES=new ArrayList<>();
    public static int start(CommandSourceStack source,String id,String mode) {
        ServerLevel world=source.getLevel();
        if(!world.getServer().getWorldData().getLevelName().equals("Xiantu-QA-BeastFlat-20261017")
                || !IDS.contains(id) || !Set.of("hit","dodge","wall").contains(mode)) return 0;
        cleanup(); level=world;species=id;scenario=mode;age=projectiles=hits=skillHits=0;snared=false;repositioned=false;moved=0;SAMPLES.clear();
        beast=(Mob)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("xiantu",id)).create(level);
        target=EntityType.PIG.create(level);
        if(beast==null || target==null)return 0;
        if(Set.of("azure_scale_python","tide_pattern_ray").contains(id)) {
            // Real swimming habitat. Restore every touched cell, including on a subsequent start.
            for(int x=3109;x<=3132;x++)for(int z=3109;z<=3132;z++)for(int y=-63;y<=-57;y++) {
                BlockPos p=new BlockPos(x,y,z);RESTORE.put(p,level.getBlockState(p));
                boolean wall=y==-63||x==3109||x==3132||z==3109||z==3132;
                level.setBlock(p,(wall?Blocks.STONE:y<=-58?Blocks.WATER:Blocks.AIR).defaultBlockState(),2);
            }
        }
        beast.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        beast.moveTo(3120.5,-60,3120.5,0,0);beast.setPersistenceRequired();beast.addTag("xiantu_photon_qa");
        target.moveTo(3120.5,-60,3120.5+(!mode.equals("wall")&&(id.contains("bear") || id.contains("lion"))?2.5:id.equals("jade_leaf_mantis")?6.5:5));
        target.setNoAi(true);target.setPersistenceRequired();target.addTag("xiantu_photon_qa");
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        level.addFreshEntity(target);level.addFreshEntity(beast);
        // Only the stationary target has NoAI; skill timing is the live creature's own Goal.
        beast.hurt(level.damageSources().mobAttack(target),.01F);previous=target.getHealth();
        source.sendSuccess(()->Component.literal("[Beast Photon QA] started "+id+" "+mode),false);
        return 1;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || beast==null || level==null)return;
        age++;
        if(age==20&&PASSIVE.contains(species)){
            boolean accepted=beast.hurt(level.damageSources().mobAttack(target),1F);
            SAMPLES.add("panic-trigger accepted="+accepted+" lastAttacker="+(beast.getLastHurtByMob()==target)+" speed="+beast.getAttributeValue(Attributes.MOVEMENT_SPEED));
        }
        boolean flying=beast instanceof net.minecraft.world.entity.FlyingMob;
        boolean locked=!flying?age>=10:beast instanceof CompletionAnimated a&&a.animationState().equals("alert");
        if(!repositioned && locked && scenario.equals("dodge")){target.teleportTo(3125,-60,3125.5);repositioned=true;}
        if(age==10 && scenario.equals("wall")) {
            for(int x=3119;x<=3122;x++)for(int y=-60;y<=-57;y++) {
                BlockPos p=new BlockPos(x,y,3123);RESTORE.put(p,level.getBlockState(p));level.setBlockAndUpdate(p,Blocks.STONE.defaultBlockState());
            }
        }
        if(target.getHealth()<previous){hits++;previous=target.getHealth();}
        snared|=target.hasEffect(ModEffects.BEAST_SNARE.get());
        moved=Math.max(moved,beast.position().distanceTo(new net.minecraft.world.phys.Vec3(3120.5,-60,3120.5)));
        projectiles=Math.max(projectiles,level.getEntitiesOfClass(FoxfireProjectile.class,new AABB(3100,-65,3100,3140,-40,3150)).size());
        if(age%5==0)SAMPLES.add(String.format(java.util.Locale.ROOT,"%d,%.3f,%.3f,%.3f,%.3f,%s,%.2f,%.2f,%s,targetY=%.3f,pathDone=%s,lastAttacker=%s",age,target.getHealth(),beast.getX(),beast.getY(),beast.getZ(),snared,beast.getYRot(),beast.yBodyRot,beast instanceof CompletionAnimated a?a.animationState():"legacy",target.getY(),beast.getNavigation().isDone(),beast.getLastHurtByMob()==target));
        if(species.equals("sand_armadillo")&&age%20==0){
            var path=beast.getNavigation().getPath();var move=beast.getMoveControl();
            SAMPLES.add("navigation tick="+beast.tickCount+" noAI="+beast.isNoAi()+" ground="+beast.onGround()+" water="+beast.isInWater()+" speed="+beast.getSpeed()+" input="+beast.xxa+","+beast.yya+","+beast.zza+" wanted="+move.hasWanted()+":"+move.getWantedX()+","+move.getWantedY()+","+move.getWantedZ()+" path="+(path==null?"null":path.getNextNodeIndex()+"/"+path.getNodeCount()+":"+(path.isDone()?"done":path.getNextNodePos())));
        }
        int end=scenario.equals("hit")?200:species.equals("crag_rock_spider")?38:70;
        if(age>=end)finish();
    }
    @SubscribeEvent public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if(beast==null || event.getEntity()!=target || event.getSource().getEntity()!=beast)return;
        double base=beast.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double ratio=species.contains("wolf")?1.2:species.contains("bear") || Set.of("gold_mane_lion","red_sun_lion").contains(species)?1.5:
                species.contains("boar") || species.contains("rhino")?1.35:species.contains("spider")?.9:
                species.contains("raven")||Set.of("copper_beak_vulture","fire_beak_falcon").contains(species)?1.25:
                Set.of("thunder_prison_lion","mushroom_toad").contains(species)?.8:
                Set.of("dark_shell_crab","azure_scale_python","ancient_branch_python").contains(species)?1.1:
                species.equals("electric_marten")||species.equals("jade_leaf_mantis")?1.2:
                Set.of("flint_armored_lizard","jade_scale_crocodile").contains(species)?1.15:
                species.equals("gold_back_beetle")?1.35:1;
        if(Math.abs(event.getAmount()-base*ratio)<.001 &&
                (!species.equals("yanwei_fox") || event.getSource().getDirectEntity() instanceof FoxfireProjectile))skillHits++;
    }
    private static void finish() {
        boolean peaceful=PASSIVE.contains(species);
        boolean pass=scenario.equals("hit")?(peaceful?hits==0 && moved>.2:skillHits>0):hits==0;
        String result="[Beast Photon QA "+(pass?"PASS":"FAIL")+"] "+species+" "+scenario+" hits="+hits+
                " skillHits="+skillHits+" damage="+(1000-target.getHealth())+" snared="+snared+" projectileMax="+projectiles+" moved="+moved;
        LogUtils.getLogger().info(result);
        level.players().forEach(p->p.sendSystemMessage(Component.literal(result)));
        try {
            Path folder=level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("xiantu-photon-qa");
            Files.createDirectories(folder);Files.writeString(folder.resolve(species+"-"+scenario+".txt"),result+"\n"+String.join("\n",SAMPLES));
        } catch(java.io.IOException ex){LogUtils.getLogger().error("Cannot save Photon QA report",ex);}
        cleanup();
    }
    private static void cleanup() {
        if(level!=null){RESTORE.forEach((p,state)->level.setBlockAndUpdate(p,state));RESTORE.clear();}
        if(beast!=null)beast.discard();if(target!=null)target.discard();beast=null;target=null;
    }
}
