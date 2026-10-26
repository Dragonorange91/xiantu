package com.longju.xiantu.qa;

import com.longju.xiantu.alchemy.*;
import com.longju.xiantu.refining.*;
import com.longju.xiantu.workstation.*;
import com.longju.xiantu.trade.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Explicit, reversible GUI fixture. Restore before leaving the QA world. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class ManufacturingPreview {
    private record Backup(Vec3 position,float yaw,float pitch,ListTag inventory,BlockPos origin,Map<BlockPos,BlockState> blocks,List<UUID> entities,CompoundTag cultivation,CompoundTag persistent){}
    private static final Map<UUID,Backup> ACTIVE=new HashMap<>();
    private static RefiningBlockEntity place(ServerPlayer p,BlockPos at,String id){
        var b=(WorkstationBlock)WorkstationRegistry.EQUIPMENT.get(id).get();var state=((Block)b).defaultBlockState();
        WorkstationBlock.replaceAssembly(()->{for(var part:b.partProperty().getPossibleValues())p.serverLevel().setBlock(b.position(at,part,Direction.NORTH),state.setValue(b.partProperty(),part),2);});
        return (RefiningBlockEntity)p.serverLevel().getBlockEntity(at);
    }
    private static void restore(ServerPlayer p){
        var saved=ACTIVE.remove(p.getUUID());if(saved==null)return;p.closeContainer();
        for(var id:saved.entities()){var entity=p.serverLevel().getEntity(id);if(entity!=null)entity.discard();}
        WorkstationBlock.replaceAssembly(()->saved.blocks().forEach((pos,state)->p.serverLevel().setBlock(pos,state,2)));
        p.getPersistentData().getAllKeys().clear();p.getPersistentData().merge(saved.persistent());((com.longju.xiantu.cultivation.PlayerCultivationData)com.longju.xiantu.gongfa.GongfaService.data(p)).load(saved.cultivation());com.longju.xiantu.gongfa.GongfaService.refresh(p);com.longju.xiantu.cultivation.CultivationEvents.sync(p);p.getInventory().load(saved.inventory());p.inventoryMenu.broadcastChanges();
        p.teleportTo(p.serverLevel(),saved.position().x,saved.position().y,saved.position().z,saved.yaw(),saved.pitch());
    }
    @SubscribeEvent public static void register(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantumanufacturingpreview").then(Commands.argument("mode",StringArgumentType.word()).executes(c->{
            var p=c.getSource().getPlayerOrException();
            if(!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")){c.getSource().sendFailure(Component.literal("仅限Xiantu-QA测试存档"));return 0;}
            String mode=StringArgumentType.getString(c,"mode");
            if(!List.of("smelting","alchemy","alchemy_busy","refining","forging","skills","skills_inventory","skills_none","smith","blueprint","repair","sword","axe","spear","staff","restore").contains(mode))return 0;
            restore(p);if(mode.equals("restore"))return 1;
            var at=p.blockPosition().offset(240,300-p.blockPosition().getY(),240);var blocks=new LinkedHashMap<BlockPos,BlockState>();
            for(int x=-1;x<=3;x++)for(int y=-1;y<=2;y++)for(int z=-1;z<=4;z++){var pos=at.offset(x,y,z);p.serverLevel().getChunkAt(pos);var state=p.serverLevel().getBlockState(pos);if(!state.isAir())throw new IllegalStateException("预览场地不是空地");blocks.put(pos,state);}
            var savedData=new CompoundTag();((com.longju.xiantu.cultivation.PlayerCultivationData)com.longju.xiantu.gongfa.GongfaService.data(p)).save(savedData);
            var backup=new Backup(p.position(),p.getYRot(),p.getXRot(),p.getInventory().save(new ListTag()),at,blocks,new ArrayList<>(),savedData,p.getPersistentData().copy());ACTIVE.put(p.getUUID(),backup);
            try{
                p.closeContainer();for(int x=-1;x<=3;x++)for(int z=-1;z<=4;z++)p.serverLevel().setBlock(at.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
                p.teleportTo(p.serverLevel(),at.getX()+.5,300,at.getZ()+3.5,180,0);
                if(mode.equals("smelting")){
                    p.serverLevel().setBlock(at,((Block)com.longju.xiantu.registry.ModBlocks.SMELTING_FURNACE.get()).defaultBlockState(),2);
                    var be=(com.longju.xiantu.smelting.SmeltingBlockEntity)p.serverLevel().getBlockEntity(at);
                    RefiningProgress.learn(p,com.longju.xiantu.smelting.SmeltingRecipes.KNOWLEDGE);
                    var recipe=com.longju.xiantu.smelting.SmeltingRecipes.ALL.get(0);
                    ((Container)be).setItem(0,new ItemStack(com.longju.xiantu.mining.MetalMaterialRegistry.RAW_ORES.get(recipe.metal()).get(),2));
                    ((Container)be).setItem(1,new ItemStack(com.longju.xiantu.registry.ModItems.FAN_LING_SHI.get()));
                    NetworkHooks.openScreen(p,be,b->b.writeBlockPos(at));
                }else if(mode.equals("alchemy")||mode.equals("alchemy_busy")){
                    var be=place(p,at,"qingmu_alchemy_furnace");var r=AlchemyRecipes.ALL.get(0);
                    for(int i=0;i<r.inputs().size();i++)((Container)be).setItem(4+i,r.inputs().get(i).example());
                    ((Container)be).setItem(3,new ItemStack(SpiritFire.COMMON.get()));be.energyMilli=1000000;
                    NetworkHooks.openScreen(p,new SimpleMenuProvider((id,inv,title)->new AlchemyMenu(id,inv,be),Component.literal("一阶青木丹炉")),b->b.writeBlockPos(at));
                    if(mode.equals("alchemy_busy")){var d=com.longju.xiantu.gongfa.GongfaService.data(p);d.setCultivationUnlocked(true);d.setRealmStage(8,0);RefiningProgress.learn(p,r.knowledge());String error=AlchemyJob.start(be,p,r);if(!error.isEmpty())throw new IllegalStateException(error);com.longju.xiantu.cultivation.CultivationEvents.sync(p);}
                }else if(mode.equals("refining")||mode.equals("forging")){
                    boolean forge=mode.equals("forging");
                    var be=place(p,at,forge?"artisan_forging_table":"lifeng_refining_furnace");
                    if(!forge){
                        var r=RefiningRecipes.find("equipment_001_tier_1");for(int i=0;i<r.inputs().size();i++)((Container)be).setItem(4+i,r.inputs().get(i).example());
                        ((Container)be).setItem(3,new ItemStack(SpiritFire.COMMON.get()));be.energyMilli=1000000;
                    }
                    NetworkHooks.openScreen(p,new SimpleMenuProvider((id,inv,title)->new RefiningMenu(id,inv,be,at,forge),Component.literal(forge?"锻打台":"一阶砺锋炉")),b->{b.writeBlockPos(at);b.writeBoolean(forge);b.writeBoolean(false);});
                }else if(List.of("sword","axe","spear","staff").contains(mode)){
                    var d=com.longju.xiantu.gongfa.GongfaService.data(p);d.setCultivationUnlocked(true);d.setRealmStage(8,0);
                    var r=mode.equals("staff")?RefiningRecipes.definition("jingu_staff"):RefiningRecipes.ALL.stream().filter(recipe->recipe.kind().equals(mode)).findFirst().orElseThrow();
                    for(var slot:EquipmentSlot.values())if(slot.getType()==EquipmentSlot.Type.ARMOR)((LivingEntity)p).setItemSlot(slot,ItemStack.EMPTY);
                    ((LivingEntity)p).setItemInHand(InteractionHand.MAIN_HAND,RefinedEquipment.finish(r,1));
                    ((LivingEntity)p).setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                    com.longju.xiantu.gongfa.GongfaService.refresh(p);com.longju.xiantu.cultivation.CultivationEvents.sync(p);
                    p.inventoryMenu.broadcastChanges();
                }else if(mode.startsWith("skills")){
                    var d=com.longju.xiantu.gongfa.GongfaService.data(p);d.setCultivationUnlocked(true);d.setRealmStage(3,0);
                    d.gongfa().clear();d.medicine().clearTemporary();
                    p.getInventory().clearContent();p.getPersistentData().remove(QitianSkills.KEY);
                    p.getPersistentData().putLong("xiantu_equipment_last_combat",com.longju.xiantu.spell.SpellService.clock()-1000);
                    int index=0;for(var id:List.of("fengchi_crown","suozi_chestplate","liujin_leggings","ousi_boots")){
                        var stack=RefinedEquipment.finish(RefiningRecipes.definition(id),1);
                        if(mode.equals("skills_inventory"))p.getInventory().setItem(index++,stack);
                        else if(!mode.equals("skills_none"))p.setItemSlot(((ArmorItem)stack.getItem()).getEquipmentSlot(),stack);
                    }
                    var found=new CompoundTag();found.putBoolean("yanwei_fox",true);found.putBoolean("thunder_feather_raven",true);QitianSkills.state(p).put("Discovered",found);
                    com.longju.xiantu.gongfa.GongfaService.refresh(p);RefinedEquipment.refreshEligibility(p);EquipmentEffects.refresh(p);
                    d.setPowerMax(3000);d.setPower(3000);d.setPowerSpeed(0);
                    com.longju.xiantu.cultivation.CultivationEvents.sync(p);QitianSkills.sync(p);p.inventoryMenu.broadcastChanges();
                    if(mode.equals("skills"))EquipmentSkillMenu.open(p);
                }else if(mode.equals("repair")){
                    var d=com.longju.xiantu.gongfa.GongfaService.data(p);d.setCultivationUnlocked(true);d.setRealmStage(3,0);d.setPowerMax(100000);d.setPower(100000);com.longju.xiantu.cultivation.CultivationEvents.sync(p);
                    var repairBlock=(WorkstationBlock)EquipmentRepairBlock.BLOCK.get();
                    WorkstationBlock.replaceAssembly(()->{for(var part:repairBlock.partProperty().getPossibleValues())p.serverLevel().setBlock(repairBlock.position(at,part,Direction.NORTH),((Block)repairBlock).defaultBlockState().setValue(repairBlock.partProperty(),part),2);});
                    EquipmentRepairMenu.open(p,at);
                    var menu=(EquipmentRepairMenu)p.containerMenu;
                    var recipe=RefiningRecipes.ALL.stream().filter(r->r.theme().equals("青霜剑")&&r.tier()==1).findFirst().orElseThrow();
                    var sword=RefinedEquipment.finish(recipe,3);sword.setDamageValue(sword.getMaxDamage()*3/4);
                    ((Container)menu.inputs).setItem(0,sword);((Container)menu.inputs).setItem(1,new ItemStack(com.longju.xiantu.mining.MetalMaterialRegistry.INGOTS.get(com.longju.xiantu.mining.MetalMaterialRegistry.ALL.get(0).id()).get(),2));
                    ((AbstractContainerMenu)menu).broadcastChanges();
                }else if(mode.equals("smith")){
                    var merchant=(TownMerchantEntity)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("xiantu:smith_merchant")).create(p.serverLevel());
                    ((Entity)merchant).moveTo(at.getX()+.5,300,at.getZ()+1.5,0,0);p.serverLevel().addFreshEntity(merchant);backup.entities().add(((Entity)merchant).getUUID());TownMerchantMenu.open(p,merchant);
                }else{
                    ((LivingEntity)p).setItemInHand(InteractionHand.MAIN_HAND,RefiningItems.facilityDiagram("forging_table"));BlueprintLearningMenu.open(p,InteractionHand.MAIN_HAND);
                }
                return 1;
            }catch(Exception|LinkageError failure){restore(p);throw failure;}
        })));
    }
    private ManufacturingPreview(){}
}
