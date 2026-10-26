package com.longju.xiantu.alchemy;

import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.config.XiantuConfig;
import com.longju.xiantu.item.PillItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.*;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import java.util.*;

@Mod.EventBusSubscriber(modid="xiantu")
public final class MedicineService {
    private static final UUID HEALTH=UUID.fromString("5eb8a3a2-6f67-4c72-bd55-ab7834b6f44a");
    private static final UUID BONE_SPEED=UUID.fromString("701e2c58-f6d2-4993-bd46-809f23da7298");
    private MedicineService(){}
    public static CultivationData data(Player p){return p.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new);}
    public static long now(Player p){return p.getServer().overworld().getGameTime();}
    public static double effect(Player p,String key){return p.level().isClientSide?0:p.getCapability(CultivationCapability.DATA).map(c->c.medicine().effect(key,now(p))).orElse(0.0);}
    public static double doseToxicity(PillCatalog.Definition d,int quality){return (4+d.rank())*new double[]{1,.75,.5,.25,.1}[Math.max(1,Math.min(5,quality))-1]*XiantuConfig.MEDICINE_TOXICITY_MULTIPLIER.get();}
    public static String group(PillCatalog.Definition d){return switch(d.kind()){
        case "power","recovery"->"power";case "heal"->"heal";case "soul","soul_heal","soul_guard"->"soul";
        case "detox","miasma_immunity","gu_immunity","all_poison_immunity"->"detox";
        case "health","ice_skin","dragon_blood"->"body";case "true_sight","understanding"->"ability";
        default->d.kind();};}
    public static String effectText(PillCatalog.Definition d,int q){
        double v=d.value(q);String number=String.format(Locale.ROOT,"%.2f",v).replaceAll("0+$","").replaceAll("\\.$","");
        String unit=switch(d.kind()){
            case "breakthrough","auxiliary"->"成功率 +"+number+" 个百分点";
            case "power","heal","recovery"->"恢复 "+number+"%";
            case "rage","defense","health","capacity","cultivation","dragon_blood","ice_skin"->"增益 "+number+"%";
            case "true_sight","miasma_immunity","gu_immunity","soul_guard","decoy","all_poison_immunity"->"持续 "+number+" 秒";
            case "soul","soul_heal"->"降低心魔 "+number;
            case "wisdom","understanding"->"悟性 +"+number;
            case "physique"->number+" 个不同体质候选（确认才消耗）";
            case "roots"->q<4?"药力不足，无法使用":"重新随机一次灵根";
            case "cross_dimension"->"右键界标绑定；持丹右键准备8秒跨维传送";
            default->"清除 "+(v>=999?"全部":number+"种")+"指定毒素";};
        return unit+(d.duration()>0&&!d.kind().equals("cross_dimension")?" · "+d.duration()+"秒":"");
    }
    public static String denial(ServerPlayer p,ItemStack stack){
        if(stack.isEmpty()||!(stack.getItem() instanceof PillItem item))return "不是丹药";
        if(!p.isAlive()||p.isSpectator())return "当前无法服药";
        var d=item.definition();var c=data(p);var m=c.medicine();int q=PillCatalog.quality(stack);
        long cd=m.cooldown(group(d),now(p));if(cd>0)return "同组丹药冷却中："+((cd+19)/20)+"秒";
        return switch(d.kind()){
            case "breakthrough","auxiliary"->"请在突破丹界面选用，突破判定时才扣除";
            case "power"->c.power()>=c.powerMax()?"灵力已满":"";
            case "heal"->p.getHealth()>=p.getMaxHealth()?"生命已满":"";
            case "soul"->c.innerDemon()<=0?"无需降低心魔":"";
            case "soul_heal"->c.innerDemon()<=0&&!injured(p,"soul")?"没有魂伤或心魔":"";
            case "detox"->!p.hasEffect(MobEffects.POISON)&&!injured(p,"miasma")?"没有可清除的毒素":"";
            case "wisdom"->q==1?"凡品药力不足":c.comprehension()>=20?"永久悟性已达上限":"";
            case "roots"->q<4?"上品及极品洗灵丹才能生效":"";
            case "physique"->m.state().getIntArray("physique_candidates").length>0?"请先选择已有的体质候选":"";
            case "cross_dimension"->CrossDimensionMedicine.binding(p)==null?"请先右键界标，绑定另一维度的落点":"";
            default->"";};
    }
    public static boolean consume(ServerPlayer p,ItemStack stack){
        String rejection=denial(p,stack);if(!rejection.isEmpty()){p.displayClientMessage(Component.literal(rejection),true);return false;}
        var d=((PillItem)stack.getItem()).definition();
        if(d.kind().equals("physique")||d.kind().equals("cross_dimension"))return false;
        int q=PillCatalog.quality(stack);double v=d.value(q);var c=data(p);long time=now(p);
        // Completion runs on the authoritative server thread, after a second legality check.
        commitDose(p,stack,d,q);var m=c.medicine();
        switch(d.kind()){
            case "power"->c.setPower(Math.min(c.powerMax(),c.power()+c.powerMax()*v/100));
            case "heal"->p.heal((float)(p.getMaxHealth()*v/100));
            case "soul"->c.setInnerDemon(Math.max(0,c.innerDemon()-v));
            case "detox"->clearPoison(p,(int)v,false);
            case "miasma_immunity"->{m.clearEffect("injury_miasma");m.effect(d.kind(),1,time+(long)v*20);}
            case "gu_immunity"->{m.clearEffect("injury_gu");m.effect(d.kind(),1,time+(long)v*20);}
            case "all_poison_immunity"->{clearPoison(p,Integer.MAX_VALUE,true);m.effect(d.kind(),1,time+(long)v*20);}
            case "recovery"->{c.setPower(Math.min(c.powerMax(),c.power()+c.powerMax()*v/100));m.effect(d.kind(),v,time+d.duration()*20L);}
            case "soul_heal"->{m.clearEffect("injury_soul");m.effect(d.kind(),v,time+1200);m.state().putInt("soul_heal_elapsed",0);m.state().putInt("soul_heal_paid",0);}
            case "wisdom"->c.setComprehension(Math.min(20,c.comprehension()+(int)v));
            case "roots"->{if(c instanceof PlayerCultivationData mutable){mutable.resetSpiritRoots();int mask=CultivationService.rollSpiritRootMask(p.getRandom());
                for(var root:SpiritRootType.values())if((mask&(1<<root.ordinal()))!=0)mutable.addSpiritRoot(root);}}
            case "true_sight","soul_guard"->m.effect(d.kind(),1,time+(long)v*20);
            case "decoy"->{removeDecoy(p);PillDecoyEntity.spawn(p,(int)v);}
            default->m.effect(d.kind(),v,time+d.duration()*20L);
        }
        refreshCapacity(p);refreshHealth(p);CultivationEvents.sync(p);return true;
    }
    public static void commitDose(ServerPlayer p,ItemStack stack,PillCatalog.Definition d,int quality){
        stack.shrink(1);var m=data(p).medicine();m.recordDose(d.id(),d.kind().equals("wisdom"),doseToxicity(d,quality));
        m.cooldown(group(d),now(p),d.cooldown());
    }
    private static void clearPoison(ServerPlayer p,int count,boolean all){
        if(count>0&&p.hasEffect(MobEffects.POISON)){p.removeEffect(MobEffects.POISON);count--;}
        if(count>0&&injured(p,"miasma")){data(p).medicine().clearEffect("injury_miasma");count--;}
        if(all){data(p).medicine().clearEffect("injury_gu");
            for(var effect:List.copyOf(p.getActiveEffects()))if(isPoison(effect.getEffect()))p.removeEffect(effect.getEffect());}
    }
    public static void refreshHealth(ServerPlayer p){
        var a=p.getAttribute(Attributes.MAX_HEALTH);if(a==null)return;double value=effect(p,"health")/100;
        var old=a.getModifier(HEALTH);if((old==null?0:old.getAmount())!=value){a.removeModifier(HEALTH);
            if(value>0)a.addTransientModifier(new AttributeModifier(HEALTH,"xiantu pill health",value,AttributeModifier.Operation.MULTIPLY_BASE));
            if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());}
    }
    public static void refreshCapacity(ServerPlayer p){
        var c=data(p);var tag=p.getPersistentData();double previous=tag.getDouble("xiantu_pill_power_bonus");
        double base=Math.max(0,c.powerMax()-previous),raw=Math.max(0,base-tag.getDouble("xiantu_gongfa_power_max_bonus")-tag.getDouble("xiantu_physique_power_max_bonus")-tag.getDouble("xiantu_equipment_power_bonus")),next=raw*effect(p,"capacity")/100;
        if(Math.abs(previous-next)>.0001){c.setPowerMax(base+next);tag.putDouble("xiantu_pill_power_bonus",next);
            if(c.power()>c.powerMax())c.setPower(c.powerMax());}
    }
    public static double meleeBonus(Player p){return Math.max(effect(p,"rage"),effect(p,"dragon_blood"))/100-(injured(p,"bone")?.1:0);}
    public static double meditationMultiplier(Player p,boolean array){return (1+com.longju.xiantu.refining.EquipmentEffects.cultivationBonus(p))*data(p).medicine().cultivationMultiplier()*(injured(p,"soul")?.8:1)*(array?1+effect(p,"cultivation")/100:1);}
    public static double recoveryMultiplier(Player p){return 1+effect(p,"recovery")/100;}
    public static boolean injured(Player p,String key){return effect(p,"injury_"+key)>0;}
    /** Entry for explicit injury/poison skill hits; never applied to every ordinary melee hit. */
    public static boolean inflict(ServerPlayer p,String key,int ticks){
        if((key.equals("bone")&&effect(p,"dragon_blood")>0)||(key.equals("soul")&&effect(p,"soul_guard")>0)
            ||(key.equals("miasma")&&(effect(p,"miasma_immunity")>0||effect(p,"all_poison_immunity")>0))
            ||(key.equals("gu")&&(effect(p,"gu_immunity")>0||effect(p,"all_poison_immunity")>0)))return false;
        if(!Set.of("bone","soul","miasma","gu","meridian","bleed").contains(key))return false;
        data(p).medicine().effect("injury_"+key,1,now(p)+Math.max(1,ticks));return true;
    }
    /** Only narrative/event gains use this; failure and villager penalties retain their full values. */
    public static void eventInnerDemon(ServerPlayer p,double gain){PhysiqueService.addInnerDemon(data(p),effect(p,"soul_guard")>0?Math.ceil(gain/2):gain);}
    public static boolean detoxing(Player p){return !p.level().isClientSide&&data(p).medicine().state().getBoolean("detox_active");}
    public static boolean meditationPaused(Player p){return !p.level().isClientSide&&data(p).medicine().state().contains("seat_mode");}
    public static boolean toggleDetox(ServerPlayer p){
        var m=data(p).medicine();if(meditationPaused(p)){m.state().remove("detox_active");m.state().remove("seat_mode");p.sendSystemMessage(Component.literal("已切回普通修炼"));return true;}
        if(!(p.getVehicle() instanceof com.longju.xiantu.formation.FormationSeatEntity)||m.toxicity()<=0||m.soberTicks()<2400
                ||now(p)-m.state().getLong("last_combat")<200||InnerDemonService.active(p)||ThunderTribulationService.active(p)
                ||BreakthroughService.cooldownTicks(p)>0||com.longju.xiantu.gongfa.GongfaService.busy(p)||CrossDimensionMedicine.active(p)){
            p.sendSystemMessage(Component.literal("排毒需在蒲团上停药120秒，远离战斗10秒，且无突破或传送"));return false;}
        m.state().putBoolean("detox_active",true);m.state().putString("seat_mode","detox");m.state().putInt("detox_ticks",0);p.sendSystemMessage(Component.literal("正在调息排毒：每20秒清除1点，不消耗修炼燃料"));return true;
    }
    public static void tick(Player player){
        if(!(player instanceof ServerPlayer p)||!p.isAlive()||!p.getCapability(CultivationCapability.DATA).isPresent())return;var c=data(p);var m=c.medicine();long time=now(p);
        if(!(p.getVehicle() instanceof com.longju.xiantu.formation.FormationSeatEntity)){m.state().remove("detox_active");m.state().remove("seat_mode");}
        if(detoxing(p)&&(!p.isAlive()||com.longju.xiantu.gongfa.GongfaService.busy(p)||InnerDemonService.active(p)||ThunderTribulationService.active(p))){m.state().remove("detox_active");m.state().putString("seat_mode","idle");}
        m.onlineTick(p.isAlive()&&!p.isCreative()&&!p.isSpectator(),detoxing(p));
        if(detoxing(p)&&m.toxicity()<=0){m.state().remove("detox_active");m.state().putString("seat_mode","idle");p.sendSystemMessage(Component.literal("丹毒已清，已停止调息；点击切回普通修炼才会继续修炼"));}
        tickTransit(p);
        if(p.tickCount%20!=0)return;
        refreshHealth(p);refreshCapacity(p);
        var speed=p.getAttribute(Attributes.MOVEMENT_SPEED);if(speed!=null){boolean bone=injured(p,"bone");
            if(bone&&speed.getModifier(BONE_SPEED)==null)speed.addTransientModifier(new AttributeModifier(BONE_SPEED,"bone injury",-.15,AttributeModifier.Operation.MULTIPLY_TOTAL));
            if(!bone)speed.removeModifier(BONE_SPEED);}
        var heal=m.state().getCompound("effect_soul_heal");
        if(heal.getLong("until")>=time&&heal.getLong("until")>0){int elapsed=Math.min(1200,m.state().getInt("soul_heal_elapsed")+20);
            int due=(int)Math.floor(heal.getDouble("value")*elapsed/1200.0),paid=m.state().getInt("soul_heal_paid");
            if(due>paid){c.setInnerDemon(Math.max(0,c.innerDemon()-(due-paid)));m.state().putInt("soul_heal_paid",due);}
            m.state().putInt("soul_heal_elapsed",elapsed);if(elapsed>=1200)m.clearEffect("soul_heal");
        }else if(heal.getLong("until")>0)m.clearEffect("soul_heal");
        if(effect(p,"all_poison_immunity")>0)clearPoison(p,Integer.MAX_VALUE,true);
        if(injured(p,"miasma")&&p.tickCount%60==0&&p.getHealth()>1)p.hurt(p.damageSources().magic(),Math.min(1,p.getHealth()-1));
        if(injured(p,"gu")&&p.tickCount%100==0)c.setPower(Math.max(0,c.power()-c.powerMax()*.01));
        if(injured(p,"bleed")&&p.tickCount%40==0)p.hurt(p.damageSources().generic(),(float)Math.max(1,Math.min(4,p.getMaxHealth()*.005)));
    }
    private static void combat(ServerPlayer p){var s=data(p).medicine().state();s.putLong("last_combat",now(p));if(s.contains("seat_mode"))s.putString("seat_mode","idle");s.remove("detox_active");s.remove("detox_ticks");CrossDimensionMedicine.cancel(p);}
    @SubscribeEvent public static void attack(AttackEntityEvent e){if(e.getEntity() instanceof ServerPlayer p)combat(p);}
    @SubscribeEvent public static void use(PlayerInteractEvent.RightClickItem e){if(e.getEntity() instanceof ServerPlayer p&&e.getItemStack().getItem() instanceof com.longju.xiantu.item.ArtifactItem)combat(p);}
    @SubscribeEvent public static void damage(LivingDamageEvent e){if(e.getAmount()>0&&e.getEntity() instanceof ServerPlayer p)combat(p);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void resistance(LivingHurtEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p)||e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        double reduction=Math.max(Math.max(effect(p,"defense")/100,com.longju.xiantu.spell.SpellService.defense(p)),com.longju.xiantu.talisman.TalismanEffects.defense(p));
        if(e.getSource().is(DamageTypeTags.IS_FIRE)&&effect(p,"ice_skin")>0){
            boolean environment=e.getSource().is(DamageTypes.IN_FIRE)||e.getSource().is(DamageTypes.ON_FIRE)||e.getSource().is(DamageTypes.LAVA)||e.getSource().is(DamageTypes.HOT_FLOOR);
            reduction=Math.max(reduction,environment?.5:effect(p,"ice_skin")/100);}
        e.setAmount((float)(e.getAmount()*(1-reduction)));
    }
    private static boolean isPoison(net.minecraft.world.effect.MobEffect e){return e==MobEffects.POISON||net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.tags().getTag(net.minecraft.tags.TagKey.create(Registries.MOB_EFFECT,new ResourceLocation("xiantu","poison"))).contains(e);}
    @SubscribeEvent public static void poison(MobEffectEvent.Applicable e){if(e.getEntity() instanceof ServerPlayer p&&effect(p,"all_poison_immunity")>0&&isPoison(e.getEffectInstance().getEffect()))e.setResult(Event.Result.DENY);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){CrossDimensionMedicine.cancel(p);removeDecoy(p);p.getCapability(CultivationCapability.DATA).ifPresent(c->c.medicine().state().remove("detox_active"));}}
    @SubscribeEvent public static void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p){CrossDimensionMedicine.cancel(p);removeDecoy(p);data(p).medicine().clearTemporary();}}
    public static void removeDecoy(ServerPlayer p){for(ServerLevel level:p.getServer().getAllLevels())for(var e:level.getAllEntities())if(e instanceof PillDecoyEntity d&&p.getUUID().equals(d.ownerId()))d.discard();}
    public static Component status(MedicineData m){return Component.literal(String.format(Locale.ROOT,"丹毒 %.2f / 100 · 打坐修为 ×%.2f；停药120秒后自然代谢，蒲团调息更快",m.toxicity(),m.cultivationMultiplier()));}
    public static ServerLevel destination(ServerPlayer p){return CrossDimensionMedicine.destination(p);}
    public static void beginTeleport(ServerPlayer p,InteractionHand hand){CrossDimensionMedicine.begin(p,hand);}
    private static void tickTransit(ServerPlayer p){CrossDimensionMedicine.tick(p);}
    @SubscribeEvent public static void fall(LivingFallEvent e){if(e.getEntity() instanceof ServerPlayer p&&effect(p,"landing")>0)e.setDistance(0);}
}
