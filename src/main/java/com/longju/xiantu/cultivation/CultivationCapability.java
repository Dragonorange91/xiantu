package com.longju.xiantu.cultivation;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CultivationCapability {
    public static final Capability<CultivationData> DATA = CapabilityManager.get(new CapabilityToken<>() {});
    private static PlayerCultivationData CLIENT_DATA = new PlayerCultivationData();
    private CultivationCapability() {}
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(CultivationData.class);
    }
    public static void clientUpdate(com.longju.xiantu.network.CultivationSyncPacket packet) {
        PlayerCultivationData updated = new PlayerCultivationData();
        updated.setCultivationUnlocked(packet.cultivationUnlocked());
        updated.setPhysique(PhysiqueType.byId(packet.physique()));
        updated.gongfa().load(packet.gongfa() == null ? new CompoundTag() : packet.gongfa());
        updated.setRealmStage(packet.realm(), packet.stage());
        updated.medicine().setSummary(packet.totalDoses(), packet.toxicity());
        updated.medicine().state().putBoolean("client_true_sight",packet.trueSight());
        updated.setPower(packet.powerMilli() / 1000.0D); updated.setPowerMax(packet.powerMaxMilli() / 1000.0D);
        updated.setProgress(packet.progressMilli() / 1000.0D);
        for (SpiritRootType root : SpiritRootType.values()) if ((packet.rootMask() & (1 << root.ordinal())) != 0) updated.addSpiritRoot(root);
        CLIENT_DATA = updated;
    }
    public static void resetClient() { CLIENT_DATA = new PlayerCultivationData(); }
    public static CultivationData clientData() { return CLIENT_DATA; }
    public static final class Provider implements ICapabilityProvider, net.minecraftforge.common.util.INBTSerializable<CompoundTag> {
        private final PlayerCultivationData data = new PlayerCultivationData();
        private LazyOptional<CultivationData> optional = LazyOptional.of(() -> data);
        public void invalidate() { optional.invalidate(); }
        @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap != DATA) return LazyOptional.empty();
            // Entity.reviveCaps() exposes this provider again during PlayerEvent.Clone.
            if (!optional.isPresent()) optional = LazyOptional.of(() -> data);
            return optional.cast();
        }
        @Override public CompoundTag serializeNBT() { CompoundTag tag = new CompoundTag(); data.save(tag); return tag; }
        @Override public void deserializeNBT(CompoundTag tag) { data.load(tag); }
    }
}
