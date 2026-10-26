package com.longju.xiantu.spell;

import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.cultivation.CultivationEvents;
import com.longju.xiantu.network.*;
import net.minecraft.server.level.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;

@Mod.EventBusSubscriber(modid="xiantu")
public final class GreatFireballService {
    public static final int BEGIN=0,HOLD=1,RELEASE=2,CANCEL=3;
    private static final class Charge {
        final UUID gesture,transaction=UUID.randomUUID();final int slot;final long start;final ResourceKey<Level> dimension;final long maximumCost;
        long heartbeat,paid;int paidTicks;
        Charge(ServerPlayer p,int slot,UUID gesture){this.slot=slot;this.gesture=gesture;start=heartbeat=SpellService.now(p);dimension=p.level().dimension();maximumCost=SpellService.cost(p,spell());}
    }
    private static final class Flight {
        final UUID transaction,owner;final ServerLevel level;final Vec3 velocity;final int charge;Vec3 position;double distance;int age;
        Flight(ServerPlayer p,Charge c,int ticks,Vec3 position){transaction=c.transaction;owner=p.getUUID();level=p.serverLevel();charge=ticks;this.position=position;velocity=p.getLookAngle().normalize().scale(GreatFireballRules.speed(ticks));}
    }
    private static final Map<UUID,Charge> CHARGES=new HashMap<>();
    private static final List<Flight> FLIGHTS=new ArrayList<>();
    private static final Map<UUID,Long> REQUESTS=new HashMap<>();
    public static SpellCatalog.Spell spell(){return SpellCatalog.find("great_fireball");}
    public static boolean casting(ServerPlayer p){return CHARGES.containsKey(p.getUUID());}
    private static int ticks(ServerPlayer p,Charge c){return (int)Math.min(GreatFireballRules.MAX_TICKS,Math.max(0,SpellService.now(p)-c.start));}
    public static Vec3 center(ServerPlayer p,double radius){return p.getEyePosition().add(p.getLookAngle().normalize().scale(radius+2.1)).add(0,GreatFireballRules.lift(radius),0);}
    private static void event(ServerPlayer p,Charge c,SpellLifecycleEvent.Phase phase,Vec3 at){net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new SpellLifecycleEvent(p,"great_fireball",c.transaction,null,phase,at,p.getLookAngle()));}
    private static void send(ServerPlayer p,UUID transaction,int phase,int ticks,Vec3 position,Vec3 velocity){
        var packet=new GreatFireballStatePacket(transaction,p.getUUID(),p.getId(),p.level().dimension().location(),phase,ticks,position,velocity);
        XiantuNetwork.CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(position.x,position.y,position.z,96,p.level().dimension())),packet);
    }
    public static String input(ServerPlayer p,int slot,UUID gesture,int action){
        if(slot<0||slot>=4||action<0||action>3)return "";
        var charge=CHARGES.get(p.getUUID());
        if(action!=BEGIN){
            if(charge==null||charge.slot!=slot||!charge.gesture.equals(gesture))return "";
            if(action==HOLD)charge.heartbeat=SpellService.now(p);
            else if(action==RELEASE){if(valid(p,charge))release(p,charge);else cancel(p);}
            else cancel(p);
            return "";
        }
        if(charge!=null)return "";
        long now=SpellService.now(p);if(REQUESTS.getOrDefault(p.getUUID(),Long.MIN_VALUE)>now)return "";REQUESTS.put(p.getUUID(),now+4);
        if(!SpellService.data(p).slot(slot).equals("great_fireball"))return "";
        String error=SpellService.denial(p,spell(),true);if(!error.isEmpty())return error;
        if(p.containerMenu!=p.inventoryMenu||p.isUsingItem()||p.isPassenger()||SpellService.casting(p)||com.longju.xiantu.talisman.TalismanEffects.casting(p)||com.longju.xiantu.refining.QitianSkills.casting(p))return "当前不能凝聚火球";
        if(SpellService.data(p).cooldown("great_fireball",now)>0)return "法术冷却中";
        var next=new Charge(p,slot,gesture);
        if(!pay(p,next,0))return "灵力不足";
        GongfaService.cancel(p,"发动法术");GongfaService.markCombat(p);com.longju.xiantu.alchemy.CrossDimensionMedicine.cancel(p);com.longju.xiantu.talisman.TalismanEffects.breakInvisibility(p);
        CHARGES.put(p.getUUID(),next);com.longju.xiantu.refining.EquipmentConditions.began(p);com.longju.xiantu.refining.WeaponConditions.spellBegan(p);
        event(p,next,SpellLifecycleEvent.Phase.WINDUP,center(p,GreatFireballRules.radius(0)));
        send(p,next.transaction,0,0,center(p,GreatFireballRules.radius(0)),Vec3.ZERO);SpellService.sync(p);return "";
    }
    private static boolean pay(ServerPlayer p,Charge c,int ticks){
        long due=(long)Math.ceil(c.maximumCost*GreatFireballRules.costFraction(ticks)),delta=Math.max(0,due-c.paid);var data=GongfaService.data(p);
        if(data.power()*1000+1e-6<delta)return false;
        if(delta>0){data.setPower(Math.max(0,data.power()-delta/1000.0));c.paid=due;}c.paidTicks=ticks;return true;
    }
    private static boolean valid(ServerPlayer p,Charge c){return p.isAlive()&&!p.isSpectator()&&!p.isPassenger()&&!p.isUsingItem()&&p.level().dimension().equals(c.dimension)&&p.containerMenu==p.inventoryMenu&&SpellService.data(p).slot(c.slot).equals("great_fireball")&&SpellService.denial(p,spell(),true).isEmpty();}
    public static void cancel(ServerPlayer p){
        var c=CHARGES.remove(p.getUUID());if(c==null)return;
        send(p,c.transaction,3,ticks(p,c),center(p,GreatFireballRules.radius(ticks(p,c))),Vec3.ZERO);event(p,c,SpellLifecycleEvent.Phase.CANCEL,p.position());
        SpellService.data(p).setCooldown("great_fireball",SpellService.now(p)+20);CultivationEvents.sync(p);SpellService.sync(p);
    }
    private static void release(ServerPlayer p,Charge c){
        int ticks=ticks(p,c);
        // Charge is measured on the server and paid in whole fixed-point increments.
        if(!pay(p,c,ticks))ticks=c.paidTicks;
        if(ticks<GreatFireballRules.MIN_TICKS){cancel(p);return;}
        CHARGES.remove(p.getUUID());SpellService.data(p).setCooldown("great_fireball",SpellService.now(p)+spell().cooldown());
        com.longju.xiantu.refining.EquipmentConditions.paid(p,spell());
        var origin=center(p,GreatFireballRules.radius(ticks));var unobstructed=SpellService.clip(p.serverLevel(),p,p.getEyePosition(),origin);
        var flight=new Flight(p,c,ticks,unobstructed);event(p,c,SpellLifecycleEvent.Phase.RELEASE,unobstructed);
        send(p,c.transaction,1,ticks,unobstructed,flight.velocity);
        if(unobstructed.distanceToSqr(origin)>.001)explode(p,flight,unobstructed);else FLIGHTS.add(flight);
        CultivationEvents.sync(p);SpellService.sync(p);
    }
    private static void explode(ServerPlayer p,Flight f,Vec3 at){
        double range=GreatFireballRules.blastRadius(f.charge);
        for(var target:f.level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(range),e->SpellService.enemy(p,e))){
            var nearest=new Vec3(Math.max(target.getBoundingBox().minX,Math.min(at.x,target.getBoundingBox().maxX)),Math.max(target.getBoundingBox().minY,Math.min(at.y,target.getBoundingBox().maxY)),Math.max(target.getBoundingBox().minZ,Math.min(at.z,target.getBoundingBox().maxZ)));
            double distance=at.distanceTo(nearest);if(distance>range)continue;
            // Walls block splash; no block explosion, fire placement or terrain changes.
            Vec3 start=at.add(target.getBoundingBox().getCenter().subtract(at).normalize().scale(.08)),end=target.getBoundingBox().getCenter();
            if(SpellService.clip(f.level,p,start,end).distanceToSqr(end)>.04)continue;
            SpellService.damage(p,target,spell(),f.transaction,GreatFireballRules.damageMultiplier(f.charge)*GreatFireballRules.falloff(distance,range));
        }
        send(p,f.transaction,2,f.charge,at,Vec3.ZERO);
        f.level.playSound(null,at.x,at.y,at.z,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,net.minecraft.sounds.SoundSource.PLAYERS,1.3F,.7F);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;
        for(var p:server.getPlayerList().getPlayers()){
            var c=CHARGES.get(p.getUUID());if(c==null)continue;long now=SpellService.now(p);
            if(!valid(p,c)||now-c.heartbeat>GreatFireballRules.HEARTBEAT_TIMEOUT){cancel(p);continue;}
            int ticks=ticks(p,c);if(!pay(p,c,ticks)){release(p,c);continue;}
            if(now-c.start>=GreatFireballRules.MAX_HOLD){release(p,c);continue;}
            if((now-c.start)%4==0)send(p,c.transaction,0,ticks,center(p,GreatFireballRules.radius(ticks)),Vec3.ZERO);
            if((now-c.start)%10==0){CultivationEvents.sync(p);SpellService.sync(p);}
        }
        for(int i=FLIGHTS.size()-1;i>=0;i--){
            var f=FLIGHTS.get(i);var p=server.getPlayerList().getPlayer(f.owner);
            if(p==null||!p.isAlive()||p.level()!=f.level){if(p!=null)send(p,f.transaction,3,f.charge,f.position,Vec3.ZERO);FLIGHTS.remove(i);continue;}
            var proposed=f.position.add(f.velocity);if(!f.level.hasChunkAt(BlockPos.containing(proposed))||!f.level.getWorldBorder().isWithinBounds(BlockPos.containing(proposed))){send(p,f.transaction,3,f.charge,f.position,Vec3.ZERO);FLIGHTS.remove(i);continue;}
            double radius=GreatFireballRules.radius(f.charge)*.8;Vec3 end=SpellService.clip(f.level,p,f.position,proposed);
            // Seven swept rays approximate the visible sphere without scanning blocks or loading chunks.
            for(var offset:List.of(new Vec3(radius,0,0),new Vec3(-radius,0,0),new Vec3(0,radius,0),new Vec3(0,-radius,0),new Vec3(0,0,radius),new Vec3(0,0,-radius))){
                var hit=SpellService.clip(f.level,p,f.position.add(offset),proposed.add(offset)).subtract(offset);if(hit.distanceToSqr(f.position)<end.distanceToSqr(f.position))end=hit;
            }
            boolean hit=end.distanceToSqr(proposed)>.001;
            for(var target:f.level.getEntitiesOfClass(LivingEntity.class,new AABB(f.position,end).inflate(radius),t->SpellService.enemy(p,t))){
                var intersection=target.getBoundingBox().inflate(radius).clip(f.position,end);if(target.getBoundingBox().inflate(radius).contains(f.position)){end=f.position;hit=true;break;}
                if(intersection.isPresent()){end=intersection.get();hit=true;}
            }
            f.distance+=f.position.distanceTo(end);f.position=end;f.age++;
            if(hit||f.distance>=spell().range()||f.age>=80){explode(p,f,end);FLIGHTS.remove(i);}else if(f.age%2==0)send(p,f.transaction,1,f.charge,f.position,f.velocity);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void damage(LivingDamageEvent e){if(e.getEntity() instanceof ServerPlayer p&&e.getAmount()>0)cancel(p);}
    private static void leave(ServerPlayer p){cancel(p);REQUESTS.remove(p.getUUID());FLIGHTS.removeIf(f->{if(!f.owner.equals(p.getUUID()))return false;send(p,f.transaction,3,f.charge,f.position,Vec3.ZERO);return true;});}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)leave(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)leave(p);}
    @SubscribeEvent public static void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)leave(p);}
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent e){CHARGES.clear();FLIGHTS.clear();REQUESTS.clear();}
    private GreatFireballService(){}
}
