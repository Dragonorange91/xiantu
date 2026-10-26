package com.longju.xiantu.qa;

import com.longju.xiantu.refining.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.equipment.*;
import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.spell.*;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Native effect application, real hit callbacks and atomic equipment skills, with full fixture restoration. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class EquipmentConditionsAudit {
    private static int checks;
    private static void check(boolean value,String text){checks++;if(!value)throw new IllegalStateException(text);}
    private static void near(double a,double b,String text){check(Math.abs(a-b)<.01,text+" actual="+a+" expected="+b);}
    private static ItemStack item(String theme,int tier){var r=RefiningRecipes.ALL.stream().filter(x->x.theme().equals(theme)&&x.tier()==tier).findFirst().orElseThrow(()->new IllegalArgumentException("No enabled recipe: "+theme+" tier "+tier));return RefinedEquipment.finish(r,1);}
    private static void accessories(ServerPlayer p,ItemStack ring,ItemStack necklace){var inv=new AccessoryInventory(p);((Container)inv).setItem(0,ring);((Container)inv).setItem(1,ItemStack.EMPTY);((Container)inv).setItem(2,necklace);((Container)inv).setChanged();}
    private static void armor(ServerPlayer p,boolean top){for(var id:List.of("fengchi_crown","suozi_chestplate","liujin_leggings","ousi_boots")){var s=RefinedEquipment.finish(RefiningRecipes.definition(id),top?5:4);p.setItemSlot(((ArmorItem)s.getItem()).getEquipmentSlot(),s);}}
    private static SpellLifecycleEvent hit(ServerPlayer p,UUID cast){return new SpellLifecycleEvent(p,"fire_bolt",cast,UUID.randomUUID(),SpellLifecycleEvent.Phase.HIT,p.position(),p.getLookAngle());}
    @SubscribeEvent public static void register(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("xiantuequipmentconditionsaudit").executes(c->{try{return run(c);}catch(Exception|LinkageError failure){com.mojang.logging.LogUtils.getLogger().error("Equipment conditions setup failed",failure);c.getSource().sendFailure(Component.literal("[Equipment conditions QA] SETUP FAIL "+failure));return 0;}}));}
    private static int run(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var p=c.getSource().getPlayerOrException();if(!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        p.closeContainer();checks=0;var d=(PlayerCultivationData)GongfaService.data(p);var data=new CompoundTag();d.save(data);var persistent=p.getPersistentData().copy();var inventory=p.getInventory().save(new ListTag());var mode=p.gameMode.getGameModeForPlayer();float health=p.getHealth(),absorption=p.getAbsorptionAmount();var position=p.position();float yaw=p.getYRot(),pitch=p.getXRot();var effects=p.getActiveEffects().stream().map(MobEffectInstance::new).toList();var food=new CompoundTag();p.getFoodData().addAdditionalSaveData(food);boolean oldSneaking=p.isShiftKeyDown();boolean natural=p.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
        net.minecraft.world.entity.monster.Zombie target=null;
        try{
            p.setGameMode(GameType.SURVIVAL);d.setCultivationUnlocked(true);d.setRealmStage(8,0);d.setPhysique(PhysiqueType.ORDINARY);d.gongfa().clear();d.medicine().clearTemporary();d.setPowerMax(100000);d.setPower(50000);p.getInventory().clearContent();accessories(p,ItemStack.EMPTY,ItemStack.EMPTY);p.removeAllEffects();p.setAbsorptionAmount(0);p.hurtTime=0;
            for(var key:new ArrayList<>(p.getPersistentData().getAllKeys()))if(key.startsWith("xiantu_gear_")||key.equals("xiantu_accessory_conditions")||key.equals(QitianSkills.KEY))p.getPersistentData().remove(key);
            armor(p,true);check(QitianSkills.slowReduction(p)>0,"real exquisite leggings activate duration resistance");p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,0,false,false));
            int reduced=(int)Math.ceil(100*(1-EquipmentConditions.controlReduction(p)));check(p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&p.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration()==reduced,"native negative effect is shortened on application");
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))p.setItemSlot(slot,ItemStack.EMPTY);
            p.removeAllEffects();accessories(p,item("清心戒",1),ItemStack.EMPTY);p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100));check(p.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration()==(int)Math.ceil(100*(1-EquipmentEffects.controlResistance(p))),"low ring applies only its basic control resistance");p.removeAllEffects();
            accessories(p,item("道枢戒",7),ItemStack.EMPTY);var spell=SpellCatalog.find("fire_bolt");var cast=UUID.randomUUID();EquipmentConditions.spell(hit(p,cast));EquipmentConditions.spell(hit(p,cast));EquipmentConditions.spell(hit(p,UUID.randomUUID()));EquipmentConditions.spell(hit(p,cast));near(EquipmentConditions.costReduction(p,spell),0,"several hits and interleaved duplicate casts do not build three charges");EquipmentConditions.spell(hit(p,UUID.randomUUID()));double discount=EquipmentConditions.costReduction(p,spell);check(discount>.15,"three distinct hit casts arm the next spell discount");long quote=SpellService.cost(p,spell);for(int i=0;i<10;i++)near(SpellService.cost(p,spell),quote,"repeated quote does not consume a prepared charge");EquipmentConditions.paid(p,spell);near(EquipmentConditions.costReduction(p,spell),0,"only actual payment consumes the discount");for(int i=0;i<3;i++)EquipmentConditions.spell(hit(p,UUID.randomUUID()));near(EquipmentConditions.costReduction(p,spell),0,"45 second cooldown rejects an immediate second charge");
            accessories(p,item("聚灵戒",3),ItemStack.EMPTY);p.getPersistentData().putLong("xiantu_equipment_last_combat",-1000);EquipmentConditions.tick(p);near(EquipmentConditions.recoveryBonus(p),0,"legal low tier gathering ring never gains high tier recovery burst");accessories(p,ItemStack.EMPTY,ItemStack.EMPTY);EquipmentConditions.tick(p);near(EquipmentConditions.recoveryBonus(p),0,"unequipping clears temporary gathering bonus");
            accessories(p,ItemStack.EMPTY,item("不朽链",7));near(EquipmentConditions.penetration(p,com.longju.xiantu.combat.Element.WATER,.5,.6),.03,"immortal necklace limits lost reduction to three points");accessories(p,ItemStack.EMPTY,ItemStack.EMPTY);near(EquipmentConditions.penetration(p,com.longju.xiantu.combat.Element.WATER,.5,.6),.6,"unequipping revokes penetration protection");
            accessories(p,ItemStack.EMPTY,item("长生链",5));p.setHealth(10);p.getFoodData().setFoodLevel(18);p.getFoodData().setSaturation(0);p.level().getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(true,p.server);for(int i=0;i<80;i++)p.getFoodData().tick(p);near(p.getHealth()-10,1+EquipmentConditions.naturalHealingBonus(p),"real FoodData natural regeneration uses its own source-specific multiplier");accessories(p,ItemStack.EMPTY,ItemStack.EMPTY);
            target=EntityType.ZOMBIE.create(p.serverLevel());target.setNoAi(true);((Entity)target).moveTo(p.getX()+2,p.getY(),p.getZ(),0,0);p.serverLevel().addFreshEntity(target);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);target.setHealth(10000);
            var weapon=item("含光剑",5);p.setItemSlot(EquipmentSlot.MAINHAND,weapon);WeaponConditions.attack(new AttackEntityEvent(p,target));WeaponConditions.critical(new CriticalHitEvent(p,target,1.5F,true));target.invulnerableTime=0;check(target.hurt(p.damageSources().playerAttack(p),10),"real primary hit accepted");check(WeaponConditions.windupBonus(p)>0,"critical hit arms next spell windup benefit");WeaponConditions.spellBegan(p);near(WeaponConditions.windupBonus(p),0,"next spell consumes windup benefit once");
            p.setItemSlot(EquipmentSlot.MAINHAND,item("神道枪",6));WeaponConditions.attack(new AttackEntityEvent(p,target));target.invulnerableTime=0;target.hurt(p.damageSources().playerAttack(p),10);check(target.hasEffect(MobEffects.GLOWING),"high divine spear reveals a real hit target");
            p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,item("神道枪",6));target.removeAllEffects();WeaponConditions.attack(new AttackEntityEvent(p,target));target.invulnerableTime=0;target.hurt(p.damageSources().playerAttack(p),10);check(!target.hasEffect(MobEffects.GLOWING),"offhand never triggers weapon conditions");p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
            armor(p,false);check(QitianSkills.pieces(p)==4,"non-exquisite quality activates special set");armor(p,true);check(QitianSkills.pieces(p)==4,"four actual armor slots activate the special set");near(QitianSkills.healthBonus(p),.35*QitianQuality.scale(5),"exquisite special-set health bonus scales with quality");
            for(int quality=1;quality<=5;quality++){
                d.setRealmStage(3,0);
                for(var id:List.of("fengchi_crown","suozi_chestplate","liujin_leggings","ousi_boots")){
                    var part=RefinedEquipment.finish(RefiningRecipes.definition(id),quality);var slot=((ArmorItem)part.getItem()).getEquipmentSlot();
                    check(p.inventoryMenu.slots.get(8-slot.getIndex()).mayPlace(part),"native armor slot accepts quality "+quality+" "+slot);
                    check(!p.inventoryMenu.slots.get(slot==EquipmentSlot.HEAD?6:5).mayPlace(part),"wrong armor slot rejects "+slot);
                    p.setItemSlot(slot,part);
                    var lines=new ArrayList<Component>();EquipmentTooltips.tooltip(new net.minecraftforge.event.entity.player.ItemTooltipEvent(part,p,lines,TooltipFlag.Default.NORMAL));
                    check(lines.stream().noneMatch(line->line.getString().contains("极品专属")),"all qualities describe active Qitian skills without obsolete quality gate");
                    check(lines.stream().anyMatch(line->line.getString().contains("有效齐天套装")),"all qualities show equipped Qitian count");
                }
                check(QitianSkills.pieces(p)==4,"all qualities activate at Nascent Soul "+quality);
                EquipmentEffects.refresh(p);check(p.getAttribute(Attributes.MAX_HEALTH).getModifiers().stream().anyMatch(m->m.getName().equals("health")&&m.getAmount()>=.35),"native set health modifier exists");
                check(QitianSkills.cost(p,1)<=Math.ceil(d.powerMax()*.8),"form affordable against actual capacity");
            }
            d.setPowerMax(3000);near(QitianSkills.cost(p,0),1200,"Nascent Soul step cost");near(QitianSkills.cost(p,1),2400,"Nascent Soul form cost");
            d.setRealmStage(2,0);check(QitianSkills.pieces(p)==0,"Golden Core cannot use Qitian set");d.setRealmStage(3,0);check(QitianSkills.pieces(p)==4,"Nascent Soul reactivates set");
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))p.setItemSlot(slot,ItemStack.EMPTY);
            EquipmentSkillMenu.open(p);check(p.containerMenu==p.inventoryMenu,"non-Qitian player cannot open skill menu");
            armor(p,false);EquipmentSkillMenu.open(p);check(p.containerMenu instanceof EquipmentSkillMenu,"non-exquisite Qitian set opens skill menu");p.closeContainer();
            d.setRealmStage(8,0);d.setPowerMax(100000);d.setPower(50000);
            var helmet=p.getItemBySlot(EquipmentSlot.HEAD);p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);check(QitianSkills.pieces(p)==3&&QitianSkills.healthBonus(p)==0,"missing one part revokes four piece bonus immediately");p.setItemSlot(EquipmentSlot.HEAD,helmet);
            QitianSkills.state(p).putInt("Selected",1);d.setPower(19999);String denied=QitianSkills.activate(p);check(!denied.isEmpty()&&d.power()==19999,"insufficient power is rejected without payment");d.setPower(50000);check(QitianSkills.activate(p).isEmpty(),"complete set activates form");near(d.power(),30000,"form pays fixed 20000 once");check(!QitianSkills.activate(p).isEmpty()&&d.power()==30000,"repeat request does not pay twice");check(QitianSkills.form(p),"form active after payment");p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);QitianSkills.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));check(!QitianSkills.form(p)&&QitianSkills.state(p).getLong("fatigue_until")>SpellService.clock(),"removing a part ends form and starts fatigue");near(d.power(),30000,"ending form never refunds spent power");p.setItemSlot(EquipmentSlot.HEAD,helmet);
            var staff=RefinedEquipment.finish(RefiningRecipes.definition("jingu_staff"),1);p.setItemSlot(EquipmentSlot.MAINHAND,staff);check(QitianSkills.staff(p),"ordinary quality staff enables manual skills");
            p.hurtTime=0;p.setShiftKeyDown(false);d.setPower(27000);QitianSkills.use(p.level(),p,InteractionHand.MAIN_HAND);
            near(d.power(),22000,"one right click commits staff payment immediately");check(!p.isUsingItem(),"staff needs no hold");check(QitianSkills.state(p).getLong("cooldown_staff_extend")>SpellService.clock(),"right click commits cooldown");
            QitianSkills.use(p.level(),p,InteractionHand.MAIN_HAND);near(d.power(),22000,"duplicate right click does not pay twice");
            QitianSkills.releaseStaff(staff,p,72000-100);near(d.power(),22000,"legacy key release cannot cause another payment");QitianSkills.cancel(p);
            p.setShiftKeyDown(true);QitianSkills.use(p.level(),p,InteractionHand.MAIN_HAND);near(d.power(),10000,"sneak right click pays quake once");check(QitianSkills.state(p).getLong("cooldown_staff_quake")>SpellService.clock(),"sneak right click commits independent quake cooldown");QitianSkills.cancel(p);p.setShiftKeyDown(false);
            d.setPowerMax(3000);near(QitianSkills.staffCost(p,false),540,"staff thrust proportional cost");near(QitianSkills.staffCost(p,true),1200,"staff quake proportional cost");
            d.setPowerMax(100000);d.setPower(100000);p.hurtTime=0;p.setYRot(0);p.setXRot(0);p.removeAllEffects();
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))p.setItemSlot(slot,ItemStack.EMPTY);
            for(var setting:RefiningRecipes.ALL.stream().filter(r->r.tier()>=5&&Set.of("sword","axe","spear").contains(r.kind())).collect(java.util.stream.Collectors.toMap(RefiningRecipes.Recipe::theme,r->r,(a,b)->a.tier()<b.tier()?a:b)).values()){
                var activeWeapon=RefinedEquipment.finish(setting,1);if(!WeaponActives.available(activeWeapon))continue;
                p.setItemSlot(EquipmentSlot.MAINHAND,activeWeapon);RefinedEquipment.refreshEligibility(p);p.removeAllEffects();p.setAbsorptionAmount(0);d.setPower(100000);
                ((Entity)target).setPos(p.getX(),p.getY(),p.getZ()+2.5);target.setHealth(10000);target.invulnerableTime=20;
                var result=WeaponActives.use(p.level(),p,InteractionHand.MAIN_HAND);check(result.getResult().consumesAction(),"high weapon has manual active "+setting.theme());check(d.power()<100000,"manual active pays once "+setting.theme());double paidPower=d.power();float life=target.getHealth();
                if(!Set.of("含光剑","干将剑","天罡枪").contains(setting.theme()))check(life<10000,"offensive active damages through previous hit frames "+setting.theme());
                else check(p.hasEffect(MobEffects.ABSORPTION)||p.hasEffect(MobEffects.DAMAGE_RESISTANCE),"defensive active applies actual buff "+setting.theme());
                WeaponActives.use(p.level(),p,InteractionHand.MAIN_HAND);near(d.power(),paidPower,"active cooldown rejects repeat payment "+setting.theme());near(target.getHealth(),life,"active cooldown rejects repeat hit "+setting.theme());
                p.setItemSlot(EquipmentSlot.OFFHAND,activeWeapon.copy());WeaponActives.use(p.level(),p,InteractionHand.OFF_HAND);near(d.power(),paidPower,"offhand cannot activate "+setting.theme());p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
            }
            for(int tier:new int[]{1,3})for(int quality:new int[]{1,5}){
                for(var r:RefiningRecipes.ALL.stream().filter(r->r.theme().equals("沧溟镇岳甲")&&r.tier()==tier).toList()){
                    var part=RefinedEquipment.finish(r,quality);if(part.getItem() instanceof ArmorItem a)p.setItemSlot(a.getEquipmentSlot(),part);
                }
                var loadout=EquipmentEffects.read(p);double expected=(1+.08*(tier-1))*(1+.03*(quality-1));near(loadout.scale(),expected,"ordinary set tier and quality scale "+tier+"/"+quality);near(EquipmentEffects.healthBonus(p),.06*expected,"ordinary set real health bonus "+tier+"/"+quality);EquipmentEffects.refresh(p);
                check(p.getAttribute(Attributes.MAX_HEALTH).getModifiers().stream().anyMatch(m->m.getName().equals("health")&&Math.abs(m.getAmount()-.06*expected)<.00001),"set health attribute carries quality scaling");
            }
            p.removeAllEffects();
            check(RefiningRecipes.find("jingu_staff")!=null,"special manufacturing recipe enabled by user");
            System.out.println("[Equipment conditions QA] PASS "+checks);c.getSource().sendSuccess(()->Component.literal("[Equipment conditions QA] PASS "+checks),false);return 1;
        }catch(Exception|LinkageError failure){System.out.println("[Equipment conditions QA] FAIL "+checks+" "+failure);failure.printStackTrace();c.getSource().sendFailure(Component.literal("[Equipment conditions QA] FAIL "+checks+" "+failure));return 0;}
        finally{p.setShiftKeyDown(oldSneaking);if(target!=null)((Entity)target).discard();p.stopUsingItem();QitianSkills.cancel(p);p.removeAllEffects();p.getPersistentData().getAllKeys().clear();p.getPersistentData().merge(persistent);d.load(data);p.getInventory().load(inventory);for(var effect:effects)p.addEffect(effect);p.getFoodData().readAdditionalSaveData(food);p.level().getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(natural,p.server);p.setGameMode(mode);p.teleportTo(p.serverLevel(),position.x,position.y,position.z,yaw,pitch);RefinedEquipment.refreshEligibility(p);GongfaService.refresh(p);EquipmentEffects.refresh(p);QitianSkills.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));p.setHealth(Math.min(health,p.getMaxHealth()));p.setAbsorptionAmount(absorption);p.inventoryMenu.broadcastChanges();CultivationEvents.sync(p);SpellService.sync(p);}
    }
    private EquipmentConditionsAudit(){}
}
