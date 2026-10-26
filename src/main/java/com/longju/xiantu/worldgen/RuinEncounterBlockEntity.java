package com.longju.xiantu.worldgen;
import com.longju.xiantu.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class RuinEncounterBlockEntity extends BlockEntity {
    String instance="",root="",blueprint="",wave="";BlockPos origin=BlockPos.ZERO;List<String> pool=List.of();int count=3,minRealm=0,maxRealm=0;int[] roomBounds=new int[0];private long nextAttempt;
    boolean inRoom(double x,double y,double z){return roomBounds.length!=6||(x>=roomBounds[0]&&x<roomBounds[3]+1&&y>=roomBounds[1]&&y<roomBounds[4]+1&&z>=roomBounds[2]&&z<roomBounds[5]+1);}
    public RuinEncounterBlockEntity(BlockPos p,BlockState s){super(ModBlockEntities.RUIN_ENCOUNTER.get(),p,s);}
    public void configure(String id,List<String> species,int amount,int realm){instance=id;pool=List.copyOf(species);count=amount;maxRealm=realm;setChanged();}
    public void bind(String root,String blueprint,BlockPos origin,String wave){this.root=root;this.blueprint=blueprint;this.origin=origin.immutable();this.wave=wave;setChanged();}
    @Override public void load(CompoundTag n){super.load(n);instance=n.getString("Instance");root=n.getString("RuinRoot");blueprint=n.getString("RuinBlueprint");origin=BlockPos.of(n.getLong("RuinOrigin"));wave=n.getString("RuinWave");count=Math.max(3,Math.min(12,n.getInt("Count")));maxRealm=Math.max(0,Math.min(8,n.getInt("MaxRealm")));minRealm=Math.max(0,Math.min(maxRealm,n.getInt("MinRealm")));roomBounds=n.getIntArray("RoomBounds");var p=new ArrayList<String>();for(Tag t:n.getList("Pool",Tag.TAG_STRING))p.add(t.getAsString());pool=List.copyOf(p);}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.putString("Instance",instance);n.putString("RuinRoot",root);n.putString("RuinBlueprint",blueprint);n.putLong("RuinOrigin",origin.asLong());n.putString("RuinWave",wave);n.putInt("Count",count);n.putInt("MinRealm",minRealm);n.putIntArray("RoomBounds",roomBounds);n.putInt("MaxRealm",maxRealm);ListTag p=new ListTag();pool.forEach(s->p.add(StringTag.valueOf(s)));n.put("Pool",p);}
    public static void tick(Level l,BlockPos p,BlockState s,RuinEncounterBlockEntity be){
        if(!(l instanceof ServerLevel level)||be.instance.isEmpty()||be.pool.isEmpty()||(level.getGameTime()+p.asLong())%20!=0)return;
        if(!be.root.isEmpty()){
            var record=RuinAdventureService.observe(level,be.root,be.blueprint,be.origin);
            boolean valid=false;
            for(var v:record.spec.getAsJsonArray("encounters")){var cfg=v.getAsJsonObject();if(cfg.get("id").getAsString().equals(be.wave)&&RuinSpecs.offset(be.origin,cfg.getAsJsonArray("position")).equals(p))valid=true;}
            if(!valid){RuinProtection.internal(()->level.removeBlock(p,false));return;}
            if(!RuinAdventureService.mayTrigger(level,be))return;
        }
        var ledger=RuinInstanceData.get(level);
        if(ledger.triggered(be.instance)){RuinProtection.internal(()->level.removeBlock(p,false));return;}
        if(level.getDifficulty()==Difficulty.PEACEFUL)return;
        boolean nearby=level.players().stream().anyMatch(v->v.isAlive()&&!v.isCreative()&&!v.isSpectator()&&be.inRoom(v.getX(),v.getY(),v.getZ())&&v.distanceToSqr(p.getX()+.5,p.getY()+.5,p.getZ()+.5)<=1024);
        if(nearby&&level.getGameTime()>=be.nextAttempt){be.nextAttempt=level.getGameTime()+100;RuinEncounterService.spawnWave(level,p,be);}
    }
}
