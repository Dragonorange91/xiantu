package com.longju.xiantu.qa;

import com.google.gson.*;
import com.longju.xiantu.refining.*;
import com.longju.xiantu.combat.*;
import com.longju.xiantu.cultivation.*;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Synchronous disposable-world fixture; no spawned mobs or placed blocks. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class WeaponPassiveAudit {
    private static int checks;
    private static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    private static RefiningRecipes.Recipe recipe(String theme){return RefiningRecipes.ALL.stream().filter(r->r.theme().equals(theme)).findFirst().orElseThrow();}
    private static void equip(net.minecraft.world.entity.player.Player p,EquipmentSlot slot,ItemStack stack){p.getAttributes().removeAttributeModifiers(p.getItemBySlot(slot).getAttributeModifiers(slot));p.setItemSlot(slot,stack);}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantuweaponaudit").requires(s->s.hasPermission(2)).executes(ctx->{
        var p=ctx.getSource().getPlayerOrException();
        if(!p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        checks=0;var report=new JsonObject();var inv=p.getInventory();var savedInv=inv.save(new ListTag());
        var data=(PlayerCultivationData)p.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new);var saved=new CompoundTag();data.save(saved);
        boolean sprint=p.isSprinting();var original=Attributes.MOVEMENT_SPEED;double oldSpeed=p.getAttributeValue(original);
        try{
            for(var slot:EquipmentSlot.values())equip(p,slot,ItemStack.EMPTY);inv.clearContent();data.setCultivationUnlocked(true);data.setRealmStage(8,0);
            LivingEntity target=net.minecraft.world.entity.EntityType.PIG.create(p.serverLevel());
            ((Entity)target).setPos(p.getX(),p.getY(),p.getZ()+3);target.yBodyRot=0;
            int weapons=0,described=0;
            for(var r:RefiningRecipes.ALL)if(WeaponPassives.weapon(r)){
                var stack=RefinedEquipment.finish(r,1);weapons++;equip(p,EquipmentSlot.MAINHAND,stack);
                var core=r.inputs().stream().filter(i->i.item().equals("xiantu:demon_core")).findFirst().orElseThrow();
                // The Qi Tian staff is explicitly physical, even though its recipe uses an elemental core.
                var expected=r.kind().equals("staff")?Element.PHYSICAL.id():core.element();
                check(ElementalCombat.weaponElement(stack).id().equals(expected),"weapon element "+r.id());
                check(WeaponPassives.active(p)==r,"eligible main hand "+r.id());
                check(WeaponPassives.scale(stack)>=1&&WeaponPassives.scale(stack)<=1.48+1e-8,"bounded tier scaling");
                if(!WeaponPassives.description(stack).isEmpty())described++;
                equip(p,EquipmentSlot.MAINHAND,ItemStack.EMPTY);equip(p,EquipmentSlot.OFFHAND,stack);
                check(WeaponPassives.active(p)==null&&WeaponPassives.damageBonus(p,target)==0,"offhand never triggers");equip(p,EquipmentSlot.OFFHAND,ItemStack.EMPTY);
            }
            for(String theme:List.of("青霜剑","赤霄剑","紫电剑")){
                var r=recipe(theme);equip(p,EquipmentSlot.MAINHAND,RefinedEquipment.finish(r,1));
                check(Math.abs(WeaponPassives.damageBonus(p,target)-.03*WeaponPassives.scale(p.getMainHandItem()))<1e-8,"element damage "+theme);
            }
            equip(p,EquipmentSlot.MAINHAND,RefinedEquipment.finish(recipe("承影剑"),1));
            check(WeaponPassives.damageBonus(p,target)>0,"rear hit bonus");target.yBodyRot=180;check(WeaponPassives.damageBonus(p,target)==0,"frontal hit no bonus");
            equip(p,EquipmentSlot.MAINHAND,RefinedEquipment.finish(recipe("破军枪"),1));p.setSprinting(false);check(WeaponPassives.damageBonus(p,target)==0,"non sprint no bonus");p.setSprinting(true);check(WeaponPassives.damageBonus(p,target)>0,"sprint bonus");
            var sword=RefinedEquipment.finish(recipe("青虹剑"),1);equip(p,EquipmentSlot.MAINHAND,sword);RefinedEquipment.refreshEligibility(p);
            double first=p.getAttributeValue(original);for(int i=0;i<20;i++)RefinedEquipment.refreshEligibility(p);
            check(Math.abs(first-p.getAttributeValue(original))<1e-8,"refresh never stacks movement");
            check(p.getAttribute(original).getModifier(RefinedEquipment.modifierId("passive_move",EquipmentSlot.MAINHAND))!=null,"movement actually installed");
            data.setCultivationUnlocked(false);RefinedEquipment.refreshEligibility(p);
            check(p.getAttribute(original).getModifier(RefinedEquipment.modifierId("passive_move",EquipmentSlot.MAINHAND))==null,"rollback revokes passive");
            check(WeaponPassives.active(p)==null,"uninitiated cannot trigger");data.setCultivationUnlocked(true);RefinedEquipment.refreshEligibility(p);
            check(p.getAttribute(original).getModifier(RefinedEquipment.modifierId("passive_move",EquipmentSlot.MAINHAND))!=null,"qualification restores passive");
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
                var r=RefiningRecipes.ALL.stream().filter(v->v.kind().equals("armor")&&v.slot().equals(switch(slot){case HEAD->"helmet";case CHEST->"chestplate";case LEGS->"leggings";default->"boots";})).findFirst().orElseThrow();
                equip(p,slot,RefinedEquipment.finish(r,1));RefinedEquipment.refreshEligibility(p);
                check(p.getAttribute(Attributes.ARMOR).getModifier(RefinedEquipment.modifierId("armor",slot))!=null,"qualified armor "+slot);
                data.setCultivationUnlocked(false);RefinedEquipment.refreshEligibility(p);
                check(p.getAttribute(Attributes.ARMOR).getModifier(RefinedEquipment.modifierId("armor",slot))==null,"rollback revokes armor "+slot);data.setCultivationUnlocked(true);
            }
            var chaos=RefinedEquipment.finish(recipe("混沌斧"),1);check(chaos.getMaxDamage()>30000,"chaos durability bonus applied");
            var fake=new ItemStack(Items.IRON_SWORD);fake.getOrCreateTag().put(RefinedEquipment.KEY,sword.getTag().getCompound(RefinedEquipment.KEY).copy());check(RefinedEquipment.recipe(fake)==null&&WeaponPassives.description(fake).isEmpty(),"wrong registered item rejects metadata");
            equip(p,EquipmentSlot.MAINHAND,RefinedEquipment.finish(recipe("青霜剑"),1));
            var event=new LivingHurtEvent(target,p.damageSources().playerAttack(p),100);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);check(Float.isFinite(event.getAmount())&&event.getAmount()>0,"real Forge damage chain remains finite");
            report.addProperty("weapons",weapons);report.addProperty("recipesWithImplementedPassive",described);report.addProperty("status","PASS");
        }catch(Throwable ex){report.addProperty("status","FAIL");report.addProperty("error",ex.toString());org.slf4j.LoggerFactory.getLogger(WeaponPassiveAudit.class).error("Weapon audit failed",ex);}
        finally{
            // Remove fixture modifiers before restoring the original stacks and realm.
            for(var slot:EquipmentSlot.values())equip(p,slot,ItemStack.EMPTY);inv.load(savedInv);data.load(saved);p.setSprinting(sprint);
            for(var slot:EquipmentSlot.values())for(var entry:p.getItemBySlot(slot).getAttributeModifiers(slot).entries()){
                var attribute=p.getAttribute(entry.getKey());if(attribute!=null&&attribute.getModifier(entry.getValue().getId())==null)attribute.addTransientModifier(entry.getValue());
            }RefinedEquipment.refreshEligibility(p);
            inv.setChanged();p.inventoryMenu.broadcastChanges();CultivationEvents.sync(p);
        }
        report.addProperty("checks",checks);try{var file=p.server.getServerDirectory().toPath().resolve("xiantu-qa/weapon-passives.json");java.nio.file.Files.createDirectories(file.getParent());java.nio.file.Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception ex){throw new RuntimeException(ex);}
        p.sendSystemMessage(Component.literal("装备被动测试 "+report.get("status").getAsString()+"："+checks+" 项"));return report.get("status").getAsString().equals("PASS")?1:0;
    }));}
}
