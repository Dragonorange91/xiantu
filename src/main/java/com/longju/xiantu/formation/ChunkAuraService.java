package com.longju.xiantu.formation;

import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.config.XiantuConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = "xiantu")
public final class ChunkAuraService {
    private ChunkAuraService() {}
    private record Session(ServerPlayer player, PlayerCultivationData data, FormationCoreBlockEntity core, long chunk) {}
    @SubscribeEvent public static void load(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) ChunkAuraData.get(level).loaded(event.getChunk().getPos(), true);
    }
    @SubscribeEvent public static void unload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) ChunkAuraData.get(level).loaded(event.getChunk().getPos(), false);
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level && level.getGameTime() % 20 == 0)
            settleSecond(level);
    }
    /** One server transaction per second. No entity submission queue that can double-charge a seat. */
    public static void settleSecond(ServerLevel level) {
        var aura = ChunkAuraData.get(level);
        Map<Long, List<Session>> natural = new HashMap<>();
        List<Session> powered = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || !(player.getVehicle() instanceof FormationSeatEntity seat)
                    || !level.getBlockState(seat.cushionPos()).is(com.longju.xiantu.registry.ModBlocks.MEDITATION_CUSHION.get())) continue;
            var data = player.getCapability(CultivationCapability.DATA).orElse(null);
            if (!(data instanceof PlayerCultivationData mutable)) continue;
            if (com.longju.xiantu.alchemy.MedicineService.meditationPaused(player)) continue;
            if (!GongfaService.canMeditate(data)) continue;
            if (data.progress() >= data.progressMax()) continue;
            if (ThunderTribulationService.active(player) || InnerDemonService.active(player) || GongfaService.busy(player)) continue;
            FormationCoreBlockEntity core = level.getBlockEntity(seat.cushionPos().below()) instanceof FormationCoreBlockEntity c
                    && c.structureValid() && c.runtimeTicks() > 0 ? c : null;
            if (core != null && core.unsafe(player)) continue;
            long key = new ChunkPos(seat.cushionPos()).toLong();
            Session session = new Session(player, mutable, core, key);
            if (core == null) natural.computeIfAbsent(key, k -> new ArrayList<>()).add(session);
            else powered.add(session);
        }
        aura.recover(level, natural.keySet());
        for (var entry : natural.entrySet()) {
            var pool = aura.pool(level, new ChunkPos(entry.getKey()));
            var allocation = aura.consume(pool, entry.getValue().size() * XiantuConfig.AURA_DEMAND.get());
            for (Session session : entry.getValue()) {
                grant(session, XiantuConfig.NATURAL_MEDITATION_MULTIPLIER.get() * allocation.concentration() / 100 * allocation.supply(), allocation.supply());
                if (allocation.supply() <= 0) message(session.player, "此地灵气已尽");
            }
        }
        for (Session session : powered) {
            // The existing array ticker has already debited its running spirit stone.
            // It sustains cultivation even in an empty chunk; never recharge the natural pool.
            var pool = aura.pool(level, new ChunkPos(session.chunk));
            double density = ChunkAuraRules.allocate(pool.remaining(), pool.capacity, pool.concentration, 0).concentration();
            double tier = switch (session.core.tier()) { case 2 -> 2.0; case 3 -> 2.5; default -> 1.5; };
            grant(session, tier * Math.max(1, density / 100), 1);
        }
    }
    private static void grant(Session session, double environment, double supply) {
        if (environment <= 0) return;
        var data = session.data;
        double gain = FormationRules.baseRate(data.majorRealm()) * CultivationEfficiency.personal(data)
                * FormationRules.bonusMultiplier(session.core == null ? 1 : session.core.tier(),
                    session.core == null ? 0 : session.core.bonusRank()) * environment
                * com.longju.xiantu.alchemy.MedicineService.meditationMultiplier(session.player, session.core != null);
        data.accumulateMeditation(gain);
        if (GongfaService.active(data) != null) GongfaService.addExperience(session.player, supply);
        CultivationEvents.sync(session.player);
    }
    private static void message(ServerPlayer player, String text) {
        if (player.serverLevel().getGameTime() % 40 == 0) player.displayClientMessage(Component.literal(text), true);
    }
}
