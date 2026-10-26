package com.longju.xiantu.qa;
import com.longju.xiantu.alchemy.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.item.PillItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import java.util.*;

/** Real Forge item paths, capability persistence, events and UI selection on an isolated QA player. */
public final class PillsAudit {
    private static int checks;
    private static void check(boolean result,String label){if(!result)throw new IllegalStateException("Pill QA FAIL: "+label);checks++;}
    private static void reset(ServerPlayer p){
        var d=MedicineService.data(p);d.medicine().load(new CompoundTag());d.setPowerMax(100);d.setPower(0);d.setComprehension(5);d.setInnerDemon(100);
        p.removeAllEffects();p.getPersistentData().putDouble("xiantu_pill_power_bonus",0);p.getPersistentData().putDouble("xiantu_gongfa_power_max_bonus",0);p.getPersistentData().putDouble("xiantu_physique_power_max_bonus",0);
        MedicineService.refreshHealth(p);p.setHealth(Math.max(1,p.getMaxHealth()/2));
    }
    public static int run(ServerPlayer p){
        if(!p.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        var d=(PlayerCultivationData)MedicineService.data(p);var saved=new CompoundTag();d.save(saved);var persistent=p.getPersistentData().copy();
        List<ItemStack> inventory=new ArrayList<>();for(int i=0;i<p.getInventory().getContainerSize();i++)inventory.add(p.getInventory().getItem(i).copy());
        var effects=p.getActiveEffects().stream().map(MobEffectInstance::new).toList();float health=p.getHealth();var mode=p.gameMode.getGameModeForPlayer();
        var carried=p.containerMenu.getCarried().copy();p.closeContainer();checks=0;
        try{
            p.setGameMode(GameType.SURVIVAL);d.setPhysique(PhysiqueType.ORDINARY);d.setCultivationUnlocked(true);d.setRealmStage(0,0);d.gongfa().clear();
            for(int i=0;i<inventory.size();i++)p.getInventory().setItem(i,ItemStack.EMPTY);
            for(var def:PillCatalog.ALL)for(int q=1;q<=5;q++){
                reset(p);ItemStack s=PillCatalog.stack(def.id(),q);s.setCount(2);
                check(s.getItem() instanceof PillItem&&s.getItem().getUseDuration(s)==32,"registered item / 32 ticks "+def.id()+" q"+q);
                check(s.getHoverName().getString().contains(PillCatalog.QUALITIES[q-1]),"quality name "+def.id()+" q"+q);
                double value=def.value(q);
                if(def.breakthrough()){
                    s.getItem().finishUsingItem(s,p.level(),p);check(s.getCount()==2&&d.medicine().totalDoses()==0,"breakthrough cannot be eaten "+def.id());continue;
                }
                if(def.kind().equals("physique")){
                    p.getInventory().setItem(0,s);PillChoiceMenu.open(p,1);net.minecraft.world.inventory.AbstractContainerMenu menu=p.containerMenu;
                    check(menu.clickMenuButton(p,0),"wash marrow confirmation");int[] candidates=d.medicine().state().getIntArray("physique_candidates");
                    check(candidates.length==(int)value&&Arrays.stream(candidates).distinct().count()==candidates.length&&s.getCount()==1,"distinct weighted candidates");
                    MedicineData copy=new MedicineData();copy.load(d.medicine().save());check(Arrays.equals(candidates,copy.state().getIntArray("physique_candidates")),"candidates persist without reroll");
                    p.containerMenu.clickMenuButton(p,100);p.closeContainer();continue;
                }
                if(def.kind().equals("cross_dimension")){
                    p.setItemInHand(InteractionHand.MAIN_HAND,s);MedicineService.beginTeleport(p,InteractionHand.MAIN_HAND);
                    check(s.getCount()==2&&d.medicine().totalDoses()==0,"unbound cross-dimension denial");continue;
                }
                if(def.kind().equals("detox"))p.addEffect(new MobEffectInstance(MobEffects.POISON,600));
                if(def.kind().equals("soul_heal"))MedicineService.inflict(p,"soul",2400);
                boolean shouldAccept=!(def.kind().equals("roots")&&q<4||def.kind().equals("wisdom")&&q==1);
                double baseHealth=p.getMaxHealth(),beforeProgress=d.progress();
                s.getItem().finishUsingItem(s,p.level(),p);
                check(s.getCount()==(shouldAccept?1:2)&&d.medicine().totalDoses()==(shouldAccept?1:0),"atomic consume/denial "+def.id()+" q"+q);
                if(!shouldAccept)continue;
                check(Math.abs(d.medicine().toxicity()-MedicineService.doseToxicity(def,q))<.0011,"quality-specific toxin "+def.id()+" q"+q);
                check(d.progress()==beforeProgress,"no direct cultivation gain "+def.id());
                switch(def.kind()){
                    case "power","recovery"->check(Math.abs(d.power()-Math.min(100,value))<.001,"power restoration "+def.id());
                    case "heal"->check(Math.abs(p.getHealth()-Math.min(baseHealth,baseHealth*(.5+value/100)))<.02,"healing percent");
                    case "soul"->check(Math.abs(d.innerDemon()-Math.max(0,100-value))<.001,"inner demon reduction");
                    case "detox"->check(!p.hasEffect(MobEffects.POISON),"poison removed");
                    case "health"->check(p.getMaxHealth()>baseHealth&&p.getHealth()<=baseHealth/2+.01,"health capacity no free healing");
                    case "capacity"->check(Math.abs(d.powerMax()-(100+value))<.002&&d.power()==0,"power capacity no free filling");
                    case "wisdom"->check(d.comprehension()==Math.min(20,5+(int)value),"permanent comprehension");
                    case "roots"->check(d.spiritRootCount()>=1&&d.spiritRootCount()<=5,"rerolled valid roots");
                    case "soul_heal"->check(!MedicineService.injured(p,"soul"),"soul injury cleansed");
                    case "dragon_blood"->check(!MedicineService.inflict(p,"bone",2400),"new bone injury blocked");
                    case "soul_guard"->check(!MedicineService.inflict(p,"soul",2400),"new soul injury blocked");
                    case "miasma_immunity"->check(!MedicineService.inflict(p,"miasma",600),"miasma blocked");
                    case "gu_immunity","all_poison_immunity"->check(!MedicineService.inflict(p,"gu",600),"gu blocked");
                    case "decoy"->{long n=0;for(var e:p.serverLevel().getAllEntities())if(e instanceof PillDecoyEntity decoy&&p.getUUID().equals(decoy.ownerId()))n++;check(n==1,"single harmless decoy");MedicineService.removeDecoy(p);}
                    default->check(MedicineService.effect(p,def.kind())>0,"timed effect "+def.id());
                }
                if(def.cooldown()>0){double toxin=d.medicine().toxicity();s.getItem().finishUsingItem(s,p.level(),p);
                    check(s.getCount()==1&&d.medicine().toxicity()==toxin,"cooldown preserves stack and toxin "+def.id());}
            }
            reset(p);d.setPower(100);var full=PillCatalog.stack("pill_04",5);check(!MedicineService.consume(p,full)&&full.getCount()==1,"full power refusal");
            reset(p);var low=PillCatalog.stack("pill_04",1);low.getOrCreateTag().putDouble("power_gain",1e9);low.getOrCreateTag().putDouble("toxicity",0);
            MedicineService.consume(p,low);check(d.power()==30&&d.medicine().toxicity()==5,"untrusted effect NBT ignored");
            reset(p);MedicineService.consume(p,PillCatalog.stack("pill_04",1));var recovery=PillCatalog.stack("pill_08",5);check(!MedicineService.consume(p,recovery)&&recovery.getCount()==1,"shared power cooldown");
            reset(p);MedicineService.consume(p,PillCatalog.stack("pill_10",5));MedicineService.consume(p,PillCatalog.stack("pill_32",5));
            check(Math.abs(MedicineService.meleeBonus(p)-.72)<.00001,"rage/dragon max, no stacking");
            reset(p);var m=d.medicine();m.setSummary(1,100);float hp=p.getHealth();int tick=p.tickCount;p.tickCount=20;MedicineService.tick(p);p.tickCount=tick;
            check(!p.hasEffect(MobEffects.POISON)&&!p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&!p.hasEffect(MobEffects.WEAKNESS)&&p.getHealth()==hp,"toxin has no old combat debuffs or damage");
            check(MedicineService.meditationMultiplier(p,false)==.4,"toxin affects meditation once");
            reset(p);MedicineService.consume(p,PillCatalog.stack("pill_28",5));double cap=d.powerMax();MedicineService.refreshCapacity(p);MedicineService.refreshCapacity(p);
            check(d.powerMax()==cap,"capacity refresh idempotent");d.setPower(cap);d.medicine().effect("capacity",30,MedicineService.now(p)-1);MedicineService.refreshCapacity(p);
            check(Math.abs(d.powerMax()-100)<.001&&d.power()==100,"expired capacity clamps current power");
            reset(p);var main=PillCatalog.stack("pill_01",5);main.setCount(2);p.getInventory().setItem(0,main);var aux=PillCatalog.stack("pill_20",4);p.getInventory().setItem(1,aux);
            var state=d.medicine().state();state.putString("main_pill","pill_01");state.putInt("main_quality",5);state.putString("aux_pill","pill_20");state.putInt("aux_quality",4);
            check(Math.abs(PillChoiceMenu.bonus(p,false)-.54)<.0001&&main.getCount()==2,"breakthrough preview never consumes");
            check(Math.abs(PillChoiceMenu.bonus(p,true)-.54)<.0001&&main.getCount()==1&&aux.isEmpty()&&d.medicine().totalDoses()==2,"breakthrough transaction consumes each selected pill once");
            m=d.medicine();m.effect("rage",72,MedicineService.now(p)+600);m.cooldown("power",MedicineService.now(p),20);var copy=new MedicineData();copy.load(m.save());copy.clearTemporary();
            check(copy.effect("rage",MedicineService.now(p))==0&&copy.cooldown("power",MedicineService.now(p))>0&&copy.toxicity()==m.toxicity(),"death clears temporary buffs, preserves cooldown and toxin");
            String result="[Pill QA] "+checks+" PASS; 36 pills / 180 quality variants";p.sendSystemMessage(Component.literal(result));org.slf4j.LoggerFactory.getLogger(PillsAudit.class).info(result);return checks;
        }finally{
            MedicineService.removeDecoy(p);p.closeContainer();d.load(saved);
            for(String k:Set.copyOf(p.getPersistentData().getAllKeys()))p.getPersistentData().remove(k);p.getPersistentData().merge(persistent);
            for(int i=0;i<inventory.size();i++)p.getInventory().setItem(i,inventory.get(i));p.inventoryMenu.setCarried(carried);
            p.removeAllEffects();for(var e:effects)p.addEffect(e);p.setGameMode(mode);CultivationService.applyRealmAttributes(p,d);MedicineService.refreshHealth(p);MedicineService.refreshCapacity(p);
            p.setHealth(Math.min(health,p.getMaxHealth()));CultivationEvents.sync(p);
        }
    }
}
