package com.longju.xiantu.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.UUID;

/** World-owned spent identities survive player cloning, inventory copies and restart. */
public final class ReincarnationLedger extends SavedData {
    private CompoundTag spent=new CompoundTag(),deaths=new CompoundTag();
    public static ReincarnationLedger get(ServerLevel l){return l.getServer().overworld().getDataStorage().computeIfAbsent(ReincarnationLedger::load,ReincarnationLedger::new,"xiantu_reincarnation_ledger");}
    public boolean spent(UUID instance){return spent.contains(instance.toString());}
    public boolean claim(UUID instance,UUID death,UUID owner){
        String key=owner+"/"+death;
        if(spent(instance)||deaths.contains(key))return false;
        spent.putUUID(instance.toString(),owner);deaths.putUUID(key,instance);setDirty();return true;
    }
    private static ReincarnationLedger load(CompoundTag n){var d=new ReincarnationLedger();d.spent=n.getCompound("Spent").copy();d.deaths=n.getCompound("Deaths").copy();return d;}
    @Override public CompoundTag save(CompoundTag n){n.put("Spent",spent.copy());n.put("Deaths",deaths.copy());return n;}
}
