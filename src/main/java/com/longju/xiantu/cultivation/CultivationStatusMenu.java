package com.longju.xiantu.cultivation;

import com.longju.xiantu.registry.ModMenus;
import com.longju.xiantu.mining.SpiritVeinProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class CultivationStatusMenu extends AbstractContainerMenu {
    private static final int REALM = 0;
    private static final int STAGE = 1;
    private static final int ROOT_MASK = 2;
    private static final int COMPREHENSION = 3;
    private static final int INNER_DEMON_TENTHS = 4;
    private static final int BREAKTHROUGH_CHANCE_BP = 5;
    private static final int COOLDOWN_TICKS = 6;
    private static final int VEIN_LEVEL = 7;
    private static final int VEIN_EXPERIENCE = 8;
    private static final int MEDITATION_PAUSED = 9;
    private final ContainerData data = new SimpleContainerData(10);
    private final ServerPlayer owner;

    public CultivationStatusMenu(int id, Inventory inventory, FriendlyByteBuf ignored) {
        this(id, inventory);
    }

    public CultivationStatusMenu(int id, Inventory inventory) {
        super(ModMenus.CULTIVATION_STATUS_MENU.get(), id);
        this.owner = inventory.player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        addDataSlots(data);
        if (owner != null) refresh(owner);
    }

    private void refresh(ServerPlayer player) {
        player.getCapability(CultivationCapability.DATA).ifPresent(cultivation -> {
            data.set(REALM, cultivation.majorRealm());
            data.set(STAGE, cultivation.stage());
            int roots = 0;
            for (SpiritRootType root : SpiritRootType.values()) {
                if (cultivation.hasSpiritRoot(root)) roots |= 1 << root.ordinal();
            }
            data.set(ROOT_MASK, roots);
            data.set(COMPREHENSION, cultivation.comprehension());
            data.set(INNER_DEMON_TENTHS, (int) Math.round(cultivation.innerDemon() * 10.0D));
            data.set(BREAKTHROUGH_CHANCE_BP, (int) Math.round(BreakthroughService.successChance(player) * 10_000.0D));
            data.set(COOLDOWN_TICKS, BreakthroughService.cooldownTicks(player));
        });
        data.set(VEIN_LEVEL, SpiritVeinProgress.level(player));
        data.set(MEDITATION_PAUSED, com.longju.xiantu.alchemy.MedicineService.meditationPaused(player) ? 1 : 0);
        data.set(VEIN_EXPERIENCE, SpiritVeinProgress.displayExperienceForLevel(
                SpiritVeinProgress.experience(player), SpiritVeinProgress.level(player)));
    }

    public int majorRealm() { return data.get(REALM); }
    public int stage() { return data.get(STAGE); }
    public double progress() { return CultivationCapability.clientData().progress(); }
    public double progressMax() { return CultivationCapability.clientData().progressMax(); }
    public double power() { return CultivationCapability.clientData().power(); }
    public double powerMax() { return CultivationCapability.clientData().powerMax(); }
    public int rootMask() { return data.get(ROOT_MASK); }
    public int comprehension() { return data.get(COMPREHENSION); }
    public double innerDemon() { return data.get(INNER_DEMON_TENTHS) / 10.0D; }
    public double breakthroughChance() { return data.get(BREAKTHROUGH_CHANCE_BP) / 10_000.0D; }
    public int cooldownTicks() { return data.get(COOLDOWN_TICKS); }
    public int veinLevel() { return data.get(VEIN_LEVEL); }
    public boolean meditationPaused() { return data.get(MEDITATION_PAUSED) != 0; }
    public int veinExperience() { return data.get(VEIN_EXPERIENCE); }
    public int veinNextThreshold() { return SpiritVeinProgress.nextThresholdForLevel(veinLevel()); }
    public boolean requiresBreakthroughConfirmation() {
        return BreakthroughRules.isMajorBreakthrough(majorRealm(), stage());
    }
    public boolean breakthroughAvailable() {
        return requiresBreakthroughConfirmation() && cooldownTicks() <= 0 && progress() >= progressMax()
                && com.longju.xiantu.gongfa.GongfaService.breakthroughDenial(owner == null ? CultivationCapability.clientData() : com.longju.xiantu.gongfa.GongfaService.data(owner)).isEmpty()
                && !RealmRegistry.isFinalStage(majorRealm(), stage());
    }

    @Override
    public void broadcastChanges() {
        if (owner != null && owner.containerMenu == this) refresh(owner);
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (player instanceof ServerPlayer p && player.containerMenu == this && button == 1) { com.longju.xiantu.alchemy.PillChoiceMenu.open(p,0); return true; }
        if (player instanceof ServerPlayer p && player.containerMenu == this && button == 2) { return com.longju.xiantu.alchemy.MedicineService.toggleDetox(p); }
        if (button != 0 || !requiresBreakthroughConfirmation() || player.level().isClientSide || player.containerMenu != this
                || !(player instanceof ServerPlayer serverPlayer)) return false;
        boolean succeeded = BreakthroughService.tryBreakthrough(serverPlayer);
        refresh(serverPlayer);
        broadcastChanges();
        return succeeded;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return true; }
}
