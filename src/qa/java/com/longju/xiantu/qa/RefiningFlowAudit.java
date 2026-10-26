package com.longju.xiantu.qa;

import com.google.gson.*;
import com.longju.xiantu.refining.*;
import com.longju.xiantu.material.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.registry.*;
import com.longju.xiantu.workstation.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class RefiningFlowAudit {
    private static int checks;
    private static void check(boolean ok,String label){checks++;if(!ok)throw new IllegalStateException(label);}
    @SubscribeEvent public static void register(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("xiantuforgeaudit").requires(s->s.hasPermission(2)).executes(ctx->{
        var p=ctx.getSource().getPlayerOrException();var l=p.serverLevel();
        if(!p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        checks=0;var report=new JsonObject();var inv=p.getInventory();var inventory=inv.save(new ListTag());var mode=p.gameMode.getGameModeForPlayer();
        var data=(PlayerCultivationData)p.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new);var saved=new CompoundTag();data.save(saved);
        var persistent=p.getPersistentData().copy();var ledger=RefiningLedger.get(l);var ledgerBefore=((net.minecraft.world.level.saveddata.SavedData)ledger).save(new CompoundTag());
        try{
            p.setGameMode(GameType.SURVIVAL);data.setCultivationUnlocked(true);data.setRealmStage(0,0);inv.clearContent();p.getPersistentData().remove(RefiningProgress.KEY);
            var at=p.blockPosition();var altar=new AppraisalBlockEntity(at,ModBlocks.APPRAISAL.get().defaultBlockState());((BlockEntity)altar).setLevel(l);
            int materials=0;
            for(var item:ForgeRegistries.ITEMS){var unknown=new ItemStack(item,16);if(!MaterialQuality.eligible(unknown))continue;materials++;
                unknown.getOrCreateTag().putInt("CustomModelData",731);unknown.getOrCreateTag().putString("audit_marker","preserved");
                if(item instanceof com.longju.xiantu.item.DemonCoreItem)com.longju.xiantu.item.DemonCoreItem.setData(unknown,6,"water");
                ((Container)altar).setItem(0,unknown.copy());check(altar.identify(),"identify "+item);
                int count=0;for(int q=1;q<=5;q++){var out=((Container)altar).getItem(q);if(out.isEmpty())continue;count+=out.getCount();
                    check(MaterialQuality.get(out)==q,"correct output grade");check(out.getTag().getInt("CustomModelData")==unknown.getTag().getInt("CustomModelData")&&out.getTag().getString("audit_marker").equals("preserved"),"custom data preserved");
                    if(item instanceof com.longju.xiantu.item.DemonCoreItem)check(com.longju.xiantu.item.DemonCoreItem.grade(out)==6&&com.longju.xiantu.item.DemonCoreItem.element(out).equals("water"),"core grade/element retained");
                    check(!((Container)altar).canPlaceItem(0,out),"no reroll");
                }
                check(count==16&&((Container)altar).getItem(0).isEmpty(),"conservation");
                ((Container)altar).setItem(0,unknown);check(!altar.identify()&&((Container)altar).getItem(0).getCount()==16,"full output rejects before consumption");
                var reload=new AppraisalBlockEntity(at,ModBlocks.APPRAISAL.get().defaultBlockState());((BlockEntity)reload).load(((BlockEntity)altar).saveWithoutMetadata());
                for(int q=0;q<=5;q++)check(ItemStack.matches(((Container)altar).getItem(q),((Container)reload).getItem(q)),"appraisal NBT roundtrip");
                ((Container)altar).clearContent();
            }
            check(materials>=82,"expanded material coverage");
            for(var metal:com.longju.xiantu.mining.MetalMaterialRegistry.ALL)for(String suffix:List.of("_raw","_ore","_ingot","_block")){
                var stack=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("xiantu",metal.id()+suffix)));check(!stack.isEmpty(),"metal exists");
                stack.getOrCreateTag().putInt(MaterialQuality.KEY,5);check(!MaterialQuality.eligible(stack)&&MaterialQuality.get(stack)==0&&MaterialQuality.manufacturingBonus(stack)==0,"metal hard exclusion");
            }
            RefiningProgress.learn(p,CraftingTreatises.REFINING_KEY);
            var r=RefiningRecipes.find("equipment_001_tier_1");var inputs=new ArrayList<ItemStack>();for(var input:r.inputs())inputs.add(MaterialQuality.set(input.example(),5));
            var used=RefiningRecipes.allocate(r,inputs);check(RefiningRecipes.materialQuality(inputs,used)==5,"neutral metal does not lower quality");
            inputs.add(MaterialQuality.set(r.inputs().get(1).example(),1));check(RefiningRecipes.allocate(r,inputs)==null,"mixed same-material quality rejected even with spare quantity");inputs.remove(inputs.size()-1);
            MaterialQuality.set(inputs.get(1),0);check(RefiningRecipes.materialQuality(inputs,RefiningRecipes.allocate(r,inputs))==0,"unknown consumed input removes bonus");
            MaterialQuality.set(inputs.get(1),3);check(RefiningRecipes.materialQuality(inputs,RefiningRecipes.allocate(r,inputs))==3,"minimum consumed quality");
            check(RefiningRules.qualityWeights(0,1,1,5)[0]==RefiningRules.qualityWeights(0,1,1,0)[0]-500,"materials transfer five percentage points");
            var split=new ArrayList<ItemStack>();for(var input:r.inputs())split.add(MaterialQuality.set(input.example(),5));
            var wood=split.get(1);wood.setCount(1);split.add(MaterialQuality.set(wood.copy(),1));
            check(RefiningRecipes.allocate(r,split)==null,"mixed split material rejected");MaterialQuality.set(split.get(split.size()-1),5);check(RefiningRecipes.allocate(r,split)!=null,"same-quality split stacks accepted");
            for(int i=0;i<r.inputs().size();i++)inv.setItem(i,MaterialQuality.set(r.inputs().get(i).example(),4));
            var furnaceState=WorkstationRegistry.EQUIPMENT.get("lifeng_refining_furnace").get().defaultBlockState();
            var furnace=new RefiningBlockEntity(at,furnaceState);((BlockEntity)furnace).setLevel(l);
            var before=inv.save(new ListTag());check(!furnace.start(p,r).isEmpty()&&before.equals(inv.save(new ListTag())),"no fuel no debit");
            furnace.energyMilli=500000;
            check(!furnace.start(p,r).isEmpty()&&before.equals(inv.save(new ListTag())),"backpack materials are not used");
            for(int i=0;i<r.inputs().size();i++)((Container)furnace).setItem(4+i,MaterialQuality.set(r.inputs().get(i).example(),4));
            check(furnace.start(p,r).contains("灵火")&&furnace.energyMilli==500000,"missing fire rejects without payment");
            ((Container)furnace).setItem(3,new ItemStack(SpiritFire.WONDROUS.get(0).get()));
            check(furnace.start(p,r).isEmpty(),"valid manual-input furnace start");
            check(furnace.energyMilli==400000&&before.equals(inv.save(new ListTag()))&&furnace.materials().stream().allMatch(ItemStack::isEmpty),"only furnace materials debited once");check(furnace.materialQuality==4&&furnace.fireBonus==150,"material and fire quality locked");
            check(!((Container)furnace).getItem(3).isEmpty()&&((Container)furnace).removeItem(3,1).isEmpty(),"fire locked while processing");
            check(!furnace.start(p,r).isEmpty()&&furnace.energyMilli==400000,"double start rejected");
            var restored=new RefiningBlockEntity(at,furnaceState);((BlockEntity)restored).load(((BlockEntity)furnace).saveWithoutMetadata());((BlockEntity)restored).setLevel(l);furnace=restored;
            check(furnace.busy()&&furnace.materialQuality==4&&furnace.energyMilli==400000,"paid job reload");
            furnace.elapsed=furnace.total-1;RefiningBlockEntity.tick(l,at,furnaceState,furnace);var blank=((Container)furnace).removeItem(2,1);check(blank.is(RefiningItems.BLANK.get())&&ledger.inspect(blank).getInt("Material")==4&&ledger.inspect(blank).getInt("FireBonus")==150,"portable authenticated blank");
            check(RefiningProgress.xp(p)==0,"melting does not grant final XP");
            var forgeState=WorkstationRegistry.EQUIPMENT.get("artisan_forging_table").get().defaultBlockState();var forge=new RefiningBlockEntity(at,forgeState);((BlockEntity)forge).setLevel(l);
            ((Container)forge).setItem(1,blank.copy());check(forge.start(p,null).isEmpty()&&forge.materialQuality==4&&forge.fireBonus==150,"forge keeps source material and fire quality");
            check(ledger.inspect(blank)==null,"copied blank rejected after reservation");
            var forgeReload=new RefiningBlockEntity(at,forgeState);((BlockEntity)forgeReload).load(((BlockEntity)forge).saveWithoutMetadata());((BlockEntity)forgeReload).setLevel(l);forge=forgeReload;
            var xpBefore=RefiningProgress.xp(p);forge.profession=9;forge.sourceFurnace=6;forge.elapsed=forge.total-1;
            RefiningBlockEntity.tick(l,at,forgeState,forge);var finished=((Container)forge).getItem(2);check(RefinedEquipment.recipe(finished)!=null,"finished equipment identity");
            check(RefiningProgress.xp(p)==xpBefore+10,"one successful craft XP");RefiningBlockEntity.tick(l,at,forgeState,forge);check(RefiningProgress.xp(p)==xpBefore+10&&((Container)forge).getItem(2).getCount()==1,"no double completion");
            check(!ledger.finish(blank.getTag().getString("Serial")),"ledger finished once");check(RefiningLedger.load(((net.minecraft.world.level.saveddata.SavedData)ledger).save(new CompoundTag())).inspect(blank)==null,"ledger reload rejects copy");
            for(int q=1;q<=5;q++){var equipment=RefinedEquipment.finish(r,q);check(equipment.getMaxDamage()==new int[]{2500,2875,3375,4000,5000}[q-1],"durability quality "+q);check(RefinedEquipment.recipe(ItemStack.of(equipment.save(new CompoundTag())))==r,"finished NBT persists");}
            var high=RefiningRecipes.ALL.stream().filter(v->v.tier()==5).findFirst().orElseThrow();check(!forge.eligibility(p,high).isEmpty(),"higher profession/realm gate");
            data.setRealmStage(4,0);p.getPersistentData().getCompound(RefiningProgress.KEY).putInt("Experience",36100);
            check(forge.eligibility(p,high).contains("器方"),"five tier blueprint required");RefiningProgress.learn(p,high.id());check(forge.eligibility(p,high).isEmpty(),"learned blueprint accepted");
            check(RefiningProgress.knows(p,high.id())&&!RefiningProgress.knows(p,r.id()),"blueprint identity is per equipment");
            check(RefiningRecipes.ALL.stream().filter(v->v.output().equals(r.output())).allMatch(v->v.tier()<=3&&!RefiningRules.blueprintRequired(v.tier())),"Qingshuang sword stops at tier three, no blueprint needed");
            var fireTest=new RefiningBlockEntity(at,furnaceState);((BlockEntity)fireTest).setLevel(l);
            var fuel=new ItemStack(ModItems.LING_JING.get());var stone=(com.longju.xiantu.item.SpiritStoneItem)fuel.getItem();long initialCharge=stone.chargeMilli(fuel);
            fireTest.energyMilli=999500;((Container)fireTest).setItem(0,fuel);
            check(fireTest.refuel()&&fireTest.energyMilli==1000000&&stone.chargeMilli(((Container)fireTest).getItem(0))==initialCharge-500,"auto-charge preserves exact remainder");
            check(!fireTest.refuel(),"full reservoir consumes nothing");
            check(((Container)fireTest).canPlaceItem(3,new ItemStack(SpiritFire.COMMON.get())),"ordinary fire accepted");
            var first=MaterialQuality.set(r.inputs().get(1).example(),1);((Container)fireTest).setItem(4,first);
            check(!((Container)fireTest).canPlaceItem(5,MaterialQuality.set(first.copy(),2)),"slot rejects mixed quality");
            check(SpiritFire.qualityBonus(1,1)==150&&SpiritFire.qualityBonus(1,5)==0&&SpiritFire.qualityBonus(6,6)==900,"rare fire target-tier attenuation");
            for(int fire=0;fire<=900;fire+=150)for(int lv=0;lv<=9;lv++){
                var weights=RefiningRules.qualityWeights(lv,6,5,5,fire);check(Arrays.stream(weights).sum()==10000&&Arrays.stream(weights).allMatch(v->v>=0),"fire quality probability normalization");
            }
            for(String id:RefiningBlockEntity.ALCHEMY){var state=WorkstationRegistry.EQUIPMENT.get(id).get().defaultBlockState();var cauldron=new RefiningBlockEntity(at,state);((BlockEntity)cauldron).setLevel(l);
                check(cauldron.alchemy()&&((Container)cauldron).canPlaceItem(3,new ItemStack(SpiritFire.COMMON.get())),"cauldron fire socket "+id);
                check(!cauldron.start(p,r).isEmpty(),"cauldron never manufactures equipment");}
            var consumable=new ItemStack(SpiritFire.COMMON.get());consumable.setDamageValue(consumable.getMaxDamage()-1);
            check(SpiritFire.burn(consumable,l)&&consumable.isEmpty(),"ordinary fire exhausted and removed");
            var rare=new ItemStack(SpiritFire.WONDROUS.get(0).get());rare.setDamageValue(100);
            rare.getOrCreateTag().putLong("FireRecoveryAt",l.getServer().overworld().getGameTime()-1000);
            SpiritFire.recover(rare,l);check(rare.getDamageValue()==90,"rare idle recovery uses elapsed ticks");
            rare.setDamageValue(rare.getMaxDamage()-1);SpiritFire.burn(rare,l);SpiritFire.recover(rare,l);check(rare.isEmpty(),"exhausted rare fire never regenerates");
            var ordinary=new ItemStack(SpiritFire.COMMON.get());ordinary.setDamageValue(100);ordinary.getOrCreateTag().putLong("FireRecoveryAt",-100000);SpiritFire.recover(ordinary,l);check(ordinary.getDamageValue()==100,"ordinary fire does not recover");
            inv.clearContent();data.resetSpiritRoots();data.addSpiritRoot(SpiritRootType.WATER);data.setPower(200);
            check(SpiritFire.condense(p).isEmpty()&&data.power()==100,"non-fire root can condense with exact power cost");
            var output=inv.items.stream().filter(v->v.is(SpiritFire.COMMON.get())).findFirst();check(output.isPresent()&&output.get().getDamageValue()==0,"condensed physical fire starts full");
            report.addProperty("status","PASS");report.addProperty("materials",materials);
        }catch(Throwable e){report.addProperty("status","FAIL");report.addProperty("error",e.toString());org.slf4j.LoggerFactory.getLogger(RefiningFlowAudit.class).error("Refining flow QA failed",e);}
        finally{
            inv.load(inventory);data.load(saved);p.getPersistentData().getAllKeys().stream().toList().forEach(p.getPersistentData()::remove);p.getPersistentData().merge(persistent);p.setGameMode(mode);
            // Restore only this synchronous fixture's ledger snapshot; no other world tick executes during the command.
            try{var field=RefiningLedger.class.getDeclaredField("jobs");field.setAccessible(true);field.set(ledger,ledgerBefore.getCompound("Jobs"));((net.minecraft.world.level.saveddata.SavedData)ledger).setDirty();}catch(Exception e){throw new RuntimeException(e);}
            inv.setChanged();p.inventoryMenu.broadcastChanges();CultivationEvents.sync(p);
        }
        report.addProperty("checks",checks);try{var file=p.server.getServerDirectory().toPath().resolve("xiantu-qa/refining-flow.json");java.nio.file.Files.createDirectories(file.getParent());java.nio.file.Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){throw new RuntimeException(e);}
        p.sendSystemMessage(Component.literal("炼器流程测试 "+report.get("status").getAsString()+"："+checks+" 项"));return report.get("status").getAsString().equals("PASS")?1:0;
    }));}
}
