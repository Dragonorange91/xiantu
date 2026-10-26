package com.longju.xiantu.formation;

import com.longju.xiantu.registry.ModBlockEntities;
import com.longju.xiantu.item.SpiritStoneItem;
import com.longju.xiantu.cultivation.CultivationCapability;
import com.longju.xiantu.cultivation.PlayerCultivationData;
import com.longju.xiantu.config.XiantuConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

public final class FormationCoreBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof SpiritStoneItem stone && stone.chargeMilli(stack) > 0
                    && (slot == 0 || SpiritStoneItem.isFullyCharged(stack));
        }
    };
    // Budget unit = 1/1200 milli-power; integer tick debit has no rounding loss.
    private long runtimeBudget;
    private int bonusTicks, bonusRank, pulseTicks;
    private boolean structureValid, visualsAligned;
    public FormationCoreBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.FORMATION_CORE.get(), pos, state); }
    public ItemStackHandler inventory() { return inventory; }
    public int powerPerMinute() { return XiantuConfig.FORMATION_POWER_PER_MINUTE.get(tier()-1).get(); }
    public long runtimeTicks() { return FormationRules.ticksForBudget(runtimeBudget, powerPerMinute()); }
    public long runtimeSeconds() { return (runtimeTicks() + 19) / 20; }
    public long runtimeBudget() { return runtimeBudget; }
    public int bonusTicks() { return bonusTicks; }
    public int bonusRank() { return bonusTicks > 0 ? bonusRank : 0; }
    public boolean structureValid() { return structureValid; }
    public void invalidateStructure() { structureValid = false; visualsAligned = false; pulseTicks = 0; }
    public int tier() { return getBlockState().getBlock() instanceof FormationCoreBlock core ? core.tier() : 1; }
    public boolean canMeditate() { return level != null && runtimeBudget > 0 && FormationStructure.isComplete(level, worldPosition, tier()); }
    public boolean unsafe(Player player) {
        return player.getCapability(CultivationCapability.DATA).map(data -> FormationRules.unsafe(data.majorRealm(), bonusRank())).orElse(true);
    }
    private static void warn(Player player) {
        player.displayClientMessage(Component.literal("当前加成超过安全阶位，修炼已停止；强行吸收将立即死亡。"), false);
    }
    private void stopUnsafeOccupants() {
        if (level == null) return;
        for (FormationSeatEntity seat : level.getEntitiesOfClass(FormationSeatEntity.class,
                new net.minecraft.world.phys.AABB(worldPosition.above()).inflate(.6))) {
            if (seat.getFirstPassenger() instanceof Player player && unsafe(player)) {
                player.stopRiding(); seat.discard(); warn(player);
            }
        }
    }
    /** Visual only: the server's existing lit state authorizes a loaded client's emitter. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, FormationCoreBlockEntity core) {
        if (!level.isClientSide || !state.getValue(FormationCoreBlock.ACTIVE)) return;
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
            () -> () -> com.longju.xiantu.client.SkillPresentationClient.formation(pos,core.tier()));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FormationCoreBlockEntity core) {
        if (core.pulseTicks++ % 20 == 0) {
            core.structureValid = FormationStructure.isComplete(level, pos, core.tier());
            if (core.structureValid && !core.visualsAligned) {
                FormationStructure.alignVisualParts(level, pos, core.tier());
                core.visualsAligned = true;
            } else if (!core.structureValid) core.visualsAligned = false;
        }
        if (!core.structureValid) { core.setBlockActive(false); return; }
        if (level.getEntitiesOfClass(FormationSeatEntity.class,new net.minecraft.world.phys.AABB(pos.above()).inflate(.6)).stream()
            .anyMatch(seat -> seat.getFirstPassenger() instanceof net.minecraft.server.level.ServerPlayer p && com.longju.xiantu.alchemy.MedicineService.meditationPaused(p))) {
            core.setBlockActive(false); return;
        }
        if (core.runtimeBudget <= 0) {
            ItemStack queued = core.inventory.getStackInSlot(0);
            if (core.inventory.isItemValid(0, queued)) {
                core.runtimeBudget = ((SpiritStoneItem) queued.getItem()).chargeMilli(queued) * 1200L;
                core.inventory.extractItem(0, 1, false);
            }
        }
        if (core.runtimeBudget <= 0) { core.setBlockActive(false); return; }
        core.startBonusStoneIfNeeded();
        core.stopUnsafeOccupants();
        core.runtimeBudget = Math.max(0, core.runtimeBudget - core.powerPerMinute() * 1000L);
        if (core.bonusTicks > 0 && --core.bonusTicks == 0) core.bonusRank = 0;
        core.setBlockActive(core.runtimeBudget > 0);
        core.setChanged();
    }
    public void beginMeditation(Player player) {
        if (level == null || level.isClientSide || !canMeditate()) return;
        structureValid = true;
        if (unsafe(player)) { player.stopRiding(); warn(player); }
    }
    private void startBonusStoneIfNeeded() {
        ItemStack queued = inventory.getStackInSlot(1);
        if (bonusTicks > 0 || !inventory.isItemValid(1, queued)) return;
        bonusRank = ((SpiritStoneItem) queued.getItem()).tier().rank();
        bonusTicks = FormationRules.bonusTicks(bonusRank);
        inventory.extractItem(1, 1, false);
    }
    public boolean switchBonus(Player player) {
        if (!canMeditate() || !inventory.isItemValid(1, inventory.getStackInSlot(1))) return false;
        bonusTicks = 0; bonusRank = 0; startBonusStoneIfNeeded(); stopUnsafeOccupants(); setChanged(); return true;
    }
    // Invoked only by the explicit GUI confirmation, never by automatic bonus activation.
    public boolean forceMeditation(Player player, int expectedRank) {
        if (!player.isAlive() || !canMeditate() || !unsafe(player) || expectedRank != bonusRank()) return false;
        BlockPos cushion = worldPosition.above();
        if (!level.getEntitiesOfClass(FormationSeatEntity.class, new net.minecraft.world.phys.AABB(cushion).inflate(.6)).isEmpty()) return false;
        if (player.distanceToSqr(cushion.getX()+.5, cushion.getY()+.5, cushion.getZ()+.5) > 16) return false;
        player.kill(); return true;
    }
    public void dropStoredItems() {
        if (level == null || level.isClientSide) return;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX()+.5,
                    worldPosition.getY()+.5, worldPosition.getZ()+.5, stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
        runtimeBudget = 0; bonusTicks = 0; bonusRank = 0; setChanged();
    }
    private void setBlockActive(boolean active) {
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.hasProperty(FormationCoreBlock.ACTIVE) && state.getValue(FormationCoreBlock.ACTIVE) != active)
            level.setBlock(worldPosition, state.setValue(FormationCoreBlock.ACTIVE, active), 3);
    }
    @Override public Component getDisplayName() { return Component.translatable("container.xiantu.formation_core"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new FormationMenu(id, inv, this); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.put("inventory", inventory.serializeNBT());
        tag.putLong("runtime_budget", runtimeBudget); tag.putInt("bonus_ticks", bonusTicks); tag.putInt("bonus_rank", bonusRank);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) inventory.deserializeNBT(tag.getCompound("inventory"));
        runtimeBudget = Math.max(0, Math.min(1_000_000_000_000_000L, tag.getLong("runtime_budget")));
        bonusRank = Math.max(0, Math.min(8, tag.getInt("bonus_rank")));
        bonusTicks = bonusRank == 0 ? 0 : Math.max(0, Math.min(FormationRules.bonusTicks(bonusRank), tag.getInt("bonus_ticks")));
        structureValid = false; pulseTicks = 0;
    }
}
