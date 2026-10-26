package com.longju.xiantu.qa;
import com.google.gson.GsonBuilder;
import com.longju.xiantu.refining.RefiningProgress;
import com.longju.xiantu.item.SpiritStoneItem;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import java.nio.file.*;
/** Actual RecipeManager matches, including vanilla and tag overlap; four grids per tick. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class RecipeAudit {
 private record Sample(CraftingRecipe recipe,List<ItemStack> cells,String variant){}
 private static SpellAudit.Session state;
 private static final Deque<Sample> pending=new ArrayDeque<>();
 private static final List<Map<String,Object>> conflicts=new ArrayList<>(),failures=new ArrayList<>();
 private static List<CraftingRecipe> recipes;private static CraftingContainer grid;
 private static int samples,checks,custom;private static final Set<String> reachable=new TreeSet<>();
 private static void add(CraftingRecipe r,int alternate,boolean mirror,int dx,int dy){
  var cells=new ArrayList<ItemStack>(Collections.nCopies(9,ItemStack.EMPTY));var ingredients=r.getIngredients();
  for(int i=0;i<ingredients.size();i++){var options=ingredients.get(i).getItems();if(options.length==0)continue;
   var stack=options[Math.floorMod(alternate,options.length)].copy();stack.setCount(1);int target=i;
   if(r instanceof ShapedRecipe s){int x=i%s.getWidth(),y=i/s.getWidth();target=(dy+y)*3+dx+(mirror?s.getWidth()-1-x:x);}
   else if(mirror)target=8-i;cells.set(target,stack);
  }pending.add(new Sample(r,cells,"choice="+alternate+" mirror="+mirror+" shift="+dx+","+dy));
 }
 @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xianturecipeaudit").requires(s->s.hasPermission(2)).executes(c->{
  var p=c.getSource().getPlayerOrException();if(state!=null||!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
  state=new SpellAudit.Session(p);pending.clear();conflicts.clear();failures.clear();reachable.clear();samples=checks=custom=0;
  try{p.closeContainer();p.getInventory().clearContent();state.data.setCultivationUnlocked(true);state.data.setRealmStage(8,0);
   recipes=p.serverLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING);
   for(var r:recipes)if(r instanceof com.longju.xiantu.refining.FacilityCraftingRecipe f)RefiningProgress.learn(p,"facility/"+f.facility);
   var table=p.blockPosition().below();state.blocks.put(table,state.level.getBlockState(table));state.level.setBlock(table,net.minecraft.world.level.block.Blocks.CRAFTING_TABLE.defaultBlockState(),2);
   p.containerMenu=new CraftingMenu(0,p.getInventory(),ContainerLevelAccess.create(state.level,table));grid=(CraftingContainer)p.containerMenu.getSlot(1).container;
   for(var r:recipes)if(r.getId().getNamespace().equals("xiantu")){
    if(r instanceof com.longju.xiantu.medicine.SpiritSoilRecipe||r instanceof com.longju.xiantu.item.SpiritStoneDowncraftRecipe)custom++;
    int choices=Math.min(16,r.getIngredients().stream().mapToInt(i->i.getItems().length).max().orElse(1));
    if(r instanceof ShapedRecipe s){for(int dy=0;dy<=3-s.getHeight();dy++)for(int dx=0;dx<=3-s.getWidth();dx++)for(boolean m:List.of(false,true))add(r,0,m,dx,dy);for(int a=1;a<choices;a++)add(r,a,false,0,0);}
    else if(r instanceof ShapelessRecipe){add(r,0,false,0,0);add(r,0,true,0,0);for(int a=1;a<choices;a++)add(r,a,false,0,0);}
   }if(custom!=14)throw new IllegalStateException("Expected 14 special CRAFTING recipes, found "+custom);
   p.sendSystemMessage(Component.literal("[Recipe QA] START samples="+pending.size()));return 1;
  }catch(Exception|LinkageError x){finish(x.toString());return 0;}
 }));}
 private static void sample(Sample s){
  grid.clearContent();for(int i=0;i<9;i++)grid.setItem(i,s.cells.get(i).copy());samples++;
  if(!s.recipe.matches(grid,state.level)){failures.add(Map.of("id",s.recipe.getId().toString(),"variant",s.variant,"reason","own recipe did not match"));return;}
  reachable.add(s.recipe.getId().toString());var output=s.recipe.assemble(grid,state.level.registryAccess());
  long occupied=s.recipe.getIngredients().stream().filter(i->!i.isEmpty()).count();
  for(var other:recipes){if(!other.isSpecial()&&other.getIngredients().stream().filter(i->!i.isEmpty()).count()!=occupied)continue;checks++;
   if(other.matches(grid,state.level)){var result=other.assemble(grid,state.level.registryAccess());
    if(!ItemStack.isSameItemSameTags(output,result)||output.getCount()!=result.getCount())conflicts.add(Map.of("first",s.recipe.getId().toString(),"other",other.getId().toString(),"variant",s.variant));
   }
  }
  if(s.recipe instanceof com.longju.xiantu.medicine.SpiritSoilRecipe||s.recipe instanceof com.longju.xiantu.item.SpiritStoneDowncraftRecipe){
   for(int i=0;i<9;i++)if(grid.getItem(i).getItem() instanceof SpiritStoneItem stone){stone.setChargeMilli(grid.getItem(i),stone.maxChargeMilli()-1);checks++;if(s.recipe.matches(grid,state.level))failures.add(Map.of("id",s.recipe.getId().toString(),"reason","partially consumed stone accepted"));break;}
   grid.clearContent();for(int i=0;i<9;i++)grid.setItem(i,s.cells.get(i).copy());
   var nativeOutput=state.p.containerMenu.getSlot(0).getItem();checks++;
   if(!ItemStack.isSameItemSameTags(output,nativeOutput)||output.getCount()!=nativeOutput.getCount())failures.add(Map.of("id",s.recipe.getId().toString(),"reason","native result slot differs"));
   state.p.containerMenu.clicked(0,0,ClickType.PICKUP,state.p);checks++;
   if(!grid.isEmpty()||!ItemStack.matches(state.p.containerMenu.getCarried(),output))failures.add(Map.of("id",s.recipe.getId().toString(),"reason","native pickup/consumption differs"));
   state.p.containerMenu.setCarried(ItemStack.EMPTY);
  }
 }
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END||state==null)return;
  try{for(int i=0;i<4&&!pending.isEmpty();i++)sample(pending.remove());if(pending.isEmpty())finish("");}catch(Exception|LinkageError x){finish(x.toString());}
 }
 private static void finish(String error){if(state==null)return;var s=state;state=null;try{
  var report=new LinkedHashMap<String,Object>();report.put("status",error.isEmpty()&&conflicts.isEmpty()&&failures.isEmpty()?"PASS":"FAIL");report.put("error",error);report.put("samples",samples);report.put("candidateMatches",checks);report.put("reachableCount",reachable.size());report.put("specialCraftingTypeCount",custom);report.put("conflicts",conflicts);report.put("failures",failures);report.put("reachable",reachable);report.put("scope","Actual 3x3 workbench; shifted/mirrored shaped patterns; both shapeless orders; up to 16 correlated tag representatives. Does not enumerate Cartesian tag combinations or third-party recipes.");
  Files.writeString(s.p.server.getWorldPath(LevelResource.ROOT).resolve("recipe-audit.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
  s.p.sendSystemMessage(Component.literal("[Recipe QA] "+report.get("status")+" samples="+samples+" conflicts="+conflicts.size()+" failures="+failures.size()+" "+error));
 }catch(Exception x){throw new RuntimeException(x);}finally{if(grid!=null)grid.clearContent();s.p.containerMenu.setCarried(ItemStack.EMPTY);s.finish();pending.clear();}}
 private RecipeAudit(){}
}
