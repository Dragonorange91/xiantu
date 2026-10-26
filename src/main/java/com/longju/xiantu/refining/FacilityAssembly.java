package com.longju.xiantu.refining;

import com.longju.xiantu.cultivation.CultivationCapability;
import com.longju.xiantu.workstation.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.UUID;

/** Facility assembly has no fire, energy, failure or profession reward. */
public final class FacilityAssembly {
    private final String recipe;
    private final UUID owner;
    private final BlockPos table;
    private final int profession,furnace,quality;
    private int paused;
    public FacilityAssembly(String recipe,UUID owner,BlockPos table,int profession,int furnace,int quality){this.recipe=recipe;this.owner=owner;this.table=table;this.profession=profession;this.furnace=furnace;this.quality=quality;}
    public FacilityRecipes.Recipe recipe(){return FacilityRecipes.find(recipe);}
    public static BlockPos table(RefiningBlockEntity be){
        if(be.getLevel()==null||!(be.getBlockState().getBlock() instanceof WorkstationBlock block))return null;
        var facing=be.getBlockState().getValue(WorkstationBlock.FACING);
        for(var part:block.partProperty().getPossibleValues())if(part.y==0)for(var dir:Direction.Plane.HORIZONTAL){
            var p=block.position(be.getBlockPos(),part,facing).relative(dir);
            if(!be.getLevel().hasChunkAt(p))continue;
            var state=be.getLevel().getBlockState(p);
            if(state.getBlock() instanceof WorkstationBlock.RefiningTable t){
                var origin=t.anchor(p,state);
                if(be.getLevel().getBlockEntity(origin) instanceof RefiningBlockEntity forge&&!forge.busy())return origin;
            }
        }
        return null;
    }
    public static String start(RefiningBlockEntity be,ServerPlayer p,FacilityRecipes.Recipe r){
        if(r==null||be.forge()||be.alchemy()||be.busy()||!be.getItem(2).isEmpty())return "先取出产物并结束当前任务";
        if(p.isSpectator())return "旁观模式不能装配设备";
        if(!intact(be)||p.level()!=be.getLevel()||p.distanceToSqr(be.getBlockPos().getCenter())>64)return "炉具结构或操作距离无效";
        if(be.furnace()<r.furnace()||r.upgrade()&&be.furnace()!=r.tier()-1)return "炼器炉等级不足，炉具只能逐阶升级";
        if(r.id().endsWith("_pickaxe")&&RefiningProgress.level(p)<RefiningRules.requiredLevel(r.tier()))return "炼器等级不足";
        if(!RefiningProgress.knows(p,r.knowledge()))return "请先学习对应设施图";
        var d=p.getCapability(CultivationCapability.DATA).orElse(null);
        if(r.realm()>0&&(d==null||!d.cultivationUnlocked()||d.majorRealm()<r.realm()))return "当前境界不足以加工设施材料";
        var table=table(be);if(table==null)return "请在炼器炉旁紧邻放置空闲锻打台";
        if(r.upgrade()&&(be.energyMilli!=0||!be.getItem(0).isEmpty()||!be.getItem(1).isEmpty()||!be.getItem(3).isEmpty()))return "升级前取出灵石、灵火及其他物品，并用完剩余储能";
        var used=RefiningRecipes.allocate(r.allocationRecipe(),be.materials());
        if(used==null)return "设施材料不足，或同种材料混用品质";
        if(r.upgrade())for(int i=0;i<used.length;i++)if(be.materials().get(i).getCount()!=used[i])return "升级时只能放入本次恰好所需的材料";
        // First irreversible action is this confirmed assembly start. No result roll is exposed early.
        for(int i=0;i<used.length;i++)be.materials().get(i).shrink(used[i]);
        be.assembly=new FacilityAssembly(r.id(),p.getUUID(),table,RefiningProgress.level(p),be.furnace(),be.furnaceQuality);
        be.elapsed=0;be.total=200;be.setChanged();return "";
    }
    public static void tick(ServerLevel level,RefiningBlockEntity be){
        var job=be.assembly;if(job==null)return;var r=job.recipe();
        var p=level.getServer().getPlayerList().getPlayer(job.owner);
        boolean valid=r!=null&&p!=null&&p.isAlive()&&!p.isSpectator()&&p.level()==level&&p.distanceToSqr(be.getBlockPos().getCenter())<=64
            &&p.containerMenu instanceof RefiningMenu menu&&menu.contents==be&&p.hurtTime==0
            &&RefiningProgress.knows(p,r.knowledge())
            &&p.getCapability(CultivationCapability.DATA).map(d->r.realm()==0||d.cultivationUnlocked()&&d.majorRealm()>=r.realm()).orElse(r.realm()==0)
            &&intact(be)&&validTable(be,job.table)&&be.getItem(2).isEmpty();
        if(!valid){if(++job.paused>=6000){be.assembly=null;be.elapsed=be.total=0;}be.setChanged();return;}
        job.paused=0;
        if(++be.elapsed<be.total){be.setChanged();return;}
        int q=RefiningRules.rollQuality(level.random.nextInt(10000),RefiningRules.qualityWeights(job.profession,job.furnace,job.quality,0));
        be.assembly=null;be.elapsed=be.total=0;be.setChanged();
        if(!r.upgrade()){
            var output=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(r.output())),r.count());
            if(output.getItem() instanceof com.longju.xiantu.mining.MiningPickaxeItem)output.getOrCreateTag().putInt("quality",q);
            else if(output.getItem() instanceof net.minecraft.world.item.BlockItem block&&block.getBlock() instanceof WorkstationBlock)output.getOrCreateTag().putInt("FurnaceQuality",q);
            be.outputOwner=job.owner;be.setItem(2,output);
        }else{
            // Swap all eight cells under a scoped onRemove guard; preserve exactly one anchor inventory.
            CompoundTag saved=be.saveWithoutMetadata();saved.putInt("FurnaceQuality",q);
            var old=(WorkstationBlock)be.getBlockState().getBlock();var facing=be.getBlockState().getValue(WorkstationBlock.FACING);
            var next=(WorkstationBlock)ForgeRegistries.BLOCKS.getValue(new ResourceLocation(r.output()));
            WorkstationBlock.replaceAssembly(()->{
                for(var part:old.partProperty().getPossibleValues())level.setBlock(old.position(be.getBlockPos(),part,facing),next.defaultBlockState().setValue(WorkstationBlock.FACING,facing).setValue(next.partProperty(),part),2);
            });
            if(level.getBlockEntity(be.getBlockPos()) instanceof RefiningBlockEntity replacement){replacement.load(saved);replacement.setChanged();}
            for(var part:next.partProperty().getPossibleValues())level.updateNeighborsAt(next.position(be.getBlockPos(),part,facing),next);
            if(p.containerMenu instanceof RefiningMenu menu&&menu.pos.equals(be.getBlockPos()))p.closeContainer();
        }
    }
    private static boolean intact(RefiningBlockEntity be){
        if(be.getLevel()==null||!(be.getBlockState().getBlock() instanceof WorkstationBlock block))return false;
        var facing=be.getBlockState().getValue(WorkstationBlock.FACING);
        for(var part:block.partProperty().getPossibleValues()){
            var pos=block.position(be.getBlockPos(),part,facing);if(!be.getLevel().hasChunkAt(pos))return false;
            var state=be.getLevel().getBlockState(pos);
            if(!state.is(block)||state.getValue(WorkstationBlock.FACING)!=facing||state.getValue(block.partProperty())!=part)return false;
        }return true;
    }
    private static boolean validTable(RefiningBlockEntity be,BlockPos expected){
        if(!be.getLevel().hasChunkAt(expected)||!(be.getLevel().getBlockEntity(expected) instanceof RefiningBlockEntity forge)||!forge.forge()||forge.busy()||!intact(forge))return false;
        var block=(WorkstationBlock)be.getBlockState().getBlock();var facing=be.getBlockState().getValue(WorkstationBlock.FACING);
        for(var part:block.partProperty().getPossibleValues())if(part.y==0)for(var dir:Direction.Plane.HORIZONTAL){
            var pos=block.position(be.getBlockPos(),part,facing).relative(dir);if(!be.getLevel().hasChunkAt(pos))continue;
            var state=be.getLevel().getBlockState(pos);
            if(state.getBlock() instanceof WorkstationBlock.RefiningTable t&&t.anchor(pos,state).equals(expected))return true;
        }return false;
    }
    public CompoundTag save(){var n=new CompoundTag();n.putString("Recipe",recipe);n.putUUID("Owner",owner);n.putLong("Table",table.asLong());n.putInt("Profession",profession);n.putInt("Furnace",furnace);n.putInt("Quality",quality);n.putInt("Paused",paused);return n;}
    public static FacilityAssembly load(CompoundTag n){
        if(!n.hasUUID("Owner")||FacilityRecipes.find(n.getString("Recipe"))==null)return null;
        var job=new FacilityAssembly(n.getString("Recipe"),n.getUUID("Owner"),BlockPos.of(n.getLong("Table")),n.getInt("Profession"),n.getInt("Furnace"),n.getInt("Quality"));job.paused=n.getInt("Paused");return job;
    }
}
