package com.longju.xiantu.qa;

import com.longju.xiantu.instance.*;

import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.registry.ModItems;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Disposable save only. Uses actual player, native teleport/movement/menu/container paths. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class InstanceAudit {
    private static Run run;
    private static ItemStack previewHand;
    private static final class Run {
        ServerPlayer p;ServerLevel oldRealmLevel,caveLevel;long oldSeed;InstanceLedger.Slot firstRealm;ServerLevel original;Vec3 pos;float yaw,pitch,health;GameType mode;
        CompoundTag cultivation,persistent,returns,oldCave;ListTag inventory,deathInventory;InstanceLedger.Slot cave,realm;
        boolean keepInventory,tour;int food,phaseWait;float saturation;
        ServerLevel guardLevel;Set<ChunkPos> guardChunks=new HashSet<>();
        Map<BlockPos,BlockState> fixture=new LinkedHashMap<>();int checks,wait,phase;ItemStack token,deathAccessory;
    }
    private static void check(boolean condition,String name){run.checks++;if(!condition)throw new IllegalStateException(name);}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantuinstanceaudit").requires(s->s.hasPermission(2))
            .then(Commands.literal("start").executes(c->{start(c.getSource().getPlayerOrException());return 1;}))
            .then(Commands.literal("tour").executes(c->{start(c.getSource().getPlayerOrException());if(run!=null)run.tour=true;return 1;}))
            .then(Commands.literal("continue").executes(c->{if(run!=null){run.tour=false;run.wait=0;}return 1;}))
            .then(Commands.literal("abort").executes(c->{restore();return 1;}))
            .then(Commands.literal("tour_view").then(Commands.argument("site",com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,4)).executes(c->{
                if(run==null||!run.tour||run.phase!=2||run.p!=c.getSource().getPlayerOrException())return 0;
                double[][] views={{8,104,-25,0,55},{-65,66,28,0,35},{-14,69,72,-35,20},{45,70,25,-20,20},{64,66,86,0,0}};
                var v=views[com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"site")];var p=run.p;
                InstanceService.internal(()->p.teleportTo(p.serverLevel(),v[0],v[1],v[2],(float)v[3],(float)v[4]));return 1;
            })))
            .then(Commands.literal("menu").executes(c->{var p=c.getSource().getPlayerOrException();if(!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;InstanceMenu.open(p);return 1;}))
            .then(Commands.literal("weapon").executes(c->{var p=c.getSource().getPlayerOrException();if(run!=null||!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;if(previewHand==null)previewHand=p.getMainHandItem().copy();var recipe=com.longju.xiantu.refining.RefiningRecipes.ALL.stream().filter(r->r.theme().equals("开天斧")&&r.tier()==6).findFirst().orElseThrow();p.setItemInHand(InteractionHand.MAIN_HAND,com.longju.xiantu.refining.RefinedEquipment.finish(recipe,5));com.longju.xiantu.refining.EquipmentSkillMenu.open(p);return 1;}))
            .then(Commands.literal("restore").executes(c->{var p=c.getSource().getPlayerOrException();if(previewHand!=null){p.closeContainer();p.setItemInHand(InteractionHand.MAIN_HAND,previewHand);previewHand=null;p.inventoryMenu.broadcastChanges();}return 1;}))
            .then(Commands.literal("recover").executes(c->{var p=c.getSource().getPlayerOrException();var d=InstanceService.data(p.server);var n=d.runtime().getCompound("QARecovery");
                boolean okay=!d.blocked()&&n.hasUUID("Cave")&&d.ledger().cave(p.getUUID()).map(s->s.instance().equals(n.getUUID("Cave"))&&s.state()==InstanceLedger.State.ACTIVE).orElse(false)&&d.ledger().find(n.getString("RealmDimension")).map(s->s.state()==InstanceLedger.State.FREE&&s.generation()==n.getLong("Generation")).orElse(false);
                com.mojang.logging.LogUtils.getLogger().info("[Instance recovery QA] "+(okay?"PASS":"FAIL"));p.sendSystemMessage(Component.literal("[Instance recovery QA] "+(okay?"PASS":"FAIL")));return okay?1:0;})));
    }
    private static void start(ServerPlayer p){
        if(run!=null||!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")||p.level().dimension()!=Level.OVERWORLD){p.sendSystemMessage(Component.literal("需要空闲的Xiantu-QA主世界存档"));return;}
        var r=new Run();run=r;r.p=p;r.original=p.serverLevel();r.pos=p.position();r.yaw=p.getYRot();r.pitch=p.getXRot();r.health=p.getHealth();r.mode=p.gameMode.getGameModeForPlayer();r.keepInventory=p.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);r.food=p.getFoodData().getFoodLevel();r.saturation=p.getFoodData().getSaturationLevel();r.inventory=p.getInventory().save(new ListTag());r.persistent=p.getPersistentData().copy();r.cultivation=new CompoundTag();((PlayerCultivationData)GongfaService.data(p)).save(r.cultivation);r.returns=InstanceService.data(p.server).runtime().getCompound("Returns").copy();
        try{
            p.closeContainer();p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();var c=GongfaService.data(p);c.setCultivationUnlocked(true);c.setRealmStage(0,0);
            var d=InstanceService.data(p.server);check(!d.blocked(),"healthy persistent manifest");
            int dimensions=0;for(var s:d.slots())if(InstanceService.level(p.server,s)!=null)dimensions++;check(dimensions==272,"all 16+256 fixed levels loaded");
            var chunk=p.chunkPosition();var base=new BlockPos(chunk.getMinBlockX()+8,300,chunk.getMinBlockZ()+8);
            InstanceService.internal(()->{for(int dx=-8;dx<=8;dx++)for(int dz=-6;dz<=6;dz++){var at=base.offset(dx,0,dz);r.fixture.put(at,r.original.getBlockState(at));r.original.setBlockAndUpdate(at,Blocks.STONE.defaultBlockState());}});
            InstanceService.internal(()->p.teleportTo(r.original,base.getX()+.5,301,base.getZ()+.5,0,0));
            var existing=d.ledger().cave(p.getUUID()).orElse(null);if(existing!=null)r.oldCave=d.session(existing).copy();
            r.token=existing==null?new ItemStack(InstanceRegistry.TOKEN.get()):InstanceService.token(d,existing,"master",d.session(existing).getInt("MasterVersion"));p.setItemInHand(InteractionHand.MAIN_HAND,r.token);
            String result=InstanceService.useToken(p,r.token,false);r.cave=d.ledger().cave(p.getUUID()).orElseThrow();check(p.serverLevel()==InstanceService.level(p.server,r.cave),"create/bind/enter actual permanent cave: "+result);check(r.token.getCount()==1&&r.token.getTag().hasUUID("Cave"),"binding retains reusable signed main token");
            var l=p.serverLevel();check(l.getWorldBorder().getSize()==192&&l.getWorldBorder().getMinX()==0,"grid aligned twelve chunk build region");check(l.getBlockState(new BlockPos(0,64,0)).isAir(),"cave is void outside basic platform");
            var bad=r.token.copy();bad.getTag().putInt("Version",9999);check(InstanceService.validate(p,bad)==null,"forged master version denied");bad=r.token.copy();bad.getTag().putString("Seal","a".repeat(64));check(InstanceService.validate(p,bad)==null,"forged signature denied");
            var motion=Entity.collideBoundingBox(p,new Vec3(0,-10,0),new AABB(94,-62,94,94.6,-60.2,94.6),l,List.of());check(motion.y>=-1.001,"native entity movement hits lower virtual wall");
            motion=Entity.collideBoundingBox(p,new Vec3(0,10,0),new AABB(94,316,94,94.6,317.8,94.6),l,List.of());check(motion.y<=1.201,"native entity movement hits upper virtual wall");
            p.teleportTo(l,400,70,96,0,0);check(p.getX()<192,"native teleport outside boundary canceled");
            InstanceService.returnHome(p);check(p.level().dimension()==Level.OVERWORLD,"token exit returns to saved overworld location");
            p.teleportTo(l,96.5,65,96.5,0,0);check(p.level().dimension()==Level.OVERWORLD,"external teleport cannot replace token transaction");
            long count=d.slots().stream().filter(s->s.kind()==InstanceLedger.Kind.CAVE&&s.owner()!=null&&s.owner().equals(p.getUUID())).count();InstanceService.useToken(p,new ItemStack(InstanceRegistry.TOKEN.get()),false);check(d.slots().stream().filter(s->s.kind()==InstanceLedger.Kind.CAVE&&s.owner()!=null&&s.owner().equals(p.getUUID())).count()==count,"second unbound token cannot create a second cave");
            InstanceService.useToken(p,r.token,false);check(p.serverLevel()==l,"original token reenters same cave");
            var session=d.session(r.cave);String grant=UUID.randomUUID().toString();session.getCompound("Grants").putInt(grant,1);var child=InstanceService.token(d,r.cave,grant,1);check(InstanceService.validate(p,child)!=null,"signed child grant accepted");
            p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.LING_YU.get(),16));InstanceMenu.open(p);var menu=p.containerMenu;check(menu instanceof InstanceMenu,"manual opens actual management container");check(menu.clickMenuButton(p,0),"reissue transaction commits");check(!menu.clickMenuButton(p,0),"same closed menu cannot replay payment");check(InstanceService.validate(p,r.token)==null&&InstanceService.validate(p,child)!=null,"reissue revokes old master, preserves independent child grant");
            session.getCompound("Grants").putInt(grant,0);check(InstanceService.validate(p,child)==null,"revoked child fails later validation");
            r.token=InstanceService.token(d,r.cave,"master",session.getInt("MasterVersion"));p.setItemInHand(InteractionHand.MAIN_HAND,r.token);InstanceService.returnHome(p);
            var entry=base.offset(2,1,0);r.fixture.put(entry,r.original.getBlockState(entry));
            var params=new InstanceRules.Parameters(InstanceRules.Abundance.BARREN,InstanceRules.Size.COMPACT,16,20,true,false);String created=InstanceService.createRealm(p.server,entry,params,0,false);
            r.realm=d.slots().stream().filter(s->s.kind()==InstanceLedger.Kind.REALM&&s.state()==InstanceLedger.State.PREPARING).findFirst().orElseThrow(()->new IllegalStateException("Realm preparation rejected: "+created+"; active="+d.ledger().activeRealm()));check(!r.realm.instance().equals(r.cave.instance()),"realm and cave allocations independent: "+created);
            r.phase=1;p.sendSystemMessage(Component.literal("实例QA：洞府检查通过，等待分批准备秘境……"));
        }catch(Exception|LinkageError failure){fail(failure);}
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||run==null)return;var r=run;
        try{
            if(++r.wait>(r.tour?18000:4800))throw new IllegalStateException("instance audit timeout");var p=r.p;var d=InstanceService.data(p.server);var n=d.session(r.realm);
            if(r.phase==1){
                if(!n.getBoolean("Ready")||d.ledger().find(r.realm.dimension()).orElseThrow().state()!=InstanceLedger.State.ACTIVE)return;
                var a=InstanceRules.Abundance.BARREN;var members=n.getCompound("Members");for(int i=0;i<a.capacity;i++)members.putString(UUID.randomUUID().toString(),"inside");
                String denied=InstanceService.enterRealm(p,r.realm);check(p.level().dimension()==Level.OVERWORLD&&denied.contains("满"),"full realm rejects free admission without teleport");members.getAllKeys().clear();
                var result=InstanceService.enterRealm(p,r.realm);check(p.serverLevel()==InstanceService.level(p.server,r.realm),"actual portal admission registers then teleports: "+result);check(members.size()==1,"one UUID consumes exactly one place");
                var l=p.serverLevel();check(l.getBlockState(InstanceService.corePos(r.realm)).is(InstanceRegistry.PORTAL.get()),"walk-in return portal exists");
                var core=l.getBlockEntity(RealmLayout.get(p.server,n.getString("Theme")).rewards().get("core"));check(core instanceof Container,"native core container generated");var chest=(Container)core;int tokens=0;for(int i=0;i<chest.getContainerSize();i++)if(chest.getItem(i).is(InstanceRegistry.TOKEN.get()))tokens+=chest.getItem(i).getCount();check(tokens==1,"one persisted cave-token roll supplies exactly one core token");chest.clearContent();chest.setChanged();
                String refused=InstanceService.useToken(p,new ItemStack(InstanceRegistry.TOKEN.get()),false);check(refused.contains("天道秘境"),"temporary realm cannot create or enter permanent cave");
                InstanceService.returnHome(p);check(members.isEmpty(),"normal exit releases capacity");check(InstanceService.enterRealm(p,r.realm).contains("已进入"),"reentry allowed with new capacity check");check(chest.isEmpty(),"reentry does not refill looted core");
                // These districts extend beyond the entry player's entity-ticking area.
                // Keep only this test's otherwise-unforced chunks loaded while counting.
                r.guardLevel=l;
                for(var zone:RealmLayout.get(p.server,n.getString("Theme")).zones())
                    for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
                        var chunk=new ChunkPos((zone.x()>>4)+dx,(zone.z()>>4)+dz);
                        if(!l.getForcedChunks().contains(chunk.toLong())){l.setChunkForced(chunk.x,chunk.z,true);r.guardChunks.add(chunk);}
                    }
                InstanceContent.begin(p.server,r.realm,n);r.phase=5;return;
            }
            if(r.phase==5){
                // Admission and the immediate reentry replace player chunk tickets.
                // Entity sections are not accessible until those tickets finish loading.
                if(++r.phaseWait<100)return;
                if(r.wait%20!=0)return;var l=p.serverLevel();var a=InstanceRules.Abundance.valueOf(n.getString("Abundance"));
                var terrain=new InstanceTerrain(n.getLong("Seed"),n.getInt("Diameter"));
                check(n.getString("Theme").equals(InstanceTerrain.THEMES[terrain.theme()]),"theme persisted with session seed");
                check(l.getBlockState(new BlockPos(terrain.generationRadius()+2,63,0)).isAir(),"actual generated chunk outside two chunk buffer is void");
                int herbs=0,ores=0;for(var key:n.getAllKeys()){if(key.startsWith("Herb_")&&n.getBoolean(key))herbs++;if(key.startsWith("Node_")&&n.getBoolean(key))ores++;}
                check(herbs==10&&ores==12,"abundance scales independent herb and ore attempts once");
                var layout=RealmLayout.get(p.server,n.getString("Theme"));int realHerbs=0;for(var at:layout.herbs())if(l.getBlockState(at).getBlock() instanceof com.longju.xiantu.medicine.MedicinePlantBlock)realHerbs++;
                check(realHerbs==10,"native wild herbs survive their grass beds");
                int guards=0;for(var entity:l.getAllEntities())if(entity instanceof net.minecraft.world.entity.Mob mob&&mob.isPersistenceRequired()&&mob.getPersistentData().hasUUID("xiantu_realm_instance")&&mob.getPersistentData().getUUID("xiantu_realm_instance").equals(r.realm.instance())){
                    guards++;check(mob.getPersistentData().getString(com.longju.xiantu.entity.BeastRealmService.ELEMENT).equals(n.getString("Theme")),"guardian element matches theme");
                    check(mob.getPersistentData().getInt(com.longju.xiantu.entity.BeastResourceDrops.REALM)<=a.maxRealm,"guardian never exceeds announced realm");
                }
                check(guards==10,"quarry, herb, battle and core guardians exist: "+guards);
                check(layout.zones().stream().allMatch(z->!l.getBlockState(z.at().below()).isAir()),"independent districts have grounded floors");
                var reward=layout.rewards().get("core");check(!l.getBlockState(reward.offset(0,1,-2)).getCollisionShape(l,reward.offset(0,1,-2)).isEmpty(),"offset alcove conceals final reward");
                com.mojang.logging.LogUtils.getLogger().info("[Instance adventure QA] PASS "+r.checks+"; guardians="+guards+"; herbs="+realHerbs+"; theme="+n.getString("Theme"));
                p.sendSystemMessage(Component.literal("[Instance adventure QA] PASS "+r.checks+"; guardians="+guards+"; herbs="+realHerbs+"; theme="+n.getString("Theme")));
                releaseGuardChunks(r);r.phase=2;return;
            }
            if(r.phase==2){
                if(r.tour)return;
                if(r.wait%20!=0)return;var l=InstanceService.level(r.p.server,r.realm);check(((Container)l.getBlockEntity(RealmLayout.get(p.server,n.getString("Theme")).rewards().get("core"))).isEmpty(),"resumed completed build cannot reroll core reward");
                p.level().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false,p.server);
                ((PlayerCultivationData)GongfaService.data(p)).setProgress(120);
                p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.DIAMOND,7));p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.GOLDEN_HELMET));
                r.deathAccessory=com.longju.xiantu.refining.RefinedEquipment.finish(com.longju.xiantu.refining.RefiningRecipes.ALL.stream().filter(v->v.kind().equals("ring")&&v.tier()==1).findFirst().orElseThrow(),1);
                ((Container)new com.longju.xiantu.equipment.AccessoryInventory(p)).setItem(0,r.deathAccessory.copy());
                r.deathInventory=p.getInventory().save(new ListTag());p.setGameMode(GameType.SURVIVAL);p.invulnerableTime=0;
                p.hurt(p.damageSources().fellOutOfWorld(),Float.MAX_VALUE);check(p.isDeadOrDying(),"actual connected player dies in realm with keepInventory false");
                check(p.getInventory().save(new ListTag()).equals(r.deathInventory),"native death does not drop or erase inventory and equipped armor");
                r.phase=4;return;
            }
            if(r.phase==4){
                var replacement=r.p.server.getPlayerList().getPlayer(r.p.getUUID());
                if(replacement==null||replacement==r.p||!replacement.isAlive()||replacement.getPersistentData().getBoolean("xiantu_instance_death"))return;
                p=replacement;r.p=p;
                var actualInventory=p.getInventory().save(new ListTag());
                check(actualInventory.equals(r.deathInventory),"native respawn clone retains mainhand, armor and offhand exactly once; expected="+r.deathInventory+" actual="+actualInventory);
                check(ItemStack.matches(((Container)new com.longju.xiantu.equipment.AccessoryInventory(p)).getItem(0),r.deathAccessory),"native respawn preserves accessory slot contents");
                check(p.level().dimension()==Level.OVERWORLD,"death respawn returns to overworld");check(GongfaService.data(p).progress()==0,"ordinary cultivation death penalty still applies");
                p.setGameMode(GameType.CREATIVE);p.level().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(r.keepInventory,p.server);
                var returned=p;InstanceService.internal(()->returned.teleportTo(r.original,r.pos.x,r.pos.y,r.pos.z,r.yaw,r.pitch));
                // Reenter at the actual source portal after the native death round trip.
                var entry=BlockPos.of(n.getLong("Entry"));var again=p;InstanceService.internal(()->again.teleportTo(r.original,entry.getX()+.5,entry.getY(),entry.getZ()+.5,0,0));
                String admission=InstanceService.enterRealm(p,r.realm);check(admission.contains("已进入"),"native respawn released admission capacity: "+admission);
                n.putLong("EndAt",r.p.server.overworld().getGameTime()+InstanceRules.EARLY_RETURN_TICKS);((net.minecraft.world.level.saveddata.SavedData)d).setDirty();r.phase=3;return;
            }
            if(r.phase==3){
                if(r.p.level().dimension()!=Level.OVERWORLD)return;check(r.p.getOffhandItem().is(Items.DIAMOND)&&r.p.getOffhandItem().getCount()==7,"automatic closing keeps carried inventory");check(n.getBoolean("Frozen"),"closing freezes interactions before return");check(!InstanceService.writable(InstanceService.level(r.p.server,r.realm),new BlockPos(40,65,40)),"frozen world refuses block mutations");
                r.oldRealmLevel=InstanceService.level(p.server,r.realm);r.caveLevel=InstanceService.level(p.server,r.cave);r.oldSeed=n.getLong("Seed");r.firstRealm=r.realm;
                InstanceService.internal(()->r.oldRealmLevel.setBlockAndUpdate(new BlockPos(35,180,35),Blocks.DIAMOND_BLOCK.defaultBlockState()));
                var marker=new net.minecraft.world.entity.item.ItemEntity(r.oldRealmLevel,35.5,181,35.5,new ItemStack(Items.EMERALD,31));marker.addTag("xiantu_online_old_generation");r.oldRealmLevel.addFreshEntity(marker);
                InstanceService.close(p.server,r.realm,"QA关闭");check(d.ledger().find(r.realm.dimension()).orElseThrow().state()==InstanceLedger.State.QUARANTINED,"closed realm quarantined until handles close");check(d.ledger().reserve(InstanceLedger.Kind.REALM,null).isEmpty(),"global lock held until verified archive");r.phase=6;return;
            }
            if(r.phase==6){
                if(d.ledger().find(r.realm.dimension()).orElseThrow().state()!=InstanceLedger.State.FREE)return;
                check(d.ledger().activeRealm()==null,"online archive releases global lock without restarting server");
                check(d.runtime().getLong("NextRealmAtMillis")==InstanceRules.nextRealmAt(n.getLong("ClosedAtMillis")),"next two-hour countdown begins at close, not previous spawn or cleanup tick");
                var blank=InstanceService.level(p.server,r.realm);check(blank!=null&&blank!=r.oldRealmLevel,"fresh ServerLevel replaces closed level");
                check(blank.getChunkSource().getGenerator()!=r.oldRealmLevel.getChunkSource().getGenerator(),"fresh generator has no stale session configuration");
                check(InstanceService.level(p.server,r.cave)==r.caveLevel,"permanent cave level is unchanged by temporary cleanup");
                check(blank.getChunkSource().getLoadedChunksCount()==0,"replacement has no stale loaded chunks before configuration");
                var entry=BlockPos.of(n.getLong("Entry"));
                var params=new InstanceRules.Parameters(InstanceRules.Abundance.BARREN,InstanceRules.Size.COMPACT,16,20,false,false);
                String created=InstanceService.createRealm(p.server,entry,params,0,false);
                r.realm=d.slots().stream().filter(v->v.kind()==InstanceLedger.Kind.REALM&&v.state()==InstanceLedger.State.PREPARING).findFirst().orElseThrow();
                check(r.realm.index()==r.firstRealm.index()&&r.realm.generation()==r.firstRealm.generation()+1&&!r.realm.instance().equals(r.firstRealm.instance()),"same slot recycled online with new generation and UUID: "+created);
                check(d.session(r.realm).getLong("Seed")!=r.oldSeed,"new realm draws independent terrain seed");
                check(!d.runtime().contains("NextRealmAtMillis"),"active realm has no concurrently running next-realm timer");
                r.phase=7;return;
            }
            if(r.phase==7){
                if(!n.getBoolean("Ready")||d.ledger().find(r.realm.dimension()).orElseThrow().state()!=InstanceLedger.State.ACTIVE)return;
                var l=InstanceService.level(p.server,r.realm);check(l.getBlockState(new BlockPos(35,180,35)).isAir(),"rebuilt second realm has no old placed blocks");
                for(var entity:l.getAllEntities())check(!entity.getTags().contains("xiantu_online_old_generation"),"old entities cannot leak into new realm");
                check(!((Container)l.getBlockEntity(RealmLayout.get(p.server,n.getString("Theme")).rewards().get("core"))).isEmpty(),"second session owns newly generated rewards");
                var entry=BlockPos.of(n.getLong("Entry"));var again=p;InstanceService.internal(()->again.teleportTo(r.original,entry.getX()+.5,entry.getY(),entry.getZ()+.5,0,0));
                check(InstanceService.enterRealm(p,r.realm).contains("已进入"),"actual player enters rebuilt same-key dimension without reconnect");
                InstanceService.close(p.server,r.realm,"QA第二次在线关闭");check(p.level().dimension()==Level.OVERWORLD,"second close returns actual player online");r.phase=8;return;
            }
            if(r.phase==8){
                if(d.ledger().find(r.realm.dimension()).orElseThrow().state()!=InstanceLedger.State.FREE)return;
                check(d.ledger().activeRealm()==null,"second consecutive cleanup releases lock online");
                check(InstanceService.level(p.server,r.cave)==r.caveLevel,"same permanent cave survives two online cleanup cycles");
                var recovery=new CompoundTag();recovery.putUUID("Cave",r.cave.instance());recovery.putString("RealmDimension",r.realm.dimension());recovery.putLong("Generation",r.realm.generation());d.runtime().put("QARecovery",recovery);((net.minecraft.world.level.saveddata.SavedData)d).setDirty();
                com.mojang.logging.LogUtils.getLogger().info("[Instance online QA] PASS "+r.checks+"; generation="+r.firstRealm.generation()+"->"+r.realm.generation());p.sendSystemMessage(Component.literal("[Instance online QA] PASS "+r.checks+"；两轮秘境在线回收复用通过，无需重开。"));restore();
            }
        }catch(Exception|LinkageError failure){fail(failure);}
    }
    private static void fail(Throwable failure){com.mojang.logging.LogUtils.getLogger().info("[Instance runtime QA] FAIL "+(run==null?0:run.checks)+" "+failure);failure.printStackTrace();if(run!=null)run.p.sendSystemMessage(Component.literal("[Instance runtime QA] FAIL "+run.checks+" "+failure));restore();}
    private static void releaseGuardChunks(Run r){
        if(r.guardLevel!=null)for(var chunk:r.guardChunks)r.guardLevel.setChunkForced(chunk.x,chunk.z,false);
        r.guardChunks.clear();r.guardLevel=null;
    }
    private static void restore(){
        var r=run;if(r==null)return;run=null;try{
            releaseGuardChunks(r);if(r.realm!=null)InstanceService.close(r.p.server,r.realm,"QA结束");InstanceService.returnHome(r.p);r.p.closeContainer();
            if(r.cave!=null&&r.oldCave!=null)InstanceService.data(r.p.server).session(r.cave,r.oldCave);
            InstanceService.internal(()->{r.fixture.forEach((at,s)->r.original.setBlockAndUpdate(at,s));r.p.teleportTo(r.original,r.pos.x,r.pos.y,r.pos.z,r.yaw,r.pitch);});
            r.p.getPersistentData().getAllKeys().clear();r.p.getPersistentData().merge(r.persistent);r.p.getInventory().load(r.inventory);((PlayerCultivationData)GongfaService.data(r.p)).load(r.cultivation);r.p.setGameMode(r.mode);r.p.level().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(r.keepInventory,r.p.server);r.p.getFoodData().setFoodLevel(r.food);r.p.getFoodData().setSaturation(r.saturation);com.longju.xiantu.gongfa.GongfaService.refresh(r.p);CultivationService.applyRealmAttributes(r.p,GongfaService.data(r.p));r.p.setHealth(Math.min(r.health,r.p.getMaxHealth()));
            var d=InstanceService.data(r.p.server);d.runtime().put("Returns",r.returns);((net.minecraft.world.level.saveddata.SavedData)d).setDirty();CultivationEvents.sync(r.p);r.p.inventoryMenu.broadcastChanges();
        }catch(Exception|LinkageError failure){failure.printStackTrace();}
    }
}
