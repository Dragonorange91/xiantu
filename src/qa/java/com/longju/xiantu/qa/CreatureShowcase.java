package com.longju.xiantu.qa;

import com.longju.xiantu.entity.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.*;
import java.util.*;

/** Guided real-AI presentation, isolated to an explicitly named disposable world. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class CreatureShowcase {
    public static final String WORLD="Xiantu-QA-Showcase-20261018";
    private static final String TAG="xiantu_showcase_20261018";
    private record Entry(String id,boolean passive,boolean boss,boolean water) {}
    private static final List<Entry> ENTRIES=new ArrayList<>();
    static {
        for(String id:List.of("yanwei_fox","cracked_blade_wolf","cold_fang_wolf","crag_rock_spider",
                "moss_back_bear","mountain_bear","gold_mane_lion","red_sun_lion","rock_back_boar",
                "thick_horn_rhino","thunder_feather_raven","vine_deer","thunder_deer","iron_tooth_badger"))
            ENTRIES.add(new Entry(id,Set.of("vine_deer","thunder_deer","iron_tooth_badger").contains(id),false,false));
        CompletionBestiary.ALL.stream().filter(d->!d.boss()).forEach(d->ENTRIES.add(new Entry(d.id(),d.passive(),false,d.kind().equals("WATER"))));
        CompletionBestiary.ALL.stream().filter(CompletionBestiary.Definition::boss).forEach(d->ENTRIES.add(new Entry(d.id(),false,true,false)));
        int expected=14+CompletionBestiary.ALL.size();
        if(ENTRIES.size()!=expected||ENTRIES.stream().map(Entry::id).distinct().count()!=expected)throw new IllegalStateException("Showcase roster drift");
    }
    private static ServerLevel level;
    private static UUID viewer;
    private static Mob beast;
    private static Pig victim;
    private static int index,age,damageEvents,meleeEvents,projectileEvents,attackActionHits;
    private static float damage;
    private static boolean running,paused,automatic;
    private static final Set<String> actions=new LinkedHashSet<>();
    private static final List<String> results=new ArrayList<>();
    private static String previous="";

    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantushowcase").requires(s->s.hasPermission(2))
            .then(Commands.literal("start").executes(c->start(c.getSource(),0,true)))
            .then(Commands.literal("one").then(Commands.argument("number",IntegerArgumentType.integer(1,ENTRIES.size()))
                .executes(c->start(c.getSource(),IntegerArgumentType.getInteger(c,"number")-1,false))))
            .then(Commands.literal("next").executes(c->{if(!active(c.getSource()))return 0;finish("manual_next");return advance();}))
            .then(Commands.literal("pause").executes(c->{if(!active(c.getSource()))return 0;paused=true;message("已暂停自动换场，当前妖兽仍保持真实 AI。");return 1;}))
            .then(Commands.literal("resume").executes(c->{if(!active(c.getSource()))return 0;paused=false;return 1;}))
            .then(Commands.literal("status").executes(c->{c.getSource().sendSuccess(()->Component.literal(status()),false);return 1;}))
            .then(Commands.literal("stop").executes(c->{if(!active(c.getSource()))return 0;finish("stopped");stop();return 1;})));
    }
    private static boolean allowed(ServerLevel world){return world.getServer().getWorldData().getLevelName().equals(WORLD);}
    private static boolean active(CommandSourceStack source){return running&&level==source.getLevel()&&allowed(level);}
    private static int start(CommandSourceStack source,int number,boolean auto){
        if(!allowed(source.getLevel())){source.sendFailure(Component.literal("仅允许在 "+WORLD+" 展示存档运行。"));return 0;}
        var player=source.getServer().getPlayerList().getPlayers().stream().filter(p->!(p instanceof FakePlayer)).findFirst().orElse(null);
        if(player==null)return 0;
        if(running){finish("restarted");stop();}
        level=source.getLevel();viewer=player.getUUID();index=number;automatic=auto;paused=false;running=true;results.clear();
        level.getServer().setDifficulty(Difficulty.PEACEFUL,true);
        player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
        // Only this new world and this bounded presentation stage are changed.
        for(int x=-15;x<=15;x++)for(int z=-12;z<=18;z++){
            level.getChunk(x>>4,z>>4);
            level.setBlockAndUpdate(new BlockPos(x,-61,z),
                (x==-15||x==15||z==-12||z==18)?Blocks.SEA_LANTERN.defaultBlockState():Blocks.GRAY_CONCRETE.defaultBlockState());
            for(int y=-60;y<=-49;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
        }
        // A distant wall is deliberately omitted: effects must remain visible against the world.
        return begin();
    }
    private static int duration(){return ENTRIES.get(index).boss?1500:ENTRIES.get(index).passive?260:500;}
    private static ServerPlayer player(){return level==null?null:level.getServer().getPlayerList().getPlayer(viewer);}
    private static int begin(){
        clearActors();age=damageEvents=meleeEvents=projectileEvents=attackActionHits=0;damage=0;actions.clear();previous="";
        Entry entry=ENTRIES.get(index);var p=player();if(p==null){stop();return 0;}
        level.getServer().setDifficulty(Difficulty.PEACEFUL,true);
        // Replace only the centre pool, never the surrounding save.
        for(int x=-8;x<=8;x++)for(int z=-5;z<=11;z++)for(int y=-60;y<=-58;y++)
            level.setBlock(new BlockPos(x,y,z),entry.water?
                ((x==-8||x==8||z==-5||z==11)?Blocks.GLASS.defaultBlockState():Blocks.WATER.defaultBlockState()):Blocks.AIR.defaultBlockState(),2);
        p.teleportTo(level,9,-56,-9,39,17);p.getAbilities().flying=true;p.onUpdateAbilities();
        try {
            var type=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("xiantu",entry.id));
            if(type==null)throw new IllegalStateException("Missing "+entry.id);
            Entity entity=type.create(level);if(!(entity instanceof Mob mob))throw new IllegalStateException("Not a Mob "+entry.id);
            beast=mob;entity.moveTo(0,entry.water?-59.5:-60,1,180,0);entity.addTag(TAG);mob.setPersistenceRequired();
            if(!level.addFreshEntity(entity))throw new IllegalStateException("Spawn rejected");
            if(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE)!=null)mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            Component title=Component.literal("["+(index+1)+"/"+ENTRIES.size()+"] ").append(type.getDescription());
            p.sendSystemMessage(title.copy().append(Component.literal(entry.passive?" · 和平生物：展示移动、受惊和现有特效，无主动攻击":" · 先看待机，随后展示真实 AI 攻击与特效")));
            record("START "+(index+1)+" "+entry.id+" passive="+entry.passive+" boss="+entry.boss+" noAI="+mob.isNoAi());
            return 1;
        }catch(RuntimeException|LinkageError ex){record("ERROR "+entry.id+" "+ex);LogUtils.getLogger().error("Showcase initialization failed",ex);stop();return 0;}
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!running||level==null)return;
        if(player()==null||!allowed(level)){stop();return;}
        if(paused)return;
        age++;Entry entry=ENTRIES.get(index);
        if(beast==null||!((Entity)beast).isAlive()){record("ERROR missing presentation subject");finish("missing_subject");advance();return;}
        String action=beast instanceof CompletionAnimated animated?animated.animationState():legacyAction(beast);
        actions.add(action);
        if(!action.equals(previous)){record("ACTION t="+age+" "+entry.id+" "+action);previous=action;}
        if(age==80) {
            level.getServer().setDifficulty(Difficulty.NORMAL,true);
            ((Entity)beast).moveTo(0,entry.water?-59.5:-60,1,180,0);beast.getNavigation().stop();beast.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            victim=EntityType.PIG.create(level);
            if(victim==null){finish("missing_target");stop();return;}
            victim.setNoAi(true);victim.setPersistenceRequired();victim.addTag(TAG);
            boolean flyingMelee=Set.of("copper_beak_vulture","fire_beak_falcon","purple_lightning_bat","thunder_feather_raven").contains(entry.id);
            ((Entity)victim).moveTo(0,-60,flyingMelee?.3:-3,0,0);
            victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);victim.setHealth(1024);
            victim.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            if(!level.addFreshEntity(victim)){finish("target_spawn_rejected");stop();return;}
            ((LivingEntity)beast).hurt(level.damageSources().mobAttack(victim),.1F);
            if(!entry.passive)beast.setTarget(victim);
        }
        if(age>80&&age%100==0&&victim!=null){
            victim.setHealth(1024);
            if(!entry.passive&&beast.getTarget()==null)beast.setTarget(victim);
            if(!entry.passive&&((Entity)victim).distanceToSqr(beast)>12*12)
                ((Entity)victim).moveTo(((Entity)beast).getX(),-60,((Entity)beast).getZ()-4,0,0);
        }
        if(entry.boss&&age==1160)((LivingEntity)beast).setHealth(((LivingEntity)beast).getMaxHealth()*.29F);
        if(age%20==0){
            String text="展示 "+(index+1)+"/"+ENTRIES.size()+"："+((Entity)beast).getType().getDescription().getString()+"  "+(age/20)+"/"+(duration()/20)+"秒  "+action;
            if(entry.passive)text+=" · 和平：无主动攻击";
            player().displayClientMessage(Component.literal(text),true);
            writeState();
        }
        if(age>=duration()){
            finish("completed");if(automatic)advance();else {paused=true;message("本只展示结束，可 /xiantushowcase next 或 one <序号>。");}
        }
    }
    private static String legacyAction(Mob mob){
        if(mob instanceof YanweiFoxEntity f)return f.isAttacking()?"attack":mob.getTarget()!=null?"pursuit":"idle_or_walk";
        return "live_goal";
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent event){
        if(!running||victim==null||event.getEntity()!=victim||event.getSource().getEntity()!=beast)return;
        damageEvents++;damage+=event.getAmount();
        if(event.getSource().getDirectEntity()==beast)meleeEvents++;
        else projectileEvents++;
        if(beast instanceof CompletionAnimated animated && animated.animationState().equals("attack"))attackActionHits++;
    }
    private static int advance(){
        if(++index>=ENTRIES.size()){message("全部 "+ENTRIES.size()+" 种妖兽与Boss展示完毕；和平生物未伪造攻击。可用 one <序号> 重看。");stop();return 1;}
        return begin();
    }
    private static void finish(String reason){
        if(level==null||index>=ENTRIES.size())return;
        Entry entry=ENTRIES.get(index);
        boolean checksMelee=Set.of("copper_beak_vulture","fire_beak_falcon","purple_lightning_bat","thunder_feather_raven").contains(entry.id);
        String meleeCheck=checksMelee?(meleeEvents>0&&attackActionHits>0?"PASS":"FAIL"):"n/a";
        record("RESULT "+(index+1)+" "+entry.id+" reason="+reason+" ticks="+age+" damageEvents="+damageEvents
                +" meleeEvents="+meleeEvents+" projectileOrSkillEvents="+projectileEvents+" attackActionHits="+attackActionHits
                +" meleeCheck="+meleeCheck+" damage="+damage+" actions="+actions);
        writeState();
    }
    private static String status(){return "running="+running+" paused="+paused+" index="+(index+1)+"/"+ENTRIES.size()+" id="+(index<ENTRIES.size()?ENTRIES.get(index).id:"complete")+" age="+age+" damageEvents="+damageEvents+" meleeEvents="+meleeEvents+" projectileOrSkillEvents="+projectileEvents+" attackActionHits="+attackActionHits+" damage="+damage+" actions="+actions;}
    private static void record(String line){results.add(line);LogUtils.getLogger().info("[Creature Showcase] {}",line);writeState();}
    private static void writeState(){
        if(level==null)return;
        try{
            Path dir=level.getServer().getWorldPath(LevelResource.ROOT).resolve("xiantu-showcase");Files.createDirectories(dir);
            Files.writeString(dir.resolve("status.txt"),status());Files.write(dir.resolve("events.txt"),results);
        }catch(java.io.IOException ex){LogUtils.getLogger().error("Cannot save showcase evidence",ex);}
    }
    private static void message(String text){var p=player();if(p!=null)p.sendSystemMessage(Component.literal(text));}
    private static void clearActors(){
        if(beast!=null)((Entity)beast).discard();if(victim!=null)((Entity)victim).discard();beast=null;victim=null;
        if(level!=null){var stale=new ArrayList<Entity>();level.getEntities().getAll().forEach(entity->{if(entity.getTags().contains(TAG))stale.add(entity);});stale.forEach(Entity::discard);}
    }
    private static void stop(){clearActors();running=false;paused=false;if(level!=null)level.getServer().setDifficulty(Difficulty.PEACEFUL,true);writeState();}
    private CreatureShowcase(){}
}
