package com.longju.xiantu.refining;

import com.longju.xiantu.combat.*;
import com.longju.xiantu.gongfa.GongfaEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Locale;
import net.minecraft.world.level.block.state.BlockState;

/** Only authenticated, currently usable main-hand equipment contributes weapon effects. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class WeaponPassives {
    private static final ThreadLocal<BlockState> MINING=new ThreadLocal<>();
    public static void beginMining(BlockState state){MINING.set(state);}
    public static void endMining(){MINING.remove();}
    public static int durabilityCost(ItemStack stack,int amount,LivingEntity entity){
        if(amount<=0||!(entity instanceof Player p)||!RefinedEquipment.usable(p,stack))return amount;
        var r=RefinedEquipment.recipe(stack);if(r==null)return amount;
        var state=MINING.get();double chance=switch(r.theme()){
            case "干将剑"->.10;
            case "开天斧"->state==null?0:.15;
            case "玄铁斧"->state!=null&&state.is(net.minecraftforge.common.Tags.Blocks.ORES)?.10:0;
            default->0;
        }*scale(stack);
        int saved=0;for(int i=0;i<amount;i++)if(entity.getRandom().nextDouble()<Math.min(.8,chance))saved++;
        return amount-saved;
    }
    public static double controlResistance(Player p){var r=active(p);return (r!=null&&r.theme().equals("天罡枪")?.03*scale(p.getMainHandItem()):0)+(p instanceof net.minecraft.server.level.ServerPlayer server?WeaponConditions.controlBonus(server):0);}
    /** Bonus is consumed only by shield capacity, never allowed to spill into health. */
    public static double shieldBonus(Player p){var r=active(p);return r!=null&&r.theme().equals("太阿剑")?.05*scale(p.getMainHandItem()):0;}
    public static double threatMultiplier(Player p){var r=active(p);return r!=null&&r.theme().equals("仙灵枪")?1-.10*scale(p.getMainHandItem()):1;}
    public static boolean weapon(RefiningRecipes.Recipe r){return r!=null&&(r.kind().equals("sword")||r.kind().equals("axe")||r.kind().equals("spear")||r.kind().equals("staff"));}
    public static RefiningRecipes.Recipe active(Player p){var s=p.getMainHandItem();var r=RefinedEquipment.recipe(s);return weapon(r)&&RefinedEquipment.usable(p,s)?r:null;}
    public static double scale(ItemStack s){var r=RefinedEquipment.recipe(s);if(r==null)return 0;int q=Math.max(1,Math.min(5,RefinedEquipment.quality(s)));return (1+.08*(r.tier()-1))*(1+.03*(q-1));}
    /** The mandatory elemental core, not the item name or client tooltip, determines the element. */
    public static Element element(ItemStack s){
        var r=RefinedEquipment.recipe(s);if(!weapon(r))return Element.PHYSICAL;
        if(r.kind().equals("staff"))return Element.PHYSICAL;
        Element found=Element.PHYSICAL;
        for(var in:r.inputs())if(in.item().equals("xiantu:demon_core")){
            Element next;try{next=Element.valueOf(in.element().toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ex){return Element.PHYSICAL;}
            if(found.elemental()&&found!=next)return Element.PHYSICAL;found=next;
        }return found;
    }
    public static boolean sideOrRear(LivingEntity target,LivingEntity attacker){
        Vec3 toward=attacker.position().subtract(target.position()).multiply(1,0,1);
        Vec3 forward=Vec3.directionFromRotation(0,target.yBodyRot);
        return toward.lengthSqr()>1e-8&&forward.dot(toward.normalize())<=.5;
    }
    /** Added to the existing physique/method multiplier, before vanilla armor and the Boss cap. */
    public static double damageBonus(Player p,LivingEntity target){
        var r=active(p);if(r==null)return 0;Element e=element(p.getMainHandItem());
        var targetInfo=GongfaEvents.target(target);
        double base=switch(r.theme()){
            case "青霜剑"->e==Element.WATER?.03:0;
            case "紫电剑"->e==Element.THUNDER?.03:0;
            case "赤霄剑"->e==Element.FIRE?.03:0;
            case "承影剑"->sideOrRear(target,p)?.03:0;
            case "龙渊剑"->!(target instanceof Player)&&(targetInfo.elite()||targetInfo.boss())?.02:0;
            case "碎星斧","弑神枪"->!(target instanceof Player)&&targetInfo.elite()?.03:0;
            case "断岳斧"->target.getArmorValue()>p.getArmorValue()?.03:0;
            case "破军斧"->target.isBlocking()?.03:0;
            case "破军枪"->p.isSprinting()?.03:0;
            case "玄龙枪"->target.getBbWidth()>=1.4||target.getBbHeight()>=2.5?.03:0;
            default->0;
        };return base*scale(p.getMainHandItem())+(p instanceof net.minecraft.server.level.ServerPlayer server?WeaponConditions.damageBonus(server,target):0);
    }
    public static void attributes(ItemAttributeModifierEvent e,RefiningRecipes.Recipe r){
        if(!weapon(r))return;double k=scale(e.getItemStack());
        switch(r.theme()){
            case "青虹剑"->add(e,Attributes.MOVEMENT_SPEED,"move",.03*k,AttributeModifier.Operation.MULTIPLY_TOTAL);
            case "裂地斧"->add(e,Attributes.ATTACK_KNOCKBACK,"knockback",.05*k,AttributeModifier.Operation.MULTIPLY_TOTAL);
            case "镇岳斧"->add(e,Attributes.KNOCKBACK_RESISTANCE,"steadfast",.05*k,AttributeModifier.Operation.ADDITION);
            case "巨灵斧"->add(e,Attributes.ATTACK_SPEED,"giant_speed",3.1*.05*k,AttributeModifier.Operation.ADDITION);
            case "灭道枪"->{for(var attr:ElementalAttributes.PENETRATION.values())add(e,attr.get(),"penetration",.03*k,AttributeModifier.Operation.ADDITION);}
            case "神道枪"->add(e,ElementalAttributes.RESISTANCE.get(Element.METAL).get(),"metal_resist",.03*k,AttributeModifier.Operation.ADDITION);
            default->{}
        }
    }
    private static void add(ItemAttributeModifierEvent e,Attribute a,String id,double amount,AttributeModifier.Operation op){
        e.addModifier(a,new AttributeModifier(RefinedEquipment.modifierId("passive_"+id,e.getSlotType()),"武器被动",amount,op));
    }
    @SubscribeEvent public static void mining(PlayerEvent.BreakSpeed e){var r=active(e.getEntity());if(r!=null&&r.theme().equals("开山斧")&&e.getEntity().getMainHandItem().isCorrectToolForDrops(e.getState()))e.setNewSpeed((float)(e.getNewSpeed()*(1+.05*scale(e.getEntity().getMainHandItem()))));}
    @SubscribeEvent public static void critical(CriticalHitEvent e){
        var p=e.getEntity();if(p.level().isClientSide||e.isVanillaCritical()||e.getResult()==net.minecraftforge.eventbus.api.Event.Result.DENY||p.getAttackStrengthScale(.5f)<.9||!(e.getTarget() instanceof LivingEntity))return;
        var r=active(p);if(r!=null&&r.theme().equals("含光剑")&&p.getRandom().nextDouble()<.02*scale(p.getMainHandItem())){e.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);e.setDamageModifier(1.5f);}
    }
    @SubscribeEvent public static void defense(LivingHurtEvent e){
        if(e.getEntity().level().isClientSide||!(e.getEntity() instanceof Player p)||e.getAmount()<=0)return;
        var r=active(p);if(r!=null&&r.theme().equals("宵练剑")&&e.getSource().getEntity() instanceof com.longju.xiantu.entity.InnerDemonEntity&&!e.getSource().is(DamageTypes.GENERIC_KILL))e.setAmount((float)(e.getAmount()*(1-.05*scale(p.getMainHandItem()))));
    }
    public static String description(ItemStack s){
        var r=RefinedEquipment.recipe(s);if(!weapon(r))return "";double k=scale(s);
        String text=switch(r.theme()){
            case "青霜剑"->"水属性普攻伤害 +%s%%";case "紫电剑"->"雷属性普攻伤害 +%s%%";case "赤霄剑"->"火属性普攻伤害 +%s%%";
            case "承影剑"->"背侧普攻伤害 +%s%%";case "龙渊剑"->"对精英与首领普攻伤害 +%s%%";
            case "碎星斧","弑神枪"->"对精英普攻伤害 +%s%%";case "断岳斧"->"对护甲高于自身目标普攻伤害 +%s%%";
            case "破军斧"->"对正在持盾目标普攻伤害 +%s%%";case "破军枪"->"冲刺普攻伤害 +%s%%";
            case "玄龙枪"->"对大型目标普攻伤害 +%s%%";case "青虹剑"->"移动速度 +%s%%";
            case "裂地斧"->"已有击退强度 +%s%%";case "镇岳斧"->"击退抗性 +%s%%";
            case "巨灵斧"->"攻击速度惩罚减轻 %s%%";case "灭道枪"->"属性穿透 +%s%%";
            case "神道枪"->"金属性抗性 +%s%%";case "开山斧"->"有效采掘速度 +%s%%";
            case "含光剑"->"满蓄力普攻额外暴击率 +%s%%";case "宵练剑"->"心魔幻影伤害减免 %s%%";
            case "混沌斧"->"耐久上限 +%s%%";case "干将剑"->"耐久消耗降低 %s%%";case "开天斧"->"采掘耐久消耗降低 %s%%";case "玄铁斧"->"采矿耐久消耗降低 %s%%";case "天罡枪"->"控制抗性 +%s个百分点";case "太阿剑"->"护盾伤害 +%s%%";default->"";
        };
        double base=switch(r.theme()){case "龙渊剑","含光剑"->2;case "开山斧","宵练剑","裂地斧","镇岳斧","巨灵斧"->5;case "混沌斧","干将剑","玄铁斧"->10;case "开天斧"->15;case "太阿剑"->5;default->3;};
        return text.isEmpty()?"":String.format(Locale.ROOT,text,String.format(Locale.ROOT,"%.2f",base*k));
    }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent e){
        var s=e.getItemStack();String line=description(s);if(!line.isEmpty())e.getToolTip().add(Component.literal("主手被动："+line).withStyle(ChatFormatting.GREEN));
        var r=RefinedEquipment.recipe(s);if(!weapon(r))return;if(r.id().equals("jingu_staff")){
            e.getToolTip().add(Component.literal("三击定海：连续三次命中同一目标施加减速").withStyle(ChatFormatting.GREEN));
            e.getToolTip().add(Component.literal("右键：如意伸缩突刺；潜行右键：金箍镇岳").withStyle(ChatFormatting.AQUA));
            e.getToolTip().add(Component.literal("消耗灵力上限18%/40%（封顶5000/12000）；冷却25/90秒").withStyle(ChatFormatting.GRAY));
            return;
        }
        WeaponActives.tooltip(e.getToolTip(),s);
        var setting=WeaponSkillSettings.get(r.theme());if(setting==null)return;
        e.getToolTip().add(Component.literal("器技："+setting.name()).withStyle(ChatFormatting.AQUA));
        if(r.tier()>=5){
            e.getToolTip().add(Component.literal(setting.condition()).withStyle(ChatFormatting.GREEN));
            e.getToolTip().add(Component.literal(setting.activation()).withStyle(ChatFormatting.GRAY));
        }
        else e.getToolTip().add(Component.literal(setting.maxTier()<5?"此武器为基础被动型":"五阶成品解锁高阶条件；品质不能替代阶位").withStyle(ChatFormatting.GRAY));
    }
    private WeaponPassives(){}
}
