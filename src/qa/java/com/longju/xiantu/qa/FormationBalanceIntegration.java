package com.longju.xiantu.qa;

import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.formation.*;
import com.longju.xiantu.item.*;
import com.longju.xiantu.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import java.util.*;
import org.apache.logging.log4j.LogManager;

/** Disposable, loaded-world transactions. Never advances the server wall clock or claims full-session timing. */
public final class FormationBalanceIntegration {
    private static int checks;
    private static boolean guard(ServerPlayer p) {
        return p.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA");
    }
    private static void check(boolean ok, String label) {
        if (!ok) throw new IllegalStateException("[Formation balance FAIL] "+label);
        checks++; LogManager.getLogger("Xiantu QA").info("[Formation balance PASS] "+label);
    }
    private static void ticks(ServerLevel l, FormationCoreBlockEntity c, int n) {
        for(int i=0;i<n;i++) FormationCoreBlockEntity.serverTick(l,((BlockEntity)c).getBlockPos(),((BlockEntity)c).getBlockState(),c);
    }
    private static void meditate(FormationCoreBlockEntity core, ServerPlayer player, int seconds) {
        if (!core.canMeditate() || core.unsafe(player)) return;
        var level=player.serverLevel();
        FormationSeatEntity seat=ModEntities.FORMATION_SEAT.get().create(level);
        BlockPos pos=((BlockEntity)core).getBlockPos().above();
        seat.setCushionPos(pos);((Entity)seat).setPos(pos.getX()+.5,pos.getY()+.05,pos.getZ()+.5);
        level.addFreshEntity(seat);player.startRiding(seat,true);
        try { for(int i=0;i<seconds;i++) ChunkAuraService.settleSecond(level); }
        finally {player.stopRiding();((Entity)seat).discard();}
    }
    private static void build(ServerLevel l,BlockPos c,int tier,Map<BlockPos,BlockState> backup) {
        int r=FormationLayout.outerRadius(tier);
        for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)for(int y=0;y<=tier+2;y++) {
            BlockPos p=c.offset(x,y,z); backup.putIfAbsent(p,l.getBlockState(p));
        }
        l.setBlock(c,FormationStructure.coreBlock(tier).defaultBlockState(),3);
        l.setBlock(c.above(),ModBlocks.MEDITATION_CUSHION.get().defaultBlockState(),3);
        for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++) {
            BlockPos p=c.offset(x,0,z);
            int baseTier=FormationLayout.baseRingTierAt(tier,x,z);
            int runeTier=FormationLayout.runeRingTierAt(tier,x,z);
            if(baseTier>0)l.setBlock(p,FormationStructure.partBlock(FormationPartBlock.Part.BASE,baseTier).defaultBlockState(),3);
            else if(runeTier>0)l.setBlock(p,FormationStructure.partBlock(FormationPartBlock.Part.RUNE,runeTier).defaultBlockState(),3);
        }
        for(int ringTier=1;ringTier<=tier;ringTier++) {
            int ringRadius=FormationLayout.outerRadius(ringTier);
            for(Direction d:Direction.Plane.HORIZONTAL)for(int y=1;y<=ringTier;y++)
                l.setBlock(c.relative(d,ringRadius).above(y),FormationStructure.partBlock(FormationPartBlock.Part.PILLAR,ringTier).defaultBlockState(),3);
        }
    }
    private static BlockPos site(ServerLevel l) {
        for(int attempt=0;attempt<32;attempt++) {
            BlockPos c=new BlockPos(7040+attempt*128,-61,1408);
            boolean clear=true;
            for(int x=-30;x<=30 && clear;x++)for(int z=-30;z<=158 && clear;z++)for(int y=1;y<=6;y++) {
                BlockPos p=c.offset(x,y,z);
                if(!l.getBlockState(p).isAir()) { clear=false; break; }
            }
            if(clear)return c;
        }
        throw new IllegalStateException("No clear isolated formation site");
    }
    public static int run(ServerPlayer player) {
        if(!guard(player))return 0;
        checks=0;
        ServerLevel level=player.serverLevel();
        PlayerCultivationData data=(PlayerCultivationData)player.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new);
        CompoundTag saved=new CompoundTag();data.save(saved);
        CompoundTag methodPersistent=player.getPersistentData().copy();
        Vec3 position=player.position(); float health=player.getHealth(); int hurt=player.hurtTime;
        Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();
        Set<UUID> priorDrops=new HashSet<>();
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(7000,-64,1360,11200,-40,1600)).forEach(e->priorDrops.add(e.getUUID()));
        try {
            data.setPhysique(PhysiqueType.ORDINARY);
            player.getPersistentData().putDouble("xiantu_physique_power_max_bonus",0);
            BlockPos start=site(level);
            data.setCultivationUnlocked(true);data.setCharacterInitialized(true);data.gongfa().clear();
            data.setRealmStage(0,0); data.setProgress(0);data.setPowerMax(100);data.setPower(0);data.setInnerDemon(0);player.hurtTime=0;
            data.resetSpiritRoots();data.addSpiritRoot(SpiritRootType.values()[0]);data.addSpiritRoot(SpiritRootType.values()[1]);data.addSpiritRoot(SpiritRootType.values()[2]);
            for(int tier=1;tier<=3;tier++) {
                BlockPos center=start.offset(0,0,(tier-1)*64);
                build(level,center,tier,blocks);
                FormationCoreBlockEntity core=(FormationCoreBlockEntity)level.getBlockEntity(center);
                check(FormationStructure.isComplete(level,center,tier),"Circular structure tier "+tier);
                ticks(level,core,1);
                int radius=FormationLayout.outerRadius(tier);
                verifyTraces(level,center,tier);
                var probe=FormationTrace.ringStage(tier,true).get(1);
                BlockPos joint=center.offset(probe.x(),0,probe.z());BlockState jointState=level.getBlockState(joint);
                level.setBlock(joint,Blocks.AIR.defaultBlockState(),3);ticks(level,core,1);
                check(!FormationStructure.isComplete(level,center,tier),"Missing trace tile invalidates array "+tier);
                level.setBlock(joint,jointState.setValue(FormationPartBlock.TRACE,0),3);ticks(level,core,1);
                check(level.getBlockState(joint).getValue(FormationPartBlock.TRACE)==probe.trace(),"Replaced tile automatically rejoins ring "+tier);
                for(int y=1;y<=tier;y++) {
                    BlockPos p=center.relative(Direction.EAST,radius).above(y);
                    BlockState state=level.getBlockState(p);
                    int expectedSection=tier==1?0:y==1?1:y==tier?3:2;
                    check(state.getValue(FormationPartBlock.SECTION)==expectedSection,"Native pillar stack section "+tier+"/"+y);
                    var bounds=state.getCollisionShape(level,p).bounds();
                    check(bounds.getXsize()<1&&bounds.getZsize()<1,"Pillar collision is slimmer than full cube "+tier+"/"+y);
                }
                var cushionShape=level.getBlockState(center.above()).getCollisionShape(level,center.above());
                check(Math.abs(cushionShape.max(Direction.Axis.Y)-FormationVisualShapes.CUSHION_HEIGHT)<1e-6,"Cushion collision matches the accepted imported model");
                check(!cushionShape.toAabbs().stream().anyMatch(b->b.contains(.01,.1,.01)),"Cushion corners remain open");
                if(tier>1) {
                    BlockPos top=center.relative(Direction.EAST,radius).above(tier);
                    BlockState savedTop=level.getBlockState(top);level.setBlock(top,Blocks.AIR.defaultBlockState(),3);
                    check(level.getBlockState(top.below()).getValue(FormationPartBlock.SECTION)==(tier==2?0:3),"Removing crown updates adjoining segment");
                    level.setBlock(top,savedTop,3);ticks(level,core,1);
                    check(FormationStructure.isComplete(level,center,tier),"Crown replacement preserves complete structure");
                }
                for(SpiritStoneTier stoneTier:SpiritStoneTier.values()) {
                    // Fresh block entity fixture prevents queued time from previous case.
                    CompoundTag fresh=new CompoundTag();((BlockEntity)core).load(fresh);
                    ItemStack stone=new ItemStack(ModItems.getSpiritStone(stoneTier),2);
                    core.inventory().setStackInSlot(0,stone);
                    core.inventory().setStackInSlot(1,stone.copy());
                    ticks(level,core,1);
                    long expected=stoneTier.spiritualPower()*1000*1200-core.powerPerMinute()*1000L;
                    check(core.runtimeBudget()==expected,"Exact runtime debit tier "+tier+" stone "+stoneTier.rank());
                    check(core.bonusTicks()==FormationRules.bonusTicks(stoneTier.rank())-1 && core.bonusRank()==stoneTier.rank(),"Bonus duration and rank");
                    check(core.inventory().getStackInSlot(0).getCount()==1&&core.inventory().getStackInSlot(1).getCount()==1,"One stone from each independent queue");
                    long budget=core.runtimeBudget();int bonus=core.bonusTicks();ticks(level,core,20);
                    check(core.runtimeBudget()==budget-core.powerPerMinute()*20000L&&core.bonusTicks()==bonus-20,"Empty running array consumes both timers");
                    CompoundTag nbt=((BlockEntity)core).saveWithoutMetadata();
                    BlockEntity restored=new FormationCoreBlockEntity(center,((BlockEntity)core).getBlockState());restored.setLevel(level);restored.load(nbt);
                    FormationCoreBlockEntity copy=(FormationCoreBlockEntity)restored;
                    check(copy.runtimeBudget()==core.runtimeBudget()&&copy.bonusRank()==core.bonusRank()&&copy.bonusTicks()==core.bonusTicks(),"NBT retains exact timers without catch-up");
                }
                ((BlockEntity)core).load(new CompoundTag());
                ItemStack partial=new ItemStack(ModItems.getSpiritStone(SpiritStoneTier.DAO_SHI));
                ((SpiritStoneItem)partial.getItem()).setChargeMilli(partial,12500);
                check(core.inventory().isItemValid(0,partial)&&!core.inventory().isItemValid(1,partial),"Partial stone runtime only");
                core.inventory().setStackInSlot(0,partial);ticks(level,core,1);
                check(core.runtimeBudget()==12500*1200L-core.powerPerMinute()*1000L,"Partial stone uses actual residual energy");
                ((BlockEntity)core).load(new CompoundTag());
                core.inventory().setStackInSlot(0,new ItemStack(ModItems.getSpiritStone(SpiritStoneTier.DAO_SHI)));
                core.inventory().setStackInSlot(1,new ItemStack(ModItems.SPIRIT_STONE.get(),2));ticks(level,core,1);
                long before=core.runtimeBudget();int effect=core.bonusTicks();
                BlockPos removed=center.offset(FormationLayout.outerRadius(tier),0,0);BlockState ring=level.getBlockState(removed);
                level.setBlock(removed,Blocks.AIR.defaultBlockState(),3);ticks(level,core,20);
                check(!core.structureValid()&&core.runtimeBudget()==before&&core.bonusTicks()==effect,"Broken array pauses immediately");
                level.setBlock(removed,ring,3);ticks(level,core,1);
                check(core.structureValid()&&core.bonusTicks()==effect-1,"Repair resumes exact remaining timers");
                // Boundary: consume exactly two basic bonus stones, then no active bonus.
                ((BlockEntity)core).load(new CompoundTag());core.inventory().setStackInSlot(0,new ItemStack(ModItems.getSpiritStone(SpiritStoneTier.DAO_SHI)));
                core.inventory().setStackInSlot(1,new ItemStack(ModItems.SPIRIT_STONE.get(),2));ticks(level,core,4800);
                check(core.bonusTicks()==0&&core.inventory().getStackInSlot(1).isEmpty(),"Queued bonus expires exactly at two full durations");
                if(tier==1) {
                    player.teleportTo(center.getX()+.5,center.getY()+2,center.getZ()+2.5);
                    core.inventory().setStackInSlot(1,new ItemStack(ModItems.SPIRIT_STONE.get()));ticks(level,core,1);
                    data.setProgress(0);
                    meditate(core,player,60);
                    // Three roots, tier-one environment, and an active safe first-tier stone.
                    check(Math.abs(data.progress()-FormationRules.baseRate(0)*1.5*FormationRules.bonusMultiplier(1,1)*60)<.0011,"One minute entrance meditation matches current base rate with safe first-tier bonus; actual="+data.progress());
                    data.setProgress(data.progressMax()-.001);meditate(core,player,1);meditate(core,player,1);
                    check(data.progress()==data.progressMax(),"Full stage stops at cap");data.setProgress(0);
                    data.setPower(data.powerMax());meditate(core,player,1);
                    check(data.power()==data.powerMax(),"Meditation power restoration cannot overflow its cap");
                    core.inventory().setStackInSlot(1,new ItemStack(ModItems.getSpiritStone(SpiritStoneTier.LING_JING)));
                    Block cushion=ModBlocks.MEDITATION_CUSHION.get();
                    cushion.use(level.getBlockState(center.above()),level,center.above(),player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(center.above()),Direction.UP,center.above(),false));
                    check(player.getVehicle() instanceof FormationSeatEntity,"Safe cushion mounts one seat");
                    check(core.switchBonus(player),"Explicit bonus switch succeeds");
                    check(player.getVehicle()==null&&player.isAlive()&&core.bonusRank()==2,"Third-party dangerous switch stops rider without killing");
                    double progress=data.progress();meditate(core,player,1);
                    check(data.progress()==progress,"Unsafe direct meditation gives no progress");
                    check(!core.forceMeditation(player,8)&&player.isAlive(),"Stale death-confirmation rank rejected");
                    cushion.use(level.getBlockState(center.above()),level,center.above(),player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(center.above()),Direction.UP,center.above(),false));
                    check(player.getVehicle()==null&&player.isAlive(),"Default dangerous cushion use opens GUI without death");
                    player.closeContainer();
                    // Forge fake player exercises actual lethal server path without changing the human character.
                    ServerPlayer fake=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.fromString("28e67aef-8305-4bcc-a1dd-590bdc97a799"),"XiantuQA")) {
                        @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) { return false; }
                    };
                    ((Entity)fake).setPos(center.getX()+.5,center.getY()+2,center.getZ()+2.5);
                    fake.getCapability(CultivationCapability.DATA).ifPresent(d->d.setRealmStage(0,0));
                    fake.setHealth(fake.getMaxHealth());
                    check(core.forceMeditation(fake,2)&&!fake.isAlive(),"Explicit confirmation executes lethal vanilla damage on non-immune fake player");
                    fake.setHealth(fake.getMaxHealth());
                    // Long sync fixture must exceed a signed short and preserve low and high limbs.
                    FormationMenu menu=new FormationMenu(0,player.getInventory(),core);
                    check(menu.runtimeSeconds()==core.runtimeSeconds()&&menu.runtimeSeconds()>32767,"Menu retains long runtime beyond signed-short range");
                }
                // Exhausted runtime pauses an already active bonus; never consumes next bonus stone.
                CompoundTag paused=((BlockEntity)core).saveWithoutMetadata();paused.putLong("runtime_budget",0);paused.putInt("bonus_ticks",500);paused.putInt("bonus_rank",1);
                ((BlockEntity)core).load(paused);core.inventory().setStackInSlot(0,ItemStack.EMPTY);core.inventory().setStackInSlot(1,new ItemStack(ModItems.SPIRIT_STONE.get(),2));
                ticks(level,core,20);check(core.bonusTicks()==500&&core.inventory().getStackInSlot(1).getCount()==2,"No power pauses active bonus and its queue");
                if(tier==3) for(int realm=0;realm<9;realm++) {
                    player.teleportTo(center.getX()+.5,center.getY()+2,center.getZ()+2.5);
                    int rank=SpiritStoneAbsorptionRules.stoneRankForRealm(realm);
                    data.setRealmStage(realm,0);data.setProgress(0);
                    data.gongfa().clear();data.gongfa().learn("tai_chu_dao_jing");data.gongfa().commit("tai_chu_dao_jing",UUID.randomUUID());
                    CompoundTag sample=new CompoundTag();sample.putLong("runtime_budget",299000000L*1200);sample.putInt("bonus_ticks",10000);sample.putInt("bonus_rank",rank);
                    ((BlockEntity)core).load(sample);ticks(level,core,1);
                    meditate(core,player,60);
                    double expected=FormationRules.progressPerSecond(realm,3,0,3,rank,1,1,0)*60*2.5/1.5;
                    check(Math.abs(data.progress()-expected)<.0011,"Actual capability one-minute base-rate sample realm "+realm);
                    check(data.gongfa().activeLevel()==1 && data.gongfa().activeProgress().experience()==0,"Actual progress awards one method XP per second realm "+realm);
                }
                core.inventory().setStackInSlot(0,new ItemStack(ModItems.SPIRIT_STONE.get(),2));
                core.inventory().setStackInSlot(1,new ItemStack(ModItems.getSpiritStone(SpiritStoneTier.LING_JING),2));
                CompoundTag stored=((BlockEntity)core).saveWithoutMetadata();
                stored.putLong("runtime_budget",1_234_000L);stored.putInt("bonus_ticks",500);stored.putInt("bonus_rank",1);
                ((BlockEntity)core).load(stored);
                level.setBlock(center,Blocks.AIR.defaultBlockState(),3);
                var dismantleDrops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(center).inflate(2))
                        .stream().filter(e->!priorDrops.contains(e.getUUID())).toList();
                int runtimeStones=dismantleDrops.stream().filter(e->e.getItem().is(ModItems.SPIRIT_STONE.get()))
                        .mapToInt(e->e.getItem().getCount()).sum();
                int bonusStones=dismantleDrops.stream().filter(e->e.getItem().is(ModItems.getSpiritStone(SpiritStoneTier.LING_JING)))
                        .mapToInt(e->e.getItem().getCount()).sum();
                int storedEnergyStones=dismantleDrops.stream().filter(e->e.getItem().is(ModItems.getSpiritStone(SpiritStoneTier.DAO_SHI)))
                        .mapToInt(e->e.getItem().getCount()).sum();
                check(runtimeStones==2&&bonusStones==2&&storedEnergyStones==0,
                        "Breaking array drops both queued slots but never refunds converted energy");
                check(core.runtimeTicks()==0&&core.bonusTicks()==0&&core.inventory().getStackInSlot(0).isEmpty()
                                &&core.inventory().getStackInSlot(1).isEmpty(),
                        "Actual core removal clears stored timers and both queues");
            }
            data.gongfa().clear();data.setRealmStage(0,0);data.setPowerMax(100);data.setPowerSpeed(1);data.setPower(0);data.setProgress(0);player.hurtTime=0;
            player.getPersistentData().putDouble("xiantu_gongfa_power_max_bonus",0);
            player.getPersistentData().putDouble("xiantu_gongfa_power_speed_bonus",0);
            BlockPos standalone=start.offset(32,1,0);blocks.put(standalone,level.getBlockState(standalone));level.setBlock(standalone,ModBlocks.MEDITATION_CUSHION.get().defaultBlockState(),3);
            player.teleportTo(standalone.getX()+.5,standalone.getY()+1,standalone.getZ()+2.5);
            Block cushion=ModBlocks.MEDITATION_CUSHION.get();
            cushion.use(level.getBlockState(standalone),level,standalone,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(standalone),Direction.UP,standalone,false));
            Entity standaloneSeat=player.getVehicle();check(standaloneSeat instanceof FormationSeatEntity,"Standalone cushion allows power recovery");
            for(int i=0;i<20;i++)standaloneSeat.tick();
            check(Math.abs(data.power()-1)<.001&&data.progress()==0,"Seat ticks restore power; central settlement owns progress");
            ChunkAuraService.settleSecond(level);
            check(data.progress()>0,"Standalone cushion receives natural aura cultivation");
            player.setShiftKeyDown(true);standaloneSeat.tick();player.setShiftKeyDown(false);
            check(player.getVehicle()==null,"Sneaking stops seated meditation");
            player.sendSystemMessage(Component.literal("[Formation balance QA] All "+checks+" assertions passed"));
            LogManager.getLogger("Xiantu QA").info("[Formation balance QA] All {} assertions passed",checks);
            return 1;
        } catch(Throwable error) {
            LogManager.getLogger("Xiantu QA").error("[Formation balance QA] FAILED after "+checks,error);
            player.sendSystemMessage(Component.literal("[Formation balance QA] FAILED: "+error));return 0;
        } finally {
            player.closeContainer();player.stopRiding();
            for(BlockPos p:blocks.keySet())if(level.getBlockEntity(p) instanceof FormationCoreBlockEntity c) {c.inventory().setStackInSlot(0,ItemStack.EMPTY);c.inventory().setStackInSlot(1,ItemStack.EMPTY);}
            blocks.forEach((p,s)->level.setBlock(p,s,3));
            for(Entity e:level.getEntitiesOfClass(FormationSeatEntity.class,new AABB(7000,-64,1360,11200,-40,1600)))e.discard();
            for(var e:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(7000,-64,1360,11200,-40,1600)))if(!priorDrops.contains(e.getUUID()))e.discard();
            data.load(saved);
            for(String key:java.util.List.copyOf(player.getPersistentData().getAllKeys()))player.getPersistentData().remove(key);
            player.getPersistentData().merge(methodPersistent);
            CultivationService.applyRealmAttributes(player,data);player.setHealth(health);player.hurtTime=hurt;
            player.teleportTo(position.x,position.y,position.z);CultivationEvents.sync(player);
        }
    }
    public static int gallery(ServerPlayer player) {
        if(!guard(player))return 0;
        ServerLevel l=player.serverLevel();BlockPos start=site(l);
        for(int tier=1;tier<=3;tier++) {
            BlockPos c=start.offset(0,0,(tier-1)*64);build(l,c,tier,new LinkedHashMap<>());
            decorate(l,c,tier);
            FormationCoreBlockEntity core=(FormationCoreBlockEntity)l.getBlockEntity(c);
            core.inventory().setStackInSlot(0,new ItemStack(ModItems.getSpiritStone(SpiritStoneTier.DAO_SHI),2));
            core.inventory().setStackInSlot(1,new ItemStack(ModItems.SPIRIT_STONE.get(),2));ticks(l,core,1);
        }
        player.teleportTo(start.getX()+.5,start.getY()+2,start.getZ()+2.5);
        player.sendSystemMessage(Component.literal("[Formation gallery] Tier centers: "+start.toShortString()+" / "+start.offset(0,0,64).toShortString()+" / "+start.offset(0,0,128).toShortString()));
        return 1;
    }

    private static void verifyTraces(ServerLevel l,BlockPos c,int tier) {
        for(int ringTier=1;ringTier<=tier;ringTier++) for(boolean outer:new boolean[]{true,false}) {
            boolean aligned=true,reciprocal=true;
            for(var tile:FormationTrace.ringStage(ringTier,outer)) {
                BlockPos p=c.offset(tile.x(),0,tile.z());BlockState s=l.getBlockState(p);
                aligned &= s.getValue(FormationPartBlock.TRACE)==tile.trace();
                for(int port:FormationTrace.ports(tile.trace())) {
                    BlockState n=l.getBlockState(p.offset(FormationTrace.DX[port],0,FormationTrace.DZ[port]));
                    reciprocal &= n.hasProperty(FormationPartBlock.TRACE)&&java.util.Arrays.stream(FormationTrace.ports(n.getValue(FormationPartBlock.TRACE))).anyMatch(v->v==(port+4)%8);
                }
                for(Rotation rotation:Rotation.values()) {
                    int turns=switch(rotation){case NONE->0;case CLOCKWISE_90->1;case CLOCKWISE_180->2;case COUNTERCLOCKWISE_90->3;};
                    aligned &= s.rotate(rotation).getValue(FormationPartBlock.TRACE)==FormationTrace.transform(tile.trace(),turns,false,false);
                }
                aligned &= s.mirror(Mirror.FRONT_BACK).getValue(FormationPartBlock.TRACE)==FormationTrace.transform(tile.trace(),0,true,false);
                aligned &= s.mirror(Mirror.LEFT_RIGHT).getValue(FormationPartBlock.TRACE)==FormationTrace.transform(tile.trace(),0,false,true);
            }
            check(aligned,"World-space trace state/rotation/mirror array="+tier+" ring="+ringTier+" outer="+outer);
            check(reciprocal,"Every loaded trace port has reciprocal neighbor array="+tier+" ring="+ringTier+" outer="+outer);
        }
    }

    /** Optional native building-block paving: never changes functional recipe requirements. */
    private static void decorate(ServerLevel level,BlockPos center,int tier) {
        int r=FormationLayout.outerRadius(tier);
        Block plain=tier==1?ModBlocks.AZURE_BRICK_SMOOTH.get():tier==2?ModBlocks.CLOUD_JADE.get():ModBlocks.BLACK_ROCK_SMOOTH.get();
        Block accent=tier==1?ModBlocks.AZURE_BRICK_CARVED.get():tier==2?ModBlocks.CLOUD_JADE_CARVED.get():ModBlocks.CLOUD_JADE.get();
        int centerRadius=3;
        for(int x=-centerRadius;x<=centerRadius;x++)for(int z=-centerRadius;z<=centerRadius;z++) {
            int d=x*x+z*z;
            if(d>centerRadius*centerRadius||x==0&&z==0||FormationLayout.isBaseRing(tier,x,z)||FormationLayout.isRuneRing(tier,x,z))continue;
            boolean guide=(x==0||z==0)&&d>=4;
            level.setBlock(center.offset(x,0,z),(guide?accent:plain).defaultBlockState(),3);
        }
        for(Direction side:Direction.Plane.HORIZONTAL) {
            for(int step=1;step<=3;step++)for(int cross=-1;cross<=1;cross++) {
                BlockPos p=center.relative(side,r+step).relative(side.getClockWise(),cross);
                level.setBlock(p,plain.defaultBlockState(),3);
            }
        }
    }
}
