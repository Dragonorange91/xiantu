package com.longju.xiantu.qa;

import com.longju.xiantu.alchemy.*;
import com.longju.xiantu.refining.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.workstation.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class ManufacturingAudit{
    private static int checks;
    private static void check(boolean ok,String text){checks++;if(!ok)throw new IllegalStateException(text);}
    private static RefiningBlockEntity place(ServerLevel l,BlockPos at,String id){
        var block=(WorkstationBlock)WorkstationRegistry.EQUIPMENT.get(id).get();var state=((Block)block).defaultBlockState();
        WorkstationBlock.replaceAssembly(()->{for(var part:block.partProperty().getPossibleValues())l.setBlock(block.position(at,part,Direction.NORTH),state.setValue(block.partProperty(),part),2);});
        return (RefiningBlockEntity)l.getBlockEntity(at);
    }
    private static void fill(RefiningBlockEntity be,List<RefiningRecipes.Input> inputs){((Container)be).clearContent();be.alchemyAux.clearContent();for(int i=0;i<inputs.size();i++)((Container)be).setItem(4+i,inputs.get(i).example());}
    private static RefiningBlockEntity reload(ServerLevel l,RefiningBlockEntity be){var data=((BlockEntity)be).saveWithoutMetadata();var at=((BlockEntity)be).getBlockPos();BlockEntity replacement=new RefiningBlockEntity(at,l.getBlockState(at));replacement.load(data);l.setBlockEntity(replacement);return (RefiningBlockEntity)replacement;}
    @SubscribeEvent public static void register(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("xiantumanufacturingaudit").executes(c->{
        ServerPlayer p=c.getSource().getPlayerOrException();var l=p.serverLevel();
        if(!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")){c.getSource().sendFailure(Component.literal("仅限Xiantu-QA测试存档"));return 0;}
        checks=0;var originalPosition=p.position();float yaw=p.getYRot(),pitch=p.getXRot();var mode=p.gameMode.getGameModeForPlayer();
        var persistent=p.getPersistentData().copy();var inventory=p.getInventory().save(new ListTag());var data=(PlayerCultivationData)p.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new);var cultivation=new CompoundTag();data.save(cultivation);
        // Keep the fixture in the player's already tracked chunk. getChunkAt in a remote
        // chunk does not make its newly spawned entities queryable during this command.
        var playerChunk=p.chunkPosition();
        var at=new BlockPos(playerChunk.getMinBlockX()+2,300,playerChunk.getMinBlockZ()+2);var original=new LinkedHashMap<BlockPos,BlockState>();
        for(int x=-1;x<8;x++)for(int y=-1;y<3;y++)for(int z=-1;z<4;z++){var pos=at.offset(x,y,z);l.getChunkAt(pos);var state=l.getBlockState(pos);if(!state.isAir())throw new IllegalStateException("QA场地不是空地，停止放置");original.put(pos,state);}
        try{
            p.closeContainer();p.setGameMode(GameType.SURVIVAL);data.setCultivationUnlocked(true);data.setRealmStage(8,0);p.hurtTime=0;
            p.teleportTo(l,at.getX()+2.5,301,at.getZ()+3.5,180,0);
            for(int x=-1;x<8;x++)for(int z=-1;z<4;z++)l.setBlock(at.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            var be=place(l,at,"lifeng_refining_furnace");place(l,at.offset(2,0,0),"artisan_forging_table");
            p.getPersistentData().remove(RefiningProgress.KEY);
            // Actual recipe manager/serializer, not a duplicate of a hand-written matching rule.
            p.containerMenu=new CraftingMenu(0,p.getInventory());var grid=(CraftingContainer)p.containerMenu.getSlot(1).container;var starter=l.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("xiantu:lifeng_refining_furnace")).orElseThrow(()->new IllegalStateException("Starter furnace recipe missing from live RecipeManager"));
            var shaped=(ShapelessRecipe)starter;int cell=0;for(var ingredient:shaped.getIngredients())grid.setItem(cell++,ingredient.getItems()[0].copy());
            check(!shaped.matches(grid,l),"unlearned starter workbench blocked");RefiningProgress.learn(p,"facility/refining_station_1");check(shaped.matches(grid,l),"learned starter matches original workbench ingredients");
            check(!RefiningProgress.knows(p,"facility/forging_table"),"facilities learn independently");
            var dan1=FacilityRecipes.find("alchemy_furnace_1");fill(be,dan1.inputs());var noLearn=((BlockEntity)be).saveWithoutMetadata();
            check(FacilityAssembly.start(be,p,dan1).contains("设施图")&&noLearn.equals(((BlockEntity)be).saveWithoutMetadata()),"unlearned assembly no debit");
            RefiningProgress.learn(p,dan1.knowledge());check(FacilityAssembly.start(be,p,dan1).isEmpty(),"first alchemy furnace starts without profession/fire/energy");
            check(!FacilityAssembly.start(be,p,dan1).isEmpty(),"double assembly start blocked");be=reload(l,be);
            p.containerMenu=new RefiningMenu(0,p.getInventory(),be,at,false);for(int t=0;t<200;t++)FacilityAssembly.tick(l,be);
            var previousDan=((Container)be).removeItem(2,1);check(!previousDan.isEmpty()&&be.energyMilli==0&&RefiningProgress.xp(p)==0,"first assembly paid once, no energy or XP");
            FacilityAssembly.tick(l,be);check(((Container)be).getItem(2).isEmpty(),"assembly callback replay no second output");
            for(int tier=2;tier<=6;tier++){
                var dan=FacilityRecipes.find("alchemy_furnace_"+tier);RefiningProgress.learn(p,dan.knowledge());fill(be,dan.inputs());
                check(FacilityAssembly.start(be,p,dan).contains("等级"),"same-tier alchemy furnace gate "+tier);
                var upgrade=FacilityRecipes.find("refining_station_"+tier);RefiningProgress.learn(p,upgrade.knowledge());fill(be,upgrade.inputs());be.energyMilli=1000;
                check(FacilityAssembly.start(be,p,upgrade).contains("储能"),"upgrade cannot erase stored energy");be.energyMilli=0;
                check(FacilityAssembly.start(be,p,upgrade).isEmpty(),"sequential upgrade starts "+tier);be=reload(l,be);
                p.containerMenu=new RefiningMenu(0,p.getInventory(),be,at,false);for(int t=0;t<200;t++)FacilityAssembly.tick(l,be);be=(RefiningBlockEntity)l.getBlockEntity(at);
                check(be.furnace()==tier&&!be.busy()&&((Container)be).getItem(2).isEmpty(),"in-place upgrade produces no duplicate block item "+tier);
                var block=(WorkstationBlock)l.getBlockState(at).getBlock();for(var part:block.partProperty().getPossibleValues())check(l.getBlockState(block.position(at,part,Direction.NORTH)).is(block),"all eight cells upgraded");
                fill(be,dan.inputs());for(int i=0;i<dan.inputs().size();i++)if(dan.inputs().get(i).item().endsWith("_alchemy_furnace"))((Container)be).setItem(4+i,previousDan.copy());
                check(FacilityAssembly.start(be,p,dan).isEmpty(),"same-tier alchemy assembly starts "+tier);p.containerMenu=new RefiningMenu(0,p.getInventory(),be,at,false);for(int t=0;t<200;t++)FacilityAssembly.tick(l,be);
                previousDan=((Container)be).removeItem(2,1);check(previousDan.is(dan.allocationRecipe().result().getItem()),"correct upgraded alchemy result");
                check(RefiningProgress.xp(p)==0,"facilities never farm profession XP");
            }
            check(l.getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(10)).isEmpty(),"upgrades did not drop old or duplicate furnaces");
            for(String pickId:List.of("jiuyou_pickaxe","hundun_pickaxe")){
                var pick=(FacilityCraftingRecipe)l.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("xiantu:"+pickId)).orElseThrow();
                p.containerMenu=new CraftingMenu(0,p.getInventory());grid=(CraftingContainer)p.containerMenu.getSlot(1).container;grid.clearContent();
                cell=0;for(var input:((ShapelessRecipe)pick).getIngredients())grid.setItem(cell++,input.getItems()[0].copy());
                check(!((ShapelessRecipe)pick).matches(grid,l),"unlearned advanced pick blocked "+pickId);RefiningProgress.learn(p,"facility/"+pickId);
                check(((ShapelessRecipe)pick).matches(grid,l),"learned advanced pick matches workbench "+pickId);
                int high=0;for(int sample=0;sample<128;sample++){
                    var result=((ShapelessRecipe)pick).assemble(grid,l.registryAccess());check(!result.hasTag()||!result.getTag().contains("quality"),"recipe preview does not expose quality roll");
                    result.getItem().onCraftedBy(result,l,p);int firstQuality=com.longju.xiantu.mining.MiningPickaxeItem.quality(result);
                    result.getItem().onCraftedBy(result,l,p);check(firstQuality==com.longju.xiantu.mining.MiningPickaxeItem.quality(result),"repeated crafting callback cannot reroll");
                    int quality=com.longju.xiantu.mining.MiningPickaxeItem.quality(result);check(quality>=1&&quality<=5,"crafted pick legal grade");if(quality>1)high++;
                }check(high>0,"workbench pick not fixed ordinary grade "+pickId);
                // Exercise both native ResultSlot pickup and workbench shift-click.
                for(boolean quick:List.of(false,true)){
                    p.getInventory().clearContent();p.containerMenu=new CraftingMenu(0,p.getInventory(),ContainerLevelAccess.create(l,at));
                    grid=(CraftingContainer)p.containerMenu.getSlot(1).container;cell=0;
                    for(var input:((ShapelessRecipe)pick).getIngredients())grid.setItem(cell++,input.getItems()[0].copy());
                    check(p.containerMenu.getSlot(0).hasItem(),"native pick result available");
                    p.containerMenu.clicked(0,0,quick?ClickType.QUICK_MOVE:ClickType.PICKUP,p);
                    var crafted=quick?p.getInventory().items.stream().filter(s->s.getItem() instanceof com.longju.xiantu.mining.MiningPickaxeItem).findFirst().orElseThrow():p.containerMenu.getCarried();
                    check(crafted.hasTag()&&crafted.getTag().getInt("quality")>=1,"native "+(quick?"shift":"normal")+" pickup retains quality");
                    check(grid.isEmpty(),"native pick craft consumes inputs once");p.containerMenu.setCarried(ItemStack.EMPTY);
                }
            }
            for(var facility:FacilityRecipes.ALL)if(facility.id().startsWith("array_")||facility.id().endsWith("_pickaxe")){
                check(facility.inputs().stream().allMatch(i->!i.example().isEmpty()),"registered progression materials "+facility.id());
                fill(be,facility.inputs());
                if(!RefiningProgress.knows(p,facility.knowledge()))check(!FacilityAssembly.start(be,p,facility).isEmpty(),"unknown progression plan refused "+facility.id());
                RefiningProgress.learn(p,facility.knowledge());
                if(facility.id().endsWith("_pickaxe")){
                    check(FacilityAssembly.start(be,p,facility).contains("炼器等级"),"advanced pick profession gate "+facility.id());
                    p.getPersistentData().getCompound(RefiningProgress.KEY).putInt("Experience",36100);
                }
                if(facility.furnace()>be.furnace()){check(FacilityAssembly.start(be,p,facility).contains("等级"),"higher furnace gate "+facility.id());continue;}
                check(FacilityAssembly.start(be,p,facility).isEmpty(),"progression assembly starts "+facility.id());
                p.containerMenu=new RefiningMenu(0,p.getInventory(),be,at,false);
                for(int tick=0;tick<200;tick++)FacilityAssembly.tick(l,be);
                var output=((Container)be).removeItem(2,64);
                check(output.is(facility.allocationRecipe().result().getItem())&&output.getCount()==facility.count(),"correct progression output count "+facility.id());
                check(be.materials().stream().allMatch(ItemStack::isEmpty),"progression materials consumed once "+facility.id());
                FacilityAssembly.tick(l,be);check(((Container)be).getItem(2).isEmpty(),"no replay output "+facility.id());
                if(facility.id().endsWith("_pickaxe"))p.getPersistentData().getCompound(RefiningProgress.KEY).putInt("Experience",0);
            }
            int middleOffers=0;var soldKeys=new HashSet<String>();
            for(var offer:com.longju.xiantu.trade.TownTradeService.OFFERS)if(offer.kind().equals("gear")){
                var sold=RefiningRecipes.find(offer.key());
                check(sold!=null&&sold.tier()>=4&&sold.tier()<=6&&!sold.theme().equals("齐天"),"merchant sells middle equipment diagrams only");
                check(soldKeys.add(RefiningProgress.blueprintKey(offer.key())),"no duplicate middle diagram offers");middleOffers++;
            }
            check(middleOffers>0,"middle diagrams remain obtainable from merchants");
            var middle=RefiningRecipes.ALL.stream().filter(v->v.tier()==4).findFirst().orElseThrow(()->new IllegalStateException("No live tier-four equipment recipes"));
            var savedTraining=RefiningProgress.data(p).copy();var maxTraining=new CompoundTag();maxTraining.putInt("Experience",36100);p.getPersistentData().put(RefiningProgress.KEY,maxTraining);
            check(be.eligibility(p,middle).contains("器方"),"fourth tier crafting needs a learned diagram");
            var low=RefiningRecipes.ALL.stream().filter(v->v.tier()==3).findFirst().orElseThrow(()->new IllegalStateException("No live tier-three equipment recipes"));
            check(be.eligibility(p,low).contains("器道真解"),"low-tier equipment requires profession treatise");
            var treatise=new ItemStack(CraftingTreatises.REFINING.get());((net.minecraft.world.entity.LivingEntity)p).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,treatise);
            var study=new BlueprintLearningMenu(0,p.getInventory(),net.minecraft.world.InteractionHand.MAIN_HAND);p.containerMenu=study;
            check(((AbstractContainerMenu)study).clickMenuButton(p,0)&&treatise.isEmpty()&&RefiningProgress.knows(p,CraftingTreatises.REFINING_KEY),"real treatise confirmation unlocks refining and consumes once");
            check(!((AbstractContainerMenu)study).clickMenuButton(p,0),"treatise replay rejected");
            var repeatBook=new ItemStack(CraftingTreatises.REFINING.get());((net.minecraft.world.entity.LivingEntity)p).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,repeatBook);var repeatStudy=new BlueprintLearningMenu(0,p.getInventory(),net.minecraft.world.InteractionHand.MAIN_HAND);p.containerMenu=repeatStudy;
            check(((AbstractContainerMenu)repeatStudy).clickMenuButton(p,0)&&repeatBook.getCount()==1,"known profession treatise never consumes a second copy");
            check(be.eligibility(p,low).isEmpty(),"third tier requires no individual diagram after treatise");
            RefiningProgress.learn(p,middle.id());
            for(var sibling:RefiningRecipes.ALL)if(sibling.output().equals(middle.output()))check(RefiningProgress.knows(p,sibling.id()),"one diagram unlocks named item tiers "+sibling.tier());
            var upper=RefiningRecipes.ALL.stream().filter(v->v.tier()==7).findFirst().orElseThrow(()->new IllegalStateException("No live tier-seven equipment recipes"));RefiningProgress.learn(p,upper.id().replaceFirst("_tier_7$","_tier_4"));
            check(RefiningProgress.knows(p,upper.id()),"same named item also unlocks seventh tier");
            check(!RefiningProgress.knows(p,"unrelated_tier_4"),"unrelated equipment not unlocked");
            p.getPersistentData().put(RefiningProgress.KEY,savedTraining);
            // Generate from Minecraft's real loot table, then learn every resulting special diagram.
            var qitianIds=Set.of("jingu_staff","fengchi_crown","suozi_chestplate","liujin_leggings","ousi_boots");var seenQitian=new HashSet<String>();
            var bossLoot=p.server.getLootData().getLootTable(new net.minecraft.resources.ResourceLocation("xiantu:chests/ruins/boss_monkey"));
            for(int draw=0;draw<128;draw++){
                var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(l).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,p.position()).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
                var loot=bossLoot.getRandomItems(params);var papers=loot.stream().filter(v->v.is(RefiningItems.BLUEPRINT.get())).toList();
                check(papers.size()==1&&papers.get(0).getCount()==1,"Boss reward generates exactly one Qitian diagram");
                var paper=papers.get(0);String id=RefiningItems.knowledge(paper);check(qitianIds.contains(id),"Boss diagram has valid registered knowledge");seenQitian.add(id);
                check(RefiningRecipes.find(id)!=null,"special recipe is available after explicit activation");
            }
            check(seenQitian.equals(qitianIds),"all five Qitian diagrams occur in Boss table");
            for(String id:qitianIds){
                var paper=new ItemStack(RefiningItems.BLUEPRINT.get());paper.getOrCreateTag().putString("Recipe",id);paper.getOrCreateTag().putInt("BlueprintVersion",1);
                check(!RefiningItems.diagramName(paper).isEmpty(),"special diagram has its equipment name");
                ((net.minecraft.world.entity.LivingEntity)p).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,paper);
                var specialLearning=new BlueprintLearningMenu(0,p.getInventory(),net.minecraft.world.InteractionHand.MAIN_HAND);p.containerMenu=specialLearning;
                check(((AbstractContainerMenu)specialLearning).clickMenuButton(p,0)&&paper.isEmpty()&&RefiningProgress.knows(p,id),"special diagram can be confirmed and consumed once");
            }
            var diagram=RefiningItems.facilityDiagram("forging_table");diagram.setCount(2);((net.minecraft.world.entity.LivingEntity)p).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,diagram);
            var learning=new BlueprintLearningMenu(0,p.getInventory(),net.minecraft.world.InteractionHand.MAIN_HAND);p.containerMenu=learning;
            check(((AbstractContainerMenu)learning).clickMenuButton(p,0)&&diagram.getCount()==1,"confirmation consumes one original diagram");
            check(!((AbstractContainerMenu)learning).clickMenuButton(p,0)&&diagram.getCount()==1,"replayed confirmation does not consume twice");
            learning=new BlueprintLearningMenu(0,p.getInventory(),net.minecraft.world.InteractionHand.MAIN_HAND);p.containerMenu=learning;
            check(((AbstractContainerMenu)learning).clickMenuButton(p,0)&&diagram.getCount()==1,"known diagram never consumed");
            diagram=RefiningItems.facilityDiagram("refining_station_2");((net.minecraft.world.entity.LivingEntity)p).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,diagram);
            learning=new BlueprintLearningMenu(0,p.getInventory(),net.minecraft.world.InteractionHand.MAIN_HAND);p.containerMenu=learning;
            ((net.minecraft.world.entity.LivingEntity)p).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,RefiningItems.facilityDiagram("refining_station_3"));
            check(!((AbstractContainerMenu)learning).clickMenuButton(p,0)&&diagram.getCount()==1,"swapped hand invalidates confirmation");
            var mixed=RefiningItems.facilityDiagram("forging_table");mixed.getOrCreateTag().putString("Alchemy","pill_03");
            check(RefiningItems.knowledge(mixed).isEmpty(),"mixed diagram types rejected");
            var badVersion=RefiningItems.facilityDiagram("forging_table");badVersion.getOrCreateTag().putInt("BlueprintVersion",99);
            check(RefiningItems.knowledge(badVersion).isEmpty(),"unknown diagram version rejected");
            var trained=new CompoundTag();trained.putInt("Experience",36100);p.getPersistentData().put(RefiningProgress.KEY,trained);
            try(var stream=RefiningRecipes.class.getResourceAsStream("/data/xiantu/refining/catalog.json")){
                var catalogue=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();int special=0;
                for(var entry:catalogue.getAsJsonArray("recipes")){var spec=entry.getAsJsonObject();if(!spec.get("theme").getAsString().equals("齐天"))continue;special++;
                    var specialRecipe=new RefiningRecipes.Recipe(spec.get("id").getAsString(),spec.get("name").getAsString(),spec.get("output").getAsString(),spec.get("tier").getAsInt(),spec.get("kind").getAsString(),"","齐天",spec.get("profession").getAsInt(),spec.get("furnace").getAsInt(),spec.get("energy").getAsInt(),spec.get("seconds").getAsInt(),List.of());
                    RefiningProgress.learn(p,specialRecipe.id());
                    var station=place(l,at.offset(4,0,0),"tiangong_refining_furnace");check(station.eligibility(p,specialRecipe).contains("等级"),"Qitian rejected by fourth furnace");
                    station=place(l,at.offset(4,0,0),"zaohua_refining_furnace");check(station.eligibility(p,specialRecipe).isEmpty(),"Qitian fifth furnace gate accepted");
                    station=place(l,at.offset(4,0,0),"kaitian_refining_furnace");check(station.eligibility(p,specialRecipe).isEmpty(),"Qitian sixth furnace gate accepted");
                    check(RefiningRecipes.find(specialRecipe.id())!=null,"all Qitian manufacturing recipes are enabled");
                }check(special==5,"all five Qitian furnace specifications checked");
            }catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}
            be=place(l,at.offset(4,0,0),"hongmeng_alchemy_furnace");var r=AlchemyRecipes.find("pill_03");
            check(AlchemyJob.eligibility(be,p,r).contains("丹道真解"),"refining treatise does not unlock alchemy");
            var danBook=new ItemStack(CraftingTreatises.ALCHEMY.get());((net.minecraft.world.entity.LivingEntity)p).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,danBook);
            var danStudy=new BlueprintLearningMenu(0,p.getInventory(),net.minecraft.world.InteractionHand.MAIN_HAND);p.containerMenu=danStudy;
            check(((AbstractContainerMenu)danStudy).clickMenuButton(p,0)&&danBook.isEmpty(),"alchemy treatise confirmed and consumed once");
            for(var lowPill:AlchemyRecipes.ALL)if(lowPill.rank()<=3){check(RefiningProgress.knows(p,lowPill.knowledge()),"all low pills share treatise knowledge");var removed=new ItemStack(RefiningItems.BLUEPRINT.get());removed.getOrCreateTag().putString("Alchemy",lowPill.id());check(RefiningItems.knowledge(removed).isEmpty(),"low pill individual diagrams are invalid");}
            for(var offer:com.longju.xiantu.trade.TownTradeService.OFFERS){if(offer.kind().equals("alchemy"))check(AlchemyRecipes.find(offer.key()).rank()>=4,"no low pill diagrams sold");if(offer.kind().equals("treatise"))check(offer.rank()==2&&offer.price()==3&&offer.purchases()==0&&!offer.stack().isEmpty(),"entry treatises are available for three tier-two stones");}
            fill(be,r.inputs());
            ((Container)be).setItem(3,new ItemStack(SpiritFire.COMMON.get()));be.energyMilli=1000000;
            var aux=new ItemStack(com.longju.xiantu.registry.ModItems.DEMON_CORE.get(),2);com.longju.xiantu.item.DemonCoreItem.setData(aux,1,r.element());be.alchemyAux.setItem(0,aux);
            p.containerMenu=new AlchemyMenu(0,p.getInventory(),be);
            check(((AbstractContainerMenu)p.containerMenu).clickMenuButton(p,999)&&((AlchemyMenu)p.containerMenu).recipe()==null,"empty recipe filter clears server selection");
            var beforeEmptyStart=((BlockEntity)be).saveWithoutMetadata();check(!((AbstractContainerMenu)p.containerMenu).clickMenuButton(p,0),"removed manual start rejected");((AbstractContainerMenu)p.containerMenu).clickMenuButton(p,1);
            check(beforeEmptyStart.equals(((BlockEntity)be).saveWithoutMetadata()),"empty selection cannot start previous recipe or consume materials");
            ((AbstractContainerMenu)p.containerMenu).clickMenuButton(p,1000+AlchemyRecipes.ALL.indexOf(r));
            check(AlchemyJob.start(be,p,r).isEmpty(),"timed alchemy starts and locks required + auxiliary cores");
            check(be.alchemyAux.getItem(0).isEmpty()&&be.materials().stream().allMatch(ItemStack::isEmpty),"materials moved into inaccessible work cache");
            be=reload(l,be);p.containerMenu=new AlchemyMenu(0,p.getInventory(),be);check(be.alchemyJob!=null&&!be.alchemyJob.committed,"uncommitted work cache survives reload");
            for(int t=0;t<10;t++)RefiningBlockEntity.tick(l,((BlockEntity)be).getBlockPos(),l.getBlockState(((BlockEntity)be).getBlockPos()),be);
            long afterWarm=be.energyMilli;var oldJob=be.alchemyJob;oldJob.cancel(be);check(be.energyMilli==afterWarm&&be.alchemyAux.getItem(0).getCount()==2,"early cancel restores cores but not energy");
            var afterRefund=((BlockEntity)be).saveWithoutMetadata();oldJob.cancel(be);check(afterRefund.equals(((BlockEntity)be).saveWithoutMetadata()),"stale cancellation cannot duplicate cached inputs");
            for(int i=0;i<r.inputs().size();i++)check(((Container)be).getItem(4+i).getCount()==r.inputs().get(i).count(),"early cancel restores every ingredient");
            // Guaranteed success through the real server completion path; no random flaky test.
            var progress=new CompoundTag();progress.putInt("Experience",36100);p.getPersistentData().put(AlchemyProgress.KEY,progress);be.energyMilli=1000000;
            check(AlchemyJob.start(be,p,r).isEmpty(),"automatic alchemy starts");int fireBefore=SpiritFire.remaining(((Container)be).getItem(3));long energyBefore=be.energyMilli;
            p.containerMenu=p.inventoryMenu;RefiningBlockEntity.tick(l,((BlockEntity)be).getBlockPos(),l.getBlockState(((BlockEntity)be).getBlockPos()),be);check(be.elapsed==0&&be.energyMilli==energyBefore,"closed GUI freezes energy and work");p.containerMenu=new AlchemyMenu(0,p.getInventory(),be);
            for(int t=0;t<AlchemyRules.ticks(r.rank());t++)RefiningBlockEntity.tick(l,((BlockEntity)be).getBlockPos(),l.getBlockState(((BlockEntity)be).getBlockPos()),be);
            var output=((Container)be).getItem(2);check(output.is(PillCatalog.ITEMS.get(r.id()).get())&&output.getCount()>=1&&output.getCount()<=2,"actual alchemy output and optional extra pill");
            check(!((Container)be).canTakeItem(new SimpleContainer(1),2,output),"hopper cannot steal reserved product");
            check(be.energyMilli==energyBefore-(long)AlchemyRules.energy(r.rank())*1000,"exact per-tick energy, no second fee");check(SpiritFire.remaining(((Container)be).getItem(3))==fireBefore-AlchemyRules.ticks(r.rank())/20,"fire burns once per active second");
            var result=output.copy();RefiningBlockEntity.tick(l,((BlockEntity)be).getBlockPos(),l.getBlockState(((BlockEntity)be).getBlockPos()),be);check(ItemStack.matches(result,((Container)be).getItem(2)),"no repeated result after completion");
            fill(be,r.inputs());((Container)be).setItem(3,new ItemStack(SpiritFire.COMMON.get()));be.energyMilli=1000000;
            check(AlchemyJob.start(be,p,r).isEmpty(),"late cancellation fixture starts");
            while(!be.alchemyJob.committed)RefiningBlockEntity.tick(l,((BlockEntity)be).getBlockPos(),l.getBlockState(((BlockEntity)be).getBlockPos()),be);
            be=reload(l,be);p.containerMenu=new AlchemyMenu(0,p.getInventory(),be);check(be.alchemyJob.committed,"committed state survives reload");
            be.alchemyJob.cancel(be);check(be.materials().stream().allMatch(ItemStack::isEmpty),"late cancellation cannot refund consumed ingredients");
            fill(be,r.inputs());((Container)be).setItem(3,new ItemStack(SpiritFire.COMMON.get()));be.energyMilli=1000000;
            check(AlchemyJob.start(be,p,r).isEmpty(),"uncommitted break fixture starts");var brokenJob=be.alchemyJob;
            l.destroyBlock(((BlockEntity)be).getBlockPos(),false);
            var drops=l.getEntitiesOfClass(ItemEntity.class,new AABB(at.offset(4,0,0)).inflate(3));
            check(l.getBlockState(at.offset(4,0,0)).isAir()&&be.alchemyJob==null,"active furnace really removed and cache retired");
            for(var input:r.inputs())check(drops.stream().noneMatch(item->input.matches(item.getItem())),"breaking active furnace forfeits invested input "+input.item());
            int beforeRepeatedDrop=drops.stream().mapToInt(item->item.getItem().getCount()).sum();brokenJob.cancel(be);
            check(l.getEntitiesOfClass(ItemEntity.class,new AABB(at.offset(4,0,0)).inflate(3)).stream().mapToInt(item->item.getItem().getCount()).sum()==beforeRepeatedDrop,"break followed by stale cancel cannot duplicate inputs");
            for(var item:drops)((net.minecraft.world.entity.Entity)item).discard();
            be=place(l,at.offset(4,0,0),"hongmeng_alchemy_furnace");fill(be,r.inputs());
            var beforeIdleDrops=l.getEntitiesOfClass(ItemEntity.class,new AABB(at.offset(4,0,0)).inflate(3)).stream().map(item->((net.minecraft.world.entity.Entity)item).getUUID()).collect(java.util.stream.Collectors.toSet());
            check(l.destroyBlock(at.offset(4,0,0),false),"idle furnace removed");
            var idleDrops=l.getEntitiesOfClass(ItemEntity.class,new AABB(at.offset(4,0,0)).inflate(3)).stream().filter(item->!beforeIdleDrops.contains(((net.minecraft.world.entity.Entity)item).getUUID())).toList();
            for(var input:r.inputs())check(idleDrops.stream().filter(item->input.matches(item.getItem())).mapToInt(item->item.getItem().getCount()).sum()==input.count(),"idle furnace drops exact unused input "+input.item()+" expected="+input.count()+" actual="+idleDrops.stream().filter(item->input.matches(item.getItem())).mapToInt(item->item.getItem().getCount()).sum()+" drops="+idleDrops.stream().map(item->item.getItem().toString()).toList()+" busy="+be.busy());
            for(var item:idleDrops)((net.minecraft.world.entity.Entity)item).discard();
            for(var recipe:AlchemyRecipes.ALL){for(var input:recipe.inputs())check(!input.example().isEmpty(),"registered alchemy ingredient "+input.item());check(PillCatalog.get(recipe.pill()).rank()==recipe.rank(),"recipe and pill effect rank match");}
            be=place(l,at.offset(4,0,0),"hongmeng_alchemy_furnace");checks+=ManufacturingCodeAudit.run(p,be);
            c.getSource().sendSuccess(()->Component.literal("[Manufacturing QA] PASS "+checks+" assertions"),false);return 1;
        }catch(Exception | LinkageError failure){com.mojang.logging.LogUtils.getLogger().error("Manufacturing failure after {} assertions",checks,failure);c.getSource().sendFailure(Component.literal("[Manufacturing QA] FAIL "+checks+": "+failure));throw failure;}
        finally{
            p.containerMenu=p.inventoryMenu;
            WorkstationBlock.replaceAssembly(()->original.forEach((pos,state)->l.setBlock(pos,state,2)));
            p.getInventory().load(inventory);p.getPersistentData().getAllKeys().stream().toList().forEach(p.getPersistentData()::remove);p.getPersistentData().merge(persistent);data.load(cultivation);com.longju.xiantu.gongfa.GongfaService.refresh(p);CultivationService.applyRealmAttributes(p,data);p.setGameMode(mode);p.teleportTo(l,originalPosition.x,originalPosition.y,originalPosition.z,yaw,pitch);p.inventoryMenu.broadcastChanges();CultivationEvents.sync(p);
        }
    }));}
}
