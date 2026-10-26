package com.longju.xiantu.qa;

import com.longju.xiantu.smelting.*;
import com.longju.xiantu.refining.*;
import com.longju.xiantu.registry.*;
import com.longju.xiantu.mining.MetalMaterialRegistry;
import com.longju.xiantu.item.SpiritStoneItem;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Real registry, menu, persistence and transaction audit; only disposable QA saves. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class SmeltingAudit {
    private static int checks;
    private static void check(boolean ok,String what){checks++;if(!ok)throw new IllegalStateException(what);}
    private static SmeltingBlockEntity place(ServerLevel l,BlockPos at){
        if(l.getBlockEntity(at) instanceof SmeltingBlockEntity old){old.discardJob();((Container)old).clearContent();}
        l.setBlock(at,Blocks.AIR.defaultBlockState(),2);
        l.setBlock(at,((Block)ModBlocks.SMELTING_FURNACE.get()).defaultBlockState(),2);
        return (SmeltingBlockEntity)l.getBlockEntity(at);
    }
    private static SmeltingBlockEntity load(ServerLevel l,BlockPos at,CompoundTag n){
        BlockEntity be=new SmeltingBlockEntity(at,l.getBlockState(at));be.load(n);l.setBlockEntity(be);return (SmeltingBlockEntity)be;
    }
    private static SmeltingBlockEntity energy(ServerLevel l,SmeltingBlockEntity be,long amount){
        var n=((BlockEntity)be).saveWithoutMetadata();n.putLong("EnergyMilli",amount);return load(l,((BlockEntity)be).getBlockPos(),n);
    }
    private static void tick(ServerLevel l,SmeltingBlockEntity be){var at=((BlockEntity)be).getBlockPos();SmeltingBlockEntity.tick(l,at,l.getBlockState(at),be);}
    private static ItemStack raw(SmeltingRecipes.Recipe r,int count){return new ItemStack(MetalMaterialRegistry.RAW_ORES.get(r.metal()).get(),count);}
    @SubscribeEvent public static void register(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantusmeltingaudit").executes(c->{
        var p=c.getSource().getPlayerOrException();var l=p.serverLevel();
        if(!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        checks=0;var persistent=p.getPersistentData().copy();var inventory=p.getInventory().save(new ListTag());var origin=p.position();float yaw=p.getYRot(),pitch=p.getXRot();
        var at=new BlockPos(p.chunkPosition().getMinBlockX()+2,300,p.chunkPosition().getMinBlockZ()+2);
        var old=l.getBlockState(at);if(!old.isAir())throw new IllegalStateException("QA场地不是空地");
        var box=new AABB(at).inflate(2);var originalDrops=new HashSet<UUID>();for(var item:l.getEntitiesOfClass(ItemEntity.class,box))originalDrops.add(item.getUUID());
        try{
            p.closeContainer();p.teleportTo(l,at.getX()+.5,300,at.getZ()+2.5,180,0);p.getPersistentData().remove(RefiningProgress.KEY);
            var be=place(l,at);var r=SmeltingRecipes.ALL.get(0);var inv=(Container)be;
            inv.setItem(0,raw(r,2));be=energy(l,be,1_000_000);inv=be;var before=((BlockEntity)be).saveWithoutMetadata();
            check(!be.start(p).isEmpty()&&before.equals(((BlockEntity)be).saveWithoutMetadata()),"unlearned start is atomic/no debit");
            check(!((AbstractContainerMenu)new SmeltingMenu(0,p.getInventory(),be,at)).getSlot(0).mayPlace(raw(r,1)),"unlearned player cannot input");
            p.containerMenu=new CraftingMenu(0,p.getInventory());var grid=(CraftingContainer)p.containerMenu.getSlot(1).container;
            var craft=(ShapelessRecipe)l.getRecipeManager().byKey(new ResourceLocation("xiantu:smelting_furnace")).orElseThrow();
            int cell=0;for(var ingredient:craft.getIngredients())grid.setItem(cell++,ingredient.getItems()[0].copy());
            check(!craft.matches(grid,l),"real crafting manager blocks unknown facility");
            var paper=RefiningItems.facilityDiagram("smelting_furnace");check(RefiningItems.knowledge(paper).equals(SmeltingRecipes.KNOWLEDGE),"facility paper uses shared knowledge key");
            ((net.minecraft.world.entity.LivingEntity)p).setItemInHand(InteractionHand.MAIN_HAND,paper);
            p.containerMenu=new BlueprintLearningMenu(0,p.getInventory(),InteractionHand.MAIN_HAND);
            check(((AbstractContainerMenu)p.containerMenu).clickMenuButton(p,0)&&paper.isEmpty(),"real paper confirmation consumes once");
            p.containerMenu=new CraftingMenu(0,p.getInventory());grid=(CraftingContainer)p.containerMenu.getSlot(1).container;cell=0;for(var ingredient:craft.getIngredients())grid.setItem(cell++,ingredient.getItems()[0].copy());
            check(craft.matches(grid,l)&&craft.getResultItem(l.registryAccess()).is(ModBlockItems.SMELTING_FURNACE.get()),"learned starter workbench produces furnace");
            check(com.longju.xiantu.trade.TownTradeService.OFFERS.stream().filter(o->o.kind().equals("facility")&&o.key().equals("smelting_furnace")).anyMatch(o->o.shop().equals("smith_merchant")&&o.rank()==1&&o.price()==3&&o.purchases()==0),"starter merchant source has no purchase gate");
            for(var material:MetalMaterialRegistry.ALL)if(material.tier()<=2)check(SmeltingRecipes.find(new ItemStack(MetalMaterialRegistry.RAW_ORES.get(material.id()).get()))==null,"low ore stays vanilla");
            for(var rejected:List.of(new ItemStack(Items.COAL),new ItemStack(Items.IRON_INGOT),r.output(),new ItemStack(MetalMaterialRegistry.ORES.get(r.metal()).get())))check(!((Container)be).canPlaceItem(0,rejected),"reject non-raw input");
            var partial=new ItemStack(ModItems.FAN_LING_SHI.get());((SpiritStoneItem)partial.getItem()).setChargeMilli(partial,99000);inv=be;inv.setItem(1,partial);
            check(!((Container)be).canPlaceItem(1,partial)&&!be.charge()&&partial.getCount()==1,"partly used spirit stone never charged");
            inv.setItem(1,new ItemStack(ModItems.FAN_LING_SHI.get(),2));be=energy(l,be,SmeltingBlockEntity.CAPACITY-99_000);inv=be;
            check(!be.charge()&&inv.getItem(1).getCount()==2,"overflow leaves whole stone intact");
            be=energy(l,be,SmeltingBlockEntity.CAPACITY-100_000);inv=be;check(be.charge()&&be.energyMilli()==SmeltingBlockEntity.CAPACITY&&inv.getItem(1).getCount()==1,"exact capacity accepts one stone");
            check(!be.charge()&&inv.getItem(1).getCount()==1,"full capacity cannot consume next stone");
            be=place(l,at);inv=be;inv.setItem(1,new ItemStack(ModItems.DAO_SHI.get()));check(be.charge()&&be.energyMilli()==299_000_000&&inv.getItem(1).isEmpty(),"top stone deposits its complete value");
            var stranger=FakePlayerFactory.get(l,new GameProfile(UUID.fromString("be762e76-a376-4ff8-b09f-b9ece33b3b41"),"SmeltingQA"));
            var strangerData=stranger.getPersistentData().copy();
            try{
                RefiningProgress.learn(stranger,SmeltingRecipes.KNOWLEDGE);
                for(var recipe:SmeltingRecipes.ALL){
                    be=place(l,at);be=energy(l,be,10_000_000);inv=be;var input=raw(recipe,2);input.getOrCreateTag().putInt("Quality",4);inv.setItem(0,input);
                    long initial=be.energyMilli();check(be.start(p).isEmpty(),"legal raw starts "+recipe.metal());check(inv.getItem(0).getCount()==1&&be.energyMilli()==initial-recipe.power()*1000L,"exactly one input/fee locked");
                    var started=((BlockEntity)be).saveWithoutMetadata();check(!be.start(p).isEmpty()&&started.equals(((BlockEntity)be).saveWithoutMetadata()),"double start no debit");
                    AbstractContainerMenu menu=new SmeltingMenu(0,p.getInventory(),be,at);check(!menu.getSlot(0).mayPickup(p)&&!menu.getSlot(0).mayPlace(raw(recipe,1))&&!menu.getSlot(2).mayPickup(stranger),"active input/output locked");
                    check(!be.cancel(stranger),"foreign cancellation rejected");
                    for(int t=0;t<7;t++)tick(l,be);be=load(l,at,((BlockEntity)be).saveWithoutMetadata());inv=be;check(be.elapsed()==7&&be.busy(),"saved work resumes exact tick");
                    for(int t=7;t<recipe.ticks()-1;t++)tick(l,be);check(inv.getItem(2).isEmpty()&&be.busy(),"not instant and no early product");
                    tick(l,be);var out=inv.getItem(2);check(!be.busy()&&out.is(recipe.output().getItem())&&out.getCount()==1&&!out.hasTag(),"one clean ingot at due tick");
                    tick(l,be);check(inv.getItem(2).getCount()==1&&be.energyMilli()==initial-recipe.power()*1000L,"no replay and no per-tick second fee");
                    menu=new SmeltingMenu(0,p.getInventory(),be,at);check(menu.getSlot(2).mayPickup(p)&&!menu.getSlot(2).mayPickup(stranger)&&!be.start(stranger).isEmpty(),"completed product remains with owner");
                    inv.removeItem(2,1);be.outputTaken();check(be.start(stranger).isEmpty()&&be.cancel(stranger),"another learned player may use empty furnace");
                }
            }finally{stranger.getPersistentData().getAllKeys().stream().toList().forEach(stranger.getPersistentData()::remove);stranger.getPersistentData().merge(strangerData);}
            be=place(l,at);inv=be;inv.setItem(0,raw(r,1));be=energy(l,be,r.power()*1000L-1);var insufficient=((BlockEntity)be).saveWithoutMetadata();check(!be.start(p).isEmpty()&&insufficient.equals(((BlockEntity)be).saveWithoutMetadata()),"insufficient energy consumes nothing");
            be=energy(l,be,SmeltingBlockEntity.CAPACITY);inv=be;check(be.start(p).isEmpty(),"full refund boundary starts");inv.setItem(1,new ItemStack(ModItems.FAN_LING_SHI.get()));check(!be.charge(),"busy charge reserves refund headroom");tick(l,be);
            be=load(l,at,((BlockEntity)be).saveWithoutMetadata());check(be.cancel(p)&&be.energyMilli()==SmeltingBlockEntity.CAPACITY&&((Container)be).getItem(0).getCount()==1,"cancel after load refunds one raw and exact fee");before=((BlockEntity)be).saveWithoutMetadata();check(!be.cancel(p)&&before.equals(((BlockEntity)be).saveWithoutMetadata()),"repeated cancel cannot mint resources");
            be=place(l,at);be=energy(l,be,100_000);inv=be;inv.setItem(0,raw(r,1));check(be.start(p).isEmpty(),"offline fixture starts");var saved=((BlockEntity)be).saveWithoutMetadata();saved.putUUID("Owner",UUID.randomUUID());be=load(l,at,saved);tick(l,be);check(be.elapsed()==0,"offline owner never advances");
            saved.putUUID("Owner",p.getUUID());be=load(l,at,saved);inv=be;inv.setItem(2,new ItemStack(Items.DIRT,64));tick(l,be);check(be.elapsed()==0,"blocked output freezes completion");
            check(!be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent(),"no unsided automation handler");
            for(var direction:Direction.values())check(((WorldlyContainer)be).getSlotsForFace(direction).length==0&&!be.getCapability(ForgeCapabilities.ITEM_HANDLER,direction).isPresent()&&!((WorldlyContainer)be).canTakeItemThroughFace(2,r.output(),direction)&&!((WorldlyContainer)be).canPlaceItemThroughFace(0,raw(r,1),direction),"sided automation rejected "+direction);
            check(!((Container)be).canTakeItem(new SimpleContainer(1),2,r.output()),"unsided vanilla extraction rejected");
            // Real breaking path: one active input is forfeited; idle items and output survive.
            be=place(l,at);be=energy(l,be,100_000);inv=be;inv.setItem(0,raw(r,2));check(be.start(p).isEmpty(),"break fixture starts");inv.setItem(1,new ItemStack(ModItems.DAO_SHI.get()));
            l.destroyBlock(at,true);check(!be.busy()&&be.energyMilli()==0,"removed furnace discards work and energy");
            var drops=l.getEntitiesOfClass(ItemEntity.class,box).stream().filter(i->!originalDrops.contains(i.getUUID())).toList();
            check(drops.stream().filter(i->r.matches(i.getItem())).mapToInt(i->i.getItem().getCount()).sum()==1,"only idle raw drops, active raw lost");check(drops.stream().anyMatch(i->i.getItem().is(ModBlockItems.SMELTING_FURNACE.get()))&&drops.stream().anyMatch(i->i.getItem().is(ModItems.DAO_SHI.get())),"furnace itself and idle stone drop");
            for(var item:drops)((net.minecraft.world.entity.Entity)item).discard();
            be=place(l,at);inv=be;inv.setItem(2,r.output());l.destroyBlock(at,true);check(l.getEntitiesOfClass(ItemEntity.class,box).stream().anyMatch(i->i.getItem().is(r.output().getItem())),"finished output drops on break");
            check(!RefiningProgress.data(p).contains("Experience")||RefiningProgress.xp(p)==0,"smelting never grants profession XP");
            c.getSource().sendSuccess(()->Component.literal("[Smelting QA] PASS "+checks+" assertions"),false);return 1;
        }catch(Exception|LinkageError ex){c.getSource().sendFailure(Component.literal("[Smelting QA] FAIL "+checks+": "+ex));throw ex;}
        finally{
            p.containerMenu=p.inventoryMenu;l.setBlock(at,old,2);for(var item:l.getEntitiesOfClass(ItemEntity.class,box))if(!originalDrops.contains(item.getUUID()))((net.minecraft.world.entity.Entity)item).discard();
            p.getInventory().load(inventory);p.getPersistentData().getAllKeys().stream().toList().forEach(p.getPersistentData()::remove);p.getPersistentData().merge(persistent);p.teleportTo(l,origin.x,origin.y,origin.z,yaw,pitch);p.inventoryMenu.broadcastChanges();
        }
    }));}
}
