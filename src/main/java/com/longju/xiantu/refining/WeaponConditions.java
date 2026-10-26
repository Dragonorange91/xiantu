package com.longju.xiantu.refining;

import com.longju.xiantu.combat.*;
import com.longju.xiantu.gongfa.*;
import com.longju.xiantu.spell.SpellService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Conditional weapon gameplay; no client animation or particle is involved in a hit decision. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class WeaponConditions {
    private static final Map<UUID,Hand> HANDS=new HashMap<>();
    private static final ThreadLocal<Boolean> SECONDARY=ThreadLocal.withInitial(()->false);
    private static final String TIMERS="xiantu_weapon_timers",MODS="xiantu_weapon_modifiers";
    private record Proc(ServerPlayer owner,LivingEntity target,ItemStack weapon,double damage,Element element){}
    private static final List<Proc> PROCS=new ArrayList<>();
    private static final class Hand {
        final ItemStack stack;UUID target;long attackTick=-1,criticalTick=-1,empoweredUntil,armorUntil,speedUntil,controlUntil,windupUntil,dragonUntil;
        int combo,dragon;UUID chargeTransaction;boolean empowered;float charge;net.minecraft.world.phys.Vec3 chargeStart;
        Hand(ItemStack stack){this.stack=stack;}
    }
    private static long now(){return SpellService.clock();}
    private static CompoundTag timers(LivingEntity e){var root=e.getPersistentData();if(!root.contains(TIMERS))root.put(TIMERS,new CompoundTag());return root.getCompound(TIMERS);}
    private static boolean available(LivingEntity e,String key){return timers(e).getLong(key)<=now();}
    private static boolean take(LivingEntity e,String key,int ticks){var t=timers(e);long now=now();t.getAllKeys().removeIf(k->t.getLong(k)<=now);if(t.getLong(key)>now||t.size()>=64)return false;t.putLong(key,now+ticks);return true;}
    private static String targetKey(ServerPlayer p,String effect){return p.getUUID()+"_"+effect;}
    private static Hand hand(ServerPlayer p){
        var r=WeaponPassives.active(p);if(r==null||r.tier()<5){HANDS.remove(p.getUUID());return null;}
        var s=p.getMainHandItem();var current=HANDS.get(p.getUUID());if(current==null||current.stack!=s){p.getPersistentData().remove("xiantu_weapon_guard_until");p.getPersistentData().remove("xiantu_weapon_active_control_until");current=new Hand(s);HANDS.put(p.getUUID(),current);}return current;
    }
    private static boolean primary(LivingHurtEvent e,ServerPlayer p){var h=hand(p);return !SECONDARY.get()&&h!=null&&e.getSource().is(DamageTypes.PLAYER_ATTACK)&&e.getSource().getDirectEntity()==p&&h.attackTick==now()&&e.getEntity().getUUID().equals(h.target);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void attack(AttackEntityEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p)||e.isCanceled())return;var h=hand(p);if(h==null)return;
        if(h.target!=null&&!h.target.equals(e.getTarget().getUUID())){h.combo=0;h.dragon=0;}
        h.target=e.getTarget().getUUID();h.attackTick=now();h.criticalTick=-1;h.charge=p.getAttackStrengthScale(.5F);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void critical(CriticalHitEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&e.getResult()!=Event.Result.DENY&&(e.isVanillaCritical()||e.getResult()==Event.Result.ALLOW)){var h=hand(p);if(h!=null&&e.getTarget().getUUID().equals(h.target))h.criticalTick=now();}
    }
    public static double damageBonus(ServerPlayer p,LivingEntity target){
        if(SECONDARY.get())return 0;var h=hand(p);if(h==null)return 0;var r=WeaponPassives.active(p);double bonus=0;
        if(h.empowered&&h.empoweredUntil>now())bonus+=r.theme().equals("混沌斧")?.06:r.theme().equals("承影剑")?.05:r.theme().equals("开天斧")?.08:0;
        if(h.dragonUntil>now()&&target.getUUID().equals(h.target)&&(r.theme().equals("龙渊剑")||r.theme().equals("玄龙枪")))bonus+=h.dragon*.01;
        return bonus*WeaponPassives.scale(h.stack);
    }
    public static double controlBonus(ServerPlayer p){var h=hand(p);double bonus=h!=null&&h.controlUntil>now()&&WeaponPassives.active(p).theme().equals("大道枪")?.05*WeaponPassives.scale(h.stack):0;return bonus+(h!=null&&p.getPersistentData().getLong("xiantu_weapon_active_control_until")>now()?.15:0);}
    public static double windupBonus(ServerPlayer p){var h=hand(p);return h!=null&&h.windupUntil>now()&&WeaponPassives.active(p).theme().equals("含光剑")?.1*WeaponPassives.scale(h.stack):0;}
    public static void spellBegan(ServerPlayer p){var h=hand(p);if(h!=null)h.windupUntil=0;}
    /** A future authoritative dodge implementation may call this after it cancels a real melee hit. */
    public static void meleeEvaded(ServerPlayer p){var h=hand(p);if(h!=null&&WeaponPassives.active(p).theme().equals("承影剑")&&take(p,"shadow",300)){h.empowered=true;h.empoweredUntil=now()+120;}}
    private static UUID modifierId(String key){return UUID.nameUUIDFromBytes(("xiantu.weapon."+key).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private static void timed(LivingEntity target,Attribute attribute,String key,double value,int ticks){
        if(attribute==null)return;var a=target.getAttribute(attribute);if(a==null)return;
        var root=target.getPersistentData();if(!root.contains(MODS))root.put(MODS,new CompoundTag());var data=root.getCompound(MODS);var n=new CompoundTag();
        var old=data.getCompound(key);if(old.getLong("Until")>now()&&old.getDouble("Value")<value)return;
        n.putString("Attribute",String.valueOf(net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getKey(attribute)));n.putLong("Until",now()+ticks);n.putDouble("Value",value);data.put(key,n);
        a.removeModifier(modifierId(key));a.addTransientModifier(new AttributeModifier(modifierId(key),key,value,key.equals("attack")||key.equals("slow")?AttributeModifier.Operation.MULTIPLY_TOTAL:AttributeModifier.Operation.ADDITION));
    }
    public static void weakenResistance(LivingEntity t,Element element,int ticks){
        var attribute=ElementalAttributes.RESISTANCE.get(element);
        if(attribute!=null)timed(t,attribute.get(),element.id(),-.05,ticks);
    }
    public static void applySlow(LivingEntity t,double value,int ticks){slow(t,value,ticks);}
    private static void slow(LivingEntity t,double value,int ticks){
        if(t instanceof Player p){double reduction=EquipmentConditions.controlReduction(p);if(reduction>=1)return;ticks=Math.max(1,(int)Math.ceil(ticks*(1-reduction)));}
        timed(t,Attributes.MOVEMENT_SPEED,"slow",-Math.min(.5,value),ticks);
    }
    private static void reveal(LivingEntity t,int ticks){t.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING,ticks,0,false,false));}
    @SubscribeEvent public static void living(LivingEvent.LivingTickEvent e){
        var t=e.getEntity();if(t.level().isClientSide)return;var data=t.getPersistentData().getCompound(MODS);
        for(var key:new ArrayList<>(data.getAllKeys()))if(data.getCompound(key).getLong("Until")<=now()){
            var attr=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(new net.minecraft.resources.ResourceLocation(data.getCompound(key).getString("Attribute")));var a=attr==null?null:t.getAttribute(attr);if(a!=null)a.removeModifier(modifierId(key));data.remove(key);
        }
    }
    @SubscribeEvent public static void shield(ShieldBlockEvent e){
        var t=e.getEntity();if(t.level().isClientSide||e.getBlockedDamage()<=0)return;
        var n=t.getPersistentData();if(n.getLong("xiantu_weapon_block_until")>now())e.setBlockedDamage((float)(e.getBlockedDamage()*(1-Math.min(.5,n.getDouble("xiantu_weapon_block_loss")))));
        if(t instanceof ServerPlayer p){var h=hand(p);if(h==null)return;String theme=WeaponPassives.active(p).theme();
            if(theme.equals("干将剑")&&take(p,"ganjiang",240))h.armorUntil=now()+80;
            if(theme.equals("天罡枪")&&take(p,"tiangang",300))p.getPersistentData().putLong("xiantu_weapon_guard_until",now()+40);
        }
    }
    public static double armorPoints(ServerPlayer p,LivingEntity t){
        var h=hand(p);double points=t.getPersistentData().getLong("xiantu_weapon_armor_until")>now()?t.getPersistentData().getDouble("xiantu_weapon_armor_loss"):0;
        if(h==null)return points;String theme=WeaponPassives.active(p).theme();double k=WeaponPassives.scale(h.stack);
        if(theme.equals("干将剑")&&h.armorUntil>now())points+=.05*k;
        if((theme.equals("开山斧")&&!t.isBlocking()||theme.equals("玄铁斧"))&&available(p,"armor_"+theme))points+=.03*k;
        return Math.min(.15,points);
    }
    @SubscribeEvent(priority=EventPriority.LOW) public static void hurt(LivingHurtEvent e){
        if(e.getEntity().level().isClientSide||e.getAmount()<=0)return;
        if(e.getEntity() instanceof ServerPlayer victim){var h=hand(victim);if(h!=null&&WeaponPassives.active(victim).theme().equals("干将剑")&&take(victim,"ganjiang",240))h.armorUntil=now()+80;
            if(victim.getPersistentData().getLong("xiantu_weapon_guard_until")>now()&&WeaponPassives.active(victim)!=null&&WeaponPassives.active(victim).theme().equals("天罡枪")&&!e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))e.setAmount(e.getAmount()*.95F);
            if(e.getSource().getDirectEntity() instanceof LivingEntity&&e.getSource().getEntity()==e.getSource().getDirectEntity()&&h!=null&&WeaponPassives.active(victim).theme().equals("镇岳斧")&&take(victim,"mountain",240)){h.empowered=true;h.empoweredUntil=now()+120;}
        }
        if(!(e.getSource().getEntity() instanceof ServerPlayer p)||e.getSource().getDirectEntity()!=p||!e.getSource().is(DamageTypes.PLAYER_ATTACK))return;
        double points=armorPoints(p,e.getEntity());if(points>0&&!e.getSource().is(DamageTypeTags.BYPASSES_ARMOR)){
            float raw=e.getAmount(),after=net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(raw,e.getEntity().getArmorValue(),(float)e.getEntity().getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            double ignored=Math.min(points,Math.max(0,1-after/raw)),wanted=after+raw*ignored;float low=raw,high=raw*2;for(int i=0;i<24;i++){float mid=(low+high)/2;if(net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(mid,e.getEntity().getArmorValue(),(float)e.getEntity().getAttributeValue(Attributes.ARMOR_TOUGHNESS))<wanted)low=mid;else high=mid;}e.setAmount(high);
        }
        if(!primary(e,p))return;
        var h=hand(p);var r=WeaponPassives.active(p);double k=WeaponPassives.scale(h.stack);var target=e.getEntity();
        if((r.theme().equals("太阿剑")||r.theme().equals("碎星斧"))&&available(target,targetKey(p,"shield"))){
            double shield=target.getAbsorptionAmount()+target.getPersistentData().getDouble("xiantu_spell_shield");if(shield>0&&take(target,targetKey(p,"shield"),240)){
                double extra=Math.min(target.getMaxHealth()*.01,shield*(r.theme().equals("太阿剑")?.03:.02)*k),custom=Math.min(extra,target.getPersistentData().getDouble("xiantu_spell_shield"));target.getPersistentData().putDouble("xiantu_spell_shield",Math.max(0,target.getPersistentData().getDouble("xiantu_spell_shield")-custom));target.setAbsorptionAmount((float)Math.max(0,target.getAbsorptionAmount()-(extra-custom)));
            }
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void hit(LivingDamageEvent e){
        if(e.getAmount()<=0||SECONDARY.get()||!(e.getSource().getEntity() instanceof ServerPlayer p)||!e.getSource().is(DamageTypes.PLAYER_ATTACK)||e.getSource().getDirectEntity()!=p)return;
        var h=hand(p);if(h==null||h.attackTick!=now()||!e.getEntity().getUUID().equals(h.target))return;
        var target=e.getEntity();var r=WeaponPassives.active(p);String theme=r.theme();double k=WeaponPassives.scale(h.stack);boolean boss=GongfaEvents.target(target).boss();
        if(theme.equals("混沌斧")){if(h.empowered&&h.empoweredUntil>now())h.empowered=false;else if(take(p,"chaos",400)){h.empowered=true;h.empoweredUntil=now()+200;}}
        else if(theme.equals("承影剑")||theme.equals("开天斧"))h.empowered=false;
        if(theme.equals("镇岳斧")&&h.empowered&&h.empoweredUntil>now()){h.empowered=false;if(!boss){var v=target.position().subtract(p.position()).multiply(1,0,1).normalize();target.knockback(GongfaEvents.target(target).elite()?.5:1,-v.x,-v.z);}}
        if(theme.equals("干将剑"))h.armorUntil=0;
        if(theme.equals("开山斧")||theme.equals("玄铁斧"))take(p,"armor_"+theme,200);
        if(theme.equals("青霜剑")&&take(target,targetKey(p,"frost"),240))timed(target,ElementalAttributes.RESISTANCE.get(Element.WATER).get(),"water",-.05*k,60);
        if(theme.equals("含光剑")&&h.criticalTick==now()&&take(p,"light",300))h.windupUntil=now()+100;
        if((theme.equals("宵练剑")&&(target instanceof com.longju.xiantu.entity.InnerDemonEntity||target.isInvisible())||theme.equals("神道枪"))&&take(target,targetKey(p,"reveal"),400))reveal(target,80);
        if(theme.equals("紫电剑")&&h.criticalTick==now()&&p.getRandom().nextDouble()<.2&&take(target,targetKey(p,"paralysis"),400))slow(target,boss?.1:.5,boss?20:10);
        if((theme.equals("青虹剑")||theme.equals("穿云枪"))&&take(p,"speed_"+theme,240))h.speedUntil=now()+60;
        if(theme.equals("断岳斧")&&take(target,targetKey(p,"slow"),280))slow(target,(boss?.04:.08)*k,40);
        if(theme.equals("巨灵斧")&&h.charge>=.9&&take(p,"giant",320))timed(target,Attributes.ATTACK_DAMAGE,"attack",-.05*k,boss?20:60);
        if(theme.equals("破军斧")&&take(p,"shield_weaken",300)){target.getPersistentData().putLong("xiantu_weapon_block_until",now()+60);target.getPersistentData().putDouble("xiantu_weapon_block_loss",.10*k);}
        if(theme.equals("破军枪")&&p.isSprinting()&&take(target,targetKey(p,"armor"),240)){target.getPersistentData().putLong("xiantu_weapon_armor_until",now()+60);target.getPersistentData().putDouble("xiantu_weapon_armor_loss",.04*k);}
        if(theme.equals("灭道枪")&&take(target,targetKey(p,"resist"),300))for(var attr:ElementalAttributes.RESISTANCE.entrySet())timed(target,attr.getValue().get(),attr.getKey().id(),-.05*k,60);
        if((theme.equals("龙渊剑")&&(GongfaEvents.target(target).elite()||boss)||theme.equals("玄龙枪"))&&take(target,targetKey(p,"dragon_"+theme),40)){if(h.dragonUntil<=now())h.dragon=0;h.dragon=Math.min(3,h.dragon+1);h.dragonUntil=now()+200;}
        if(theme.equals("赤霄剑")&&++h.combo>=3&&take(target,targetKey(p,"fire"),240)){h.combo=0;enqueue(p,target,e.getAmount()*.05,Element.FIRE);}
        if(theme.equals("裂地斧")&&target.onGround()&&take(p,"quake",240))for(var other:p.serverLevel().getEntitiesOfClass(LivingEntity.class,target.getBoundingBox().inflate(2),t->t!=target&&SpellService.enemy(p,t)))if(other.distanceToSqr(target)<=4&&p.hasLineOfSight(other))enqueue(p,other,e.getAmount()*.2,Element.PHYSICAL);
        if(theme.equals("弑神枪")&&target instanceof com.longju.xiantu.entity.CompletionLandEntity beast&&beast.weaponInterruptible()&&p.getRandom().nextDouble()<.1&&take(p,"interrupt",400))beast.weaponInterrupt();
        if(theme.equals("仙灵枪")&&take(p,"spirit",400)){var d=GongfaService.data(p);d.setPower(Math.min(d.powerMax(),d.power()+Math.min(50,d.powerMax()*.01*k)));}
        if(theme.equals("大道枪")&&++h.combo>=4&&take(p,"dao_control",400)){h.combo=0;h.controlUntil=now()+60;}
        // Clear the authenticated swing so sweeping/secondary callbacks cannot trigger again.
        h.attackTick=-1;
    }
    private static void enqueue(ServerPlayer p,LivingEntity t,double amount,Element element){if(PROCS.size()<512)PROCS.add(new Proc(p,t,p.getMainHandItem(),amount,element));}
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var pending=new ArrayList<>(PROCS);PROCS.clear();
        for(var proc:pending){var p=proc.owner;var t=proc.target;if(!p.isAlive()||p.getMainHandItem()!=proc.weapon||!RefinedEquipment.usable(p,proc.weapon)||p.hasDisconnected()||p.level()!=t.level()||!SpellService.enemy(p,t)||!p.hasLineOfSight(t)||p.distanceToSqr(t)>64)continue;
            double damage=proc.damage;
            int invulnerability=t.invulnerableTime;SECONDARY.set(true);try{t.invulnerableTime=0;float before=t.getHealth()+t.getAbsorptionAmount();boolean hit=t.hurt(new ElementalDamageSource(p.damageSources().mobAttack(p),proc.element),(float)damage);
                if(hit&&t.getHealth()+t.getAbsorptionAmount()<before)com.longju.xiantu.spell.SkillPresentation.emit(p,UUID.randomUUID(),proc.element==Element.FIRE?"weapon_fire":proc.element==Element.PHYSICAL||proc.element==Element.EARTH?"weapon_earth":"weapon_metal","HIT",t.position().add(0,t.getBbHeight()*.5,0),p.getLookAngle());}finally{t.invulnerableTime=Math.max(t.invulnerableTime,invulnerability);SECONDARY.remove();}
        }
    }
    public static boolean chargeWeapon(ServerPlayer p){var r=WeaponPassives.active(p);return r!=null&&r.tier()>=5&&(r.theme().equals("开天斧")||r.theme().equals("贯日枪"));}
    public static net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand which){
        var active=WeaponActives.use(level,player,which);if(active.getResult()!=net.minecraft.world.InteractionResult.PASS)return active;
        var stack=player.getItemInHand(which);var r=RefinedEquipment.recipe(stack);
        if(which!=net.minecraft.world.InteractionHand.MAIN_HAND||r==null||r.tier()<5||!(r.theme().equals("开天斧")||r.theme().equals("贯日枪")))return net.minecraft.world.InteractionResultHolder.pass(stack);
        // Client starts using only after a server-authorized WINDUP acknowledgement.
        if(player instanceof ServerPlayer p){var h=hand(p);if(h==null||!available(p,"charge_"+r.theme())||p.containerMenu!=p.inventoryMenu||p.isUsingItem()||SpellService.casting(p)||QitianSkills.casting(p)||com.longju.xiantu.talisman.TalismanEffects.locked(p)||com.longju.xiantu.talisman.TalismanEffects.casting(p))return net.minecraft.world.InteractionResultHolder.fail(stack);h.chargeStart=p.position();h.chargeTransaction=UUID.randomUUID();p.startUsingItem(which);com.longju.xiantu.spell.SkillPresentation.emit(p,h.chargeTransaction,WeaponActives.skillId(r.theme()),"WINDUP",p.position(),p.getLookAngle());}
        return net.minecraft.world.InteractionResultHolder.consume(stack);
    }
    public static void release(ItemStack stack,net.minecraft.world.level.Level level,LivingEntity entity,int remaining){
        if(!(entity instanceof ServerPlayer p)||!chargeWeapon(p))return;var h=hand(p);var r=WeaponPassives.active(p);if(h==null||h.chargeStart==null)return;
        var start=h.chargeStart;var transaction=h.chargeTransaction;h.chargeStart=null;h.chargeTransaction=null;int elapsed=72000-remaining;
        com.longju.xiantu.spell.SkillPresentation.emit(p,transaction,WeaponActives.skillId(r.theme()),"END",p.position(),p.getLookAngle());
        if(p.getMainHandItem()!=stack||p.hurtTime>0||p.position().distanceToSqr(start)>.04||elapsed<(r.theme().equals("开天斧")?20:16))return;
        if(!take(p,"charge_"+r.theme(),r.theme().equals("开天斧")?600:360))return;
        com.longju.xiantu.spell.SkillPresentation.emit(p,transaction,WeaponActives.skillId(r.theme()),"RELEASE",p.position(),p.getLookAngle());
        if(r.theme().equals("开天斧")){h.empowered=true;h.empoweredUntil=now()+120;return;}
        var from=p.getEyePosition();var to=SpellService.clip(p.serverLevel(),p,from,from.add(p.getLookAngle().scale(3)));
        var targets=p.serverLevel().getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(from,to).inflate(.5),t->SpellService.enemy(p,t)&&t.getBoundingBox().inflate(.25).clip(from,to).isPresent()).stream().sorted(Comparator.comparingDouble(p::distanceToSqr)).limit(3).toList();
        for(int i=0;i<targets.size();i++)enqueue(p,targets.get(i),p.getAttributeValue(Attributes.ATTACK_DAMAGE)*(i==0?1:i==1?.7:.5),WeaponPassives.element(stack));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        var h=hand(p);var attr=p.getAttribute(Attributes.MOVEMENT_SPEED);if(attr!=null){var id=modifierId("self_speed");attr.removeModifier(id);if(h!=null&&h.speedUntil>now())attr.addTransientModifier(new AttributeModifier(id,"武器疾行",.05*WeaponPassives.scale(h.stack),AttributeModifier.Operation.MULTIPLY_TOTAL));}
        if(h!=null&&h.chargeStart!=null&&(p.hurtTime>0||p.position().distanceToSqr(h.chargeStart)>.04||p.getUseItem()!=h.stack)){com.longju.xiantu.spell.SkillPresentation.emit(p,h.chargeTransaction,WeaponActives.skillId(WeaponPassives.active(p).theme()),"CANCEL",p.position(),p.getLookAngle());h.chargeStart=null;h.chargeTransaction=null;p.stopUsingItem();}
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){HANDS.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void died(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)HANDS.remove(p.getUUID());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){HANDS.remove(e.getEntity().getUUID());}
    public static void copyCooldowns(net.minecraft.world.entity.player.Player original,net.minecraft.world.entity.player.Player next){for(String key:original.getPersistentData().getAllKeys())if(key.startsWith("xiantu_active_"))next.getPersistentData().putLong(key,original.getPersistentData().getLong(key));if(original.getPersistentData().contains(TIMERS))next.getPersistentData().put(TIMERS,original.getPersistentData().getCompound(TIMERS).copy());}
    private WeaponConditions(){}
}
