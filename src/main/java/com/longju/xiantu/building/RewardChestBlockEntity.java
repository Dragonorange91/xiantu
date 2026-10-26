package com.longju.xiantu.building;

import com.longju.xiantu.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla 27 slots, persistence, lid events, hopper capability and loot behavior. */
public final class RewardChestBlockEntity extends ChestBlockEntity {
    private String ruinRoot="",ruinBlueprint="",ruinChest="",ruinGrade="";
    private BlockPos ruinOrigin=BlockPos.ZERO;
    public RewardChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUIN_REWARD_CHEST.get(), pos, state);
    }
    @Override protected Component getDefaultName() {
        return Component.translatable("container.xiantu.ruin_reward_chest");
    }
    @Override public void load(net.minecraft.nbt.CompoundTag tag){
        super.load(tag);ruinRoot=tag.getString("RuinRoot");ruinBlueprint=tag.getString("RuinBlueprint");ruinChest=tag.getString("RuinChest");ruinGrade=tag.getString("RuinGrade");ruinOrigin=BlockPos.of(tag.getLong("RuinOrigin"));
    }
    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag){
        super.saveAdditional(tag);tag.putString("RuinRoot",ruinRoot);tag.putString("RuinBlueprint",ruinBlueprint);tag.putString("RuinChest",ruinChest);tag.putString("RuinGrade",ruinGrade);tag.putLong("RuinOrigin",ruinOrigin.asLong());
    }
    private com.longju.xiantu.worldgen.RuinAdventureData.Instance instance(){
        return !ruinRoot.isEmpty()&&level instanceof net.minecraft.server.level.ServerLevel l?com.longju.xiantu.worldgen.RuinAdventureService.observe(l,ruinRoot,ruinBlueprint,ruinOrigin):null;
    }
    private boolean validPosition(com.longju.xiantu.worldgen.RuinAdventureData.Instance i){
        for(var v:i.spec.getAsJsonArray("markers")){var m=v.getAsJsonObject();if(m.has("id")&&m.get("id").getAsString().equals(ruinChest))return i.absolute(m.getAsJsonArray("position")).equals(getBlockPos());}return false;
    }
    @Override public void onLoad(){super.onLoad();var i=instance();if(i!=null&&!validPosition(i)){lootTable=null;setItems(net.minecraft.core.NonNullList.withSize(27,net.minecraft.world.item.ItemStack.EMPTY));setChanged();}}
    public boolean ruinLocked(){var i=instance();return i!=null&&(!validPosition(i)||ruinGrade.equals("core")&&(i.boss()?(!i.defeated()||!i.state.getBoolean("RewardUnlocked")):!i.guardiansDone()));}
    @Override public void unpackLootTable(net.minecraft.world.entity.player.Player player){
        if(ruinRoot.isEmpty()){super.unpackLootTable(player);return;}
        var i=instance();if(i==null||ruinLocked())return;
        if(lootTable==null)return;
        // Realm restrictions belong to the boss encounter. Once unlocked, both
        // player opening and vanilla break/hopper access must roll the same loot.
        var ledger=com.longju.xiantu.worldgen.RuinAdventureData.get((net.minecraft.server.level.ServerLevel)level);
        if(!ledger.claim(i,ruinChest)){lootTable=null;setItems(net.minecraft.core.NonNullList.withSize(27,net.minecraft.world.item.ItemStack.EMPTY));setChanged();return;}
        super.unpackLootTable(player);
        var talisman=com.longju.xiantu.talisman.TalismanSources.chest(i,ruinChest,ruinGrade);
        if(!talisman.isEmpty()){
            var empty=new java.util.ArrayList<Integer>();for(int slot=0;slot<getContainerSize();slot++)if(getItem(slot).isEmpty())empty.add(slot);
            if(!empty.isEmpty())setItem(empty.get(level.random.nextInt(empty.size())),talisman);
            else net.minecraft.world.Containers.dropItemStack(level,getBlockPos().getX()+.5,getBlockPos().getY()+1,getBlockPos().getZ()+.5,talisman);
        }
        if(player instanceof net.minecraft.server.level.ServerPlayer p){
            var scroll=com.longju.xiantu.spell.SpellSources.chest(p,i,ruinChest,ruinGrade);
            if(!scroll.isEmpty()){
                boolean placed=false;for(int slot=0;slot<getContainerSize();slot++)if(getItem(slot).isEmpty()){setItem(slot,scroll);placed=true;break;}
                if(!placed)net.minecraft.world.Containers.dropItemStack(level,getBlockPos().getX()+.5,getBlockPos().getY()+1,getBlockPos().getZ()+.5,scroll);
            }
        }setChanged();
    }
}
