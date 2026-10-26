package com.longju.xiantu.instance;

import com.google.gson.*;
import com.longju.xiantu.cultivation.RealmRegistry;
import com.longju.xiantu.entity.*;
import com.longju.xiantu.registry.*;
import com.longju.xiantu.medicine.*;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Resumable geometry batches; reward/node/guardian operations have persisted, single-use tickets. */
public final class InstanceContent {
    private record Placement(BlockPos pos,BlockState state){}
    private static final class Job {
        final InstanceLedger.Slot slot;final List<Placement> blocks;List<Runnable> population;
        Job(InstanceLedger.Slot s,List<Placement> b){slot=s;blocks=b;}
    }
    private static final Map<UUID,Job> JOBS=new LinkedHashMap<>();
    public static void begin(MinecraftServer server,InstanceLedger.Slot s,CompoundTag n)throws java.io.IOException{
        var blocks=new ArrayList<Placement>();
        if(n.getBoolean("Natural")){JOBS.put(s.instance(),new Job(s,List.of()));return;}
        var layout=RealmLayout.get(server,n.getString("Theme"));
        for(var cell:layout.blocks()){
            try{var state=BlockStateParser.parseForBlock(server.registryAccess().lookupOrThrow(Registries.BLOCK),cell.state(),false).blockState();blocks.add(new Placement(cell.at(),state));}
            catch(com.mojang.brigadier.exceptions.CommandSyntaxException failure){throw new java.io.IOException("Invalid realm blueprint: "+cell.state(),failure);}
        }
        JOBS.put(s.instance(),new Job(s,List.copyOf(blocks)));
    }
    public static void stop(UUID id){JOBS.remove(id);}
    public static void clear(){JOBS.clear();RealmLayout.clear();}
    public static void tick(MinecraftServer server){
        for(var job:List.copyOf(JOBS.values())){
            var d=InstanceService.data(server);var n=d.session(job.slot);if(n.getBoolean("Frozen")){stop(job.slot.instance());continue;}var l=InstanceService.level(server,job.slot);if(l==null)continue;
            int from=n.getInt("BuildIndex"),to=Math.min(job.blocks.size(),from+256);
            InstanceService.internal(()->{for(int i=from;i<to;i++){var p=job.blocks.get(i);l.setBlock(p.pos,p.state,2);}});
            n.putInt("BuildIndex",to);d.setDirty();
            if(to==job.blocks.size()){
                var a=InstanceRules.Abundance.valueOf(n.getString("Abundance"));
                try{
                    if(job.population==null)job.population=n.getBoolean("Natural")?naturalPopulation(l,job.slot,n,a):population(l,job.slot,n,a);
                    int index=n.getInt("PopulateIndex"),end=Math.min(job.population.size(),index+4);
                    InstanceService.internal(()->{for(int i=index;i<end;i++)job.population.get(i).run();});
                    n.putInt("PopulateIndex",end);d.setDirty();
                    if(end==job.population.size()){RealmPortal.exit(l,InstanceService.corePos(job.slot));n.putBoolean("Ready",true);d.setDirty();stop(job.slot.instance());}
                }
                catch(RuntimeException failure){com.mojang.logging.LogUtils.getLogger().error("秘境内容准备失败",failure);InstanceService.close(server,job.slot,"内容准备失败");}
            }
        }
    }
    private static List<Runnable> population(ServerLevel l,InstanceLedger.Slot s,CompoundTag n,InstanceRules.Abundance a){
        var actions=new ArrayList<Runnable>();var random=new Random(n.getLong("Seed"));var d=InstanceService.data(l.getServer());var layout=RealmLayout.get(l.getServer(),n.getString("Theme"));
        int maxTier=Math.min(8,a.maxRealm+1),minTier=Math.max(1,a.minRealm+1);
        var ores=List.of(ModBlocks.FAN_LING_SHI_ORE,ModBlocks.LING_JING_ORE,ModBlocks.LING_YU_ORE,ModBlocks.XUAN_JING_ORE,ModBlocks.DI_SUI_ORE,ModBlocks.TIAN_JING_ORE,ModBlocks.XIAN_YU_ORE,ModBlocks.DAO_SHI_ORE);
        // Node attempts scale once; single-block ore drops and item quality remain unchanged.
        var rockPositions=new ArrayList<BlockPos>(layout.ores());
        Collections.shuffle(rockPositions,random);
        for(int i=0;i<12*a.resources;i++){
            int tier=InstanceRules.resourceTier(a,false,random);var pos=rockPositions.get(i);String key="Node_"+i;
            actions.add(()->once(n,key,d,()->l.setBlockAndUpdate(pos,ores.get(tier-1).get().defaultBlockState())));
        }
        var herbPositions=new ArrayList<BlockPos>(layout.herbs());Collections.shuffle(herbPositions,random);
        for(int i=0;i<10*a.resources;i++){
            int tier=InstanceRules.resourceTier(a,false,random);var herbs=MedicineCatalog.HERBS.stream().filter(h->h.tier()==tier&&MedicineRegistry.WILD_PLANTS.containsKey(h.registryId())).toList();
            if(herbs.isEmpty())throw new IllegalStateException("No registered herbs for realm tier "+tier);
            var block=MedicineRegistry.WILD_PLANTS.get(herbs.get(random.nextInt(herbs.size())).registryId()).get().defaultBlockState();var pos=herbPositions.get(i);String key="Herb_"+i;
            actions.add(()->once(n,key,d,()->{if(!block.canSurvive(l,pos))throw new IllegalStateException("Invalid realm herb soil");l.setBlockAndUpdate(pos,block);}));
        }
        var ordinary=BeastEcology.profile().species().stream().filter(v->v.element().equals(n.getString("Theme"))&&!v.elite()&&Set.of("land","cave").contains(v.mode())&&!BeastRealmService.peacefulSpecies(v.id())).toList();
        var elite=BeastEcology.profile().species().stream().filter(v->v.element().equals(n.getString("Theme"))&&v.elite()&&Set.of("land","cave").contains(v.mode())).toList();
        if(ordinary.isEmpty())throw new IllegalStateException("No land guardian matches realm theme");
        int guard=0;var zones=layout.zones().stream().filter(z->!z.id().equals("entrance")).toList();
        for(int zi=0;zi<zones.size();zi++){
            var zone=zones.get(zi);if(zone.id().equals("hidden")&&a.resources<3)continue;int count=(zone.risk()==3?3:2)+a.ordinal();
            for(int j=0;j<count;j++){
                boolean high=zone.risk()==3;int realm=InstanceRules.guardianRealm(a,high,random);
                String id=high&&j==0&&a.resources>=3&&!elite.isEmpty()?elite.get(random.nextInt(elite.size())).id():ordinary.get(random.nextInt(ordinary.size())).id();
                int x=zone.x()-8+random.nextInt(17),z=zone.z()-7+random.nextInt(15);String key="Guard_"+(guard++);float yaw=random.nextFloat()*360;
                actions.add(()->once(n,key,d,()->{
                    var type=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("xiantu",id));if(type==null)throw new IllegalStateException("Unregistered realm guardian");
                    var entity=type.create(l);if(!(entity instanceof Mob mob))throw new IllegalStateException("Non-mob realm guardian");
                    BlockPos at=spawnPoint(l,x,zone.y(),z,zone.x(),zone.z());mob.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,yaw,0);realmBeast(mob,realm,realm);mob.setPersistenceRequired();mob.getPersistentData().putUUID("xiantu_realm_instance",s.instance());
                    if(!l.addFreshEntity(mob))throw new IllegalStateException("Realm guardian insertion rejected");
                }));
            }
        }
        actions.add(()->chest(l,s,n,"CoreReward",layout.rewards().get("core"),maxTier,a.resources,true));
        actions.add(()->chest(l,s,n,"ResourceReward",layout.rewards().get("quarry"),Math.max(minTier,maxTier-1),a.resources,false));
        actions.add(()->chest(l,s,n,"HerbReward",layout.rewards().get("herb"),Math.max(minTier,maxTier-1),a.resources,false));
        if(a.resources>=3){actions.add(()->chest(l,s,n,"HiddenReward",layout.rewards().get("hidden"),maxTier,a.resources,false));actions.add(()->chest(l,s,n,"BattleReward",layout.rewards().get("battle"),maxTier,a.resources,false));}
        return List.copyOf(actions);
    }
    private static List<Runnable> naturalPopulation(ServerLevel l,InstanceLedger.Slot s,CompoundTag n,InstanceRules.Abundance a){
        var out=new ArrayList<Runnable>();var random=new Random(n.getLong("Seed")^0x77696c646572L);var d=InstanceService.data(l.getServer());
        var species=BeastEcology.profile().species().stream().filter(v->v.element().equals(n.getString("Theme"))&&!v.elite()&&Set.of("land","fly").contains(v.mode())).toList();
        for(int i=0;i<12+a.ordinal()*2;i++){
            double angle=i*Math.PI*2/12;int x=(int)(Math.cos(angle)*(48+random.nextInt(42))),z=(int)(Math.sin(angle)*(48+random.nextInt(42)));
            String key="WildGuard_"+i;var chosen=species.get(random.nextInt(species.size()));
            out.add(()->once(n,key,d,()->{l.getChunk(x>>4,z>>4);var at=l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(x,65,z));if(!BeastNaturalSpawns.habitat(l,at,chosen))return;
                var mob=BeastNaturalSpawns.spawn(l,at,chosen);if(mob!=null){mob.setPersistenceRequired();mob.getPersistentData().putUUID("xiantu_realm_instance",s.instance());}
            }));
        }
        // Small weathered caches are optional discoveries, not buildings or mandatory corridors.
        int[][] caches={{66,-57},{-71,67},{72,83}};
        for(int i=0;i<caches.length;i++){
            int index=i,x=caches[i][0],z=caches[i][1];String key="WildCache_"+i;
            out.add(()->{if(n.getBoolean(key))return;l.getChunk(x>>4,z>>4);var at=l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(x,65,z));
                if(!InstanceService.safe(l,at))return;
                chest(l,s,n,key,at,Math.min(8,a.maxRealm+1),a.resources,index==2);
                for(int dx:new int[]{-1,1})l.setBlockAndUpdate(at.offset(dx,0,0),Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                l.setBlockAndUpdate(at.above(),Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState());
            });
        }
        return List.copyOf(out);
    }
    private static void once(CompoundTag n,String key,InstanceSavedData d,Runnable action){if(n.getBoolean(key))return;n.putBoolean(key,true);d.setDirty();action.run();}
    private static BlockPos spawnPoint(ServerLevel l,int x,int y,int z,int cx,int cz){
        for(int radius=0;radius<=8;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            var p=new BlockPos(x+dx,y,z+dz);if(InstanceService.safe(l,p)&&l.getBlockState(p.above(2)).getCollisionShape(l,p.above(2)).isEmpty())return p;
        }
        var p=new BlockPos(cx,y,cz);if(!InstanceService.safe(l,p))throw new IllegalStateException("No safe guardian position");return p;
    }
    private static void chest(ServerLevel l,InstanceLedger.Slot s,CompoundTag n,String key,BlockPos at,int tier,int groups,boolean core){
        if(n.getBoolean(key))return;n.putBoolean(key,true);InstanceService.data(l.getServer()).setDirty();
        l.setBlockAndUpdate(at,ModBlocks.RUIN_REWARD_CHEST.get().defaultBlockState());if(!(l.getBlockEntity(at) instanceof ChestBlockEntity chest))throw new IllegalStateException("Missing realm reward container");
        var params=new LootParams.Builder(l).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(at)).create(LootContextParamSets.CHEST);
        var slots=new ArrayList<Integer>();for(int i=0;i<chest.getContainerSize();i++)slots.add(i);
        Collections.shuffle(slots,new Random(n.getLong("Seed")^at.asLong()^0x6c6f6f74736c6f74L));
        var items=new ArrayList<ItemStack>();
        if(core&&n.getBoolean("TokenReward"))items.add(new ItemStack(InstanceRegistry.TOKEN.get()));
        if(core&&InstanceRules.Abundance.valueOf(n.getString("Abundance")).maxRealm>=3&&n.getBoolean("SoulReward"))items.add(new ItemStack(ModItems.INNER_DEMON_CRYSTAL.get()));
        var abundance=InstanceRules.Abundance.valueOf(n.getString("Abundance"));
        var rewardRandom=new Random(n.getLong("Seed")^at.asLong()^0x7265776172647469L);
        var talisman=com.longju.xiantu.talisman.TalismanSources.roll(tier,core&&tier>=4,rewardRandom,core?.12:.04);
        if(!talisman.isEmpty())items.add(talisman);
        for(int i=0;i<groups;i++){
            int rolledTier=Math.min(tier,InstanceRules.resourceTier(abundance,core,rewardRandom));
            var table=l.getServer().getLootData().getLootTable(new ResourceLocation("xiantu:instance/material_"+rolledTier));
            var rewards=table.getRandomItems(params,n.getLong("Seed")+i+at.asLong());
            // Generic beast materials in the loot table must carry the selected
            // grade too, otherwise they are unusable ungraded recipe inputs.
            for(var stack:rewards)if(stack.is(ModItems.BEAST_BONE.get())||stack.getItem() instanceof com.longju.xiantu.item.BeastMaterialItem)
                com.longju.xiantu.item.BeastMaterialItem.setData(stack,rolledTier,n.getString("Theme"));
            items.addAll(rewards);
        }
        for(int i=0;i<Math.min(items.size(),slots.size());i++)chest.setItem(slots.get(i),items.get(i));
        chest.setChanged();
    }
    public static void realmBeast(Mob mob,int min,int max){
        var random=mob.getRandom();int realm=min+random.nextInt(max-min+1);var n=mob.getPersistentData();String id=BeastRealmService.species(mob);var species=BeastEcology.species(id);
        n.putInt(BeastResourceDrops.REALM,realm);n.putInt(BeastRealmService.STAGE,random.nextInt(RealmRegistry.STAGE_COUNTS[realm]));n.putBoolean(BeastRealmService.INITIALIZED,true);n.putString(BeastRealmService.SPECIES,id);n.putString(BeastRealmService.ELEMENT,species==null?"earth":species.element());n.putBoolean(BeastRealmService.ELITE,species!=null&&species.elite());n.putLong(BeastRealmService.HOME,mob.blockPosition().asLong());BeastRealmService.initialize(mob);mob.setHealth(mob.getMaxHealth());
    }
    private InstanceContent(){}
}
