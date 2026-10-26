package com.longju.xiantu.qa;

import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.refining.*;
import com.longju.xiantu.spell.SpellService;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Real production entry points, tick-driven native item holds, scoped to a dedicated world. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class SkillPresentationAudit {
    private static final String WORLD="Xiantu-QA-BeastFlat-20261017";
    private static final Set<String> CASES=new HashSet<>(Set.of("staff_extend","staff_quake","cloud_step","great_sage_form","seventy_two_forms","axe_charge","spear_charge","fire_passive","earth_passive"));
    static { CASES.addAll(WeaponActives.SKILLS); CASES.remove("heaven_split"); CASES.remove("sun_pierce"); }
    private static Session active;
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantuskillqa").requires(s->s.hasPermission(2))
            .then(Commands.argument("case",com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c->{
                var p=c.getSource().getPlayerOrException();String id=com.mojang.brigadier.arguments.StringArgumentType.getString(c,"case");
                if(!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().equals(WORLD))return 0;
                if(id.equals("restore")){if(active!=null){active.restore();active=null;}return 1;}
                if(active!=null||!CASES.contains(id))return 0;
                active=new Session(p,id);try{active.prepare();return 1;}catch(Exception|LinkageError ex){active.finish(ex);active=null;return 0;}
            })));
    }
    @SubscribeEvent public static void event(EquipmentSkillEvent e){if(active!=null&&e.owner==active.p)active.events.add(e.skill+":"+e.phase);}
    @SubscribeEvent public static void damage(LivingDamageEvent e){if(active!=null&&active.targets.contains(e.getEntity())&&e.getSource().getEntity()==active.p){active.hits++;active.damage+=e.getAmount();}}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST) public static void beforePlayerTick(TickEvent.PlayerTickEvent e){if(active!=null&&e.player==active.p&&e.phase==TickEvent.Phase.END)active.recoveryBefore=active.d.power();}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void afterPlayerTick(TickEvent.PlayerTickEvent e){if(active!=null&&e.player==active.p&&e.phase==TickEvent.Phase.END&&active.age>=20)active.recovered+=Math.max(0,active.d.power()-active.recoveryBefore);}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||active==null)return;
        try{active.step();}catch(Exception|LinkageError ex){active.finish(ex);active=null;}
    }
    private static final class Session {
        final ServerPlayer p;final String id;final PlayerCultivationData d;
        final CompoundTag cultivation=new CompoundTag(),persistent;final ListTag inventory;
        final net.minecraft.world.phys.Vec3 position;final float yaw,pitch,health,absorption;
        final GameType mode;final boolean noGravity;final net.minecraft.world.Difficulty difficulty;
        final List<Zombie> targets=new ArrayList<>();final List<String> events=new ArrayList<>();
        int age,checks,hits,maxHeld;long activeCooldown;double damage,paid,beforeHold,recovered,recoveryBefore;net.minecraft.world.phys.Vec3 castPosition;
        Session(ServerPlayer p,String id){this.p=p;this.id=id;d=(PlayerCultivationData)GongfaService.data(p);d.save(cultivation);persistent=p.getPersistentData().copy();inventory=p.getInventory().save(new ListTag());position=p.position();yaw=p.getYRot();pitch=p.getXRot();health=p.getHealth();absorption=p.getAbsorptionAmount();mode=p.gameMode.getGameModeForPlayer();noGravity=p.isNoGravity();difficulty=p.level().getDifficulty();}
        void check(boolean value,String message){checks++;if(!value)throw new IllegalStateException(message);}
        ItemStack item(String theme){return RefinedEquipment.finish(RefiningRecipes.ALL.stream().filter(r->r.theme().equals(theme)&&(Set.of("赤霄剑","裂地斧").contains(theme)||r.tier()>=5)).findFirst().orElseThrow(),5);}
        void prepare(){
            p.closeContainer();p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);p.hurtTime=0;p.server.setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
            p.teleportTo(p.serverLevel(),5000.5,-60,5000.5,0,id.equals("cloud_step")?0:10);castPosition=p.position();
            d.setCultivationUnlocked(true);d.setRealmStage(8,0);d.setPhysique(PhysiqueType.ORDINARY);d.gongfa().clear();d.medicine().clearTemporary();
            for(var root:SpiritRootType.values())d.addSpiritRoot(root);
            d.gongfa().learn("tai_chu_dao_jing");d.gongfa().commit("tai_chu_dao_jing",UUID.randomUUID());GongfaService.refresh(p);d.setPowerMax(1000000);d.setPower(1000000);d.setPowerSpeed(0);
            p.getInventory().clearContent();p.getPersistentData().remove(QitianSkills.KEY);p.getPersistentData().putLong("xiantu_equipment_last_combat",SpellService.clock()-1000);
            if(Set.of("staff_extend","staff_quake","cloud_step","great_sage_form","seventy_two_forms").contains(id)){
                for(var r:RefiningRecipes.ALL)if(r.theme().equals("齐天")&&!r.kind().equals("staff")){
                    var stack=RefinedEquipment.finish(r,5);var armor=(ArmorItem)stack.getItem();p.setItemSlot(armor.getEquipmentSlot(),stack);
                }
                check(QitianSkills.pieces(p)==4,"all four genuine exquisite armor parts");
            }
            ItemStack weapon=switch(id){case "staff_extend","staff_quake"->RefinedEquipment.finish(RefiningRecipes.definition("jingu_staff"),5);case "axe_charge"->item("开天斧");case "spear_charge"->item("贯日枪");case "fire_passive"->item("赤霄剑");case "earth_passive"->item("裂地斧");default->ItemStack.EMPTY;};
            if(WeaponActives.SKILLS.contains(id))weapon=RefinedEquipment.finish(RefiningRecipes.ALL.stream().filter(r->r.tier()>=5&&WeaponActives.skillId(r.theme()).equals(id)).findFirst().orElseThrow(),1);
            p.setItemSlot(EquipmentSlot.MAINHAND,weapon);RefinedEquipment.refreshEligibility(p);p.setHealth(p.getMaxHealth());
            for(int i=0;i<(id.equals("cloud_step")?0:id.equals("earth_passive")?2:1);i++){
                Zombie target=EntityType.ZOMBIE.create(p.serverLevel());target.setNoAi(true);target.setPersistenceRequired();target.addTag("xiantu_skill_presentation_qa");
                ((Entity)target).moveTo(5000.5+i*.8,-60,5003.0,180,0);target.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));
                target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000000);target.setHealth(target.getMaxHealth());target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
                p.serverLevel().addFreshEntity(target);targets.add(target);
            }
            CultivationEvents.sync(p);p.inventoryMenu.broadcastChanges();
            p.sendSystemMessage(Component.literal("[Skill presentation QA] START "+id));
        }
        void melee(){var target=targets.get(0);target.invulnerableTime=0;p.resetAttackStrengthTicker();
            // The server's native player attack posts Forge hooks and drives weapon conditions.
            p.attack(target);p.swing(InteractionHand.MAIN_HAND,true);
        }
        void step(){
            age++;
            maxHeld=Math.max(maxHeld,p.getTicksUsingItem());
            if(age==20){
                if(WeaponActives.SKILLS.contains(id)){
                    check(WeaponActives.available(p.getMainHandItem()),"genuine active weapon eligible");beforeHold=d.power();recovered=0;
                    net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->SkillNativeUseAudit.queue(60));
                }else if(id.startsWith("staff_")||id.endsWith("_charge")){
                    check(id.startsWith("staff_")?QitianSkills.staff(p):WeaponConditions.chargeWeapon(p),"genuine charge equipment eligible");beforeHold=d.power();recovered=0;
                    int ticks=id.equals("staff_quake")?46:id.equals("axe_charge")?27:24;
                    net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->{if(id.startsWith("staff_"))SkillNativeUseAudit.click(id.equals("staff_quake"));else SkillNativeUseAudit.queue(ticks);});
                }
                else if(id.equals("seventy_two_forms")){
                    String species=QitianSkills.DISGUISES.get(0);var discoveries=new CompoundTag();discoveries.putBoolean(species,true);QitianSkills.state(p).put("Discovered",discoveries);
                    check(QitianSkills.disguise(p,0).isEmpty(),"discovered disguise accepted");check(QitianSkills.state(p).getLong("disguise_until")>SpellService.clock(),"real disguise timer active");
                }else if(!Set.of("fire_passive","earth_passive").contains(id)){
                    int selected=id.equals("cloud_step")?0:1;QitianSkills.state(p).putInt("Selected",selected);
                    double before=d.power();String denial=QitianSkills.activate(p);check(denial.isEmpty(),"real armor activation accepted: "+denial);paid=before-d.power();
                    if(selected==1)check(Math.abs(paid-20000)<.01,"single armor payment");
                }
            }
            if((id.startsWith("staff_")||id.endsWith("_charge"))&&age==100){
                if(id.startsWith("staff_"))check(maxHeld==0,"staff uses immediate native click without hold");else check(maxHeld>=(id.equals("axe_charge")?20:16),"actual continuous held ticks "+maxHeld);
                check(!p.isUsingItem(),"native client release completed");paid=beforeHold-d.power()+recovered;
                check(Math.abs(paid-(id.equals("staff_extend")?5000:id.equals("staff_quake")?12000:0))<.01,"single held release payment");
                if(id.endsWith("_charge"))check(p.getPersistentData().getCompound("xiantu_weapon_timers").getLong("charge_"+(id.equals("axe_charge")?"开天斧":"贯日枪"))>SpellService.clock(),"real native release entered weapon cooldown");
            }
            if(WeaponActives.SKILLS.contains(id)&&age==35){
                activeCooldown=p.getPersistentData().getLong("xiantu_active_"+RefinedEquipment.recipe(p.getMainHandItem()).theme());
                check(activeCooldown>SpellService.clock(),"native use began authoritative cooldown");
            }
            if(id.equals("axe_charge")&&age==110)melee();
            if(id.equals("fire_passive")&&(age==25||age==55||age==85)||id.equals("earth_passive")&&age==40)melee();
            if(age>=170){
                if(WeaponActives.SKILLS.contains(id)){
                    check(p.getPersistentData().getLong("xiantu_active_"+RefinedEquipment.recipe(p.getMainHandItem()).theme())==activeCooldown,"held native right click cannot extend cooldown");
                    paid=beforeHold-d.power()+recovered;check(paid>0,"native skill consumed power");
                    check(maxHeld==0&&!p.isUsingItem(),"instant active never enters use/charge state");
                    if(!Set.of("radiant_return","forged_guard","celestial_guard","great_dao_quake").contains(id))check(hits>0&&damage>0,"real active target damage");
                }
                if(id.equals("cloud_step"))check(p.position().distanceToSqr(castPosition)>4,"real displacement");
                if(id.equals("great_sage_form"))check(QitianSkills.form(p),"real form active");
                if(Set.of("staff_extend","staff_quake","spear_charge","fire_passive","earth_passive").contains(id))check(hits>0&&damage>0,"real target damage");
                String skill=switch(id){case "axe_charge","spear_charge"->"weapon_charge";default->id;};
                if(!WeaponActives.SKILLS.contains(id)&&!Set.of("fire_passive","earth_passive","axe_charge","spear_charge").contains(id))check(events.contains(skill+":RELEASE"),"real equipment-event release lifecycle "+events);
                if(id.equals("fire_passive"))check(hits==3&&!events.contains("weapon_fire:HIT"),"low-tier fire weapon keeps its actual passive, no unavailable high-tier combo");
                if(id.equals("earth_passive"))check(hits==1&&!events.contains("weapon_earth:HIT"),"low-tier earth weapon keeps its actual passive, no unavailable high-tier splash");
                finish(null);active=null;
            }
        }
        void finish(Throwable failure){
            String result="[Skill presentation QA "+(failure==null?"PASS":"FAIL")+"] "+id+" checks="+checks+" hits="+hits+" damage="+damage+" paid="+paid+" maxHeld="+maxHeld+" events="+events+(failure==null?"":" error="+failure);
            com.mojang.logging.LogUtils.getLogger().info(result);p.sendSystemMessage(Component.literal(result));
            try{Path folder=p.server.getWorldPath(LevelResource.ROOT).resolve("xiantu-skill-qa");Files.createDirectories(folder);Files.writeString(folder.resolve(id+".txt"),result);}catch(Exception ex){com.mojang.logging.LogUtils.getLogger().error("Skill QA report",ex);}
            restore();
        }
        void restore(){
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->SkillNativeUseAudit::cancel);
            p.stopUsingItem();QitianSkills.cancel(p);targets.forEach(t->((Entity)t).discard());targets.clear();
            p.getInventory().load(inventory);for(String key:new ArrayList<>(p.getPersistentData().getAllKeys()))p.getPersistentData().remove(key);p.getPersistentData().merge(persistent);d.load(cultivation);
            p.setGameMode(mode);p.setNoGravity(noGravity);p.server.setDifficulty(difficulty,true);p.teleportTo(p.serverLevel(),position.x,position.y,position.z,yaw,pitch);
            GongfaService.refresh(p);RefinedEquipment.refreshEligibility(p);EquipmentEffects.refresh(p);p.setHealth(Math.min(health,p.getMaxHealth()));p.setAbsorptionAmount(absorption);p.inventoryMenu.broadcastChanges();CultivationEvents.sync(p);
        }
    }
    private SkillPresentationAudit(){}
}
