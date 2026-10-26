package com.longju.xiantu.refining;

import com.longju.xiantu.gongfa.*;
import com.longju.xiantu.cultivation.CultivationEvents;
import com.longju.xiantu.combat.*;
import com.longju.xiantu.spell.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.*;
import java.util.*;

/** Extra manual techniques for genuine tier-five and higher weapons; low-tier passives stay intact. */
public final class WeaponActives {
    private record Technique(String name,String effect,int cooldown,double cost,double reach,double multiplier){}
    private static final Map<String,Technique> ALL=Map.ofEntries(
        Map.entry("承影剑",new Technique("无影截脉","直线截击并迟滞目标",240,.06,6,1.05)),
        Map.entry("含光剑",new Technique("含光归元","收光护体，获得短时吸收护盾",360,.08,0,0)),
        Map.entry("龙渊剑",new Technique("龙渊破军","贯穿剑气，最多命中三敌",320,.08,8,1.2)),
        Map.entry("干将剑",new Technique("百炼护锋","护持自身，短时减少所受伤害",400,.07,0,0)),
        Map.entry("碎星斧",new Technique("碎星震击","前方扇面重击，击退并削减护盾",360,.10,4,1.35)),
        Map.entry("混沌斧",new Technique("混沌镇压","近身环形震击并迟滞",440,.12,3.5,1.45)),
        Map.entry("弑神枪",new Technique("弑法穿心","直线穿刺，可打断普通妖兽施法",360,.09,6,1.3)),
        Map.entry("灭道枪",new Technique("灭道蚀脉","贯穿并削弱目标的对应属性抗性",400,.10,7,1.25)),
        Map.entry("天罡枪",new Technique("天罡护身","凝聚短时护盾并抵御控制",400,.08,0,0)),
        Map.entry("仙灵枪",new Technique("仙灵荡尘","前方扇面驱敌，同时恢复少量生命",400,.09,4.5,1.05)),
        Map.entry("神道枪",new Technique("神道照隐","前方穿刺并使敌人显形",360,.08,8,1.2)),
        Map.entry("大道枪",new Technique("大道定岳","近身震击，短时控制减免",480,.12,4,1.5))
    );
    public static String skillId(String theme){return switch(theme){
        case "承影剑"->"shadow_sever";case "含光剑"->"radiant_return";case "龙渊剑"->"dragon_pierce";case "干将剑"->"forged_guard";
        case "碎星斧"->"star_shatter";case "混沌斧"->"chaos_suppress";case "弑神枪"->"godslayer_pierce";case "灭道枪"->"dao_corrosion";
        case "天罡枪"->"celestial_guard";case "仙灵枪"->"spirit_sweep";case "神道枪"->"divine_reveal";case "大道枪"->"great_dao_quake";
        case "开天斧"->"heaven_split";case "贯日枪"->"sun_pierce";default->"weapon_charge";};}
    public static final java.util.Set<String> SKILLS=java.util.Set.of("shadow_sever","radiant_return","dragon_pierce","forged_guard","star_shatter","chaos_suppress","godslayer_pierce","dao_corrosion","celestial_guard","spirit_sweep","divine_reveal","great_dao_quake","heaven_split","sun_pierce");
    public static boolean available(ItemStack stack){var r=RefinedEquipment.recipe(stack);return r!=null&&r.tier()>=5&&ALL.containsKey(r.theme());}
    public static void tooltip(List<Component> lines,ItemStack stack){var r=RefinedEquipment.recipe(stack);if(!available(stack))return;var a=ALL.get(r.theme());
        lines.add(Component.literal("右键主动："+a.name+" · "+a.effect).withStyle(net.minecraft.ChatFormatting.AQUA));
        lines.add(Component.literal("灵力 "+Math.round(a.cost*100)+"% · 冷却 "+a.cooldown/20+"秒").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    public static InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(hand!=InteractionHand.MAIN_HAND||!available(stack))return InteractionResultHolder.pass(stack);
        if(player instanceof ServerPlayer p){var r=RefinedEquipment.recipe(stack);var a=ALL.get(r.theme());var data=GongfaService.data(p);var n=p.getPersistentData();String timer="xiantu_active_"+r.theme();long now=SpellService.clock();
            double cost=Math.max(1,Math.ceil(data.powerMax()*a.cost));String error="";
            if(com.longju.xiantu.talisman.TalismanEffects.locked(p))error="灵力被封锁";
            else if(!p.isAlive()||p.isSpectator()||!RefinedEquipment.usable(p,stack))error="未达到武器使用境界";
            else if(p.containerMenu!=p.inventoryMenu||p.isUsingItem()||com.longju.xiantu.talisman.TalismanEffects.casting(p)||SpellService.casting(p)||QitianSkills.casting(p)||com.longju.xiantu.cultivation.InnerDemonService.active(p))error="请结束当前动作再发动器技";
            else if(n.getLong(timer)>now)error="器技冷却中";else if(data.power()<cost)error="灵力不足";
            if(!error.isEmpty()){p.displayClientMessage(Component.literal(error),true);return InteractionResultHolder.fail(stack);}
            data.setPower(data.power()-cost);n.putLong(timer,now+a.cooldown);GongfaService.markCombat(p);CultivationEvents.sync(p);
            var direction=p.getLookAngle();var from=p.getEyePosition();var to=SpellService.clip(p.serverLevel(),p,from,from.add(direction.scale(a.reach)));
            var tx=UUID.randomUUID();SkillPresentation.emit(p,tx,skillId(r.theme()),"RELEASE",p.position(),direction);
            double scale=WeaponPassives.scale(stack);WeaponConditions.controlBonus(p);
            switch(r.theme()){
                case "含光剑","天罡枪"->p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION,100,Math.min(3,(int)scale),false,false));
                case "干将剑"->p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,80,0,false,false));
                case "大道枪"->n.putLong("xiantu_weapon_active_control_until",now+60);
            }
            if(r.theme().equals("天罡枪"))n.putLong("xiantu_weapon_active_control_until",now+100);
            if(a.reach>0){boolean radial=r.theme().equals("混沌斧")||r.theme().equals("大道枪");boolean fan=r.theme().equals("碎星斧")||r.theme().equals("仙灵枪");
                var bounds=radial||fan?p.getBoundingBox().inflate(a.reach):new AABB(from,to).inflate(.45);
                var targets=p.serverLevel().getEntitiesOfClass(LivingEntity.class,bounds,t->SpellService.enemy(p,t)&&p.hasLineOfSight(t))
                    .stream().filter(t->radial?p.distanceToSqr(t)<=a.reach*a.reach:fan?p.distanceToSqr(t)<=a.reach*a.reach&&t.position().subtract(p.position()).normalize().dot(direction)>.35:t.getBoundingBox().inflate(.4).clip(from,to).isPresent())
                    .sorted(Comparator.comparingDouble(p::distanceToSqr)).limit(radial||fan?8:3).toList();
                for(var target:targets){double amount=p.getAttributeValue(Attributes.ATTACK_DAMAGE)*a.multiplier*(1+.10*(scale-1));var info=GongfaEvents.target(target);
                    int realm=target instanceof ServerPlayer other?GongfaService.data(other).majorRealm()+1:info.realmRank();amount*=SpellRules.realmMultiplier(data.majorRealm()+1,realm);
                    int previousImmunity=target.invulnerableTime;boolean hit;target.invulnerableTime=0;
                    try{hit=target.hurt(new ElementalDamageSource(p.damageSources().mobAttack(p),WeaponPassives.element(stack)),(float)amount);}finally{target.invulnerableTime=Math.max(previousImmunity,target.invulnerableTime);}
                    if(!hit)continue;
                    SkillPresentation.emit(p,tx,skillId(r.theme()),"HIT",target.position().add(0,target.getBbHeight()*.5,0),direction);
                    switch(r.theme()){
                        case "承影剑","混沌斧"->WeaponConditions.applySlow(target,.25,info.boss()?15:40);
                        case "碎星斧"->{target.setAbsorptionAmount(Math.max(0,target.getAbsorptionAmount()-target.getMaxHealth()*.03f));if(!info.boss())target.knockback(.8,-direction.x,-direction.z);}
                        case "弑神枪"->{if(target instanceof com.longju.xiantu.entity.CompletionLandEntity beast&&beast.weaponInterruptible())beast.weaponInterrupt();}
                        case "灭道枪"->WeaponConditions.weakenResistance(target,WeaponPassives.element(stack),40);
                        case "神道枪"->target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING,80,0,false,false));
                        case "仙灵枪"->{if(!info.boss())target.knockback(.7,-direction.x,-direction.z);}
                    }
                }
                if(r.theme().equals("仙灵枪"))p.heal((float)(p.getMaxHealth()*.03));
            }
        }
        // Only the accepted server transaction swings; prediction consumes the click silently.
        return InteractionResultHolder.consume(stack);
    }
    private WeaponActives(){}
}
