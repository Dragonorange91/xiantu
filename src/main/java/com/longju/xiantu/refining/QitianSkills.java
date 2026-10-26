package com.longju.xiantu.refining;

import com.longju.xiantu.combat.*;
import com.longju.xiantu.cultivation.CultivationEvents;
import com.longju.xiantu.gongfa.*;
import com.longju.xiantu.network.*;
import com.longju.xiantu.spell.SpellService;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;

/** Code-only special equipment. Cooldowns survive logout/death; presentation is handed off separately. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class QitianSkills {
    public static final String KEY="xiantu_qitian_skills";
    public static final String[] IDS={"cloud_step","great_sage_form"};
    public static final String[] NAMES={"筋斗步","大圣法相"};
    private static final int[] COSTS={4000,20000},COOLDOWNS={900,3600};
    private record Pending(String skill,UUID transaction,ItemStack stack,Vec3 start,Vec3 direction,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,long finish){}
    private static final Map<UUID,Pending> PENDING=new HashMap<>();
    private record Swing(UUID target,long tick){}
    private static final Map<UUID,Swing> SWINGS=new HashMap<>();
    public static final List<String> DISGUISES=com.longju.xiantu.entity.BeastResourceRules.SPECIES.stream().sorted().toList();
    public static CompoundTag state(Player p){var n=p.getPersistentData();if(!n.contains(KEY))n.put(KEY,new CompoundTag());return n.getCompound(KEY);}
    public static boolean casting(ServerPlayer p){return PENDING.containsKey(p.getUUID());}
    private static long now(){return SpellService.clock();}
    public static int cost(ServerPlayer p,int skill){return Math.max(1,Math.min(COSTS[skill],(int)Math.ceil(GongfaService.data(p).powerMax()*new double[]{.40,.80}[skill])));}
    public static String disguiseSpecies(Player p){return pieces(p)>=3&&state(p).getLong("disguise_until")>now()?state(p).getString("Disguise"):"";}
    public static void syncAppearance(ServerPlayer p){XiantuNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p),new QitianAppearancePacket(p.getUUID(),p.level().dimension().location(),disguiseSpecies(p)));}
    public static boolean piece(Player p,EquipmentSlot slot){
        var s=p.getItemBySlot(slot);var r=RefinedEquipment.recipe(s);
        return r!=null&&r.theme().equals("齐天")&&!r.kind().equals("staff")&&s.getCount()==1&&RefinedEquipment.usable(p,s)&&switch(slot){case HEAD->r.id().equals("fengchi_crown");case CHEST->r.id().equals("suozi_chestplate");case LEGS->r.id().equals("liujin_leggings");case FEET->r.id().equals("ousi_boots");default->false;};
    }
    public static int pieces(Player p){int count=0;for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))if(piece(p,slot))count++;return count;}
    public static boolean staff(Player p){var s=p.getMainHandItem();var r=RefinedEquipment.recipe(s);return r!=null&&r.id().equals("jingu_staff")&&RefinedEquipment.usable(p,s)&&s.getCount()==1;}
    public static boolean form(Player p){return pieces(p)==4&&state(p).getLong("form_until")>now();}
    public static double pieceScale(Player p,EquipmentSlot slot){return piece(p,slot)?QitianQuality.scale(RefinedEquipment.quality(p.getItemBySlot(slot))):1;}
    public static double setScale(Player p){double k=2;for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){if(!piece(p,slot))return 1;k=Math.min(k,pieceScale(p,slot));}return k;}
    public static double twoPieceScale(Player p){return java.util.stream.Stream.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET).filter(slot->piece(p,slot)).mapToDouble(slot->pieceScale(p,slot)).min().orElse(1);}
    public static double meleeBonus(Player p){return ((pieces(p)==4?.30:0)+(form(p)?.20:0))*setScale(p);}
    public static double damageBonus(Player p){return form(p)?.20*setScale(p):0;}
    public static double healthBonus(Player p){return pieces(p)==4?.35*setScale(p):0;}
    public static double speedBonus(Player p){return (piece(p,EquipmentSlot.FEET)?.25*pieceScale(p,EquipmentSlot.FEET):0)-(state(p).getLong("fatigue_until")>now()?.10:0);}
    public static double slowReduction(Player p){return piece(p,EquipmentSlot.LEGS)?.20*pieceScale(p,EquipmentSlot.LEGS):0;}
    public static boolean steadfast(Player p){return piece(p,EquipmentSlot.CHEST)&&state(p).getLong("steadfast_until")>now();}
    public static String denial(ServerPlayer p,int skill){
        if(com.longju.xiantu.talisman.TalismanEffects.locked(p))return "灵力被封锁";
        if(skill<0||skill>=IDS.length||!p.isAlive()||p.isSpectator()||com.longju.xiantu.cultivation.InnerDemonService.active(p))return "当前不能发动装备技能";
        if(skill==0&&!piece(p,EquipmentSlot.FEET)||skill==1&&pieces(p)!=4)return "需要对应的齐天装备及使用境界";
        if(skill==1&&(form(p)||state(p).getLong("fatigue_until")>now()))return "法相生效或疲劳中";
        if(state(p).getLong("cooldown_"+IDS[skill])>now())return "装备技能冷却中";
        if(GongfaService.data(p).power()<cost(p,skill))return "灵力不足：需要 "+cost(p,skill);return "";
    }
    private static void event(ServerPlayer p,Pending cast,EquipmentSkillEvent.Phase phase,LivingEntity target){net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new EquipmentSkillEvent(p,cast.skill,cast.transaction,target==null?null:target.getUUID(),phase,target==null?(cast.skill.startsWith("staff_")?cast.start:p.position()):target.position(),cast.direction));}
    private static void pay(ServerPlayer p,String id,int cost,int cooldown){com.longju.xiantu.talisman.TalismanEffects.breakInvisibility(p);var d=GongfaService.data(p);d.setPower(d.power()-cost);state(p).putLong("cooldown_"+id,now()+cooldown);GongfaService.markCombat(p);state(p).remove("disguise_until");CultivationEvents.sync(p);}
    public static String activate(ServerPlayer p){
        if(com.longju.xiantu.talisman.TalismanEffects.casting(p)||p.containerMenu!=p.inventoryMenu||PENDING.containsKey(p.getUUID())||SpellService.casting(p)||p.isUsingItem())return "请结束当前动作并关闭界面";
        int skill=Math.max(0,Math.min(IDS.length-1,state(p).getInt("Selected")));String error=denial(p,skill);if(!error.isEmpty())return error;
        if(skill==0&&QitianMovement.destination(p,QitianMovement.direction(p)).distanceToSqr(p.position())<4)return "前方没有至少两格的安全路径";
        var cast=new Pending(IDS[skill],UUID.randomUUID(),ItemStack.EMPTY,p.position(),QitianMovement.direction(p),p.level().dimension(),now()+(skill==0?10:0));
        if(skill==0){PENDING.put(p.getUUID(),cast);event(p,cast,EquipmentSkillEvent.Phase.START,null);}else release(p,cast);sync(p);return "";
    }
    public static int staffCost(ServerPlayer p,boolean quake){
        return Math.max(1,Math.min(quake?12000:5000,(int)Math.ceil(GongfaService.data(p).powerMax()*(quake?.40:.18))));
    }
    public static InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level l,Player player,InteractionHand hand){
        var item=player.getItemInHand(hand);var recipe=RefinedEquipment.recipe(item);
        if(hand!=InteractionHand.MAIN_HAND||recipe==null||!recipe.id().equals("jingu_staff"))return InteractionResultHolder.pass(item);
        if(player instanceof ServerPlayer p){
            boolean quake=p.isShiftKeyDown();String id=quake?"staff_quake":"staff_extend";String error="";
            if(com.longju.xiantu.talisman.TalismanEffects.locked(p))error="灵力被封锁";
            else if(!staff(p))error="修为未达到金箍棒的使用要求";
            else if(!p.isAlive()||p.isSpectator()||com.longju.xiantu.talisman.TalismanEffects.casting(p)||PENDING.containsKey(p.getUUID())||SpellService.casting(p)||p.isUsingItem()||p.containerMenu!=p.inventoryMenu||com.longju.xiantu.cultivation.InnerDemonService.active(p))error="请结束当前动作后再发动棍技";
            else if(state(p).getLong("cooldown_"+id)>now())error="棍技冷却中";
            else if(GongfaService.data(p).power()<staffCost(p,quake))error="灵力不足：需要 "+staffCost(p,quake);
            if(!error.isEmpty()){p.displayClientMessage(net.minecraft.network.chat.Component.literal(error),true);return InteractionResultHolder.fail(item);}
            var direction=p.getLookAngle();var side=new Vec3(-direction.z,0,direction.x).normalize().scale(p.getMainArm()==HumanoidArm.RIGHT?.28:-.28);var origin=p.getEyePosition().add(side).add(0,-.38,0);
            // For the thrust, direction carries the immutable, block-clipped displacement.
            if(!quake)direction=SpellService.clip(p.serverLevel(),p,origin,origin.add(direction.scale(12))).subtract(origin);
            var c=new Pending(id,UUID.randomUUID(),item,origin,direction,l.dimension(),now()+(quake?7:4));
            pay(p,id,staffCost(p,quake),quake?1800:500);PENDING.put(p.getUUID(),c);
            event(p,c,EquipmentSkillEvent.Phase.RELEASE,null);sync(p);
        }
        return InteractionResultHolder.consume(item);
    }
    /** Immediate-click staff skills never depend on releasing the use key. */
    public static void releaseStaff(ItemStack item,LivingEntity user,int remaining){}
    private static boolean valid(ServerPlayer p,Pending c){
        boolean staff=c.skill.startsWith("staff_");
        return p.isAlive()&&!p.isSpectator()&&p.level().dimension().equals(c.dimension)&&p.containerMenu==p.inventoryMenu
            &&(staff?staff(p)&&p.getMainHandItem()==c.stack&&p.getEyePosition().distanceToSqr(c.start)<16:p.hurtTime==0&&p.position().distanceToSqr(c.start)<=.04)
            &&!com.longju.xiantu.cultivation.InnerDemonService.active(p);
    }
    private static void release(ServerPlayer p,Pending c){
        if(!valid(p,c)){event(p,c,EquipmentSkillEvent.Phase.CANCEL,null);return;}
        int skill=Arrays.asList(IDS).indexOf(c.skill);
        if(skill>=0){
            String error=denial(p,skill);var destination=skill==0?QitianMovement.destination(p,c.direction):p.position();
            if(!error.isEmpty()||skill==0&&destination.distanceToSqr(p.position())<4){event(p,c,EquipmentSkillEvent.Phase.CANCEL,null);return;}
            pay(p,c.skill,cost(p,skill),COOLDOWNS[skill]);
            if(skill==0){p.teleportTo(p.serverLevel(),destination.x,destination.y,destination.z,p.getYRot(),p.getXRot());p.fallDistance=0;}
            if(skill==1){state(p).putLong("form_until",now()+400);state(p).putUUID("form_transaction",c.transaction);}
            event(p,c,EquipmentSkillEvent.Phase.RELEASE,null);return;
        }
        var from=c.start;var direction=c.direction.normalize();
        var end=SpellService.clip(p.serverLevel(),p,from,from.add(c.direction));
        var center=from.add(0,.38-p.getEyeHeight(),0).add(direction.multiply(1,0,1).normalize().scale(3));
        var bounds=c.skill.equals("staff_extend")?new AABB(from,end).inflate(.45):new AABB(center,center).inflate(4);
        p.level().playSound(null,net.minecraft.core.BlockPos.containing(c.skill.equals("staff_extend")?end:center),
            c.skill.equals("staff_extend")?net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG:net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
            net.minecraft.sounds.SoundSource.PLAYERS,.85f,c.skill.equals("staff_extend")?.65f:1.25f);
        for(var target:p.serverLevel().getEntitiesOfClass(LivingEntity.class,bounds,t->SpellService.enemy(p,t)).stream().limit(32).toList()){
            if(!p.hasLineOfSight(target)||c.skill.equals("staff_extend")&&!target.getBoundingBox().inflate(.45).clip(from,end).isPresent()||c.skill.equals("staff_quake")&&target.distanceToSqr(center)>16)continue;
            double amount=p.getAttributeValue(Attributes.ATTACK_DAMAGE)*(c.skill.equals("staff_extend")?1:1.4)*Math.max(.1,Math.min(form(p)?2.8:2.2,1+GongfaService.attackBonus(GongfaService.data(p))+EquipmentEffects.attackBonus(p)+damageBonus(p)));
            var info=GongfaEvents.target(target);int realm=target instanceof Player other?other.getCapability(com.longju.xiantu.cultivation.CultivationCapability.DATA).map(d->d.majorRealm()+1).orElse(1):info.realmRank();amount*=com.longju.xiantu.spell.SpellRules.realmMultiplier(GongfaService.data(p).majorRealm()+1,realm);
            int previousImmunity=target.invulnerableTime;boolean hit;target.invulnerableTime=0;
            try{hit=target.hurt(new ElementalDamageSource(p.damageSources().mobAttack(p),Element.PHYSICAL),(float)amount);}finally{target.invulnerableTime=Math.max(previousImmunity,target.invulnerableTime);}
            if(hit){
                event(p,c,EquipmentSkillEvent.Phase.HIT,target);
                if(c.skill.equals("staff_extend")&&!info.boss())target.knockback(1.4,-direction.x,-direction.z);
                if(c.skill.equals("staff_quake")){if(info.boss()&&target instanceof com.longju.xiantu.entity.CompletionLandEntity boss){for(var anchor:p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class,target.getBoundingBox().inflate(8)))if(anchor.getPersistentData().hasUUID("BossOwner")&&anchor.getPersistentData().getUUID("BossOwner").equals(target.getUUID()))boss.interruptBoss(p,anchor);}else if(!info.boss())WeaponConditions.applySlow(target,.5,info.elite()?30:60);}
            }
        }
    }
    private static void endForm(ServerPlayer p){var s=state(p);if(!s.contains("form_until"))return;s.remove("form_until");s.putLong("fatigue_until",now()+3600);if(s.hasUUID("form_transaction"))event(p,new Pending("great_sage_form",s.getUUID("form_transaction"),ItemStack.EMPTY,p.position(),p.getLookAngle(),p.level().dimension(),now()),EquipmentSkillEvent.Phase.END,null);s.remove("form_transaction");}
    public static void cancel(ServerPlayer p){var c=PENDING.remove(p.getUUID());if(c!=null)event(p,c,EquipmentSkillEvent.Phase.CANCEL,null);}
    public static void sync(ServerPlayer p){
        var n=state(p).copy();n.putInt("Pieces",pieces(p));n.putDouble("Power",GongfaService.data(p).power());for(int i=0;i<IDS.length;i++){n.putString("Reason"+i,denial(p,i));n.putInt("Cost"+i,cost(p,i));}
        XiantuNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new EquipmentSkillStatePacket(n,now()));syncAppearance(p);
    }
    public static String disguise(ServerPlayer p,int index){
        if(index<0||index>=DISGUISES.size()||!p.isAlive()||p.isSpectator()||pieces(p)<3||com.longju.xiantu.cultivation.InnerDemonService.active(p))return "需要三件有效齐天战甲";
        var s=state(p);String species=DISGUISES.get(index);
        if(!s.getCompound("Discovered").getBoolean(species))return "尚未发现这种普通妖兽";
        if(QitianMovement.combat(p))return "脱离战斗5秒后可变化";
        if(s.getLong("cooldown_disguise")>now())return "变化冷却中：还需 "+((s.getLong("cooldown_disguise")-now()+19)/20)+"秒";
        if(PENDING.containsKey(p.getUUID())||SpellService.casting(p))return "请先结束当前施法";
        s.putString("Disguise",species);s.putLong("disguise_until",now()+2400);s.putLong("cooldown_disguise",now()+3600);
        event(p,new Pending("seventy_two_forms",UUID.randomUUID(),ItemStack.EMPTY,p.position(),p.getLookAngle(),p.level().dimension(),now()),EquipmentSkillEvent.Phase.RELEASE,null);sync(p);return "";
    }
    @SubscribeEvent public static void discover(PlayerEvent.StartTracking e){
        if(e.getEntity() instanceof ServerPlayer viewer&&e.getTarget() instanceof ServerPlayer owner)
            XiantuNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->viewer),new QitianAppearancePacket(owner.getUUID(),owner.level().dimension().location(),disguiseSpecies(owner)));
        if(e.getEntity() instanceof ServerPlayer p&&e.getTarget() instanceof LivingEntity t&&com.longju.xiantu.entity.BeastRealmService.isBeast(t)&&!GongfaEvents.target(t).boss()){
            String species=com.longju.xiantu.entity.BeastRealmService.species(t);if(!DISGUISES.contains(species))return;
            var n=state(p).getCompound("Discovered");n.putBoolean(species,true);state(p).put("Discovered",n);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;var s=state(p);long time=now();
        var c=PENDING.get(p.getUUID());if(c!=null){if(!valid(p,c))cancel(p);else if(time>=c.finish){PENDING.remove(p.getUUID());release(p,c);sync(p);}}
        if(s.contains("form_until")&&(!p.isAlive()||pieces(p)!=4||s.getLong("form_until")<=time))endForm(p);
        if(!piece(p,EquipmentSlot.HEAD))s.remove("sight_until");if(!piece(p,EquipmentSlot.CHEST))s.remove("steadfast_until");
        if(s.contains("disguise_until")&&(pieces(p)<3||QitianMovement.combat(p)||s.getLong("disguise_until")<=time)){s.remove("disguise_until");syncAppearance(p);}
        QitianShapeAbilities.tick(p,disguiseSpecies(p));
        s.remove("sight_until");s.remove("SightTargets");if(p.tickCount%20==0)sync(p);
        var reach=p.getAttribute(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());if(reach!=null){var id=UUID.fromString("862d8b99-f139-45c1-bce7-8592abfd132e");var old=reach.getModifier(id);if(form(p)&&old==null)reach.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id,"大圣法相攻击范围",1,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));if(!form(p))reach.removeModifier(id);}
    }
    @SubscribeEvent(priority=EventPriority.LOW) public static void hurt(LivingHurtEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p)||e.getAmount()<=0)return;String shape=disguiseSpecies(p);
        if(QitianShapeAbilities.FIRE.contains(shape)&&e.getSource().is(DamageTypeTags.IS_FIRE))e.setAmount(e.getAmount()*.5F);
        cancel(p);state(p).remove("disguise_until");QitianShapeAbilities.clear(p);syncAppearance(p);
        if(e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;var s=state(p);
        if(piece(p,EquipmentSlot.CHEST)&&e.getAmount()>p.getMaxHealth()*.2&&s.getLong("steadfast_ready")<=now()){s.putLong("steadfast_until",now()+20);s.putLong("steadfast_ready",now()+600);}
        if(piece(p,EquipmentSlot.HEAD)&&(e.getSource().is(DamageTypes.IN_FIRE)||e.getSource().is(DamageTypes.ON_FIRE)||e.getSource().is(DamageTypes.LAVA)||e.getSource().is(DamageTypes.HOT_FLOOR)||e.getSource().is(DamageTypes.LIGHTNING_BOLT)))e.setAmount(e.getAmount()*(float)(1-Math.min(.85,.5*pieceScale(p,EquipmentSlot.HEAD))));
        if(pieces(p)>=2&&e.getSource().getEntity() instanceof LivingEntity)e.setAmount(e.getAmount()*(float)(1-.10*twoPieceScale(p)));
    }
    @SubscribeEvent public static void attack(AttackEntityEvent e){if(e.getEntity() instanceof ServerPlayer p){state(p).remove("disguise_until");QitianShapeAbilities.clear(p);syncAppearance(p);cancel(p);if(staff(p))SWINGS.put(p.getUUID(),new Swing(e.getTarget().getUUID(),now()));}}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void staffHit(LivingDamageEvent e){
        if(e.getAmount()<=0||!(e.getSource().getEntity() instanceof ServerPlayer p)||e.getSource().getDirectEntity()!=p||!e.getSource().is(DamageTypes.PLAYER_ATTACK)||!staff(p))return;
        var swing=SWINGS.remove(p.getUUID());var t=e.getEntity();if(swing==null||swing.tick!=now()||!swing.target.equals(t.getUUID()))return;
        var s=state(p);if(!s.hasUUID("staff_target")||!s.getUUID("staff_target").equals(t.getUUID())){s.putUUID("staff_target",t.getUUID());s.putInt("staff_hits",0);}
        int count=s.getInt("staff_hits")+1;s.putInt("staff_hits",count);if(count<3)return;s.putInt("staff_hits",0);
        var timers=s.getCompound("staff_targets");timers.getAllKeys().removeIf(key->timers.getLong(key)<=now());String key=t.getUUID().toString();if(timers.getLong(key)>now()||timers.size()>=64)return;timers.putLong(key,now()+160);s.put("staff_targets",timers);WeaponConditions.applySlow(t,GongfaEvents.target(t).boss()?.15:.2,40);
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent e){if(e.getEntity() instanceof ServerPlayer p&&piece(p,EquipmentSlot.FEET))p.setDeltaMovement(p.getDeltaMovement().multiply(1,Math.sqrt(1+.2*pieceScale(p,EquipmentSlot.FEET)),1));}
    @SubscribeEvent public static void knockback(LivingKnockBackEvent e){if(e.getEntity() instanceof ServerPlayer p){if(steadfast(p))e.setCanceled(true);else if(piece(p,EquipmentSlot.LEGS))e.setStrength(e.getStrength()*(float)(1-.2*pieceScale(p,EquipmentSlot.LEGS)));}}
    @SubscribeEvent public static void fall(LivingFallEvent e){if(e.getEntity() instanceof ServerPlayer p){if(QitianShapeAbilities.FLYING.contains(disguiseSpecies(p))){e.setCanceled(true);return;}if(piece(p,EquipmentSlot.FEET))e.setDamageMultiplier(e.getDamageMultiplier()*(float)(1-Math.min(.95,.8*pieceScale(p,EquipmentSlot.FEET))));}}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){QitianShapeAbilities.clear(p);state(p).remove("disguise_until");syncAppearance(p);}}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){cancel(p);QitianShapeAbilities.clear(p);endForm(p);state(p).remove("sight_until");state(p).remove("disguise_until");syncAppearance(p);}}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){cancel(p);QitianShapeAbilities.clear(p);endForm(p);state(p).remove("sight_until");state(p).remove("disguise_until");syncAppearance(p);}}
    @SubscribeEvent public static void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p){cancel(p);QitianShapeAbilities.clear(p);endForm(p);state(p).remove("sight_until");state(p).remove("disguise_until");syncAppearance(p);}}
    public static void copy(Player a,Player b){if(a instanceof ServerPlayer original)QitianShapeAbilities.clear(original);var n=state(a).copy();n.remove("sight_until");n.remove("steadfast_until");n.remove("disguise_until");b.getPersistentData().put(KEY,n);}
    private QitianSkills(){}
}
