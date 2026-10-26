package com.longju.xiantu.formation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** One SavedData per dimension. No real-time timestamps or forced chunk tickets. */
public final class ChunkAuraData extends SavedData {
    private static final TagKey<Biome> RICH = TagKey.create(Registries.BIOME, new ResourceLocation("xiantu", "aura_rich"));
    private static final TagKey<Biome> SPARSE = TagKey.create(Registries.BIOME, new ResourceLocation("xiantu", "aura_sparse"));
    private final Map<Long, Pool> pools = new HashMap<>();
    private final Set<Long> loaded = new HashSet<>();
    public static final class Pool {
        public double capacity;
        public final double concentration;
        private double remaining;
        private Pool(double capacity, double concentration, double remaining) {
            this.capacity = capacity; this.concentration = concentration;
            this.remaining = Math.max(0, Math.min(capacity, remaining));
        }
        public double remaining() { return remaining; }
    }
    public static ChunkAuraData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ChunkAuraData::load, ChunkAuraData::new, "xiantu_chunk_aura");
    }
    public Pool pool(ServerLevel level, ChunkPos pos) {
        long key = pos.toLong();
        Pool pool = pools.computeIfAbsent(key, ignored -> {
            // Fixed column sample prevents moving vertically or placing blocks from rerolling capacity.
            var biome = level.getBiome(new BlockPos(pos.getMiddleBlockX(), level.getSeaLevel(), pos.getMiddleBlockZ()));
            double concentration = biome.is(RICH) ? 140 : biome.is(SPARSE) ? 60 : 100;
            double capacity = com.longju.xiantu.config.XiantuConfig.AURA_CAPACITY.get() * concentration / 100;
            setDirty(); return new Pool(capacity, concentration, capacity);
        });
        updateCapacity(pool);
        if (level.getChunkSource().getChunkNow(pos.x, pos.z) != null) loaded.add(key);
        return pool;
    }
    private void updateCapacity(Pool pool) {
        double desired = com.longju.xiantu.config.XiantuConfig.AURA_CAPACITY.get() * pool.concentration / 100;
        if (Double.compare(desired, pool.capacity) != 0) {
            pool.remaining = ChunkAuraRules.rescale(pool.remaining, pool.capacity, desired);
            pool.capacity = desired;
            setDirty();
        }
    }
    public void loaded(ChunkPos pos, boolean value) {
        if (value && pools.containsKey(pos.toLong())) loaded.add(pos.toLong());
        else if (!value) loaded.remove(pos.toLong());
    }
    public ChunkAuraRules.Allocation consume(Pool pool, double demand) {
        var result = ChunkAuraRules.allocate(pool.remaining, pool.capacity, pool.concentration, demand);
        if (result.remaining() != pool.remaining) { pool.remaining = result.remaining(); setDirty(); }
        return result;
    }
    public void recover(ServerLevel level, Set<Long> occupied) {
        for (long key : List.copyOf(loaded)) {
            ChunkPos pos = new ChunkPos(key);
            if (level.getChunkSource().getChunkNow(pos.x, pos.z) == null) { loaded.remove(key); continue; }
            Pool pool = pools.get(key);
            if (pool == null) { loaded.remove(key); continue; }
            updateCapacity(pool);
            double next = ChunkAuraRules.recover(pool.remaining, pool.capacity,
                    com.longju.xiantu.config.XiantuConfig.AURA_RECOVERY.get() * pool.concentration / 100, occupied.contains(key));
            if (next != pool.remaining) { pool.remaining = next; setDirty(); }
            if (next >= pool.capacity && !occupied.contains(key)) { pools.remove(key); loaded.remove(key); setDirty(); }
        }
    }
    public static ChunkAuraData load(CompoundTag tag) {
        var data = new ChunkAuraData();
        for (Tag value : tag.getList("pools", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag)value;
            double cap = row.getDouble("capacity"), stock = row.getDouble("remaining"), density = row.getDouble("concentration");
            if (Double.isFinite(cap) && cap > 0 && Double.isFinite(stock) && Double.isFinite(density) && density > 0)
                data.pools.put(row.getLong("chunk"), new Pool(cap, density, stock));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        pools.forEach((key, pool) -> {
            if (pool.remaining >= pool.capacity) return;
            CompoundTag row = new CompoundTag(); row.putLong("chunk", key);
            row.putDouble("capacity", pool.capacity); row.putDouble("remaining", pool.remaining);
            row.putDouble("concentration", pool.concentration); list.add(row);
        });
        tag.put("pools", list); return tag;
    }
}
