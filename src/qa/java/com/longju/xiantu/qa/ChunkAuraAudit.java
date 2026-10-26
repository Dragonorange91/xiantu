package com.longju.xiantu.qa;

import com.longju.xiantu.formation.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.registry.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class ChunkAuraAudit {
    private static int checks;
    private static void check(boolean value,String label) { checks++; if(!value)throw new IllegalStateException(label); }
    private static FormationSeatEntity sit(ServerPlayer player,BlockPos pos) {
        if(player.getVehicle()!=null) {Entity old=player.getVehicle();player.stopRiding();if(old instanceof FormationSeatEntity)old.discard();}
        var seat=ModEntities.FORMATION_SEAT.get().create(player.serverLevel());
        seat.setCushionPos(pos);((Entity)seat).setPos(pos.getX()+.5,pos.getY()+.05,pos.getZ()+.5);
        player.serverLevel().addFreshEntity(seat);player.startRiding(seat,true);return seat;
    }
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("xiantuauraaudit").requires(s->s.hasPermission(2)).executes(c->{
            var level=c.getSource().getLevel();var player=c.getSource().getPlayerOrException();
            if(!level.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA") || player.getVehicle()!=null){c.getSource().sendFailure(Component.literal("Aura QA requires Xiantu-QA world and an unmounted player"));return 0;}
            c.getSource().sendSuccess(()->Component.literal("Starting chunk aura audit"),false);
            var data=(PlayerCultivationData)player.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new);
            var saved=new CompoundTag();data.save(saved);var savedAura=((net.minecraft.world.level.saveddata.SavedData)ChunkAuraData.get(level)).save(new CompoundTag());
            var position=((Entity)player).position();float yaw=player.getYRot(),pitch=player.getXRot();
            BlockPos a=new BlockPos((player.getBlockX()>>4)*16+8,Math.min(level.getMaxBuildHeight()-4,player.getBlockY()+12),(player.getBlockZ()>>4)*16+8), b=a.east(16);
            if(!level.getBlockState(a).isAir() || !level.getBlockState(b).isAir()){c.getSource().sendFailure(Component.literal("Aura QA fixture positions occupied: "+a+" / "+b));return 0;}
            checks=0;
            java.util.Map<BlockPos,net.minecraft.world.level.block.state.BlockState> arrayBackup=new java.util.LinkedHashMap<>();
            try {
                check(com.longju.xiantu.config.XiantuConfig.AURA_CAPACITY.get()==3600,"deployed default aura capacity is 3600");
                var legacyRow=new CompoundTag();legacyRow.putLong("chunk",new ChunkPos(a).toLong());
                legacyRow.putDouble("capacity",1200);legacyRow.putDouble("remaining",600);legacyRow.putDouble("concentration",100);
                var legacyRows=new net.minecraft.nbt.ListTag();legacyRows.add(legacyRow);var legacyTag=new CompoundTag();legacyTag.put("pools",legacyRows);
                var adjusted=ChunkAuraData.load(legacyTag).pool(level,new ChunkPos(a));
                check(adjusted.capacity==3600&&adjusted.remaining()==1800,"persisted half-full pool stays half-full after capacity change");
                legacyRow.putDouble("remaining",0);adjusted=ChunkAuraData.load(legacyTag).pool(level,new ChunkPos(a));
                check(adjusted.capacity==3600&&adjusted.remaining()==0,"capacity change cannot refill exhausted pool");
                check(!level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE)
                        .containsKey(new net.minecraft.resources.ResourceLocation("xiantu","qing_wen_elm_tree_forest")),"obsolete dense forest feature absent in live registry");
                data.setRealmStage(0,1);check(data.progressMax()==380,"live second Qi layer cap is 380");
                data.setRealmStage(0,8);check(data.progressMax()==726,"live ninth Qi layer cap is 726");
                level.setBlock(a,ModBlocks.MEDITATION_CUSHION.get().defaultBlockState(),3);
                level.setBlock(b,ModBlocks.MEDITATION_CUSHION.get().defaultBlockState(),3);
                data.setRealmStage(0,0);data.setCultivationUnlocked(false);data.setCharacterInitialized(true);
                data.setProgress(0);data.gongfa().clear();data.setInnerDemon(0);data.setPhysique(PhysiqueType.ORDINARY);
                data.resetSpiritRoots();data.addSpiritRoot(SpiritRootType.values()[0]);
                sit(player,a);var store=ChunkAuraData.get(level);var pool=store.pool(level,new ChunkPos(a));
                double initial=pool.remaining();ChunkAuraService.settleSecond(level);
                check(data.progress()==0 && pool.remaining()==initial,"mortal neither gains nor consumes aura");
                data.setCultivationUnlocked(true);ChunkAuraService.settleSecond(level);pool=store.pool(level,new ChunkPos(a));
                check(data.progress()>0,"ordinary cushion cultivates without array");
                check(Math.abs(pool.remaining()-(initial-com.longju.xiantu.config.XiantuConfig.AURA_DEMAND.get()))<1e-8,"natural debit once per second");
                check(store.pool(level,new ChunkPos(a.above(70)))==pool,"vertical column shares pool");
                check(store.pool(level,new ChunkPos(a.east(3)))==pool,"nearby cushion shares pool");
                store.consume(pool,Double.MAX_VALUE);double before=data.progress();ChunkAuraService.settleSecond(level);
                check(pool.remaining()==0 && data.progress()==before,"depleted chunk stops cultivation");
                var serialized=((net.minecraft.world.level.saveddata.SavedData)store).save(new CompoundTag());var restored=ChunkAuraData.load(serialized);
                check(restored.pool(level,new ChunkPos(a)).remaining()==0,"save/reload does not refill depleted chunk");
                var next=store.pool(level,new ChunkPos(b));check(next!=pool,"adjacent chunk owns independent pool");
                sit(player,b);ChunkAuraService.settleSecond(level);check(data.progress()>before,"moving to fresh chunk resumes cultivation");
                check(pool.remaining()>0,"idle loaded original chunk recovers");
                store.consume(pool,Double.MAX_VALUE);store.loaded(new ChunkPos(a),false);store.recover(level,java.util.Set.of());
                check(pool.remaining()==0,"unloaded pool does not recover");
                store.loaded(new ChunkPos(a),true);store.recover(level,java.util.Set.of());check(pool.remaining()>0,"loaded idle pool resumes recovery");
                data.setProgress(data.progressMax());double stock=next.remaining();ChunkAuraService.settleSecond(level);
                check(next.remaining()>=stock && data.progress()==data.progressMax(),"full stage no further aura debit");
                data.setProgress(0);player.stopRiding();before=data.progress();ChunkAuraService.settleSecond(level);check(data.progress()==before,"no seat no cultivation");
                // Real first-tier structure and its existing fuel ticker; no fake powered flag.
                BlockPos center=b.east(32).below();int radius=FormationLayout.outerRadius(1);
                for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)for(int y=0;y<=3;y++) {
                    BlockPos pos=center.offset(x,y,z);check(level.getBlockState(pos).isAir(),"isolated array fixture clear");
                    arrayBackup.put(pos,level.getBlockState(pos));
                }
                level.setBlock(center,ModBlocks.FORMATION_CORE.get().defaultBlockState(),3);
                level.setBlock(center.above(),ModBlocks.MEDITATION_CUSHION.get().defaultBlockState(),3);
                for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++) {
                    if(FormationLayout.isBaseRing(x,z))level.setBlock(center.offset(x,0,z),ModBlocks.ARRAY_BASE_1.get().defaultBlockState(),3);
                    else if(FormationLayout.isRuneRing(x,z))level.setBlock(center.offset(x,0,z),ModBlocks.ARRAY_RUNE_1.get().defaultBlockState(),3);
                }
                for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)
                    level.setBlock(center.relative(direction,radius).above(),ModBlocks.ARRAY_PILLAR_1.get().defaultBlockState(),3);
                var core=(FormationCoreBlockEntity)level.getBlockEntity(center);
                core.inventory().setStackInSlot(0,new net.minecraft.world.item.ItemStack(ModItems.SPIRIT_STONE.get()));
                FormationCoreBlockEntity.serverTick(level,center,level.getBlockState(center),core);
                check(core.structureValid() && core.runtimeTicks()>0,"real fueled array active");
                var dry=store.pool(level,new ChunkPos(center));store.consume(dry,Double.MAX_VALUE);
                sit(player,center.above());before=data.progress();ChunkAuraService.settleSecond(level);
                check(data.progress()>before,"fueled array cultivates in exhausted chunk");
                check(dry.remaining()<=com.longju.xiantu.config.XiantuConfig.AURA_RECOVERY.get()*1.4,"array does not refill chunk pool");
                var coreTag=((net.minecraft.world.level.block.entity.BlockEntity)core).saveWithoutMetadata();coreTag.putLong("runtime_budget",0);
                ((net.minecraft.world.level.block.entity.BlockEntity)core).load(coreTag);
                FormationCoreBlockEntity.serverTick(level,center,level.getBlockState(center),core);
                store.consume(dry,Double.MAX_VALUE);before=data.progress();ChunkAuraService.settleSecond(level);
                check(data.progress()==before && player.getVehicle()!=null,"unpowered array falls back to depleted natural seat");
                // Rest the chunk, then prove the same unpowered cushion uses local aura.
                player.stopRiding();store.recover(level,java.util.Set.of());sit(player,center.above());ChunkAuraService.settleSecond(level);
                check(data.progress()>before,"unpowered array falls back to recovered natural aura");
                var recipe=level.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("xiantu","hanxing_pickaxe"));
                check(recipe.isPresent() && recipe.get().getResultItem(level.registryAccess()).is(ModItems.HANXING_PICKAXE.get()),"survival pickaxe recipe registered");
                String result="Chunk aura QA PASS: "+checks;
                java.nio.file.Files.writeString(level.getServer().getServerDirectory().toPath().resolve("xiantu-aura-audit.json"),"{\"passed\":true,\"checks\":"+checks+"}");
                System.out.println(result);c.getSource().sendSuccess(()->Component.literal(result),false);return checks;
            } catch(Exception ex) {c.getSource().sendFailure(Component.literal("Chunk aura QA FAIL: "+ex));ex.printStackTrace();return 0;}
            finally {
                Entity seat=player.getVehicle();player.stopRiding();if(seat instanceof FormationSeatEntity)seat.discard();
                level.getEntitiesOfClass(FormationSeatEntity.class,new net.minecraft.world.phys.AABB(a,b).inflate(2)).forEach(e->((Entity)e).discard());
                level.setBlock(a,Blocks.AIR.defaultBlockState(),3);level.setBlock(b,Blocks.AIR.defaultBlockState(),3);
                arrayBackup.forEach((pos,state)->level.setBlock(pos,state,3));
                level.getDataStorage().set("xiantu_chunk_aura",ChunkAuraData.load(savedAura));
                data.load(saved);player.teleportTo(level,position.x,position.y,position.z,yaw,pitch);CultivationEvents.sync(player);
            }
        }));
    }
}
