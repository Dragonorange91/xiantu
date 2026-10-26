package com.longju.xiantu.qa;

import com.longju.xiantu.entity.*;
import com.longju.xiantu.registry.ModEntities;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import java.nio.file.*;

/** Actual goal selection and combat. No forced animation/NoAI Boss, disposable world only. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class BossCombatIntegration {
    private static final String[] IDS={"spirit_ape_king","meteor_general","five_element_warden"};
    private static CompletionLandEntity boss;
    private static Pig target;
    private static ServerLevel level;
    private static int index,age;
    private static float initialTargetHealth;
    private static final Set<String> seen=new LinkedHashSet<>();
    private static final List<String> lines=new ArrayList<>();
    private static String previous="";
    private static boolean all,probe;
    private static UUID observer;
    private static final Set<String> probed=new HashSet<>();
    private static final Path LOG=Path.of("xiantu-boss-combat-v4-qa.txt");
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantubossqa").requires(s->s.hasPermission(2))
            .then(Commands.literal("mechanisms").executes(c->{if(!allowed(c.getSource().getLevel()))return 0;cleanup();level=c.getSource().getLevel();observer=c.getSource().getPlayerOrException().getUUID();all=true;probe=true;index=0;lines.clear();start();return 1;}))
            .then(Commands.literal("all").executes(c->{if(!allowed(c.getSource().getLevel()))return 0;cleanup();level=c.getSource().getLevel();observer=c.getSource().getPlayerOrException().getUUID();all=true;probe=false;index=0;lines.clear();start();return 1;}))
            .then(Commands.literal("one").then(Commands.argument("index",com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,IDS.length-1)).executes(c->{if(!allowed(c.getSource().getLevel()))return 0;cleanup();level=c.getSource().getLevel();observer=c.getSource().getPlayerOrException().getUUID();all=false;probe=false;index=com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"index");lines.clear();start();return 1;})))
            .then(Commands.literal("cleanup").executes(c->{cleanup();return 1;})));
    }
    private static boolean allowed(ServerLevel s){return s.getServer().getWorldData().getLevelName().equals("Xiantu-QA-BeastFlat-20261017");}
    private static void start(){
        age=0;seen.clear();probed.clear();previous="";level.getServer().setDifficulty(Difficulty.NORMAL,true);
        var player=level.getServer().getPlayerList().getPlayer(observer);
        if(player!=null){player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(level,4010,-54,3996,48,22);}
        CompletionLandEntity candidate=null; Pig victim=null;
        try {
            candidate=(CompletionLandEntity)ModEntities.COMPLETION.get(IDS[index]).get().create(level);
            victim=EntityType.PIG.create(level);
            if(candidate==null||victim==null)throw new IllegalStateException("Fixture creation failed");
            Entity entity=candidate; Mob mob=candidate; LivingEntity living=candidate;
            entity.moveTo(4000,-60,4002,0,0);entity.addTag("xiantu_boss_v4_qa");mob.setPersistenceRequired();
            victim.moveTo(4000,-60,4008,180,0);victim.setNoAi(true);victim.setPersistenceRequired();victim.addTag("xiantu_boss_v4_qa");
            victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);victim.setHealth(10000);
            if(!level.addFreshEntity(candidate)||!level.addFreshEntity(victim))throw new IllegalStateException("Fixture spawn rejected");
            living.hurt(level.damageSources().mobAttack(victim),.1F);mob.setTarget(victim);
            // Publish only a fully initialized pair. QA failures must never crash the server tick.
            initialTargetHealth=victim.getHealth();target=victim;boss=candidate;record("START "+IDS[index]);
        } catch(RuntimeException|LinkageError ex) {
            if(candidate!=null)((Entity)candidate).discard();if(victim!=null)victim.discard();
            boss=null;target=null;all=false;level.getServer().setDifficulty(Difficulty.PEACEFUL,true);
            com.mojang.logging.LogUtils.getLogger().error("Boss QA fixture initialization failed",ex);
            record("ERROR START "+IDS[index]+" "+ex);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||boss==null)return;
        if(target==null){record("ERROR missing QA target");cleanup();return;}
        age++;
        String action=boss.animationState();if(!action.equals(previous)){record("t="+age+" action="+action+" hp="+target.getHealth()+" pos="+((Entity)boss).position());previous=action;}
        seen.add(action);
        if(probe)probeMechanisms();
        // Keep a durable combat target in the home region; do not force Boss skill selection.
        if(age%100==0 && ((Entity)boss).distanceToSqr(target)>14*14)((Entity)target).moveTo(((Entity)boss).getX(),((Entity)boss).getY(),((Entity)boss).getZ()+6,180,0);
        // A knocked-back target must stay inside the normal 24-block home leash.
        // Re-engage via an actual incoming hit if normal home retreat cleared it;
        // never choose a skill or override the goal/animation under test.
        if(age%40==0){
            if(target.distanceToSqr(4000,-60,4002)>18*18)target.teleportTo(4000,-60,4008);
            if(((Mob)boss).getTarget()==null){target.teleportTo(4000,-60,4008);((LivingEntity)boss).hurt(level.damageSources().mobAttack(target),.01F);record("QA target re-engaged after normal leash reset");}
        }
        if(age==1100)((LivingEntity)boss).setHealth(((LivingEntity)boss).getMaxHealth()*.29F);
        if(age>=1340){
            var expected=BossCombatSkills.forBoss(IDS[index]).stream().map(BossCombatSkills.Skill::id).toList();
            record("RESULT "+IDS[index]+" seen="+seen+" expected="+expected+" allSkills="+seen.containsAll(expected)+" damaged="+(target.getHealth()<initialTargetHealth)+" bossNoAI="+((Mob)boss).isNoAi());
            ((Entity)boss).discard();((Entity)target).discard();boss=null;target=null;
            if(all&&++index<IDS.length)start();else level.getServer().setDifficulty(Difficulty.PEACEFUL,true);
        }
    }
    /** Direct server API assertions: real AI creates the mechanism; no forced skill selection.
     * This verifies server transactions, not the player's mouse targeting UX. */
    private static void probeMechanisms(){
        var state=boss.bossVisual();var skill=BossCombatSkills.find(IDS[index],state.getString("skill"));
        if(skill==null)return;
        long elapsed=level.getGameTime()-state.getLong("start");
        var player=level.getServer().getPlayerList().getPlayer(observer);if(player==null)return;
        var anchors=level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class,
                ((Entity)boss).getBoundingBox().inflate(25),a->a.getPersistentData().hasUUID("BossOwner")
                &&a.getPersistentData().getUUID("BossOwner").equals(((Entity)boss).getUUID()));
        if(anchors.isEmpty())return;
        if(skill.ultimate() && elapsed>=5 && elapsed<skill.windup() && probed.add(skill.id())){
            var anchor=anchors.get(0);player.teleportTo(level,anchor.getX()+1,anchor.getY(),anchor.getZ(),90,0);
            var event=new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific(player,net.minecraft.world.InteractionHand.MAIN_HAND,anchor,Vec3.ZERO);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            boolean first=event.isCanceled()&&boss.bossVisual().getBoolean("cancelled"),second=boss.interruptBoss(player,anchor);
            record("MECHANISM "+skill.id()+" interrupt="+first+" duplicateRejected="+!second+" cancelled="+boss.bossVisual().getBoolean("cancelled"));return;
        }
        if(elapsed<skill.windup()+3 || elapsed>skill.windup()+skill.active()-2 || !probed.add(skill.id()))return;
        if(skill.id().equals("meteor_guard")){
            var living=(LivingEntity)boss;float before=living.getHealth();
            boolean body=living.hurt(level.damageSources().playerAttack(player),5);
            var anchor=anchors.get(0);player.teleportTo(level,anchor.getX()+1,anchor.getY(),anchor.getZ(),90,0);
            boolean core=boss.strikeBossAnchor(player,anchor);
            record("MECHANISM meteor_guard bodyBlocked="+!body+" coreHit="+core+" healthReduced="+(living.getHealth()<before));
        }else{
            int count=skill.shape().equals("links")?2:1;boolean success=true;
            for(int i=0;i<Math.min(count,anchors.size());i++){
                var anchor=anchors.get(i);player.teleportTo(level,anchor.getX()+1,anchor.getY(),anchor.getZ(),90,0);
                var attack=new net.minecraftforge.event.entity.player.AttackEntityEvent(player,anchor);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(attack);
                success &=attack.isCanceled()&&!anchor.isAlive();
            }
            record("MECHANISM "+skill.id()+" broken="+success+" bannerOff="+!boss.bossBannerActive()+" cancelled="+boss.bossVisual().getBoolean("cancelled"));
        }
        // The probe uses creative player damage; restore the test victim as the AI target.
        ((Mob)boss).setTarget(target);
    }
    private static void record(String text){lines.add(text);com.mojang.logging.LogUtils.getLogger().info("[Boss v4 QA] {}",text);try{Files.write(LOG,lines);}catch(Exception ex){com.mojang.logging.LogUtils.getLogger().error("Cannot write Boss QA log",ex);}}
    private static void cleanup(){if(boss!=null)((Entity)boss).discard();if(target!=null)((Entity)target).discard();boss=null;target=null;if(level!=null)level.getServer().setDifficulty(Difficulty.PEACEFUL,true);}
    private BossCombatIntegration(){}
}
