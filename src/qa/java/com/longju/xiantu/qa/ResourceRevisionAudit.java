package com.longju.xiantu.qa;

import com.google.gson.GsonBuilder;
import com.longju.xiantu.medicine.*;
import com.longju.xiantu.mining.SpecialMineralRegistry;
import com.longju.xiantu.item.BeastHideItem;
import com.longju.xiantu.terrain.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import java.util.*;

@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class ResourceRevisionAudit {
    private static int checks;
    @SubscribeEvent public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("xianturesourceaudit").requires(s->s.hasPermission(2)).executes(c->{
            var source=c.getSource();var level=source.getLevel();
            if(!level.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")) return 0;
            checks=0;Map<BlockPos,BlockState> before=new LinkedHashMap<>();
            var player=FakePlayerFactory.getMinecraft(level);var inventory=player.getInventory().save(new net.minecraft.nbt.ListTag());
            var mode=player.gameMode.getGameModeForPlayer();var position=player.position();
            try {
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                BlockPos base=source.getPlayerOrException().blockPosition().offset(0,8,12);
                for(int x=-1;x<8;x++)for(int y=-1;y<3;y++)for(int z=-1;z<3;z++){
                    var p=base.offset(x,y,z);require(level.getBlockEntity(p)==null,"empty fixture site");before.put(p,level.getBlockState(p));level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
                }
                var soil=base.below();level.setBlock(soil,MedicineRegistry.FARMLAND_BLOCKS.get(6).get().defaultBlockState(),3);
                level.setBlock(soil.east(),Blocks.WATER.defaultBlockState(),3);
                ((net.minecraft.world.entity.Entity)player).setPos(base.getX()+.5,base.getY(),base.getZ()-2);
                var seed=MedicineRegistry.seed("herb_0058");var stack=new ItemStack(seed,3);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
                var hit=new BlockHitResult(Vec3.atCenterOf(soil),Direction.UP,soil,false);
                Item seedItem=seed;
                require(seedItem.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"seed plants successfully");
                require(stack.getCount()==2,"one seed consumed");
                var crop=MedicineRegistry.CROPS.get("herb_0058").get();Block cropBlock=crop;
                require(level.getBlockState(base).is(crop)&&level.getBlockState(base).getValue(MedicineCropBlock.AGE)==0,"fresh seed age zero");
                seedItem.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
                require(stack.getCount()==2&&level.getBlockState(base).is(crop),"occupied crop not overwritten or charged");
                level.setBlock(base,Blocks.STONE.defaultBlockState(),3);
                seedItem.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
                require(stack.getCount()==2&&level.getBlockState(base).is(Blocks.STONE),"solid block not overwritten");
                level.setBlock(base,Blocks.AIR.defaultBlockState(),3);
                var down=new BlockHitResult(Vec3.atCenterOf(soil),Direction.DOWN,soil,false);
                require(!seedItem.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,down)).consumesAction(),"underside rejected");
                for(var d:MedicineCatalog.HERBS)if(d.plantable()){
                    var block=MedicineRegistry.CROPS.get(d.registryId()).get();double previous=0;
                    for(int age=0;age<4;age++){
                        var state=block.defaultBlockState().setValue(MedicineCropBlock.AGE,age);
                        level.setBlock(base,state,3);
                        var shape=state.getShape(level,base,CollisionContext.empty());
                        require(shape.max(Direction.Axis.Y)>previous,"increasing selection height "+d.registryId()+" age "+age);
                        require(state.canSurvive(level,base),"crop survives correct farmland");previous=shape.max(Direction.Axis.Y);
                    }
                }
                level.setBlock(base,crop.defaultBlockState(),3);
                var random=net.minecraft.util.RandomSource.create(20261020);
                for(int i=0;i<200&&level.getBlockState(base).getValue(MedicineCropBlock.AGE)<3;i++)
                    crop.randomTick(level.getBlockState(base),level,base,random);
                require(level.getBlockState(base).getValue(MedicineCropBlock.AGE)==3,"random ticks reach maturity");
                cropBlock.use(level.getBlockState(base),level,base,player,InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(base),Direction.UP,base,false));
                require(level.getBlockState(base).getValue(MedicineCropBlock.AGE)==0,"harvest resets to seedling");
                for(var m:SpecialMineralRegistry.ALL){
                    var state=SpecialMineralRegistry.ORES.get(m.id()).get().defaultBlockState();
                    require(!new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(state),"wood rejects "+m.id());
                    require(!new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state),"stone rejects "+m.id());
                    require(new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(state),"iron accepts "+m.id());
                    require(state.getDestroySpeed(level,base)==3F,"hardness three "+m.id());
                    for(int enchant=0;enchant<3;enchant++){
                        var tool=new ItemStack(Items.DIAMOND_PICKAXE);
                        if(enchant==1)tool.enchant(Enchantments.BLOCK_FORTUNE,3);
                        if(enchant==2)tool.enchant(Enchantments.SILK_TOUCH,1);
                        var drops=Block.getDrops(state,level,base,null,player,tool);
                        require(drops.size()==1&&drops.get(0).is(SpecialMineralRegistry.MATERIALS.get(m.id()).get())&&drops.get(0).getCount()==1,"one material including fortune/silk "+m.id());
                        require(!com.longju.xiantu.material.MaterialQuality.eligible(drops.get(0)),"no appraisal "+m.id());
                    }
                }
                for(int realm=0;realm<=8;realm++)for(String element:com.longju.xiantu.item.DemonCoreItem.ELEMENTS){
                    var hide=BeastHideItem.fromRealm(realm,element);
                    require(BeastHideItem.tier(hide).equals(realm<=1?"basic":realm<=3?"intermediate":"advanced"),"realm hide band");
                    require(!hide.getTag().contains("beast_grade")&&hide.getTag().getString("beast_element").equals(element),"hide separate tier and six elements");
                }
                require(BeastHideItem.fromRealm(-1,"fire").isEmpty(),"unknown realm rejected");
                // Isolated event fixtures use explicit source metadata; these do not claim natural spawning exists.
                var fox=com.longju.xiantu.registry.ModEntities.YANWEI_FOX.get().create(level);
                net.minecraft.world.entity.LivingEntity foxEntity=fox;
                var damage=level.damageSources().playerAttack(player);
                var loot=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();
                foxEntity.getPersistentData().putInt(com.longju.xiantu.entity.BeastResourceDrops.REALM,3);
                com.longju.xiantu.entity.BeastResourceDrops.damaged(new net.minecraftforge.event.entity.living.LivingDamageEvent(foxEntity,damage,1));
                com.longju.xiantu.entity.BeastResourceDrops.drops(new net.minecraftforge.event.entity.living.LivingDropsEvent(foxEntity,damage,loot,3,true));
                require(loot.isEmpty(),"command/egg entity has no resources");
                foxEntity.getPersistentData().putBoolean(com.longju.xiantu.entity.BeastResourceDrops.NATURAL,true);
                foxEntity.getPersistentData().remove(com.longju.xiantu.entity.BeastResourceDrops.REALM);
                com.longju.xiantu.entity.BeastResourceDrops.drops(new net.minecraftforge.event.entity.living.LivingDropsEvent(foxEntity,damage,loot,3,true));
                require(loot.isEmpty(),"unknown realm has no guessed hide");
                foxEntity.getPersistentData().putInt(com.longju.xiantu.entity.BeastResourceDrops.REALM,3);
                com.longju.xiantu.entity.BeastResourceDrops.drops(new net.minecraftforge.event.entity.living.LivingDropsEvent(foxEntity,level.damageSources().fall(),loot,3,true));
                require(loot.isEmpty(),"environmental death rejected");
                com.longju.xiantu.entity.BeastResourceDrops.drops(new net.minecraftforge.event.entity.living.LivingDropsEvent(foxEntity,damage,loot,3,true));
                require(loot.size()>=4&&loot.size()<=5,"fixed core/main/blood/exclusive plus at most one auxiliary");
                var droppedHide=loot.stream().map(net.minecraft.world.entity.item.ItemEntity::getItem).filter(s->s.is(com.longju.xiantu.registry.ModItems.BEAST_HIDE.get())).findFirst().orElseThrow();
                require(BeastHideItem.tier(droppedHide).equals("intermediate"),"victim realm selects hide");
                for(var item:loot)require(item.getItem().getCount()==1&&item.getPersistentData().hasUUID("xiantu_loot_owner"),"single item and pickup owner");
                int firstCount=loot.size();
                com.longju.xiantu.entity.BeastResourceDrops.drops(new net.minecraftforge.event.entity.living.LivingDropsEvent(foxEntity,damage,loot,3,true));
                require(loot.size()==firstCount,"death settled exactly once");
                var counts=new TreeMap<String,Integer>();
                for(var pair:new terrablender.api.ModifiedVanillaOverworldBuilder().build()){
                    for(var theme:List.of(TerrainBiomes.THUNDER_WASTES,TerrainBiomes.METEOR_WASTELAND,TerrainBiomes.SPIRIT_MEDICINE_PLAIN)){
                        var result=XiantuTerrainRegion.replacement(pair.getFirst(),pair.getSecond(),theme);
                        if(!result.equals(pair.getSecond()))counts.merge(result.location().toString(),1,Integer::sum);
                    }
                }
                for(String id:List.of("xiantu:thunder_wastes","xiantu:meteor_wasteland","xiantu:spirit_medicine_plain"))require(counts.getOrDefault(id,0)>0,"reachable "+id);
                var report=Map.of("checks",checks,"completeClimateCells",counts,"seed",level.getSeed());
                var path=FMLPaths.GAMEDIR.get().resolve("xiantu-resource-audit.json");java.nio.file.Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));
                source.sendSuccess(()->Component.literal("Resource revision audit PASS "+checks+" "+counts),false);return checks;
            } catch(Exception|AssertionError ex){source.sendFailure(Component.literal("Resource audit FAIL after "+checks+": "+ex));ex.printStackTrace();return 0;}
            finally {
                before.forEach((pos,state)->level.setBlock(pos,state,3));
                player.getInventory().load(inventory);player.setGameMode(mode);((net.minecraft.world.entity.Entity)player).setPos(position.x,position.y,position.z);
            }
        }));
    }
    private static void require(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
}
