package com.longju.xiantu.refining;

import com.longju.xiantu.combat.Element;
import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.spell.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Accessory transactions. Quote methods never reserve a charge or start a cooldown. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class EquipmentConditions {
    private static final String KEY="xiantu_accessory_conditions";
    private static final ThreadLocal<Boolean> APPLYING=ThreadLocal.withInitial(()->false);
    private static CompoundTag state(Player p){var root=p.getPersistentData();if(!root.contains(KEY))root.put(KEY,new CompoundTag());return root.getCompound(KEY);}
    private static long now(){return SpellService.clock();}
    private static boolean ready(Player p,String key){return state(p).getLong(key)<=now();}
    private static void cooldown(Player p,String key,int ticks){state(p).putLong(key,now()+ticks);}

    public static double windupBonus(ServerPlayer p){
        var a=EquipmentEffects.read(p);var d=GongfaService.data(p);
        return a.high("天枢戒")&&d.power()>d.powerMax()*.8&&ready(p,"star_windup")?.04*a.accessory("天枢戒"):0;
    }
    /** Consume the next-cast benefit only once a valid cast has actually started. */
    public static void began(ServerPlayer p){if(windupBonus(p)>0)cooldown(p,"star_windup",400);}
    public static double costReduction(ServerPlayer p,SpellCatalog.Spell spell){
        var a=EquipmentEffects.read(p);double discount=0;
        if(a.high("神枢戒")&&spell.realm()>GongfaService.data(p).majorRealm()&&ready(p,"divine_cost"))discount+=.08*a.accessory("神枢戒");
        if(a.high("道枢戒")&&state(p).getBoolean("dao_free"))discount+=.15*a.accessory("道枢戒");
        return Math.min(.5,discount);
    }
    public static void paid(ServerPlayer p,SpellCatalog.Spell spell){
        var a=EquipmentEffects.read(p);
        if(a.high("神枢戒")&&spell.realm()>GongfaService.data(p).majorRealm()&&ready(p,"divine_cost"))cooldown(p,"divine_cost",600);
        if(a.high("道枢戒")&&state(p).getBoolean("dao_free")){state(p).putBoolean("dao_free",false);state(p).putInt("dao_hits",0);}
    }
    @SubscribeEvent public static void spell(SpellLifecycleEvent event){
        var p=event.caster;var s=state(p);var a=EquipmentEffects.read(p);
        if(event.phase==SpellLifecycleEvent.Phase.HIT&&a.high("道枢戒")&&ready(p,"dao_cost")&&!s.getBoolean("dao_free")){
            // A projectile/area may hit several entities. It still counts as one cast.
            var recent=s.getCompound("hit_casts");recent.getAllKeys().removeIf(id->recent.getLong(id)<=now());String transaction=event.transaction.toString();if(recent.contains(transaction)||recent.size()>=64)return;recent.putLong(transaction,now()+1200);s.put("hit_casts",recent);int hits=s.getInt("dao_hits")+1;s.putInt("dao_hits",hits);
            if(hits>=3){s.putBoolean("dao_free",true);cooldown(p,"dao_cost",900);}
        }
    }
    public static void minorBreakthrough(ServerPlayer p,double actualPowerSpent){
        var a=EquipmentEffects.read(p);if(actualPowerSpent<=0||!a.high("仙枢戒")||!ready(p,"minor_refund"))return;
        var d=GongfaService.data(p);d.setPower(Math.min(d.powerMax(),d.power()+actualPowerSpent*.05*a.accessory("仙枢戒")));cooldown(p,"minor_refund",1200);
    }
    /** Learning is free in the current scroll system; a zero fee never creates free power. */
    public static void learned(ServerPlayer p,double actualPowerSpent){
        if(actualPowerSpent<=0)return;var d=GongfaService.data(p);double k=EquipmentEffects.read(p).accessory("悟道戒");
        d.setPower(Math.min(d.powerMax(),d.power()+actualPowerSpent*.05*k));
    }
    public static double naturalHealingBonus(Player p){
        var a=EquipmentEffects.read(p);boolean calm=p.getPersistentData().getLong("xiantu_equipment_last_combat")+100<=now();
        return a.accessory("长生链")*.04+(calm?a.set("芝华回生甲")*.04:0);
    }
    public static double recoveryBonus(Player p){
        var a=EquipmentEffects.read(p);return a.high("聚灵戒")&&state(p).getLong("gather_until")>now()?a.accessory("聚灵戒")*.04:0;
    }
    /** Add control resistance and declared duration reduction once, with the shared 50% cap. */
    public static double controlReduction(Player p){
        if(QitianSkills.steadfast(p))return 1;
        return Math.min(.5,EquipmentEffects.controlResistance(p)+1-EquipmentEffects.controlDuration(p));
    }
    public static void tick(ServerPlayer p){
        var a=EquipmentEffects.read(p);var s=state(p);long time=now();
        if(!a.high("道枢戒")){s.remove("dao_free");s.remove("dao_hits");s.remove("hit_casts");}
        if(!a.high("聚灵戒"))s.remove("gather_until");
        else if(p.getPersistentData().getLong("xiantu_equipment_last_combat")+100<=time&&s.getLong("gather_until")<=time&&ready(p,"gather")){s.putLong("gather_until",time+160);cooldown(p,"gather",600);}
    }
    /** Clamp only the penetration contribution; ordinary resistance and vulnerability are preserved. */
    public static double penetration(Player p,Element element,double resistance,double penetration){
        if(EquipmentEffects.read(p).accessory("不朽链")==0)return penetration;
        double before=com.longju.xiantu.combat.ElementalRules.resistance(resistance,0);
        double after=com.longju.xiantu.combat.ElementalRules.resistance(resistance,penetration);
        return after>=before-.03?penetration:Math.max(0,Math.min(penetration,.03));
    }
    @SubscribeEvent(priority=EventPriority.LOW) public static void effect(MobEffectEvent.Applicable event){
        if(APPLYING.get()||event.getResult()==Event.Result.DENY||!(event.getEntity() instanceof ServerPlayer p))return;
        var effect=event.getEffectInstance();if(effect.isInfiniteDuration()||effect.getDuration()<=1||effect.getEffect().getCategory()!=MobEffectCategory.HARMFUL)return;
        var a=EquipmentEffects.read(p);double reduction=a.high("清心戒")?.08*a.accessory("清心戒"):0;
        boolean slow=effect.getEffect()==MobEffects.MOVEMENT_SLOWDOWN;
        boolean control=slow||effect.getEffect()==MobEffects.LEVITATION||effect.getEffect()==MobEffects.CONFUSION||effect.getEffect()==MobEffects.DARKNESS||effect.getEffect()==MobEffects.BLINDNESS;
        if(control)reduction=Math.max(reduction,controlReduction(p));
        if(slow&&QitianSkills.steadfast(p)){event.setResult(Event.Result.DENY);return;}
        if(slow&&p.isInWaterOrBubble()&&a.high("避水链"))reduction=Math.max(reduction,.2*a.accessory("避水链"));
        boolean soul=effect.getEffect()==MobEffects.CONFUSION||effect.getEffect()==MobEffects.DARKNESS||effect.getEffect()==MobEffects.BLINDNESS;
        boolean useSoul=soul&&a.high("养魂戒")&&ready(p,"soul_control");if(useSoul)reduction=Math.max(reduction,.2*a.accessory("养魂戒"));
        if(reduction<=0)return;
        var reduced=new MobEffectInstance(effect.getEffect(),Math.max(1,(int)Math.ceil(effect.getDuration()*(1-Math.min(.5,reduction)))),effect.getAmplifier(),effect.isAmbient(),effect.isVisible(),effect.showIcon());
        APPLYING.set(true);boolean added;try{added=p.addEffect(reduced);}finally{APPLYING.remove();}
        // The outer application must not re-add its original, longer duration.
        event.setResult(Event.Result.DENY);if(added&&useSoul)cooldown(p,"soul_control",600);
    }
    public static void copy(Player original,Player replacement,boolean death){
        var saved=state(original).copy();
        if(death){saved.remove("dao_free");saved.remove("dao_hits");saved.remove("hit_casts");saved.remove("gather_until");}
        replacement.getPersistentData().put(KEY,saved);
    }
    /** Check the death-time equipped item, but apply only after ordinary death settlement. */
    @SubscribeEvent public static void death(net.minecraftforge.event.entity.living.LivingDeathEvent event){
        if(event.getEntity() instanceof ServerPlayer p&&EquipmentEffects.read(p).high("轮回链"))state(p).putBoolean("rebirth_heal",true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void respawn(PlayerEvent.PlayerRespawnEvent event){
        if(event.getEntity() instanceof ServerPlayer p&&state(p).getBoolean("rebirth_heal")){state(p).remove("rebirth_heal");p.heal(p.getMaxHealth()*.05F);}
    }
    private EquipmentConditions(){}
}
