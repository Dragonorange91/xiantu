package com.longju.xiantu.talisman;
import com.longju.xiantu.registry.ModBlockEntities;
import com.longju.xiantu.material.MaterialQuality;
import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.item.SpiritStoneItem;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import java.util.*;
/** One persistent owner transaction. First stroke is the sole material/durability commit. */
public final class TalismanTableEntity extends RandomizableContainerBlockEntity {
 public static final long CAPACITY=300000000L;
 private NonNullList<ItemStack> items=NonNullList.withSize(10,ItemStack.EMPTY);
 private NonNullList<ItemStack> reserved=NonNullList.withSize(7,ItemStack.EMPTY);
 public UUID identity=UUID.randomUUID(),owner,outputOwner;public long nonce,energy;public int selection,elapsed,materialQuality,mode=2;public boolean manual,committed,sealed;
 public TalismanDrawing drawing=new TalismanDrawing();private TalismanRecipes.Recipe job;
 public TalismanTableEntity(BlockPos p,BlockState s){super(ModBlockEntities.TALISMAN.get(),p,s);}
 @Override protected NonNullList<ItemStack> getItems(){return items;}
 @Override protected void setItems(NonNullList<ItemStack> n){items=n;}
 @Override public int getContainerSize(){return 10;}
 @Override protected Component getDefaultName(){return Component.translatable("block.xiantu.talisman_writing_table");}
 @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return new TalismanMenu(id,inv,this,worldPosition);}
 @Override public <T> LazyOptional<T> getCapability(Capability<T> c,Direction side){return c==ForgeCapabilities.ITEM_HANDLER?LazyOptional.empty():super.getCapability(c,side);}
 @Override public boolean canTakeItem(Container c,int slot,ItemStack s){return false;}
 public boolean busy(){return job!=null;}
 public int total(){return job==null?0:job.ticks();}
 public TalismanRecipes.Recipe recipe(){return busy()?job:TalismanRecipes.ALL.get(Math.max(0,Math.min(TalismanRecipes.ALL.size()-1,selection)));}
 public boolean mayInput(Player p){return !busy();}
 public boolean mayOutput(Player p){return outputOwner==null||outputOwner.equals(p.getUUID());}
 public boolean owns(Player p){return owner!=null&&owner.equals(p.getUUID());}
 public String start(ServerPlayer p,boolean byHand){
  if(busy())return "制符台已有任务";var r=recipe();var d=GongfaService.data(p);
  if(!d.cultivationUnlocked()||d.majorRealm()<r.realm())return "制作境界不足";
  if(!r.knowledge().isEmpty()&&!TalismanProgress.knows(p,r.knowledge()))return "尚未学习"+TalismanBlueprints.name(r.knowledge());
  if(!mayOutput(p)||!items.get(9).isEmpty())return "请先取走成品";
  if(r.kind().equals("symbol")&&(TalismanItems.brush(items.get(0))==0||items.get(0).getMaxDamage()-items.get(0).getDamageValue()<(r.tier()+1)/2))return "符笔缺失或耐久不足";
  var next=NonNullList.withSize(7,ItemStack.EMPTY);for(int i=0;i<7;i++)next.set(i,items.get(i+1).copy());
  var consumed=NonNullList.withSize(7,ItemStack.EMPTY);int quality=5;boolean qualitySeen=false;
  if(r.kind().equals("symbol")){
   int remaining=1,grade=-1;
   for(int i=0;i<7&&remaining>0;i++){var s=next.get(i);if(!s.is(TalismanItems.PAPER.get())||TalismanItems.tier(s)!=r.tier())continue;
    int q=MaterialQuality.get(s);if(grade>=0&&q!=grade)return "同种符纸不能混用品质";grade=q;int take=Math.min(remaining,s.getCount());consumed.set(i,s.split(take));remaining-=take;
   }if(remaining>0)return "需要同阶符纸";quality=grade;qualitySeen=true;
  }
  for(var in:r.ingredients()){
   int remaining=in.count(),grade=-1;
   for(int i=0;i<7&&remaining>0;i++){var s=next.get(i);if(!in.matches(s))continue;int q=MaterialQuality.get(s);if(MaterialQuality.eligible(s)){if(grade>=0&&q!=grade)return "同种材料不能混用品质";grade=q;quality=Math.min(quality,q);qualitySeen=true;}
    int take=Math.min(remaining,s.getCount());if(!consumed.get(i).isEmpty()&&!ItemStack.isSameItemSameTags(consumed.get(i),s))throw new IllegalStateException("reservation collision");var part=s.split(take);if(consumed.get(i).isEmpty())consumed.set(i,part);else consumed.get(i).grow(take);remaining-=take;
   }if(remaining>0)return "缺少 "+in.example().getHoverName().getString()+" ×"+remaining;
  }
  // Preflight total energy without charging or taking anything on failure.
  long personal=(long)(d.power()*1000),available=mode==0?energy:mode==1?personal:energy+personal;if(available<r.energy()*1000L)return "灵力不足";
  for(int i=0;i<7;i++)items.set(i+1,next.get(i));reserved=consumed;materialQuality=qualitySeen?quality:0;
  job=r;brushForJob=TalismanItems.brush(items.get(0));owner=p.getUUID();nonce=p.getRandom().nextLong()&0x0fffffffffffffffL;manual=byHand&&r.kind().equals("symbol");elapsed=0;committed=false;sealed=false;drawing=new TalismanDrawing();setChanged();return "";
 }
 public boolean cancel(ServerPlayer p){if(!busy()||!owns(p))return false;if(!committed){for(var s:reserved)if(!s.isEmpty()){var remaining=s.copy();for(int i=1;i<=7&&!remaining.isEmpty();i++){var current=items.get(i);if(current.isEmpty()){items.set(i,remaining.copy());remaining.setCount(0);}else if(ItemStack.isSameItemSameTags(current,remaining)){int n=Math.min(remaining.getCount(),current.getMaxStackSize()-current.getCount());current.grow(n);remaining.shrink(n);}}if(!remaining.isEmpty()&&!p.getInventory().add(remaining))p.drop(remaining,false);}}clearJob();setChanged();return true;}
 private void clearJob(){job=null;owner=null;nonce=0;elapsed=0;committed=sealed=manual=false;reserved=NonNullList.withSize(7,ItemStack.EMPTY);drawing=new TalismanDrawing();}
 public void discard(){if(!committed)for(var s:reserved)if(!s.isEmpty()&&level!=null)Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.8,worldPosition.getZ()+.5,s.copy());clearJob();energy=0;setChanged();}
 public void bind(ServerPlayer p){var data=TalismanProgress.data(p);var anchors=data.getCompound("Anchors");var n=new CompoundTag();n.putLong("Pos",worldPosition.asLong());n.putUUID("Table",identity);anchors.put(p.level().dimension().location().toString(),n);data.put("Anchors",anchors);p.getPersistentData().put(TalismanProgress.KEY,data);p.displayClientMessage(Component.literal("归途印记已登记"),true);}
 public boolean stroke(ServerPlayer p,long session,int index,List<TalismanDrawing.Point> points,boolean end){
  if(!busy()||!owns(p)||nonce!=session||!manual||index!=drawing.stroke||elapsed<total()*15/100||elapsed>=total()*85/100||points.isEmpty()||points.size()>32)return false;
  int count=TalismanCatalog.find(job.id()).strokes(),windowStart=TalismanControlRules.strokeStart(total(),count,index),windowEnd=TalismanControlRules.strokeEnd(total(),count,index);
  if(elapsed<windowStart||elapsed>=windowEnd)return false;
  int before=drawing.stroke,minimum=Math.max(12,(windowEnd-windowStart)/4);for(int i=0;i<points.size();i++){var pt=points.get(i);drawing.sample(job.id(),pt.x(),pt.y(),elapsed,end&&i==points.size()-1,minimum);if(drawing.started())commit();if(drawing.stroke!=before)break;}setChanged();return true;
 }
 public boolean seal(ServerPlayer p){if(!busy()||!owns(p)||!manual||elapsed<total()*85/100||drawing.stroke!=TalismanCatalog.find(job.id()).strokes())return false;sealed=true;setChanged();return true;}
 public boolean charge(){var s=items.get(8);if(!SpiritStoneItem.isFullyCharged(s))return false;long amount=((SpiritStoneItem)s.getItem()).chargeMilli(s);if(amount>CAPACITY-energy)return false;energy+=amount;s.shrink(1);setChanged();return true;}
 private boolean debit(ServerPlayer p,long amount){var d=GongfaService.data(p);long held=(long)Math.floor(d.power()*1000);long pool=mode==1?0:Math.min(energy,amount);if(mode==0&&pool<amount||held<amount-pool)return false;energy-=pool;if(amount>pool)d.setPower(Math.max(0,d.power()-(amount-pool)/1000.));return true;}
 private void commit(){if(committed)return;committed=true;reserved=NonNullList.withSize(7,ItemStack.EMPTY);if(job.kind().equals("symbol")){var brush=items.get(0);brush.setDamageValue(brush.getDamageValue()+(job.tier()+1)/2);if(brush.getDamageValue()>=brush.getMaxDamage())items.set(0,new ItemStack(TalismanItems.BROKEN_BRUSH.get()));}}
 public static void tick(Level level,BlockPos pos,BlockState state,TalismanTableEntity be){
  be.charge();if(!be.busy())return;var p=((ServerLevel)level).getServer().getPlayerList().getPlayer(be.owner);
  if(p==null||!p.isAlive()||p.level()!=level||p.distanceToSqr(pos.getCenter())>64)return;
  if(be.manual&&(!(p.containerMenu instanceof TalismanMenu m)||m.materials!=be))return;
  if(be.elapsed>=be.total()-1&&!be.items.get(9).isEmpty())return;
  long due=(long)(be.elapsed+1)*be.job.energy()*1000/be.total()-(long)be.elapsed*be.job.energy()*1000/be.total();if(!be.debit(p,due))return;
  if(!be.manual&&be.elapsed>=be.total()*15/100)be.commit();be.elapsed++;
  if(be.elapsed>=be.total()){
   var r=be.job;int q=be.materialQuality;boolean success=true;
   if(r.kind().equals("symbol")){var def=TalismanCatalog.find(r.id());int craft=be.manual?(be.drawing.precise==def.strokes()?6:0)-Math.min(20,5*be.drawing.mistakes):0;
    boolean structure=!be.manual||be.drawing.stroke==def.strokes()&&be.sealed;
    // Brush identity survives its last valid stroke; damaged item cannot change this cached grade.
    int brush=be.brushForJob;success=structure&&p.getRandom().nextInt(100)<TalismanRules.success(def.complexity(),brush,r.tier(),TalismanProgress.level(p),craft);
    if(success){q=com.longju.xiantu.refining.RefiningRules.rollQuality(p.getRandom().nextInt(10000),TalismanRules.quality(TalismanProgress.level(p),brush,r.tier(),q));TalismanProgress.award(p,r.tier());}
   }
   be.commit();ItemStack out=success?r.output(q):new ItemStack(TalismanItems.RESIDUE.get());if(r.kind().equals("symbol")&&success)out.getOrCreateTag().putUUID("Maker",p.getUUID());be.items.set(9,out);be.outputOwner=be.owner;be.clearJob();
  }be.setChanged();if(p.tickCount%20==0)com.longju.xiantu.cultivation.CultivationEvents.sync(p);
 }
 private int brushForJob;
 public void outputTaken(){if(items.get(9).isEmpty()){outputOwner=null;setChanged();}}
 @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);ContainerHelper.saveAllItems(n,items);n.putUUID("Identity",identity);n.putLong("Energy",energy);n.putInt("Selection",selection);n.putInt("Mode",mode);if(outputOwner!=null)n.putUUID("OutputOwner",outputOwner);
  if(busy()){var j=new CompoundTag();j.putInt("Recipe",TalismanRecipes.ALL.indexOf(job));j.putUUID("Owner",owner);j.putLong("Nonce",nonce);j.putInt("Elapsed",elapsed);j.putInt("Quality",materialQuality);j.putInt("Brush",brushForJob);j.putBoolean("Committed",committed);j.putBoolean("Manual",manual);j.putBoolean("Sealed",sealed);j.put("Drawing",drawing.save());var cache=new CompoundTag();ContainerHelper.saveAllItems(cache,reserved);j.put("Reserved",cache);n.put("Job",j);}}
 @Override public void load(CompoundTag n){super.load(n);items=NonNullList.withSize(10,ItemStack.EMPTY);ContainerHelper.loadAllItems(n,items);identity=n.hasUUID("Identity")?n.getUUID("Identity"):UUID.randomUUID();energy=Math.max(0,Math.min(CAPACITY,n.getLong("Energy")));selection=Math.max(0,Math.min(TalismanRecipes.ALL.size()-1,n.getInt("Selection")));mode=Math.max(0,Math.min(2,n.getInt("Mode")));outputOwner=n.hasUUID("OutputOwner")?n.getUUID("OutputOwner"):null;clearJob();var j=n.getCompound("Job");int index=j.getInt("Recipe");if(j.hasUUID("Owner")&&index>=0&&index<TalismanRecipes.ALL.size()){job=TalismanRecipes.ALL.get(index);owner=j.getUUID("Owner");nonce=j.getLong("Nonce");elapsed=Math.max(0,Math.min(job.ticks()-1,j.getInt("Elapsed")));materialQuality=Math.max(0,Math.min(5,j.getInt("Quality")));brushForJob=Math.max(1,Math.min(3,j.getInt("Brush")));committed=j.getBoolean("Committed");manual=j.getBoolean("Manual");sealed=j.getBoolean("Sealed");drawing=TalismanDrawing.load(j.getCompound("Drawing"));if(!committed)ContainerHelper.loadAllItems(j.getCompound("Reserved"),reserved);}}
}
