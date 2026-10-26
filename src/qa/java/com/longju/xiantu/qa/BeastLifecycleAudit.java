package com.longju.xiantu.qa;

import com.google.gson.GsonBuilder;
import com.longju.xiantu.entity.*;
import com.longju.xiantu.item.DemonCoreItem;
import com.longju.xiantu.registry.ModItems;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Disposable lifecycle fixtures are separate from the real scheduled natural-spawn observation. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class BeastLifecycleAudit {
    private static int checks;
    private static final Map<UUID,List<ItemEntity>> CAPTURE=new HashMap<>();
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void capture(LivingDropsEvent e){
        var out=CAPTURE.get(e.getEntity().getUUID());if(out!=null)out.addAll(e.getDrops());
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        var root=Commands.literal("xiantubeastaudit").requires(s->s.hasPermission(2));
        root.then(Commands.literal("core").executes(c->{
            var source=c.getSource();var level=source.getLevel();
            if(!level.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")){
                source.sendFailure(Component.literal("Only Xiantu-QA worlds may run fixtures"));return 0;
            }
            checks=0;var player=FakePlayerFactory.getMinecraft(level);
            var inventory=player.getInventory().save(new ListTag());var pos=player.position();var mode=player.gameMode.getGameModeForPlayer();
            var cultivation=player.getCapability(com.longju.xiantu.cultivation.CultivationCapability.DATA).orElseThrow(()->new AssertionError("capability"));
            double demon=cultivation.innerDemon();List<Entity> fixtures=new ArrayList<>();
            try{
                var coreModels=new HashSet<Integer>();
                for(String element:DemonCoreItem.ELEMENTS) for(int grade=1;grade<=DemonCoreItem.MAX_GRADE;grade++){
                    var core=new net.minecraft.world.item.ItemStack(ModItems.DEMON_CORE.get());
                    DemonCoreItem.setData(core,grade,element);
                    var restored=net.minecraft.world.item.ItemStack.of(core.save(new CompoundTag()));
                    require(DemonCoreItem.grade(restored)==grade&&DemonCoreItem.element(restored).equals(element),"core variant persists");
                    int model=DemonCoreItem.modelData(restored);
                    require(coreModels.add(model)&&model==DemonCoreItem.ELEMENTS.indexOf(element)*9+grade,"unique core model");
                    require(restored.getOrCreateTag().getInt("CustomModelData")==model,"core model NBT matches");
                    require(!restored.getHoverName().getString().contains("item.xiantu"),"core localized name");
                }
                require(coreModels.size()==54,"all six elements and nine grades");
                require(BeastEcology.profile()!=null,"ecology loaded");
                var biomes=level.registryAccess().registryOrThrow(Registries.BIOME);
                var selectors=new HashSet<String>();
                for(var s:BeastEcology.profile().species())selectors.addAll(s.biomes());
                BeastEcology.profile().featured().values().forEach(selectors::addAll);
                BeastEcology.profile().rare().values().forEach(selectors::addAll);
                for(String selector:selectors){
                    if(selector.startsWith("#"))require(biomes.getTag(net.minecraft.tags.TagKey.create(Registries.BIOME,new ResourceLocation(selector.substring(1)))).filter(t->t.size()>0).isPresent(),"nonempty habitat tag "+selector);
                    else require(biomes.containsKey(new ResourceLocation(selector)),"registered habitat "+selector);
                }
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                var origin=source.getPlayerOrException().position().add(10,0,10);((Entity)player).setPos(origin.x,origin.y,origin.z);
                // Independent expectations cover every major realm and element, including the shared tier-six boundary.
                int[] expectedCoreTiers={1,2,3,4,5,6,6,7,8};
                for(String element:DemonCoreItem.ELEMENTS) for(int realm=0;realm<9;realm++){
                    var species=BeastEcology.profile().species().stream().filter(s->s.element().equals(element)).findFirst().orElseThrow();
                    var type=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("xiantu",species.id()));
                    Mob mob=(Mob)type.create(level);Entity entity=mob;LivingEntity living=mob;
                    entity.setPos(origin.x,origin.y,origin.z);
                    BeastRealmService.initialize(mob);
                    var data=entity.getPersistentData();data.putBoolean(BeastResourceDrops.NATURAL,true);
                    data.putInt(BeastResourceDrops.REALM,realm);data.putInt(BeastRealmService.STAGE,0);
                    BeastRealmService.initialize(mob);
                    var damage=level.damageSources().playerAttack(player);var loot=new ArrayList<ItemEntity>();
                    BeastResourceDrops.damaged(new LivingDamageEvent(living,damage,1));
                    BeastResourceDrops.drops(new LivingDropsEvent(living,damage,loot,0,true));
                    var cores=loot.stream().map(ItemEntity::getItem).filter(st->st.is(ModItems.DEMON_CORE.get())).toList();
                    require(cores.size()==1&&cores.get(0).getCount()==1,"one core per realm/element "+realm+"/"+element);
                    require(DemonCoreItem.grade(cores.get(0))==expectedCoreTiers[realm]&&DemonCoreItem.element(cores.get(0)).equals(element),"explicit core matrix "+realm+"/"+element);
                    require(cores.get(0).getOrCreateTag().getInt("CustomModelData")==DemonCoreItem.ELEMENTS.indexOf(element)*9+expectedCoreTiers[realm],"drop selects artwork "+realm+"/"+element);
                }
                for(String id:List.of("forging_table","cultivation_stone","alchemy_post","forging_post","talisman_post","alchemy_table")){
                    require(!ForgeRegistries.ITEMS.containsKey(new ResourceLocation("xiantu",id)),"obsolete item absent "+id);
                    require(!ForgeRegistries.BLOCKS.containsKey(new ResourceLocation("xiantu",id)),"obsolete block absent "+id);
                }
                int index=0;
                for(var s:BeastEcology.profile().species()){
                    var type=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("xiantu",s.id()));
                    require(type!=null,"registered "+s.id());Mob mob=(Mob)type.create(level);Entity entity=mob;LivingEntity living=mob;
                    require(mob!=null,"construct "+s.id());entity.setPos(origin.x,origin.y,origin.z);
                    require(mob.getAttribute(Attributes.ATTACK_DAMAGE)!=null,"attack attribute "+s.id());
                    BeastRealmService.initialize(mob);require(BeastRealmService.valid(entity),"realm initialized "+s.id());
                    int realm=index++%9,stage=BeastRealmRules.stages(realm)-1;
                    var data=entity.getPersistentData();data.putInt(BeastResourceDrops.REALM,realm);data.putInt(BeastRealmService.STAGE,stage);
                    BeastRealmService.initialize(mob);living.setHealth((float)(living.getMaxHealth()*.6));
                    float hp=living.getHealth();double max=living.getMaxHealth();long progress=data.getLong(BeastRealmService.PROGRESS);
                    require(Math.abs(max-BeastRealmRules.health(realm,stage,s.elite()))<.01,"unclamped max health "+s.id());
                    var saved=new CompoundTag();entity.saveWithoutId(saved);Mob copy=(Mob)type.create(level);Entity copyEntity=copy;LivingEntity copyLiving=copy;
                    copyEntity.load(saved);BeastRealmService.initialize(copy);
                    require(BeastRealmService.realm(copyEntity)==realm&&copyEntity.getPersistentData().getInt(BeastRealmService.STAGE)==stage,"realm persists "+s.id());
                    require(copyEntity.getPersistentData().getLong(BeastRealmService.PROGRESS)==progress&&Math.abs(copyLiving.getHealth()-hp)<.01,"reload no reroll/heal "+s.id());
                    var damage=level.damageSources().playerAttack(player);var loot=new ArrayList<ItemEntity>();
                    BeastResourceDrops.damaged(new LivingDamageEvent(living,damage,1));
                    BeastResourceDrops.drops(new LivingDropsEvent(living,damage,loot,0,true));require(loot.isEmpty(),"non-natural rejects resources "+s.id());
                    data.putBoolean(BeastResourceDrops.NATURAL,true);
                    BeastResourceDrops.drops(new LivingDropsEvent(living,level.damageSources().fall(),loot,0,true));require(loot.isEmpty(),"environment rejects "+s.id());
                    BeastResourceDrops.drops(new LivingDropsEvent(living,damage,loot,0,true));
                    var core=loot.stream().map(ItemEntity::getItem).filter(st->st.is(ModItems.DEMON_CORE.get())).findFirst().orElseThrow();
                    require(DemonCoreItem.grade(core)==BeastRealmRules.coreTier(realm)&&DemonCoreItem.element(core).equals(s.element()),"realm/element core "+s.id());
                    for(var drop:loot)require(drop.getItem().getCount()==1&&drop.getPersistentData().getUUID("xiantu_loot_owner").equals(player.getUUID()),"single owned loot");
                    int size=loot.size();BeastResourceDrops.drops(new LivingDropsEvent(living,damage,loot,0,true));require(loot.size()==size,"idempotent "+s.id());
                    // Prove a live Forge death invokes the event chain, not only direct handler fixtures.
                    if(s.id().equals("yanwei_fox")){
                        Mob live=(Mob)type.create(level);Entity liveEntity=live;LivingEntity liveLiving=live;
                        liveEntity.setPos(origin.x,origin.y,origin.z);live.setNoAi(true);liveEntity.addTag("xiantu_beast_audit");
                        net.minecraftforge.event.ForgeEventFactory.onFinalizeSpawn(live,level,level.getCurrentDifficultyAt(liveEntity.blockPosition()),MobSpawnType.NATURAL,null,null);
                        fixtures.add(liveEntity);require(level.addFreshEntity(liveEntity),"fixture enters level");
                        require(liveEntity.getPersistentData().getBoolean(BeastResourceDrops.NATURAL)&&BeastRealmService.valid(liveEntity),"finalize source and join identity");
                        var captured=new ArrayList<ItemEntity>();CAPTURE.put(liveEntity.getUUID(),captured);
                        require(liveLiving.hurt(damage,1000000F),"real player damage");
                        require(!liveLiving.isAlive()&&captured.stream().anyMatch(it->it.getItem().is(ModItems.DEMON_CORE.get())),"real death emits core");
                        fixtures.addAll(captured);CAPTURE.remove(liveEntity.getUUID());
                    }
                }
                var bedTag=new CompoundTag();var list=new ListTag();var bed=new CompoundTag();bed.putUUID("player",UUID.randomUUID());bed.putString("dim","minecraft:overworld");bed.putLong("pos",source.getPlayerOrException().blockPosition().asLong());list.add(bed);bedTag.put("beds",list);
                net.minecraft.world.level.saveddata.SavedData bedCopy=BeastSpawnProtection.load(bedTag);
                require(bedCopy.save(new CompoundTag()).equals(bedTag),"offline bed identity persists");
                var report=Map.of("checks",checks,"species",30,"runtime","Forge lifecycle fixtures; random natural cycles separately observed","seed",level.getSeed());
                java.nio.file.Files.writeString(FMLPaths.GAMEDIR.get().resolve("xiantu-beast-audit.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
                source.sendSuccess(()->Component.literal("Beast lifecycle PASS "+checks),false);return checks;
            }catch(Exception|AssertionError|LinkageError ex){source.sendFailure(Component.literal("Beast lifecycle FAIL after "+checks+": "+ex));ex.printStackTrace();return 0;}
            finally{fixtures.forEach(Entity::discard);CAPTURE.clear();cultivation.setInnerDemon(demon);player.getInventory().load(inventory);player.setGameMode(mode);((Entity)player).setPos(pos.x,pos.y,pos.z);}
        }));
        root.then(Commands.literal("nearby").executes(c->{
            var p=c.getSource().getPlayerOrException();var level=p.serverLevel();var found=new ArrayList<Map<String,Object>>();
            for(var mob:level.getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(128),BeastRealmService::isBeast)){
                Entity entity=mob;LivingEntity living=mob;var d=entity.getPersistentData();
                found.add(Map.of("id",BeastRealmService.species(entity),"uuid",entity.getUUID().toString(),"natural",d.getBoolean(BeastResourceDrops.NATURAL),"realm",BeastRealmService.realm(entity),"stage",d.getInt(BeastRealmService.STAGE),"health",living.getHealth(),"maxHealth",living.getMaxHealth(),"pos",List.of(entity.getX(),entity.getY(),entity.getZ())));
            }
            try{java.nio.file.Files.writeString(FMLPaths.GAMEDIR.get().resolve("xiantu-beast-nearby.json"),new GsonBuilder().setPrettyPrinting().create().toJson(found));}catch(Exception ex){throw new RuntimeException(ex);}
            c.getSource().sendSuccess(()->Component.literal("Nearby beasts "+found),false);return found.size();
        }));
        event.getDispatcher().register(root);
    }
    private static void require(boolean b,String label){if(!b)throw new AssertionError(label);checks++;}
}
