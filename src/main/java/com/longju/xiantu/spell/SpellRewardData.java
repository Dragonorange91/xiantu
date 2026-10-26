package com.longju.xiantu.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-player daily cap belongs to the world, so death and dimensions cannot reset it. */
public final class SpellRewardData extends SavedData {
    private final CompoundTag days=new CompoundTag();
    public static SpellRewardData get(ServerPlayer p){return p.server.overworld().getDataStorage().computeIfAbsent(SpellRewardData::load,SpellRewardData::new,"xiantu_spell_rewards_v1");}
    public boolean available(ServerPlayer p){String id=p.getUUID().toString();return !days.contains(id)||days.getLong(id)==-1||days.getLong(id)!=day(p);}
    public boolean claim(ServerPlayer p){if(!available(p))return false;days.putLong(p.getUUID().toString(),day(p));setDirty();return true;}
    // Monotonic played ticks, not time-of-day; /time set cannot reopen the same day's rewards.
    private static long day(ServerPlayer p){return p.server.overworld().getGameTime()/24000;}
    private static SpellRewardData load(CompoundTag n){var d=new SpellRewardData();d.days.merge(n.getCompound("Days"));return d;}
    @Override public CompoundTag save(CompoundTag n){n.put("Days",days.copy());return n;}
}
