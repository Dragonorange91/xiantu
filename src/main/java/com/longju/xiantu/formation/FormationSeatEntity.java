package com.longju.xiantu.formation;

import com.longju.xiantu.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class FormationSeatEntity extends Entity {
    private BlockPos cushionPos = BlockPos.ZERO;
    private int lastDamageTime = -1;

    public FormationSeatEntity(EntityType<? extends FormationSeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void setCushionPos(BlockPos pos) { cushionPos = pos.immutable(); }
    public BlockPos cushionPos() { return cushionPos; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (getPassengers().isEmpty() || !level().getBlockState(cushionPos).is(ModBlocks.MEDITATION_CUSHION.get())) {
            discard();
            return;
        }
        Entity passenger = getFirstPassenger();
        if (passenger instanceof Player player) {
            if (lastDamageTime >= 0 && lastDamageTime != player.getLastHurtByMobTimestamp() || player.hurtTime > 0) {
                player.stopRiding(); discard(); return;
            }
            lastDamageTime = player.getLastHurtByMobTimestamp();
            BlockPos center = cushionPos.below();
            FormationCoreBlockEntity core = level().getBlockEntity(center) instanceof FormationCoreBlockEntity c ? c : null;
            if (core != null && core.structureValid() && core.runtimeTicks() > 0 && core.unsafe(player)) {
                player.stopRiding();
                discard();
                return;
            }
            if (player.isShiftKeyDown()) {
                player.stopRiding();
                discard();
                return;
            }
            // Progress is settled once per second by ChunkAuraService, fairly per chunk.
            if (!com.longju.xiantu.alchemy.MedicineService.meditationPaused(player)) player.getCapability(com.longju.xiantu.cultivation.CultivationCapability.DATA).ifPresent(data ->
                    data.setPower(Math.min(data.powerMax(), data.power() + recoveryPerTick(player))));
        }
    }

    public static double recoveryPerTick(Player player){
        return player.getCapability(com.longju.xiantu.cultivation.CultivationCapability.DATA).map(data ->
            com.longju.xiantu.cultivation.PhysiqueService.meditationRecovery(data)
                *com.longju.xiantu.alchemy.MedicineService.recoveryMultiplier(player)
                *(1+com.longju.xiantu.refining.EquipmentEffects.recoveryBonus(player))).orElse(0.0);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        cushionPos = BlockPos.of(tag.getLong("cushion_pos"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("cushion_pos", cushionPos.asLong());
    }

    @Override
    public boolean shouldBeSaved() { return false; }

    @Override
    protected boolean canAddPassenger(Entity passenger) { return getPassengers().isEmpty(); }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean canBeCollidedWith() { return false; }
}
