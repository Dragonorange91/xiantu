package com.longju.xiantu.refining;

import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.item.SpiritStoneItem;
import com.longju.xiantu.registry.ModBlockEntities;
import com.longju.xiantu.workstation.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** One durable paid job, one portable result; no offline progress or duplicate material debit. */
public final class RefiningBlockEntity extends RandomizableContainerBlockEntity {
    public static final List<String> FURNACES=List.of("lifeng_refining_furnace","cuihuo_refining_furnace","bailian_refining_furnace","tiangong_refining_furnace","zaohua_refining_furnace","kaitian_refining_furnace");
    public static final List<String> ALCHEMY=List.of("qingmu_alchemy_furnace","chiyan_alchemy_furnace","zijin_alchemy_furnace","xuantian_alchemy_furnace","taixu_alchemy_furnace","hongmeng_alchemy_furnace");
    public static final int FIRE=3,MATERIAL_START=4,SIZE=12;
    private NonNullList<ItemStack> items=NonNullList.withSize(SIZE,ItemStack.EMPTY);
    public int fireBonus;
    public FacilityAssembly assembly;
    public com.longju.xiantu.alchemy.AlchemyJob alchemyJob;
    public final SimpleContainer alchemyAux=new SimpleContainer(1);
    public UUID outputOwner;
    public long energyMilli;public int elapsed,total,materialQuality,furnaceQuality=1,sourceFurnace=1,profession;
    private String recipe="",serial="";private UUID owner;
    public RefiningBlockEntity(BlockPos p,BlockState s){super(ModBlockEntities.REFINING.get(),p,s);alchemyAux.addListener(c->setChanged());}
    public static boolean supports(BlockState s){var id=ForgeRegistries.BLOCKS.getKey(s.getBlock());return id!=null&&(FURNACES.contains(id.getPath())||ALCHEMY.contains(id.getPath())||id.getPath().equals("artisan_forging_table"));}
    public boolean forge(){return ForgeRegistries.BLOCKS.getKey(getBlockState().getBlock()).getPath().equals("artisan_forging_table");}
    public boolean alchemy(){return ALCHEMY.contains(ForgeRegistries.BLOCKS.getKey(getBlockState().getBlock()).getPath());}
    public List<ItemStack> materials(){return items.subList(MATERIAL_START,SIZE);}
    public int furnace(){return Math.max(1,(alchemy()?ALCHEMY:FURNACES).indexOf(ForgeRegistries.BLOCKS.getKey(getBlockState().getBlock()).getPath())+1);}
    public RefiningRecipes.Recipe job(){return RefiningRecipes.find(recipe);}
    public boolean busy(){return !recipe.isEmpty()||assembly!=null||alchemyJob!=null;}
    public boolean mayTakeOutput(net.minecraft.world.entity.player.Player p){return outputOwner==null||outputOwner.equals(p.getUUID());}
    public int storedEnergy(){return (int)(energyMilli/1000);}
    public int capacity(){return new int[]{1000,3000,10000,30000,100000,300000}[furnace()-1];}
    @Override public int getContainerSize(){return SIZE;}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> n){items=n;}
    @Override protected Component getDefaultName(){return getBlockState().getBlock().getName();}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return alchemy()?new com.longju.xiantu.alchemy.AlchemyMenu(id,inv,this):new RefiningMenu(id,inv,this,worldPosition,forge(),false);}
    @Override public boolean canPlaceItem(int slot,ItemStack s){
        if(slot==0)return !forge()&&s.getItem() instanceof SpiritStoneItem;
        if(busy())return false;
        if(slot==1)return forge()&&s.is(RefiningItems.BLANK.get());
        if(slot==FIRE)return !forge()&&SpiritFire.isFire(s);
        if(slot>=MATERIAL_START&&slot<SIZE&&!forge()){
            if(alchemy()?!com.longju.xiantu.alchemy.AlchemyRecipes.isMaterial(s):!RefiningRecipes.isMaterial(s)&&!FacilityRecipes.isMaterial(s))return false;
            for(int i=MATERIAL_START;i<SIZE;i++)if(i!=slot&&!items.get(i).isEmpty()&&items.get(i).is(s.getItem())&&com.longju.xiantu.material.MaterialQuality.get(items.get(i))!=com.longju.xiantu.material.MaterialQuality.get(s))return false;
            return true;
        }return false;
    }
    @Override public ItemStack removeItem(int slot,int amount){return busy()&&slot!=0&&slot!=2?ItemStack.EMPTY:super.removeItem(slot,amount);}
    @Override public boolean canTakeItem(Container destination,int slot,ItemStack stack){return (slot!=2||outputOwner==null)&&(!busy()||slot==0);}
    @Override public void load(CompoundTag n){super.load(n);items=NonNullList.withSize(SIZE,ItemStack.EMPTY);ContainerHelper.loadAllItems(n,items);energyMilli=Math.max(0,Math.min((long)capacity()*1000,n.getLong("Energy")));recipe=n.getString("Recipe");serial=n.getString("Serial");owner=n.hasUUID("Owner")?n.getUUID("Owner"):null;elapsed=n.getInt("Elapsed");total=n.getInt("Total");materialQuality=n.getInt("Material");sourceFurnace=n.getInt("SourceFurnace");furnaceQuality=Math.max(1,Math.min(5,n.getInt("FurnaceQuality")));profession=n.getInt("Profession");fireBonus=Math.max(0,Math.min(900,n.getInt("FireBonus")));assembly=n.contains("Assembly")?FacilityAssembly.load(n.getCompound("Assembly")):null;alchemyJob=n.contains("AlchemyJob")?com.longju.xiantu.alchemy.AlchemyJob.load(n.getCompound("AlchemyJob")):null;outputOwner=n.hasUUID("OutputOwner")?n.getUUID("OutputOwner"):null;for(int i=0;i<alchemyAux.getContainerSize();i++)alchemyAux.setItem(i,ItemStack.of(n.getCompound("Aux"+i)));}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);ContainerHelper.saveAllItems(n,items);n.putLong("Energy",energyMilli);n.putString("Recipe",recipe);n.putString("Serial",serial);if(owner!=null)n.putUUID("Owner",owner);n.putInt("Elapsed",elapsed);n.putInt("Total",total);n.putInt("Material",materialQuality);n.putInt("SourceFurnace",sourceFurnace);n.putInt("FurnaceQuality",furnaceQuality);n.putInt("Profession",profession);n.putInt("FireBonus",fireBonus);if(assembly!=null)n.put("Assembly",assembly.save());if(alchemyJob!=null)n.put("AlchemyJob",alchemyJob.save());if(outputOwner!=null)n.putUUID("OutputOwner",outputOwner);for(int i=0;i<alchemyAux.getContainerSize();i++)n.put("Aux"+i,alchemyAux.getItem(i).save(new CompoundTag()));}
    public static int realmNeeded(int tier){return tier==7?7:Math.max(0,tier-1);}
    public String eligibility(ServerPlayer p,RefiningRecipes.Recipe r){
        if(r==null)return "请选择器方";
        var d=p.getCapability(CultivationCapability.DATA).orElse(null);
        if(d==null||!d.cultivationUnlocked()||d.majorRealm()<realmNeeded(r.tier()))return "当前境界不足";
        if(RefiningProgress.level(p)<r.profession())return "炼器熟练等级不足";
        if(r.tier()<=3&&!RefiningProgress.knows(p,CraftingTreatises.REFINING_KEY))return "请先研习器道真解";
        if(RefiningRules.blueprintRequired(r.tier())&&!RefiningProgress.knows(p,r.id()))return "四阶及以上需要先学习对应器方";
        if(!forge()&&furnace()<r.furnace())return "炼器炉等级不足";
        return "";
    }
    public boolean refuel(){
        if(level==null||level.isClientSide||forge()||assembly!=null)return false;var s=items.get(0);
        if(!(s.getItem() instanceof SpiritStoneItem stone)||s.getCount()!=1)return false;
        long take=Math.min(stone.chargeMilli(s),(long)capacity()*1000-energyMilli);if(take<=0)return false;
        long left=stone.chargeMilli(s)-take;energyMilli+=take;if(left==0)items.set(0,ItemStack.EMPTY);else stone.setChargeMilli(s,left);setChanged();return true;
    }
    public String start(ServerPlayer p,RefiningRecipes.Recipe chosen){
        if(alchemy())return "丹炉火源已接入，炼丹配方流程尚未开放";
        if(p.isSpectator())return "旁观模式不能炼器";
        if(!(level instanceof ServerLevel l)||busy()||!items.get(2).isEmpty())return "先取出产物，当前任务结束后才能开工";
        CompoundTag blank=null;RefiningRecipes.Recipe r=chosen;
        if(forge()){blank=RefiningLedger.get(l).inspect(items.get(1));r=blank==null?null:RefiningRecipes.find(blank.getString("Recipe"));if(r==null)return "放入有效的未锻打器胚";}
        String error=eligibility(p,r);if(!error.isEmpty())return error;
        int mq,fq,ft;String sid="";
        if(forge()){
            fireBonus=blank.getInt("FireBonus");mq=blank.getInt("Material");fq=blank.getInt("FurnaceQuality");ft=blank.getInt("Furnace");sid=items.get(1).getTag().getString("Serial");
            if(!RefiningLedger.get(l).reserve(items.get(1)))return "此器胚已经被使用";items.set(1,ItemStack.EMPTY);
        }else{
            var inventory=materials();
            if(!RefiningRecipes.uniformQualities(inventory))return "同种材料不能混用品质，请分开炼制";
            var used=RefiningRecipes.allocate(r,inventory);if(used==null)return "材料槽内数量不足，或妖丹属性、品阶不符";
            if(!SpiritFire.isFire(items.get(FIRE)))return "请在灵火槽放入灵火或天地奇火";
            SpiritFire.recover(items.get(FIRE),level);
            int fireCost=Math.max(1,(Math.max(20,r.seconds()*20*3/5)+19)/20);
            if(SpiritFire.remaining(items.get(FIRE))<fireCost)return "灵火耐久不足以完成此炉，请更换或等待奇火恢复";
            int cost=RefiningRules.energy(r.tier());
            if(energyMilli<(long)cost*1000)return "储能不足，请在灵石槽放入灵石自动补充";
            mq=RefiningRecipes.materialQuality(inventory,used);fq=furnaceQuality;ft=furnace();
            fireBonus=SpiritFire.qualityBonus(items.get(FIRE),r.tier());energyMilli-=(long)cost*1000;
            for(int i=0;i<used.length;i++)if(used[i]>0)inventory.get(i).shrink(used[i]);

        }
        recipe=r.id();owner=p.getUUID();serial=sid;materialQuality=mq;furnaceQuality=fq;sourceFurnace=ft;profession=RefiningProgress.level(p);elapsed=0;
        total=Math.max(20,r.seconds()*20*(forge()?2:3)/5);setChanged();return "";
    }
    public static void tick(Level world,BlockPos pos,BlockState state,RefiningBlockEntity be){
        if(!(world instanceof ServerLevel l))return;
        if(be.assembly!=null){FacilityAssembly.tick(l,be);return;}
        if(!be.forge()){
            SpiritFire.recover(be.items.get(FIRE),world);
            if(SpiritFire.rank(be.items.get(FIRE))>0&&world.getGameTime()%100==0)be.setChanged();
        }
        if(world.getGameTime()%20==0)be.refuel();
        if(be.alchemyJob!=null){com.longju.xiantu.alchemy.AlchemyJob.tick(l,be);return;}
        if(!be.busy()||be.owner==null)return;
        var p=l.getServer().getPlayerList().getPlayer(be.owner);var r=RefiningRecipes.find(be.recipe);
        if(p==null||p.level()!=l||!p.isAlive()||p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64||!be.eligibility(p,r).isEmpty())return;
        if(!be.items.get(2).isEmpty())return;
        if(!be.forge()){
            if(!SpiritFire.isFire(be.items.get(FIRE)))return;
            SpiritFire.holdActive(be.items.get(FIRE),world);
            if((be.elapsed+1)%20==0||be.elapsed+1>=be.total)SpiritFire.burn(be.items.get(FIRE),world);
        }
        if(++be.elapsed<be.total){be.setChanged();return;}
        if(be.forge()){
            if(RefiningLedger.get(l).finish(be.serial)){
                if(l.random.nextInt(100)<RefiningRules.successPercent(be.profession,be.sourceFurnace,r.furnace())){
                    int quality=RefiningRules.rollQuality(l.random.nextInt(10000),RefiningRules.qualityWeights(be.profession,be.sourceFurnace,be.furnaceQuality,be.materialQuality,be.fireBonus));
                    be.items.set(2,RefinedEquipment.finish(r,quality));RefiningProgress.award(p,r.tier());
                }else be.items.set(2,new ItemStack(RefiningItems.SCRAP.get()));
            }
        }else be.items.set(2,RefiningLedger.get(l).issue(r.id(),be.materialQuality,be.sourceFurnace,be.furnaceQuality,be.fireBonus));
        be.outputOwner=be.owner;be.recipe="";be.owner=null;be.elapsed=0;be.total=0;be.serial="";be.setChanged();
    }
}
