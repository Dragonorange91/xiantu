package com.longju.xiantu.qa;

import com.longju.xiantu.wood.*;
import com.longju.xiantu.material.MaterialQuality;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.registry.ModItems;
import com.longju.xiantu.item.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Disposable tests exercise real registered blocks, recipes, server transactions and NBT. */
public final class SpiritWoodIntegration {
    private static int checks;
    private static void check(boolean condition,String name){checks++;if(!condition)throw new IllegalStateException(name);}
    private static BlockPos site(ServerPlayer p,int width,int depth){
        var l=p.serverLevel();int base=5600;
        while(base<14000){boolean clear=true;for(int x=-20;x<width+20&&clear;x++)for(int z=-20;z<depth+20&&clear;z++)for(int y=-60;y<-45;y++)if(!l.getBlockState(new BlockPos(base+x,y,1088+z)).isAir()){clear=false;break;}
            if(clear)return new BlockPos(base,-60,1088);base+=width+64;
        }throw new IllegalStateException("No isolated test site");
    }
    private static void prepare(ServerLevel l,BlockPos p){for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)l.setBlock(p.offset(x,-1,z),Blocks.GRASS_BLOCK.defaultBlockState(),3);}
    private static void clear(ServerLevel l,BlockPos p){for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)for(int y=0;y<16;y++)l.setBlock(p.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);}
    public static int run(ServerPlayer p){
        if(!p.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        var data=p.getCapability(CultivationCapability.DATA).orElseThrow(()->new IllegalStateException("Capability missing"));
        CompoundTag saved=new CompoundTag();((PlayerCultivationData)data).save(saved);var mode=p.gameMode.getGameModeForPlayer();var hand=p.getMainHandItem().copy();var position=p.position();float health=p.getHealth();checks=0;
        BlockPos base=site(p,384,40),pos=base;ServerLevel l=p.serverLevel();prepare(l,pos);
        try{
            p.setGameMode(GameType.SURVIVAL);p.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+6.5);
            long[] cost={200,1240,4020,22320,103440,564800,1764000,9568000};
            for(SpiritWoodType t:SpiritWoodType.values()){
                for(String suffix:SpiritWoodRegistry.BUILDING){
                    var piece=new ItemStack(SpiritWoodRegistry.item(t,suffix));
                    check(!piece.getHoverName().getString().matches(".*[一二三四五六七八九]阶.*"),"Untiered construction name "+t.id+"_"+suffix);
                }
                check(!new ItemStack(SpiritWoodRegistry.item(t,"planks")).getHoverName().getString().matches(".*[一二三四五六七八九]阶.*"),"Untiered planks "+t.id);
                p.sendSystemMessage(Component.literal("[Spirit wood QA] "+t.treeName));pos=base.offset(t.ordinal()*48,0,0);prepare(l,pos);p.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+6.5);clear(l,pos);data.setRealmStage(t.realm,0);
                check(t.energyCost()==cost[t.ordinal()],"Exact energy cost "+t.id);check(SpiritWoodRules.realm(p,t),"Required realm admits");
                if(t.realm>0){data.setRealmStage(t.realm-1,0);check(!SpiritWoodRules.canHarvest(p,t,new ItemStack(Items.NETHERITE_AXE)),"Lower realm denied");data.setRealmStage(t.realm,0);}
                ItemStack axe=new ItemStack(t.axeLevel()==1?Items.STONE_AXE:t.axeLevel()==2?Items.IRON_AXE:Items.DIAMOND_AXE);
                check(SpiritWoodRules.canHarvest(p,t,axe),"Vanilla axe admits");check(!SpiritWoodRules.canHarvest(p,t,new ItemStack(Items.WOODEN_AXE)),"Wooden axe denied");
                p.setGameMode(GameType.CREATIVE);check(SpiritWoodRules.canHarvest(p,t,ItemStack.EMPTY),"Creative empty hand bypass");p.setGameMode(GameType.SURVIVAL);
                Block sapling=SpiritWoodRegistry.block(t,"sapling");l.setBlock(pos,sapling.defaultBlockState(),3);var be=(SpiritWoodBlockEntity)l.getBlockEntity(pos);
                be.growthUnits=0;SpiritWoodBlockEntity.tick(l,pos,sapling.defaultBlockState(),be);check(be.growthUnits==t.energyCost(),"One loaded tick, no offline catch-up; sky="+l.canSeeSky(pos)+" light="+l.getMaxLocalRawBrightness(pos)+" units="+be.growthUnits+" expected="+t.energyCost());
                if(t.rank()<=3){for(int i=0;i<5;i++)((BonemealableBlock)sapling).performBonemeal(l,l.random,pos,sapling.defaultBlockState());check(be.boneMealUses==5,"Five bone meal applications");check(!((BonemealableBlock)sapling).isValidBonemealTarget(l,pos,sapling.defaultBlockState(),false),"Bone meal cap");}
                else check(!((BonemealableBlock)sapling).isValidBonemealTarget(l,pos,sapling.defaultBlockState(),false),"High rank rejects bone meal");
                be.growthUnits=be.totalUnits()/3;be.boneMealUses=2;BlockEntity savedBe=be;CompoundTag tag=savedBe.saveWithoutMetadata();BlockEntity restored=new SpiritWoodBlockEntity(pos,sapling.defaultBlockState());restored.load(tag);
                check(((SpiritWoodBlockEntity)restored).growthUnits==be.growthUnits,"Large long progress survives NBT");check(((SpiritWoodBlockEntity)restored).treeId==null,"Sapling has no base identity");
                be.growthUnits=0;be.boneMealUses=0;
                // Partial stone rejection and exact one-stone debit.
                ItemStack stone=new ItemStack(ModItems.getSpiritStone(SpiritStoneTier.values()[t.ordinal()]),t.stones);
                p.setItemInHand(InteractionHand.MAIN_HAND,stone);
                // Use the production charge setter rather than guessing item-NBT keys.
                ((SpiritStoneItem)stone.getItem()).setChargeMilli(stone,1000);if(t.rank()==1)((SpiritStoneItem)stone.getItem()).setChargeMilli(stone,1);
                var denied=new SaplingGrowthMenu(100,p.getInventory(),pos,be,false);check(!((AbstractContainerMenu)denied).clickMenuButton(p,1)&&be.growthUnits==0,"Partial stone rejected");
                if(t.rank()==6){
                    ItemStack dao64=new ItemStack(ModItems.DAO_SHI.get(),64);p.setItemInHand(InteractionHand.MAIN_HAND,dao64);
                    var highStone=new SaplingGrowthMenu(106,p.getInventory(),pos,be,false);
                    check(highStone.allowed()&&((AbstractContainerMenu)highStone).clickMenuButton(p,1),"Purple cedar accepts64full Dao stones; reason="+highStone.reason());
                    check(dao64.getCount()==63&&be.growthUnits==t.growthTicks()*299000L,"Purple cedar exact one Dao debit and energy");be.growthUnits=0;be.update();
                }
                stone=new ItemStack(ModItems.getSpiritStone(SpiritStoneTier.values()[t.ordinal()]),t.stones);p.setItemInHand(InteractionHand.MAIN_HAND,stone);
                final BlockPos treeBase=pos;BlockPos obstruction=SpiritTreeGrowth.layout(pos,t).keySet().stream().filter(q->!q.equals(treeBase)&&q.getX()!=treeBase.getX()).findFirst().orElseThrow();l.setBlock(obstruction,Blocks.OBSIDIAN.defaultBlockState(),3);var blocked=new SaplingGrowthMenu(101,p.getInventory(),pos,be,false);check(!((AbstractContainerMenu)blocked).clickMenuButton(p,1)&&stone.getCount()==t.stones,"Obstruction does not debit");l.setBlock(obstruction,Blocks.AIR.defaultBlockState(),3);
                var menu=new SaplingGrowthMenu(102,p.getInventory(),pos,be,false);check(((AbstractContainerMenu)menu).clickMenuButton(p,1),"Single confirmation succeeds");check(stone.getCount()==t.stones-1&&be.growthUnits==be.totalUnits()/t.stones,"One stone exact progress");
                var batch=new SaplingGrowthMenu(103,p.getInventory(),pos,be,true);check(((AbstractContainerMenu)batch).clickMenuButton(p,2),"Nonoverflow batch succeeds");check(stone.isEmpty(),"Exact total stones consumed");
                check(l.getBlockState(pos).getBlock()==SpiritWoodRegistry.block(t,"log"),"Sapling atomically grows registered tree");
                var root=(SpiritWoodBlockEntity)l.getBlockEntity(pos);check(root.treeId!=null,"Unique base tree identity");int materialQuality=l.getBlockState(pos).getValue(SpiritPlanksBlock.QUALITY);check(materialQuality==0,"Grown wood unexamined");int bases=0,logs=0;
                for(var entry:SpiritTreeGrowth.layout(pos,t).entrySet())if(entry.getValue()){
                    var state=l.getBlockState(entry.getKey());check(state.getValue(SpiritPlanksBlock.QUALITY)==materialQuality,"Whole tree same materialQuality");logs++;
                    if(l.getBlockEntity(entry.getKey()) instanceof SpiritWoodBlockEntity logBe&&logBe.treeId!=null)bases++;
                }check(bases==1&&logs>=4,"One base only");
                var loot=new LootParams.Builder(l).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL,axe).withParameter(LootContextParams.THIS_ENTITY,p).withParameter(LootContextParams.BLOCK_STATE,l.getBlockState(pos)).withParameter(LootContextParams.BLOCK_ENTITY,root);
                var drops=((Block)SpiritWoodRegistry.block(t,"log")).getDrops(l.getBlockState(pos),loot);check(drops.size()==2&&drops.get(1).is(SpiritWoodRegistry.item(t,"sapling")),"Base loot one sapling");check(MaterialQuality.get(drops.get(0))==materialQuality,"Loot keeps materialQuality");
                // Real strip interaction retains base ID and materialQuality.
                UUID identity=root.treeId;p.setItemInHand(InteractionHand.MAIN_HAND,axe);((Block)SpiritWoodRegistry.block(t,"log")).use(l.getBlockState(pos),l,pos,p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false));
                check(l.getBlockState(pos).getBlock()==SpiritWoodRegistry.block(t,"stripped_log")&&l.getBlockState(pos).getValue(SpiritPlanksBlock.QUALITY)==materialQuality,"Strip keeps tree material");check(((SpiritWoodBlockEntity)l.getBlockEntity(pos)).treeId.equals(identity),"Strip retains unique base");
                // Recipe matching against the actual player's workbench grid.
                var oldMenu=p.containerMenu;CraftingMenu crafting=new CraftingMenu(104,p.getInventory());p.containerMenu=crafting;CraftingContainer grid=(CraftingContainer)crafting.slots.get(1).container;
                try{
                    grid.setItem(0,MaterialQuality.set(new ItemStack(SpiritWoodRegistry.item(t,"planks")),4));grid.setItem(3,MaterialQuality.set(new ItemStack(SpiritWoodRegistry.item(t,"planks")),5));
                    var recipe=l.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,l).orElseThrow();check(recipe instanceof SpiritWoodProcessingRecipe,"Wood workbench custom recipe");ItemStack result=recipe.assemble(grid,l.registryAccess());check(result.is(SpiritWoodRegistry.item(t,"stick"))&&result.getCount()==4&&MaterialQuality.get(result)==4,"Mixed quality inherits lowest grade");
                    if(t.realm>0){data.setRealmStage(t.realm-1,0);check(!((ShapedRecipe)recipe).matches(grid,l),"Processing rechecks realm");data.setRealmStage(t.realm,0);}
                    grid.clearContent();grid.setItem(0,new ItemStack(SpiritWoodRegistry.item(t,"log")));
                    var unknownRecipe=l.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,l).orElseThrow();
                    result=unknownRecipe.assemble(grid,l.registryAccess());check(result.is(SpiritWoodRegistry.item(t,"planks"))&&MaterialQuality.get(result)==0,"Unexamined log yields unexamined planks");
                    grid.clearContent();grid.setItem(0,MaterialQuality.set(new ItemStack(SpiritWoodRegistry.item(t,"log")),5));
                    result=unknownRecipe.assemble(grid,l.registryAccess());check(MaterialQuality.get(result)==5,"Supreme log yields supreme planks");
                    grid.clearContent();grid.setItem(0,MaterialQuality.set(new ItemStack(SpiritWoodRegistry.item(t,"planks")),5));grid.setItem(3,new ItemStack(SpiritWoodRegistry.item(t,"planks")));
                    result=recipe.assemble(grid,l.registryAccess());check(MaterialQuality.get(result)==0,"Unexamined mixed ingredient keeps output unexamined");

                }finally{p.containerMenu=oldMenu;}
                check(!SpiritWoodRegistry.block(t,"planks").defaultBlockState().is(net.minecraft.tags.BlockTags.PLANKS),"Spirit planks excluded from generic vanilla recipes");
                for(String suffix:SpiritWoodRegistry.BUILDING){
                    Block b=SpiritWoodRegistry.block(t,suffix);check(b.getFlammability(b.defaultBlockState(),l,pos,Direction.NORTH)>0,"Wood fire behavior "+suffix);
                    check(!b.defaultBlockState().getCollisionShape(l,pos).isEmpty(),"Building collision "+suffix);
                    Item item=SpiritWoodRegistry.item(t,suffix);check(item.getBurnTime(new ItemStack(item),null)==0,"No building fuel refund "+suffix);
                }
                // Real survival break event and actual item entities, then replacement removes base identity.
                clear(l,pos);var log=SpiritWoodRegistry.block(t,"log");l.setBlock(pos,log.defaultBlockState().setValue(SpiritPlanksBlock.QUALITY,4),3);
                var logBe=(SpiritWoodBlockEntity)l.getBlockEntity(pos);logBe.treeId=UUID.randomUUID();logBe.update();
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WOODEN_AXE));check(!p.gameMode.destroyBlock(pos)&&l.getBlockState(pos).getBlock()==log,"Wrong axe actual survival break denied");
                p.setItemInHand(InteractionHand.MAIN_HAND,axe);check(p.gameMode.destroyBlock(pos),"Correct axe actual survival harvest");
                var dropped=l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(2));
                check(dropped.stream().filter(e->e.getItem().is(SpiritWoodRegistry.item(t,"sapling"))).mapToInt(e->e.getItem().getCount()).sum()==1,"Actual harvest drops one seed");
                check(dropped.stream().anyMatch(e->e.getItem().is(log.asItem())&&MaterialQuality.get(e.getItem())==4),"Actual harvest drops revealed material");
                for(net.minecraft.world.entity.Entity e:dropped)e.discard();
                ItemStack replacement=MaterialQuality.set(new ItemStack(log),4);p.setItemInHand(InteractionHand.MAIN_HAND,replacement);
                ((BlockItem)replacement.getItem()).place(new BlockPlaceContext(p,InteractionHand.MAIN_HAND,replacement,new BlockHitResult(Vec3.atCenterOf(pos.below()),Direction.UP,pos.below(),false)));
                check(l.getBlockState(pos).getValue(SpiritPlanksBlock.QUALITY)==4&&((SpiritWoodBlockEntity)l.getBlockEntity(pos)).treeId==null,"Placed harvested log preserves materialQuality without seed identity");
                p.setGameMode(GameType.CREATIVE);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);check(p.gameMode.destroyBlock(pos)&&l.getBlockState(pos).isAir(),"Creative empty hand real break");p.setGameMode(GameType.SURVIVAL);
                // A high stone on an almost-grown seed must require explicit overflow confirmation.
                pos=pos.offset(0,0,16);prepare(l,pos);p.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+6.5);
                l.setBlock(pos,sapling.defaultBlockState(),3);be=(SpiritWoodBlockEntity)l.getBlockEntity(pos);be.growthUnits=be.totalUnits()-1;
                ItemStack finalStone=new ItemStack(ModItems.DAO_SHI.get(),2);p.setItemInHand(InteractionHand.MAIN_HAND,finalStone);
                var overflow=new SaplingGrowthMenu(105,p.getInventory(),pos,be,true);check(!((AbstractContainerMenu)overflow).clickMenuButton(p,2)&&finalStone.getCount()==2,"Batch never spends overflowing stone");
                check(!((AbstractContainerMenu)overflow).clickMenuButton(p,4)&&finalStone.getCount()==2,"Overflow needs a preview");
                check(((AbstractContainerMenu)overflow).clickMenuButton(p,3)&&((AbstractContainerMenu)overflow).clickMenuButton(p,4)&&finalStone.getCount()==1,"Explicit overflow spends one stone; reason="+overflow.reason()+" light="+l.getMaxLocalRawBrightness(pos)+" sky="+l.canSeeSky(pos)+" count="+finalStone.getCount());
                check(!((AbstractContainerMenu)overflow).clickMenuButton(p,4)&&finalStone.getCount()==1,"Repeated stale menu cannot debit or grow twice");
                clear(l,pos);
            }
            clear(l,pos);p.sendSystemMessage(Component.literal("[Spirit wood QA PASS] "+checks+" assertions; eight species, transactions, recipes and NBT"));return 1;
        }catch(Exception|LinkageError error){p.sendSystemMessage(Component.literal("[Spirit wood QA FAIL] "+error));error.printStackTrace();return 0;}
        finally{clear(l,pos);p.closeContainer();((PlayerCultivationData)data).load(saved);CultivationEvents.sync(p);p.setGameMode(mode);p.setItemInHand(InteractionHand.MAIN_HAND,hand);p.teleportTo(position.x,position.y,position.z);p.setHealth(Math.min(health,p.getMaxHealth()));}
    }
    public static int gallery(ServerPlayer p){
        if(!p.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        BlockPos base=site(p,384,40);ServerLevel l=p.serverLevel();
        for(SpiritWoodType t:SpiritWoodType.values()){
            var center=base.offset(t.ordinal()*48,0,0);prepare(l,center);check(SpiritTreeGrowth.grow(l,center,t),"Gallery tree space");
            for(int i=0;i<SpiritWoodRegistry.BUILDING.length;i++){
                String suffix=SpiritWoodRegistry.BUILDING[i];BlockPos at=center.offset(i%8*3-8,0,16+i/8*4);l.setBlock(at.below(),Blocks.GRASS_BLOCK.defaultBlockState(),3);
                Block b=SpiritWoodRegistry.block(t,suffix);ItemStack item=new ItemStack(b);p.setItemInHand(InteractionHand.MAIN_HAND,item);
                ((BlockItem)item.getItem()).place(new BlockPlaceContext(p,InteractionHand.MAIN_HAND,item,new BlockHitResult(Vec3.atCenterOf(at.below()),Direction.UP,at.below(),false)));
            }
            BlockPos col=center.offset(0,0,32);for(int y=0;y<6;y++)l.setBlock(col.above(y),SpiritWoodRegistry.block(t,new String[]{"pillar_base","pillar","pillar","pillar_capital","bracket","beam"}[y]).defaultBlockState(),3);
        }
        p.teleportTo(base.getX()+.5,base.getY()+4,base.getZ()+12.5);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.sendSystemMessage(Component.literal("[Spirit wood gallery] "+base+"; eight trees +200 wooden building components, isolated48-block family spacing"));return 1;
    }
    public static int artGallery(ServerPlayer p){
        if(!p.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        BlockPos base=site(p,96,12);ServerLevel l=p.serverLevel();
        for(SpiritWoodType t:SpiritWoodType.values()){
            BlockPos at=base.offset(t.ordinal()*10,0,0);prepare(l,at);
            l.setBlock(at,SpiritWoodRegistry.block(t,"sapling").defaultBlockState(),3);
            BlockState leaf=SpiritWoodRegistry.block(t,"leaves").defaultBlockState().setValue(LeavesBlock.PERSISTENT,true);
            for(int x=0;x<2;x++)for(int y=0;y<2;y++)l.setBlock(at.offset(3+x,y,0),leaf,3);
            l.setBlock(at.offset(0,0,4),SpiritWoodRegistry.block(t,"planks").defaultBlockState(),3);
            l.setBlock(at.offset(2,0,4),SpiritWoodRegistry.block(t,"smooth").defaultBlockState(),3);
            l.setBlock(at.offset(4,0,4),SpiritWoodRegistry.block(t,"carved").defaultBlockState(),3);
        }
        p.teleportTo(base.getX()+.5,base.getY()+1,base.getZ()-4.5);
        p.sendSystemMessage(Component.literal("[Spirit wood art gallery] "+base+"; eight saplings/leaves + wood panels, isolated test site"));return 1;
    }
    public static int gui(ServerPlayer p){
        if(!p.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        BlockPos at=site(p,16,16);prepare(p.serverLevel(),at);
        Block sapling=SpiritWoodRegistry.block(SpiritWoodType.QING_WEN_ELM,"sapling");p.serverLevel().setBlock(at,sapling.defaultBlockState(),3);
        p.teleportTo(at.getX()+.5,at.getY(),at.getZ()+2.5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.FAN_LING_SHI.get(),4));
        sapling.use(sapling.defaultBlockState(),p.level(),at,p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.SOUTH,at,false));
        p.sendSystemMessage(Component.literal("[Spirit wood GUI] Actual seed use at "+at+"; four full stones, confirm consumes exactly one"));return 1;
    }
}
