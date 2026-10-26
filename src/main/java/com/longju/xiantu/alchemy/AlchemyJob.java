package com.longju.xiantu.alchemy;

import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.refining.*;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Persisted timed automatic alchemy and work cache. No offline work, early roll or repeated debit. */
public final class AlchemyJob {
    public final String recipe;
    public final UUID owner;
    public final int profession,furnace,furnaceQuality,materialQuality,fireBonus,auxSuccess,auxQualityHalf,suitability;
    public final int[] events=new int[6]; // 0 pending, 1 completed
    public int elapsed,paused;
    public int temperature=20,fireMode=AlchemyHeat.GENTLE,heldTicks;
    public boolean committed;
    private final NonNullList<ItemStack> locked=NonNullList.withSize(9,ItemStack.EMPTY);
    private AlchemyJob(String recipe,UUID owner,int profession,int furnace,int fq,int mq,int fire,int as,int aq,int suitability){this.recipe=recipe;this.owner=owner;this.profession=profession;this.furnace=furnace;furnaceQuality=fq;materialQuality=mq;fireBonus=fire;auxSuccess=as;auxQualityHalf=aq;this.suitability=suitability;}
    public AlchemyRecipes.Recipe recipe(){return AlchemyRecipes.find(recipe);}
    public int total(){return AlchemyRules.ticks(recipe().rank());}
    public int stage(){return AlchemyRules.stage(elapsed,total());}
    public static String eligibility(RefiningBlockEntity be,ServerPlayer p,AlchemyRecipes.Recipe r){
        if(r==null)return "请选择丹方";
        var d=p.getCapability(CultivationCapability.DATA).orElse(null);
        if(d==null||!d.cultivationUnlocked()||d.majorRealm()<AlchemyRules.realm(r.rank()))return "当前境界不足以炼制此丹";
        if(AlchemyProgress.level(p)<Math.max(0,r.rank()-1))return "炼丹熟练等级不足";
        if(!RefiningProgress.knows(p,r.knowledge()))return r.rank()<=3?"请先研习丹道真解":"请先学习对应丹方";
        if(!be.alchemy()||be.furnace()<AlchemyRules.furnace(r.rank()))return "丹炉等级不足";
        return "";
    }
    public static String start(RefiningBlockEntity be,ServerPlayer p,AlchemyRecipes.Recipe r){
        if(p.isSpectator()||be.busy()||!be.getItem(2).isEmpty())return "结束当前任务并取出产物后再开炉";
        String error=eligibility(be,p,r);if(!error.isEmpty())return error;
        int[] allocated=RefiningRecipes.allocate(r.allocationRecipe(),be.materials());if(allocated==null)return "药材不足、同材料混用品质，或必需妖丹阶数/属性不符";
        var aux=AlchemyRecipes.auxiliary(r,be.alchemyAux);if(aux==null)return "辅助槽只能放同主属性妖丹，合计最多八枚";
        var fire=be.getItem(RefiningBlockEntity.FIRE);if(!SpiritFire.isFire(fire))return "放入灵火或天地奇火后再开炉";
        SpiritFire.recover(fire,be.getLevel());if(SpiritFire.remaining(fire)<(AlchemyRules.ticks(r.rank())+19)/20)return "灵火耐久不足以完成此炉";
        if(be.energyMilli<=0)return "请放入灵石为丹炉储能";
        var d=p.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new);
        int suitability=20+(d.hasSpiritRoot(SpiritRootType.WOOD)?50:0)+(d.hasSpiritRoot(SpiritRootType.WATER)?10:0)+(d.hasSpiritRoot(SpiritRootType.EARTH)?10:0)+(d.hasSpiritRoot(SpiritRootType.METAL)?10:0);
        var job=new AlchemyJob(r.id(),p.getUUID(),AlchemyProgress.level(p),be.furnace(),be.furnaceQuality,RefiningRecipes.materialQuality(be.materials(),allocated),SpiritFire.qualityBonus(fire,r.rank()),aux.success(),aux.qualityHalf(),suitability);
        for(int i=0;i<allocated.length;i++)if(allocated[i]>0){job.locked.set(i,be.materials().get(i).copyWithCount(allocated[i]));be.materials().get(i).shrink(allocated[i]);}
        job.locked.set(8,be.alchemyAux.getItem(0).copy());be.alchemyAux.setItem(0,ItemStack.EMPTY);
        be.alchemyJob=job;be.elapsed=0;be.total=job.total();be.setChanged();return "";
    }
    public int success(){return AlchemyRules.success(recipe().kind(),profession,furnace,AlchemyRules.furnace(recipe().rank()),auxSuccess);}
    public int[] qualityWeights(){return AlchemyRules.qualityWeights(profession,furnace,furnaceQuality,materialQuality,fireBonus,auxQualityHalf);}
    private void commit(){committed=true;Collections.fill(locked,ItemStack.EMPTY);}
    /** Breaking an active furnace forfeits paid ingredients, including the early work cache. */
    public void discard(RefiningBlockEntity be){
        if(be.alchemyJob!=this)return;
        commit();be.alchemyJob=null;be.elapsed=be.total=0;be.setChanged();
    }
    public void cancel(RefiningBlockEntity be){
        if(be.alchemyJob!=this)return;
        // Retire ownership before refunding. A repeated cancel/removal callback cannot refund twice.
        be.alchemyJob=null;be.elapsed=be.total=0;
        if(!committed)for(int i=0;i<locked.size();i++){
            var s=locked.get(i);locked.set(i,ItemStack.EMPTY);if(s.isEmpty())continue;
            var target=i<8?be.getItem(i+4):be.alchemyAux.getItem(i-8);
            if(target.isEmpty()){if(i<8)be.setItem(i+4,s.copy());else be.alchemyAux.setItem(i-8,s.copy());}
            else if(ItemStack.isSameItemSameTags(target,s)&&target.getCount()+s.getCount()<=target.getMaxStackSize())target.grow(s.getCount());
            else net.minecraft.world.Containers.dropItemStack(be.getLevel(),be.getBlockPos().getX()+.5,be.getBlockPos().getY()+1,be.getBlockPos().getZ()+.5,s.copy());
        }
        be.setChanged();
    }
    public static void tick(ServerLevel level,RefiningBlockEntity be){
        var job=be.alchemyJob;if(job==null)return;var p=level.getServer().getPlayerList().getPlayer(job.owner);
        long debit=AlchemyRules.debit(job.elapsed,job.total(),AlchemyRules.energy(job.recipe().rank()));
        boolean active=p!=null&&p.isAlive()&&!p.isSpectator()&&p.level()==level&&p.distanceToSqr(be.getBlockPos().getCenter())<=64&&p.hurtTime==0
            &&p.containerMenu instanceof AlchemyMenu menu&&menu.pos.equals(be.getBlockPos())&&eligibility(be,p,job.recipe()).isEmpty()&&be.getItem(2).isEmpty()&&SpiritFire.isFire(be.getItem(3))&&be.energyMilli>=debit;
        if(!active){if(++job.paused>=6000)job.cancel(be);be.setChanged();return;}
        job.paused=0;SpiritFire.holdActive(be.getItem(3),level);be.energyMilli-=debit;
        int stage=job.stage();
        int start=job.total()*AlchemyRules.BOUNDS[stage]/100,end=job.total()*AlchemyRules.BOUNDS[stage+1]/100;
        int target=stage==0?job.recipe().warmFire():job.recipe().blendFire();
        job.fireMode=target;
        job.temperature=AlchemyHeat.step(job.temperature,job.fireMode);
        if(stage==4&&job.fireMode==target&&AlchemyHeat.inBand(job.temperature,target))job.heldTicks++;
        if(job.elapsed>=start+(end-start)/2&&job.events[stage]==0){job.events[stage]=1;if(stage==1)job.commit();}
        if(job.elapsed+1>=end&&job.events[stage]==0)job.events[stage]=-1;
        if(stage==4&&job.elapsed+1>=end&&job.events[stage]>0&&!AlchemyHeat.held(job.heldTicks,end-start))job.events[stage]=-2;
        job.elapsed++;be.elapsed=job.elapsed;
        if(job.elapsed%20==0||job.elapsed>=job.total())SpiritFire.burn(be.getItem(3),level);
        if(job.elapsed>=job.total()){
            boolean structure=Arrays.stream(job.events).allMatch(v->v>0||v==-2);
            // Snapshot is retired before exposing its one output; a second tick cannot repeat a result.
            be.alchemyJob=null;be.elapsed=be.total=0;be.outputOwner=job.owner;
            if(structure&&level.random.nextInt(10000)<job.success()){
                int quality=AlchemyRules.quality(level.random.nextInt(20000),job.qualityWeights());var output=PillCatalog.stack(job.recipe().pill(),quality);
                if(level.random.nextDouble()<Math.min(.5,job.profession*.04+job.suitability/1000.0))output.setCount(2);
                be.setItem(2,output);AlchemyProgress.award(p,job.recipe().rank());
            }else be.setItem(2,new ItemStack(AlchemyItems.FAILED.get()));
        }
        be.setChanged();
    }
    public CompoundTag save(){
        var n=new CompoundTag();n.putString("Recipe",recipe);n.putUUID("Owner",owner);n.putInt("Profession",profession);n.putInt("Furnace",furnace);n.putInt("FurnaceQuality",furnaceQuality);n.putInt("Material",materialQuality);n.putInt("Fire",fireBonus);n.putInt("AuxSuccess",auxSuccess);n.putInt("AuxQualityHalf",auxQualityHalf);n.putInt("Suitability",suitability);n.putInt("Temperature",temperature);n.putInt("FireMode",fireMode);n.putInt("HeldTicks",heldTicks);n.putInt("Elapsed",elapsed);n.putInt("Paused",paused);n.putBoolean("Committed",committed);n.putIntArray("Events",events);var cache=new CompoundTag();ContainerHelper.saveAllItems(cache,locked);n.put("Cache",cache);return n;
    }
    public static AlchemyJob load(CompoundTag n){
        if(!n.hasUUID("Owner")||AlchemyRecipes.find(n.getString("Recipe"))==null)return null;
        var job=new AlchemyJob(n.getString("Recipe"),n.getUUID("Owner"),n.getInt("Profession"),n.getInt("Furnace"),n.getInt("FurnaceQuality"),n.getInt("Material"),n.getInt("Fire"),n.getInt("AuxSuccess"),n.getInt("AuxQualityHalf"),n.getInt("Suitability"));job.temperature=Math.max(0,Math.min(100,n.getInt("Temperature")));job.fireMode=n.getInt("FireMode")==1?1:0;job.heldTicks=Math.max(0,Math.min(job.total(),n.getInt("HeldTicks")));job.elapsed=Math.max(0,Math.min(job.total()-1,n.getInt("Elapsed")));job.paused=Math.max(0,n.getInt("Paused"));job.committed=n.getBoolean("Committed");var events=n.getIntArray("Events");if(events.length==6)System.arraycopy(events,0,job.events,0,6);if(!job.committed)ContainerHelper.loadAllItems(n.getCompound("Cache"),job.locked);return job;
    }
}
