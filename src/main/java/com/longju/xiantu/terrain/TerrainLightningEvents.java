package com.longju.xiantu.terrain;

import com.longju.xiantu.config.XiantuConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class TerrainLightningEvents {
    private static final Map<ServerLevel, Long> NEXT_STRIKE_TICK = new WeakHashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ServerLevel level = event.getServer().getLevel(Level.OVERWORLD);
        if (level == null) return;

        long now = level.getGameTime();
        long next = NEXT_STRIKE_TICK.computeIfAbsent(level, ignored -> now + intervalTicks(level));
        if (now < next) return;
        NEXT_STRIKE_TICK.put(level, now + intervalTicks(level));

        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && player.level().getBiome(player.blockPosition()).is(TerrainBiomes.THUNDER_WASTES)) {
                candidates.add(player);
            }
        }
        if (candidates.isEmpty()) return;

        ServerPlayer observer = candidates.get(level.random.nextInt(candidates.size()));
        BlockPos strike = findStrikePosition(level, observer);
        if (strike == null) return;

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.setVisualOnly(true);
        bolt.setPos(strike.getX() + 0.5D, strike.getY(), strike.getZ() + 0.5D);
        level.addFreshEntity(bolt);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(strike).inflate(2.5D, 3.0D, 2.5D))) {
            target.hurt(level.damageSources().lightningBolt(), 5.0F);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        NEXT_STRIKE_TICK.clear();
    }

    private static BlockPos findStrikePosition(ServerLevel level, ServerPlayer observer) {
        BlockPos origin = observer.blockPosition();
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int distance = 10 + level.random.nextInt(23);
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * distance);
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, origin.getY(), z));
            if (!level.hasChunkAt(surface) || !level.canSeeSky(surface)
                    || !level.getBiome(surface).is(TerrainBiomes.THUNDER_WASTES)
                    || !level.getBlockState(surface.below()).getFluidState().isEmpty()) {
                continue;
            }
            boolean nearAnyPlayer = level.players().stream()
                    .anyMatch(player -> player.distanceToSqr(surface.getX() + 0.5D, surface.getY(), surface.getZ() + 0.5D) < 36.0D);
            if (!nearAnyPlayer) return surface;
        }
        return null;
    }

    private static long intervalTicks(ServerLevel level) {
        int minimum = XiantuConfig.THUNDER_WASTES_STRIKE_MIN_SECONDS.get();
        int maximum = Math.max(minimum, XiantuConfig.THUNDER_WASTES_STRIKE_MAX_SECONDS.get());
        return (minimum + level.random.nextInt(maximum - minimum + 1)) * 20L;
    }

    private TerrainLightningEvents() {}
}
