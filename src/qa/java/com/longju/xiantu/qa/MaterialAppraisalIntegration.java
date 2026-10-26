package com.longju.xiantu.qa;
import com.longju.xiantu.material.*;
import com.longju.xiantu.registry.*;
import com.longju.xiantu.wood.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import java.util.*;
public final class MaterialAppraisalIntegration {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new IllegalStateException(label);}
    private static BlockPos site(ServerPlayer p){
        for(int x=15000;x<20000;x+=64){boolean clear=true;for(int dx=-20;dx<=20&&clear;dx++)for(int dz=-20;dz<=20&&clear;dz++)for(int y=-60;y<-44;y++)if(!p.level().getBlockState(new BlockPos(x+dx,y,1280+dz)).isAir()){clear=false;break;}if(clear)return new BlockPos(x,-60,1280);}
        throw new IllegalStateException("No isolated appraisal site");
    }
    private static boolean qa(ServerPlayer p){return p.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA");}
    public static int run(ServerPlayer p){
        if(!qa(p))return 0;checks=0;ServerLevel l=p.serverLevel();BlockPos at=site(p);Vec3 oldPos=p.position();var oldMenu=p.containerMenu;
        var weights=com.longju.xiantu.config.XiantuConfig.MATERIAL_QUALITY_WEIGHTS;double[] saved=weights.stream().mapToDouble(w->w.get()).toArray();
        try{
            l.setBlock(at,ModBlocks.APPRAISAL.get().defaultBlockState(),3);p.teleportTo(at.getX()+.5,at.getY(),at.getZ()+3.5);
            var be=(AppraisalBlockEntity)l.getBlockEntity(at);AbstractContainerMenu menu=new AppraisalMenu(401,p.getInventory(),be,at);p.containerMenu=menu;
            for(SpiritWoodType t:SpiritWoodType.values())for(String suffix:new String[]{"log","stripped_log","planks","stick","long_shaft","fiber"}){
                ItemStack unknown=new ItemStack(SpiritWoodRegistry.item(t,suffix),64);check(MaterialQuality.eligible(unknown)&&MaterialQuality.get(unknown)==0,"New material unexamined "+suffix);
                check(MaterialQuality.manufacturingBonus(unknown)==0,"Unexamined gives no manufacturing bonus");
                ((Container)be).setItem(0,unknown);check(menu.clickMenuButton(p,0),"Free batch appraisal succeeds");
                int sum=0;for(int grade=1;grade<=5;grade++){
                    ItemStack s=((Container)be).getItem(grade);if(s.isEmpty())continue;sum+=s.getCount();
                    check(MaterialQuality.get(s)==grade&&s.is(unknown.getItem()),"Outputs retain item and grade");
                    check(!((Container)be).canPlaceItem(0,s),"Revealed item cannot reroll");
                    boolean construction=s.getItem() instanceof WoodBlockItem b&&b.isBuildingMaterial();
                    check(construction?!((Item)s.getItem()).getName(s).getString().endsWith("·"+MaterialQuality.NAMES[grade]):((Item)s.getItem()).getName(s).getString().endsWith("·"+MaterialQuality.NAMES[grade]),"Construction names stay plain; other material names show quality");
                    check(!ItemStack.isSameItemSameTags(s,unknown),"Revealed cannot stack with unexamined");
                }check(sum==64&&((Container)be).getItem(0).isEmpty(),"Exactly64 individually identified items");
                check(!menu.clickMenuButton(p,0),"Repeated click cannot duplicate");
                ((Container)be).setItem(0,new ItemStack(unknown.getItem()));check(!menu.clickMenuButton(p,0)&&((Container)be).getItem(0).getCount()==1,"Occupied outputs do not consume or reroll input");
                ((Container)be).clearContent();
            }
            check(!MaterialQuality.eligible(new ItemStack(ModItems.DAO_SHI.get())),"Spirit stones excluded from quality");
            check(!MaterialQuality.eligible(new ItemStack(ModBlockItems.DAO_SHI_ORE.get())),"Raw spirit ore blocks excluded; appraisal applies to harvested materials");
            for(int grade=1;grade<=5;grade++){
                for(int i=0;i<5;i++)weights.get(i).set(i+1==grade?1D:0D);
                ((Container)be).setItem(0,new ItemStack(SpiritWoodRegistry.item(SpiritWoodType.QING_WEN_ELM,"log"),3));
                check(menu.clickMenuButton(p,0)&&((Container)be).getItem(grade).getCount()==3,"Config deterministic grade "+grade);((Container)be).clearContent();
            }
            for(int i=0;i<5;i++)weights.get(i).set(0D);check(MaterialQuality.roll(l.random)==1,"All-zero config falls back to ordinary");
            for(int i=0;i<5;i++)weights.get(i).set(saved[i]);
            ItemStack persisted=MaterialQuality.set(new ItemStack(SpiritWoodRegistry.item(SpiritWoodType.ZI_FU_CEDAR,"log"),17),5);((Container)be).setItem(5,persisted);
            BlockEntity nativeBe=be;CompoundTag nbt=nativeBe.saveWithoutMetadata();BlockEntity restored=new AppraisalBlockEntity(at,ModBlocks.APPRAISAL.get().defaultBlockState());restored.load(nbt);
            ItemStack restoredItem=((Container)restored).getItem(5);check(restoredItem.getCount()==17&&MaterialQuality.get(restoredItem)==5,"Container NBT keeps stack and quality");
            ((Container)be).clearContent();((Container)be).setItem(5,persisted);
            l.setBlock(at,Blocks.AIR.defaultBlockState(),3);
            var drops=l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(at).inflate(2));
            check(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==17,"Breaking altar drops all inventory exactly once");
            check(drops.stream().allMatch(e->MaterialQuality.get(e.getItem())==5),"Broken inventory retains quality");for(net.minecraft.world.entity.Entity e:drops)e.discard();
            check(!menu.clickMenuButton(p,0),"Removed altar rejects stale menus");
            // Native recipe and ordinary ingredients; no quality or realm prerequisite for the altar.
            CraftingMenu craft=new CraftingMenu(402,p.getInventory());p.containerMenu=craft;CraftingContainer grid=(CraftingContainer)craft.slots.get(1).container;
            for(int i:new int[]{0,1,2,3,5})grid.setItem(i,new ItemStack(Items.STONE));grid.setItem(4,new ItemStack(Items.GLASS));for(int i:new int[]{6,8})grid.setItem(i,new ItemStack(Items.COPPER_INGOT));
            var recipe=l.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,l).orElseThrow();check(recipe.assemble(grid,l.registryAccess()).is(ModBlockItems.APPRAISAL.get()),"Survival crafting entry");
            // Visible display names are grade-prefixed but registry IDs and raw language keys stay stable.
            check(((Item)ModItems.DAO_SHI.get()).getName(new ItemStack(ModItems.DAO_SHI.get())).getString().startsWith("八阶·"),"Stone name includes rank");
            p.sendSystemMessage(Component.literal("[Material appraisal QA PASS] "+checks+" assertions; batch, no reroll, NBT, drops, names and config"));return 1;
        }catch(Exception|LinkageError e){p.sendSystemMessage(Component.literal("[Material appraisal QA FAIL] "+e));e.printStackTrace();return 0;}
        finally{
            for(int i=0;i<5;i++)weights.get(i).set(saved[i]);
            if(l.getBlockEntity(at) instanceof AppraisalBlockEntity be)((Container)be).clearContent();l.setBlock(at,Blocks.AIR.defaultBlockState(),3);
            p.containerMenu=oldMenu;p.teleportTo(oldPos.x,oldPos.y,oldPos.z);
        }
    }
    public static int gui(ServerPlayer p){
        if(!qa(p))return 0;BlockPos at=site(p);p.serverLevel().setBlock(at,ModBlocks.APPRAISAL.get().defaultBlockState(),3);
        var be=(AppraisalBlockEntity)p.level().getBlockEntity(at);((Container)be).setItem(0,new ItemStack(SpiritWoodRegistry.item(SpiritWoodType.QING_WEN_ELM,"log"),64));
        p.teleportTo(at.getX()+.5,at.getY(),at.getZ()+3.5);
        ((Block)ModBlocks.APPRAISAL.get()).use(p.level().getBlockState(at),p.level(),at,p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.SOUTH,at,false));
        p.sendSystemMessage(Component.literal("[Material appraisal GUI] "+at+";64 unexamined elm logs, free appraisal, collect outputs before next batch"));return 1;
    }
}
