package com.longju.xiantu.instance;

import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.entity.*;
import com.longju.xiantu.gongfa.GongfaService;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** All transactions execute on the server thread. Persistent state owns access, not dimension names. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class InstanceService {
    private static final ThreadLocal<Integer> INTERNAL=ThreadLocal.withInitial(()->0);
    private static final Map<MinecraftServer,InstanceSavedData> DATA=new WeakHashMap<>();
    private static final java.util.Random RANDOM=new java.util.Random();
    public static boolean trusted(){return INTERNAL.get()>0;}
    public static void internal(Runnable job){INTERNAL.set(INTERNAL.get()+1);try{job.run();}finally{INTERNAL.set(INTERNAL.get()-1);}}
    public static boolean isDimension(ResourceLocation id){return id.getNamespace().equals("xiantu")&&(id.getPath().matches("realm_\\d{2}")||id.getPath().matches("dongfu_\\d{3}"));}
    public static boolean temporary(Level l){return isDimension(l.dimension().location())&&l.dimension().location().getPath().startsWith("realm_");}
    public static InstanceSavedData data(MinecraftServer s){return DATA.computeIfAbsent(s,InstanceSavedData::get);}
    public static InstanceLedger.Slot slot(ServerLevel l){var d=data(l.getServer());return d.blocked()?null:d.ledger().find(l.dimension().location().toString()).orElse(null);}
    public static ServerLevel level(MinecraftServer s,InstanceLedger.Slot slot){return s.getLevel(ResourceKey.create(Registries.DIMENSION,new ResourceLocation(slot.dimension())));}
    private static long now(MinecraftServer s){return s.overworld().getGameTime();}
    public static boolean writable(ServerLevel l,BlockPos at){
        if(trusted()||!isDimension(l.dimension().location()))return true;
        var s=slot(l);if(s==null||s.state()!=InstanceLedger.State.ACTIVE)return false;
        var n=data(l.getServer()).session(s);
        return (s.kind()==InstanceLedger.Kind.CAVE||!n.getBoolean("Frozen"))&&inside(l,at.getX()+.5,at.getY(),at.getZ()+.5)&&!at.equals(corePos(s));
    }
    public static boolean inside(ServerLevel l,double x,double y,double z){var b=l.getWorldBorder();return x>=b.getMinX()+.35&&x<=b.getMaxX()-.35&&z>=b.getMinZ()+.35&&z<=b.getMaxZ()-.35&&y>=-63&&y<=317;}
    public static BlockPos corePos(InstanceLedger.Slot s){return s.kind()==InstanceLedger.Kind.CAVE?new BlockPos(96,65,90):new BlockPos(0,65,-6);}
    private static Vec3 arrival(InstanceLedger.Slot s){return s.kind()==InstanceLedger.Kind.CAVE?new Vec3(96.5,65,96.5):new Vec3(.5,65,.5);}
    public static String threat(ServerPlayer p){
        if(com.longju.xiantu.worldgen.RuinProtection.protectedAt(p.serverLevel(),p.blockPosition()))return "请先离开首领守护区域";
        for(var e:p.serverLevel().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(16),e->e!=p&&e.isAlive()&&!e.isSpectator()))
            if(e.distanceToSqr(p)<=256&&(e instanceof Enemy||BeastRealmService.isBeast(e)&&!BeastRealmService.peacefulSpecies(BeastRealmService.species(e))||e instanceof Mob m&&m.getTarget()==p))return "附近有敌对生物，无法使用洞府令牌";
        return "";
    }
    public static ItemStack token(InstanceSavedData d,InstanceLedger.Slot s,String grant,int version){
        var result=new ItemStack(grant.equals("master")?InstanceRegistry.TOKEN.get():InstanceRegistry.CHILD.get());var n=result.getOrCreateTag();
        n.putString("Dimension",s.dimension());n.putUUID("Cave",s.instance());n.putUUID("Owner",s.owner());n.putLong("Generation",s.generation());n.putString("Grant",grant);n.putInt("Version",version);
        n.putString("Seal",TokenSeal.sign(d.secret(),s.dimension(),s.instance().toString(),s.generation(),s.owner().toString(),grant,version));return result;
    }
    public static InstanceLedger.Slot validate(ServerPlayer p,ItemStack stack){
        if(!(stack.getItem() instanceof CaveTokenItem item)||stack.getCount()!=1||!stack.hasTag())return null;
        var d=data(p.server);if(d.blocked())return null;var n=stack.getTag();
        var s=d.ledger().find(n.getString("Dimension")).orElse(null);
        if(s==null||s.kind()!=InstanceLedger.Kind.CAVE||s.state()!=InstanceLedger.State.ACTIVE||!n.hasUUID("Cave")||!n.hasUUID("Owner")||!s.instance().equals(n.getUUID("Cave"))||!s.owner().equals(n.getUUID("Owner"))||n.getLong("Generation")!=s.generation())return null;
        String grant=n.getString("Grant");int version=n.getInt("Version");var session=d.session(s);
        if(item.child){if(grant.equals("master")||session.getCompound("Grants").getInt(grant)!=version||version<1)return null;}
        else if(!grant.equals("master")||!p.getUUID().equals(s.owner())||session.getInt("MasterVersion")!=version)return null;
        return TokenSeal.matches(TokenSeal.sign(d.secret(),s.dimension(),s.instance().toString(),s.generation(),s.owner().toString(),grant,version),n.getString("Seal"))?s:null;
    }
    public static String useToken(ServerPlayer p,ItemStack stack,boolean child){
        if(!p.isAlive()||p.isSpectator()||p.isPassenger()||p.isVehicle()||p.isSleeping())return "当前无法使用洞府令牌";
        var d=data(p.server);if(d.blocked())return "洞府记录异常，请联系管理员";
        if(temporary(p.level()))return "天道秘境内不能使用洞府令牌";
        String danger=threat(p);if(!danger.isEmpty())return danger;
        if(!stack.hasTag()||stack.getTag().isEmpty()){
            if(child)return "子令牌尚未获授权";
            if(p.level().dimension()!=Level.OVERWORLD)return "首次开辟洞府必须在主世界";
            var previous=d.ledger().cave(p.getUUID()).orElse(null);
            if(previous!=null&&previous.state()!=InstanceLedger.State.PREPARING)return "已拥有洞府，请使用原令牌或在修仙手册补发";
            var s=previous!=null?previous:d.ledger().reserve(InstanceLedger.Kind.CAVE,p.getUUID()).orElse(null);if(s==null)return "洞府名额已满，令牌未消耗";
            var n=previous!=null?d.session(s):new CompoundTag();if(previous==null){n.putInt("MasterVersion",1);n.put("Grants",new CompoundTag());n.put("Visitors",new CompoundTag());d.session(s,n);}
            try{
                var l=level(p.server,s);if(l==null)throw new IllegalStateException("Missing cave dimension");
                InstanceMaintenance.mark(p.server,s);configure(l,s,n);
                internal(()->{for(int x=88;x<=104;x++)for(int z=88;z<=104;z++)l.setBlockAndUpdate(new BlockPos(x,64,z),(x==88||x==104||z==88||z==104?Blocks.STONE_BRICKS:Blocks.SMOOTH_STONE).defaultBlockState());l.setBlockAndUpdate(corePos(s),InstanceRegistry.CORE.get().defaultBlockState());});
                n.putBoolean("Ready",true);d.ledger().activate(s);d.setDirty();
                var bound=token(d,s,"master",1);stack.setTag(bound.getTag().copy());
            }catch(Exception failure){com.mojang.logging.LogUtils.getLogger().error("永久洞府准备失败，保留记录等待修复",failure);return "洞府准备失败，记录已保留，请联系管理员";}
        }
        var s=validate(p,stack);if(s==null)return "令牌无效、已撤销或不属于你";
        if(p.level().dimension().location().toString().equals(s.dimension())){returnHome(p);return "已返回主世界";}
        var l=level(p.server,s);if(l==null)return "洞府空间尚未加载";
        Vec3 at=arrival(s);if(!safe(l,BlockPos.containing(at)))return "洞府入口被遮挡，请由持有权限的玩家清理入口";
        rememberReturn(p);String grant=stack.getTag().getString("Grant");var visitors=d.session(s).getCompound("Visitors");String previous=visitors.getString(p.getUUID().toString());visitors.putString(p.getUUID().toString(),grant);d.setDirty();
        if(!travel(p,l,at)){if(previous.isEmpty())visitors.remove(p.getUUID().toString());else visitors.putString(p.getUUID().toString(),previous);return "传送失败，令牌保留";}
        return "已进入永久洞府；主手使用令牌或右键洞天核心返回";
    }
    public static void configure(ServerLevel l,InstanceLedger.Slot s,CompoundTag n){
        if(s.kind()==InstanceLedger.Kind.REALM){
            var terrain=new InstanceTerrain(n.getLong("Seed"),n.getInt("Diameter"),InstanceRules.Abundance.valueOf(n.getString("Abundance")).ordinal());
            if(!(l.getChunkSource().getGenerator() instanceof InstanceChunkGenerator generator))throw new IllegalStateException("Unexpected realm generator");
            generator.configure(terrain,RealmLayout.get(l.getServer(),InstanceTerrain.THEMES[terrain.theme()]),l.registryAccess());
            n.putString("Theme",InstanceTerrain.THEMES[terrain.theme()]);n.putBoolean("Natural",terrain.natural());
        }
        var b=l.getWorldBorder();b.setCenter(s.kind()==InstanceLedger.Kind.CAVE?96:0,s.kind()==InstanceLedger.Kind.CAVE?96:0);b.setSize(s.kind()==InstanceLedger.Kind.CAVE?192:Math.max(256,n.getInt("Diameter")*16));b.setWarningBlocks(3);b.setDamagePerBlock(0);b.setDamageSafeZone(0);
    }
    private static boolean travel(ServerPlayer p,ServerLevel l,Vec3 at){
        p.closeContainer();p.stopRiding();internal(()->p.teleportTo(l,at.x,at.y,at.z,p.getYRot(),p.getXRot()));
        if(p.serverLevel()!=l||p.position().distanceToSqr(at)>4)return false;
        p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);p.connection.send(new ClientboundInitializeBorderPacket(l.getWorldBorder()));return true;
    }
    public static boolean safe(ServerLevel l,BlockPos p){return p.getY()>l.getMinBuildHeight()&&p.getY()<l.getMaxBuildHeight()-2&&l.getBlockState(p.below()).isCollisionShapeFullBlock(l,p.below())&&l.getBlockState(p).getCollisionShape(l,p).isEmpty()&&l.getBlockState(p.above()).getCollisionShape(l,p.above()).isEmpty()&&l.getFluidState(p).isEmpty()&&l.getFluidState(p.below()).isEmpty();}
    private static void rememberReturn(ServerPlayer p){
        if(p.level().dimension()!=Level.OVERWORLD)return;var d=data(p.server);var all=d.runtime().getCompound("Returns");
        all.putLong(p.getUUID().toString(),p.blockPosition().asLong());d.runtime().put("Returns",all);d.setDirty();
    }
    public static void returnHome(ServerPlayer p){
        var d=data(p.server);BlockPos wanted=d.runtime().getCompound("Returns").contains(p.getUUID().toString())?BlockPos.of(d.runtime().getCompound("Returns").getLong(p.getUUID().toString())):p.server.overworld().getSharedSpawnPos();
        BlockPos dest=null;var l=p.server.overworld();
        for(int radius=0;radius<=8&&dest==null;radius++)for(int dx=-radius;dx<=radius&&dest==null;dx++)for(int dz=-radius;dz<=radius&&dest==null;dz++){
            var point=wanted.offset(dx,0,dz);if(safe(l,point))dest=point;else{point=l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,point);if(safe(l,point))dest=point;}
        }
        if(dest==null){dest=l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,l.getSharedSpawnPos());if(!safe(l,dest)){
            BlockPos rescue=dest;internal(()->{l.setBlockAndUpdate(rescue.below(),Blocks.STONE.defaultBlockState());l.setBlockAndUpdate(rescue,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(rescue.above(),Blocks.AIR.defaultBlockState());});
        }}
        if(travel(p,l,Vec3.atBottomCenterOf(dest)))release(p);
    }
    private static void release(ServerPlayer p){
        var d=data(p.server);if(d.blocked())return;for(var s:d.slots())if(s.state()!=InstanceLedger.State.FREE){var n=d.session(s);n.getCompound("Members").remove(p.getUUID().toString());n.getCompound("Visitors").remove(p.getUUID().toString());}d.setDirty();
    }
    public static String core(ServerPlayer p,BlockPos pos){
        if(p.distanceToSqr(Vec3.atCenterOf(pos))>36)return "距离入口太远";
        if(isDimension(p.level().dimension().location())){
            var s=slot(p.serverLevel());if(s==null||!pos.equals(corePos(s)))return "此入口无效";
            if(s.kind()==InstanceLedger.Kind.CAVE&&p.isShiftKeyDown()&&s.owner().equals(p.getUUID())){InstanceMenu.open(p);return "";}
            returnHome(p);return "已离开独立空间";
        }
        if(p.level().dimension()!=Level.OVERWORLD)return "此入口无效";
        var d=data(p.server);if(d.blocked())return "实例记录异常";
        for(var s:d.slots())if(s.kind()==InstanceLedger.Kind.REALM&&s.state()!=InstanceLedger.State.FREE&&BlockPos.of(d.session(s).getLong("Entry")).equals(pos))return enterRealm(p,s);
        return "秘境入口已关闭";
    }
    public static String enterRealm(ServerPlayer p,InstanceLedger.Slot s){
        var d=data(p.server);var n=d.session(s);var current=d.ledger().find(s.dimension()).orElseThrow();
        if(p.isSpectator())return "旁观者不可进入生存秘境";
        if(p.level().dimension()!=Level.OVERWORLD||p.distanceToSqr(Vec3.atCenterOf(BlockPos.of(n.getLong("Entry"))))>36)return "请到主世界秘境入口进入";
        if(current.state()!=InstanceLedger.State.ACTIVE||n.getBoolean("Frozen")||!n.getBoolean("Ready"))return "秘境尚未开放或正在关闭";
        var a=InstanceRules.Abundance.valueOf(n.getString("Abundance"));var c=GongfaService.data(p);
        if(!c.cultivationUnlocked()||c.majorRealm()<a.minRealm||c.majorRealm()>a.maxRealm)return "境界不符合入场范围："+realmName(a.minRealm)+"至"+realmName(a.maxRealm);
        var members=n.getCompound("Members");String id=p.getUUID().toString();
        if(members.contains(id)||members.size()>=a.capacity)return "秘境人数已满或已有入场登记";
        members.putString(id,"reserved");d.setDirty();rememberReturn(p);
        // Return to the approach, outside the portal plane, so staying still never bounces back in.
        var approach=BlockPos.of(n.getLong("Entry")).offset(0,0,3);
        if(safe(p.server.overworld(),approach)){var returns=d.runtime().getCompound("Returns");returns.putLong(id,approach.asLong());d.runtime().put("Returns",returns);d.setDirty();}
        var l=level(p.server,s);if(l==null||!safe(l,BlockPos.containing(arrival(s)))||!travel(p,l,arrival(s))){members.remove(id);d.setDirty();return "入口暂不可用，未产生消耗";}
        members.putString(id,"inside");d.setDirty();return "已进入天道秘境，关闭前请带走随身物品";
    }
    private static String realmName(int realm){return RealmRegistry.get(realm,0).majorName();}
    private static void announce(MinecraftServer server,String message){server.getPlayerList().broadcastSystemMessage(Component.literal("【天道秘境】"+message),false);}
    public static String opening(CompoundTag n){
        var a=InstanceRules.Abundance.valueOf(n.getString("Abundance"));var t=new InstanceTerrain(n.getLong("Seed"),n.getInt("Diameter"));
        return "秘境："+t.name()+" | 人数："+n.getCompound("Members").size()+"/"+a.capacity+" | 修为："+realmName(a.minRealm)+"至"+realmName(a.maxRealm)+" | 坐标："+BlockPos.of(n.getLong("Entry")).toShortString()+" | 大小："+n.getInt("Diameter")+"×"+n.getInt("Diameter")+"区块";
    }
    public static String createRealm(MinecraftServer server,BlockPos entry,InstanceRules.Parameters params,int prepareTicks){
        return createRealm(server,entry,params,prepareTicks,null);
    }
    public static String createRealm(MinecraftServer server,BlockPos entry,InstanceRules.Parameters params,int prepareTicks,Boolean natural){
        return createRealm(server,entry,params,prepareTicks,natural,null);
    }
    /** Optional theme is used by isolated QA; normal realm rolls leave it null. */
    public static String createRealm(MinecraftServer server,BlockPos entry,InstanceRules.Parameters params,int prepareTicks,Boolean natural,String theme){
        if(theme!=null&&!java.util.Arrays.asList(InstanceTerrain.THEMES).contains(theme))throw new IllegalArgumentException("Unknown realm theme");
        var d=data(server);if(d.blocked())return "实例记录异常";
        var s=d.ledger().reserve(InstanceLedger.Kind.REALM,null).orElse(null);if(s==null)return "已有活动或待清理秘境，或无空槽";
        var n=new CompoundTag();n.putString("Abundance",params.abundance().name());n.putInt("Diameter",params.diameterChunks());n.putInt("Minutes",params.minutes());n.putBoolean("TokenReward",params.caveToken());n.putBoolean("SoulReward",params.abundance().maxRealm>=3&&params.soulCrystal());n.putLong("Entry",entry.asLong());n.putLong("Seed",RANDOM.nextLong());n.put("Members",new CompoundTag());n.putLong("OpenAt",now(server)+prepareTicks);n.putLong("EndAt",now(server)+prepareTicks+params.minutes()*1200L);d.session(s,n);
        for(;;){var t=new InstanceTerrain(n.getLong("Seed"),params.diameterChunks());if((natural==null||t.natural()==natural)&&(theme==null||InstanceTerrain.THEMES[t.theme()].equals(theme)))break;n.putLong("Seed",RANDOM.nextLong());}
        if(!new InstanceTerrain(n.getLong("Seed"),params.diameterChunks()).natural())n.putInt("Diameter",Math.max(32,params.diameterChunks()));
        try{InstanceMaintenance.mark(server,s);var l=level(server,s);if(l==null)throw new IllegalStateException("Missing realm dimension");configure(l,s,n);InstanceContent.begin(server,s,n);
            RealmPortal.place(server.overworld(),entry,n);
            d.runtime().remove("NextRealmAtMillis");d.runtime().remove("RealmSiteRetryAtMillis");d.runtime().remove("Failures");d.runtime().remove("NextCheck");d.setDirty();return "秘境已开始准备";
        }catch(Exception failure){com.mojang.logging.LogUtils.getLogger().error("秘境准备失败",failure);close(server,s,"准备失败");return "秘境准备失败，槽位已隔离";}
    }
    public static void close(MinecraftServer server,InstanceLedger.Slot s,String reason){
        if(s.kind()!=InstanceLedger.Kind.REALM)return;var d=data(server);var current=d.ledger().find(s.dimension()).orElse(null);if(current==null||current.generation()!=s.generation()||!Objects.equals(current.instance(),s.instance())||current.state()==InstanceLedger.State.FREE)return;var n=d.session(s);if(n.getBoolean("Closed"))return;
        n.putBoolean("Frozen",true);n.putBoolean("Closed",true);n.putLong("ClosedAtMillis",System.currentTimeMillis());d.runtime().putLong("NextRealmAtMillis",InstanceRules.nextRealmAt(n.getLong("ClosedAtMillis")));n.putString("CloseReason",reason);d.setDirty();InstanceContent.stop(s.instance());
        var l=level(server,s);if(l!=null)for(var p:List.copyOf(l.players()))returnHome(p);
        RealmPortal.remove(server.overworld(),n);
        d.ledger().quarantine(s);d.setDirty();announce(server,"秘境已关闭");
    }
    private static BlockPos candidate(MinecraftServer server,List<ServerPlayer> players){
        if(players.isEmpty())return null;
        List<ServerPlayer> cluster=List.of();
        // Max mutually-near cluster; no remote outlier can pull the arithmetic mean across the map.
        for(var pivot:players){var c=new ArrayList<ServerPlayer>();c.add(pivot);for(var p:players)if(p!=pivot&&c.stream().allMatch(q->q.position().distanceToSqr(p.position())<=3200.0*3200))c.add(p);if(c.size()>cluster.size())cluster=c;}
        int x=(int)cluster.stream().mapToInt(p->p.chunkPosition().x).average().orElseThrow(),z=(int)cluster.stream().mapToInt(p->p.chunkPosition().z).average().orElseThrow();
        var world=server.overworld();
        for(int tries=0;tries<64;tries++){
            int cx=x+RANDOM.nextInt(65)-32,cz=z+RANDOM.nextInt(65)-32;
            // Do not synchronously generate dozens of unexplored chunks during a probability check.
            if(!world.hasChunk(cx,cz))continue;
            var pos=world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(cx*16+8,64,cz*16+8));
            if(pos.distSqr(world.getSharedSpawnPos())<128*128||!RealmPortal.site(world,pos)||!safe(world,pos)||!world.getWorldBorder().isWithinBounds(pos)||!world.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.DIRT))continue;
            if(cluster.stream().anyMatch(p->p.position().distanceToSqr(Vec3.atCenterOf(pos))>3200.0*3200))continue;
            boolean clear=true;for(int dx=-4;dx<=4&&clear;dx++)for(int dz=-4;dz<=4;dz++)if(!safe(world,pos.offset(dx,0,dz))||!world.getBlockState(pos.offset(dx,-1,dz)).is(net.minecraft.tags.BlockTags.DIRT)){clear=false;break;}
            if(!clear||BeastSpawnProtection.inTown(world,pos)||BeastSpawnProtection.nearBed(world,pos)||com.longju.xiantu.worldgen.RuinProtection.protectedAt(world,pos))continue;
            if(world.structureManager().startsForStructure(new ChunkPos(pos),s->true).stream().anyMatch(st->st.getBoundingBox().isInside(pos)))continue;
            return pos;
        }return null;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;var d=data(server);if(d.blocked())return;
        RealmPortal.tick(server);InstanceContent.tick(server);InstanceLevelLifecycle.tick(server);for(var active:d.slots())if(active.kind()==InstanceLedger.Kind.REALM&&active.state()==InstanceLedger.State.ACTIVE){var l=level(server,active);if(l!=null)RealmEcology.tick(l,active);}long now=now(server);
        if(now%20!=0)return;
        for(var s:d.slots())if(s.state()!=InstanceLedger.State.FREE){var n=d.session(s);var l=level(server,s);
            if(s.kind()==InstanceLedger.Kind.CAVE){if(l!=null){l.setDayTime(server.overworld().getDayTime());l.setWeatherParameters(6000,6000,server.overworld().isRaining(),server.overworld().isThundering());}continue;}
            if(s.state()==InstanceLedger.State.PREPARING&&n.getBoolean("Ready")&&now>=n.getLong("OpenAt")){d.ledger().activate(s);d.setDirty();RealmPortal.activate(server.overworld(),BlockPos.of(n.getLong("Entry")));announce(server,opening(n));}
            if(n.contains("CollapseAt")&&now>=n.getLong("CollapseAt")){close(server,s,"越界崩塌");continue;}
            if(s.state()!=InstanceLedger.State.ACTIVE&&s.state()!=InstanceLedger.State.CLOSING)continue;
            long remaining=n.getLong("EndAt")-now;int flags=n.getInt("Warnings");int[] times={12000,3600,600};
            for(int i=0;i<times.length;i++)if(remaining<=times[i]&&(flags&(1<<i))==0){flags|=1<<i;announce(server,"剩余"+(remaining/20)+"秒");}
            n.putInt("Warnings",flags);d.setDirty();
            if(remaining<=0){close(server,s,"秘境已到期");continue;}
            if(remaining<=InstanceRules.EARLY_RETURN_TICKS&&s.state()==InstanceLedger.State.ACTIVE){
                n.putBoolean("Frozen",true);d.ledger().beginClose(s);InstanceContent.stop(s.instance());
                if(l!=null)for(var p:List.copyOf(l.players()))returnHome(p);
                announce(server,"秘境已封闭，玩家已返回");d.setDirty();
            }
        }
        if(d.ledger().activeRealm()!=null)return;
        long clock=System.currentTimeMillis();
        if(!d.runtime().contains("NextRealmAtMillis")){d.runtime().putLong("NextRealmAtMillis",InstanceRules.nextRealmAt(clock));d.setDirty();return;}
        if(clock<d.runtime().getLong("NextRealmAtMillis")||clock<d.runtime().getLong("RealmSiteRetryAtMillis"))return;
        // Once due, wait for an eligible loaded site. Do not lose the two-hour opportunity to
        // an empty server, an unsuitable biome, or the old 25% lottery. Retry gently once/minute.
        d.runtime().putLong("RealmSiteRetryAtMillis",clock+60000);d.setDirty();
        var people=server.getPlayerList().getPlayers().stream().filter(p->p.level().dimension()==Level.OVERWORLD&&!p.isSpectator()).toList();if(people.isEmpty())return;
        var entry=candidate(server,people);if(entry==null)return;
        createRealm(server,entry,InstanceRules.draw(RANDOM),0);

    }
    public static boolean mayTeleport(ServerPlayer p,ServerLevel target,double x,double y,double z){
        if(trusted()||!isDimension(target.dimension().location()))return true;
        var s=slot(target);if(s==null||s.state()!=InstanceLedger.State.ACTIVE||!inside(target,x,y,z))return false;
        // Cross-dimension entry is exclusively a portal/token transaction, even if old membership survives.
        if(p.serverLevel()!=target)return false;
        return authorized(p,s);
    }
    private static boolean authorized(ServerPlayer p,InstanceLedger.Slot s){
        var n=data(p.server).session(s);String id=p.getUUID().toString();
        if(s.kind()==InstanceLedger.Kind.REALM)return !n.getBoolean("Closed")&&n.getCompound("Members").contains(id);
        String grant=n.getCompound("Visitors").getString(id);return grant.equals("master")&&s.owner().equals(p.getUUID())||!grant.isEmpty()&&n.getCompound("Grants").getInt(grant)>0;
    }
    @SubscribeEvent public static void player(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||!p.isAlive())return;
        if(p.getPersistentData().getBoolean("xiantu_instance_death")){p.getPersistentData().remove("xiantu_instance_death");returnHome(p);return;}
        if(!isDimension(p.level().dimension().location()))return;
        var s=slot(p.serverLevel());if(s==null||s.state()!=InstanceLedger.State.ACTIVE||!authorized(p,s)){returnHome(p);return;}
        if(s.kind()==InstanceLedger.Kind.REALM){var a=InstanceRules.Abundance.valueOf(data(p.server).session(s).getString("Abundance"));var c=GongfaService.data(p);
            if(!c.cultivationUnlocked()||c.majorRealm()<a.minRealm){p.hurt(p.damageSources().fellOutOfWorld(),Float.MAX_VALUE);return;}
            if(c.majorRealm()>a.maxRealm){var n=data(p.server).session(s);if(!n.contains("CollapseAt")){n.putLong("CollapseAt",now(p.server)+100);n.putBoolean("Frozen",true);announce(p.server,"高境界力量引发崩塌，5秒后遣返所有人！");data(p.server).setDirty();}if(now(p.server)>=n.getLong("CollapseAt"))close(p.server,s,"越界崩塌");return;}
        }
        if(!inside(p.serverLevel(),p.getX(),p.getY(),p.getZ())){
            var b=p.serverLevel().getWorldBorder();var pos=new Vec3(Math.max(b.getMinX()+1,Math.min(b.getMaxX()-1,p.getX())),Math.max(-62,Math.min(316,p.getY())),Math.max(b.getMinZ()+1,Math.min(b.getMaxZ()-1,p.getZ())));
            travel(p,p.serverLevel(),pos);p.displayClientMessage(Component.literal("已到达洞天边界"),true);
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void travel(EntityTravelToDimensionEvent e){if(e.getEntity() instanceof ServerPlayer&&!trusted()&&isDimension(e.getDimension().location()))e.setCanceled(true);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p&&!isDimension(e.getTo().location()))release(p);}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){
        if(!isDimension(p.level().dimension().location())){release(p);return;}
        var s=slot(p.serverLevel());if(s==null||s.state()!=InstanceLedger.State.ACTIVE||!authorized(p,s))returnHome(p);
        else if(s.kind()==InstanceLedger.Kind.REALM){data(p.server).session(s).getCompound("Members").putString(p.getUUID().toString(),"inside");data(p.server).setDirty();}
    }}
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){if(e.isWasDeath()&&temporary(e.getOriginal().level())){e.getEntity().getInventory().replaceWith(e.getOriginal().getInventory());e.getEntity().getPersistentData().putBoolean("xiantu_instance_death",true);}}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p&&(p.getPersistentData().getBoolean("xiantu_instance_death")||isDimension(p.level().dimension().location()))){
        // The connection still references the old player during Forge's respawn event.
        // Release admission now, then teleport on the first tick of the new connected player.
        release(p);p.getPersistentData().putBoolean("xiantu_instance_death",true);
    }}
    @SubscribeEvent public static void pickup(EntityItemPickupEvent e){if(e.getEntity().level() instanceof ServerLevel l&&isDimension(l.dimension().location())&&!writable(l,e.getEntity().blockPosition()))e.setCanceled(true);}
    public static boolean mayAct(ServerPlayer p,BlockPos pos){var s=slot(p.serverLevel());return s!=null&&authorized(p,s)&&writable(p.serverLevel(),pos);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interact(PlayerInteractEvent.RightClickBlock e){if(e.getEntity() instanceof ServerPlayer p&&isDimension(e.getLevel().dimension().location())&&!mayAct(p,e.getPos())&&!e.getLevel().getBlockState(e.getPos()).is(InstanceRegistry.CORE.get())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void breakBlock(net.minecraftforge.event.level.BlockEvent.BreakEvent e){if(e.getPlayer() instanceof ServerPlayer p&&isDimension(p.level().dimension().location())&&!mayAct(p,e.getPos()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void placeBlock(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent e){if(e.getEntity() instanceof ServerPlayer p&&isDimension(p.level().dimension().location())&&!mayAct(p,e.getPos()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void freezeCombat(LivingAttackEvent e){if(e.getEntity().level() instanceof ServerLevel l&&temporary(l)){var s=slot(l);if(s==null||data(l.getServer()).session(s).getBoolean("Frozen"))e.setCanceled(true);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void join(EntityJoinLevelEvent e){
        if(!(e.getLevel() instanceof ServerLevel l)||!isDimension(l.dimension().location())||e.getEntity() instanceof ServerPlayer)return;
        var s=slot(l);if(s==null||s.state()==InstanceLedger.State.FREE||s.state()==InstanceLedger.State.QUARANTINED||data(l.getServer()).session(s).getBoolean("Frozen")){e.setCanceled(true);return;}
        if(!inside(l,e.getEntity().getX(),e.getEntity().getY(),e.getEntity().getZ())){e.setCanceled(true);return;}
        if(e.getEntity() instanceof Mob mob&&BeastRealmService.isBeast(mob)&&s.kind()==InstanceLedger.Kind.REALM&&!mob.getPersistentData().getBoolean(BeastRealmService.INITIALIZED)){
            var a=InstanceRules.Abundance.valueOf(data(l.getServer()).session(s).getString("Abundance"));InstanceContent.realmBeast(mob,a.minRealm,a.maxRealm);
        }
    }
    @SubscribeEvent public static void started(ServerStartedEvent e){
        var d=data(e.getServer());if(d.blocked())return;
        for(var s:d.slots())if(s.state()!=InstanceLedger.State.FREE){var l=level(e.getServer(),s);if(l==null)continue;var n=d.session(s);configure(l,s,n);
            if(s.kind()==InstanceLedger.Kind.REALM){if(n.getBoolean("Closed")||now(e.getServer())>=n.getLong("EndAt")-InstanceRules.EARLY_RETURN_TICKS)close(e.getServer(),s,"恢复关闭");else if(!n.getBoolean("Ready"))try{InstanceContent.begin(e.getServer(),s,n);}catch(java.io.IOException failure){close(e.getServer(),s,"蓝图恢复失败");}}
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){InstanceLevelLifecycle.clear(e.getServer());DATA.remove(e.getServer());InstanceContent.clear();}
    private InstanceService(){}
}
