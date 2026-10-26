package com.longju.xiantu.formation;

import com.longju.xiantu.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public final class FormationMenu extends AbstractContainerMenu {
    private final FormationCoreBlockEntity core;
    private final ContainerData data;

    public FormationMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, getCore(playerInventory, buffer));
    }

    public FormationMenu(int id, Inventory playerInventory, FormationCoreBlockEntity core) {
        super(ModMenus.FORMATION_MENU.get(), id);
        this.core = core;
        int[] client = new int[11];
        data = new ContainerData() {
            @Override public int get(int index) {
                if (playerInventory.player.level().isClientSide) return client[index];
                if (index < 4) return FormationRules.limb(core.runtimeSeconds(), index);
                return switch (index) {
                    case 4 -> (core.bonusTicks() + 19) / 20;
                    case 5 -> core.bonusRank();
                    case 6 -> core.structureValid() ? 1 : 0;
                    case 7 -> core.tier();
                    case 8 -> playerInventory.player.getCapability(com.longju.xiantu.cultivation.CultivationCapability.DATA)
                            .map(d -> com.longju.xiantu.item.SpiritStoneAbsorptionRules.stoneRankForRealm(d.majorRealm())).orElse(0);
                    case 9, 10 -> FormationRules.limb(core.powerPerMinute(), index - 9);
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) { client[index] = value; }
            @Override public int getCount() { return 11; }
        };
        addSlot(new SlotItemHandler(core.inventory(), 0, 40, 52));
        addSlot(new SlotItemHandler(core.inventory(), 1, 180, 52));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col + row * 9 + 9, 47 + col * 18, 155 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 47 + col * 18, 213));
        addDataSlots(data);
    }

    private static FormationCoreBlockEntity getCore(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        if (!(be instanceof FormationCoreBlockEntity core)) throw new IllegalStateException("聚灵阵阵眼不存在");
        return core;
    }

    public long runtimeSeconds() { return FormationRules.join(data.get(0),data.get(1),data.get(2),data.get(3)); }
    public int bonusSeconds() { return data.get(4); }
    public int bonusRank() { return data.get(5); }
    public boolean structureValid() { return data.get(6) != 0; }
    public int tier() { return Math.max(1, data.get(7)); }
    public boolean unsafe() { return bonusRank() > data.get(8); }
    public int powerPerMinute() { return (int) FormationRules.join(data.get(9), data.get(10)); }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide || !stillValid(player)) return false;
        if (id == 1) return core.switchBonus(player);
        return id >= 101 && id <= 108 && core.forceMeditation(player, id - 100);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        if (index < 2) {
            if (!moveItemStackTo(source, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(source, 0, 2, false)) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player player) {
        int range = FormationLayout.outerRadius(core.tier()) + 5;
        return core.getLevel() == player.level() && player.level().getBlockEntity(core.getBlockPos()) == core
                && player.distanceToSqr(core.getBlockPos().getX()+.5, core.getBlockPos().getY()+.5, core.getBlockPos().getZ()+.5) <= range * range;
    }
}
