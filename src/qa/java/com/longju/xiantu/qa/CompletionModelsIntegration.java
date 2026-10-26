package com.longju.xiantu.qa;
import com.longju.xiantu.entity.*;
import com.longju.xiantu.registry.ModEntities;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import java.nio.file.*;

/** Disposable world only. Real movement and combat; never freezes the tested creature. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class CompletionModelsIntegration {
    private static ServerLevel level;
    private static Mob mob;
    private static Pig target;
    private static int index,age;
    private static boolean single;
    private static double moved;
    private static Vec3 initial;
    private static final Set<String> motions=new LinkedHashSet<>();
    private static final List<String> results=new ArrayList<>();
    private static final Map<BlockPos,BlockState> restore=new HashMap<>();
    private static final Path LOG=Path.of("xiantu-completion-qa.txt");
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("xiantucompletionqa").requires(s->s.hasPermission(2))
            .then(Commands.literal("start").executes(c->{
                if(!c.getSource().getServer().getWorldData().getLevelName().equals("Xiantu-QA-BeastFlat-20261017"))return 0;
                cleanup();level=c.getSource().getLevel();index=0;single=false;results.clear();startOne();return 1;}))
            .then(Commands.literal("one").then(Commands.argument("index",com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,CompletionBestiary.ALL.size()-1)).executes(c->{
                if(!c.getSource().getServer().getWorldData().getLevelName().equals("Xiantu-QA-BeastFlat-20261017"))return 0;
                cleanup();level=c.getSource().getLevel();index=com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"index");single=true;results.clear();startOne();return 1;})))
            .then(Commands.literal("status").executes(c->{c.getSource().sendSuccess(()->Component.literal("Completion QA "+index+"/"+CompletionBestiary.ALL.size()+" age="+age+" "+results),false);return 1;}))
            .then(Commands.literal("cleanup").executes(c->{cleanup();return 1;})));
    }
    private static void block(BlockPos p,BlockState state){restore.putIfAbsent(p,level.getBlockState(p));level.setBlockAndUpdate(p,state);}
    private static void startOne(){
        age=0;moved=0;motions.clear();var d=CompletionBestiary.ALL.get(index);
        level.getServer().setDifficulty(Difficulty.PEACEFUL,true);
        ServerPlayer player=level.getServer().getPlayerList().getPlayers().stream().filter(p -> !(p instanceof net.minecraftforge.common.util.FakePlayer)).findFirst().orElse(null);
        if(player!=null){player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.teleportTo(level,3220.5,-57,3213.5,0,38);player.displayClientMessage(Component.literal("模型检查 "+(index+1)+"/"+CompletionBestiary.ALL.size()+"："+d.id()),false);}
        for(int x=3214;x<=3226;x++)for(int z=3214;z<=3226;z++){
            block(new BlockPos(x,-63,z),Blocks.STONE.defaultBlockState());
            for(int y=-62;y<=-59;y++)block(new BlockPos(x,y,z),
                    x==3214||x==3226||z==3214||z==3226?Blocks.STONE.defaultBlockState():
                            d.kind().equals("WATER")&&y<=-60?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState());
        }
        mob=ModEntities.COMPLETION.get(d.id()).get().create(level);if(mob==null)throw new IllegalStateException(d.id());
        ((Entity)mob).moveTo(3220.5,d.kind().equals("WATER")?-61:-62,3220.5,180,0);
        mob.setPersistenceRequired();mob.addTag("xiantu_completion_qa");level.addFreshEntity(mob);initial=mob.position();
        mob.getNavigation().moveTo(3223,-62,3223,1);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||mob==null)return;
        age++;moved=Math.max(moved,mob.position().distanceTo(initial));motions.add(((CompletionAnimated)mob).animationState());
        if(age==100){
            level.getServer().setDifficulty(Difficulty.NORMAL,true);
            // Place the combat fixture in the arena centre after the independent locomotion sample.
            // Prevent random stroll reaching a wall from making the attack fixture geometrically impossible.
            if(!CompletionBestiary.ALL.get(index).kind().equals("WATER"))((Entity)mob).moveTo(3220.5,-62,3218.5,0,0);
            mob.getNavigation().stop();mob.setDeltaMovement(Vec3.ZERO);
            target=EntityType.PIG.create(level);target.setNoAi(true);target.setPersistenceRequired();target.addTag("xiantu_completion_qa");
            ((Entity)target).moveTo(3220.5,-62,3221.5,180,0);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);level.addFreshEntity(target);
            ((LivingEntity)mob).hurt(level.damageSources().mobAttack(target),.1F);
        }
        if(age==260){
            boolean passive=CompletionBestiary.ALL.get(index).passive();
            boolean combat=passive?mob.getTarget()==null:target.getHealth()<1000;
            String line=CompletionBestiary.ALL.get(index).id()+" moved="+moved+" animations="+motions+" targetHealth="+target.getHealth()+" behavior="+combat;
            results.add(line);com.mojang.logging.LogUtils.getLogger().info("[Completion QA] {}",line);
            try{Files.write(LOG,results);}catch(Exception e){throw new RuntimeException(e);}
            ((LivingEntity)mob).hurt(level.damageSources().genericKill(),100000);
        }
        if(age>=290){
            ((Entity)mob).discard();((Entity)target).discard();mob=null;target=null;
            if(!single&&++index<CompletionBestiary.ALL.size())startOne();else {level.getServer().setDifficulty(Difficulty.PEACEFUL,true);}
        }
    }
    private static void cleanup(){
        if(mob!=null)((Entity)mob).discard();if(target!=null)((Entity)target).discard();mob=null;target=null;
        if(level!=null){restore.forEach((p,s)->level.setBlockAndUpdate(p,s));level.getServer().setDifficulty(Difficulty.PEACEFUL,true);}
        restore.clear();level=null;
    }
}
