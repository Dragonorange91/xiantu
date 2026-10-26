package com.longju.xiantu.item;

import com.longju.xiantu.cultivation.CultivationCapability;
import com.longju.xiantu.cultivation.CultivationEvents;
import com.longju.xiantu.cultivation.PlayerCultivationData;
import com.longju.xiantu.cultivation.RealmRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** Records one owner's cultivation state; restoration is explicit and consumes the recorded stone. */
public final class ReincarnationStoneItem extends Item {
    private final boolean talisman;
    private static final String INSTANCE = "xiantu_reincarnation_instance";
    private static final String SNAPSHOT = "xiantu_reincarnation_snapshot";
    private static final String RECORDER = "owner";
    private static final String RECORDER_NAME = "owner_name";
    private static final String INSTANCE_ID = "instance";
    private static final String REALM = "realm";
    private static final String PROGRESS = "progress";
    private static final String POWER = "power";
    private static final String ROOT_MASK = "roots";
    private static final String PHYSIQUE = "physique";
    private static final String UNLOCKED = "unlocked";
    private static final String GONGFA = "gongfa";
    private static final String DEATH_ID = "death_id";
    private static final String RECORDS = "reincarnation_stone_records";
    private static final String PENDING = "pending_reincarnation_stones";

    public ReincarnationStoneItem(Properties properties) { this(properties,false); }
    public ReincarnationStoneItem(Properties properties,boolean talisman) { super(properties.stacksTo(1));this.talisman=talisman; }

    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        if(!talisman)com.longju.xiantu.client.GltfItemRenderer.install(consumer);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.fail(stack);
        if(talisman&&!com.longju.xiantu.gongfa.GongfaService.data(serverPlayer).cultivationUnlocked())return InteractionResultHolder.fail(stack);
        UUID instance = ensureInstance(stack);
        var ledger=ReincarnationLedger.get(serverPlayer.serverLevel());
        if(ledger.spent(instance)){serverPlayer.displayClientMessage(Component.literal("这件轮回道具的记录已兑现，不能重复使用。"),true);return InteractionResultHolder.fail(stack);}
        serverPlayer.getCapability(CultivationCapability.DATA).ifPresent(raw -> {
            if (!(raw instanceof PlayerCultivationData data)) return;
            CompoundTag itemSnapshot = stack.getTagElement(SNAPSHOT);
            CompoundTag records = data.reincarnationStoneRecords();
            String recordKey = instance.toString();
            if (itemSnapshot == null || !itemSnapshot.hasUUID(RECORDER)) {
                if (records.contains(recordKey)) {
                    serverPlayer.displayClientMessage(Component.literal("此实例已登记，物品记录数据无效，无法重新绑定。"), true); return;
                }
                CompoundTag snapshot = capture(data, serverPlayer.getUUID());
                snapshot.putString(RECORDER_NAME, serverPlayer.getGameProfile().getName());
                snapshot.putUUID(INSTANCE_ID, instance);
                snapshot.putBoolean("talisman",talisman);
                stack.getOrCreateTag().put(SNAPSHOT, snapshot.copy());
                records.put(recordKey, snapshot);
                data.setReincarnationStoneRecords(records);
                serverPlayer.displayClientMessage(Component.literal("轮回石已记录当前修为。"), true);
                return;
            }
            if (!itemSnapshot.getUUID(RECORDER).equals(serverPlayer.getUUID())) {
                serverPlayer.displayClientMessage(Component.literal("此轮回石已绑定其他记录者。"), true); return;
            }
            if (!itemSnapshot.hasUUID(INSTANCE_ID) || !itemSnapshot.getUUID(INSTANCE_ID).equals(instance)) {
                serverPlayer.displayClientMessage(Component.literal("轮回石实例校验失败，无法使用。"), true); return;
            }
            ListTag pending = data.pendingReincarnationStones();
            int pendingIndex = findPending(pending, instance, serverPlayer.getUUID());
            CompoundTag saved = records.getCompound(recordKey);
            CompoundTag restoreSource = ReincarnationStoneRules.selectRestoreSource(itemSnapshot, saved, instance,
                    serverPlayer.getUUID(), pendingIndex >= 0);
            if (pendingIndex >= 0) {
                if (restoreSource == null) {
                    serverPlayer.displayClientMessage(Component.literal("角色记录与这枚石头无法匹配，无法恢复。请确认拿回的是你记录的原石。"), true); return;
                }
                CompoundTag transaction = pending.getCompound(pendingIndex);
                UUID deathId = transaction.getUUID(DEATH_ID);
                if(!ledger.claim(instance,deathId,serverPlayer.getUUID())){serverPlayer.displayClientMessage(Component.literal("本次死亡记录已恢复，不能重复兑现。"),true);return;}
                restore(data, restoreSource, deathId,talisman);
                removeDeath(pending, deathId);
                data.setPendingReincarnationStones(pending);
                records.remove(recordKey);
                data.setReincarnationStoneRecords(records);
                ItemStack remaining = consumeOne(stack);
                serverPlayer.setItemInHand(hand, remaining);
                serverPlayer.getInventory().setChanged();
                CultivationEvents.sync(serverPlayer);
                com.longju.xiantu.gongfa.GongfaService.refresh(serverPlayer);
                serverPlayer.displayClientMessage(Component.literal(talisman?"转生符生效：已恢复记录，练气降一层，其余降三小阶段；此符已消耗。":"轮回石生效：已恢复记录的修为，境界下降一小阶段，轮回石消耗。"), true);
                return;
            }
            if (restoreSource == null) {
                serverPlayer.displayClientMessage(Component.literal("当前角色没有这枚轮回石对应的有效死亡记录，无法恢复修为。请确认这是你本人登记的原石，且死亡时它仍然存在。"), true); return;
            }
            if (!serverPlayer.isShiftKeyDown()) {
                serverPlayer.displayClientMessage(Component.literal("轮回石已记录；只有死亡后才能恢复，蹲下右键可更新至更高修为。"), true); return;
            }
            int currentRealm = data.realm();
            int savedRealm = itemSnapshot.getInt(REALM);
            double currentProgress = data.progress();
            double savedProgress = itemSnapshot.getDouble(PROGRESS);
            if (!ReincarnationStoneRules.isHigherRecord(data.cultivationUnlocked(), currentRealm, currentProgress,
                    itemSnapshot.getBoolean(UNLOCKED), savedRealm, savedProgress)) {
                serverPlayer.displayClientMessage(Component.literal("只有修为高于当前记录后，蹲下右键才能更新轮回石。"), true); return;
            }
            CompoundTag updated = capture(data, serverPlayer.getUUID());
            updated.putString(RECORDER_NAME, serverPlayer.getGameProfile().getName());
            updated.putUUID(INSTANCE_ID, instance);
            updated.putBoolean("talisman",talisman);
            stack.getOrCreateTag().put(SNAPSHOT, updated.copy());
            records.put(recordKey, updated);
            data.setReincarnationStoneRecords(records);
            serverPlayer.displayClientMessage(Component.literal("轮回石记录已更新至当前更高修为。"), true);
        });
        return InteractionResultHolder.sidedSuccess(serverPlayer.getItemInHand(hand), false);
    }

    @Override public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> tooltip,
                                           net.minecraft.world.item.TooltipFlag flag) {
        if(talisman)tooltip.add(Component.literal("恢复记录后降三小阶段；练气只降一层。仅消耗此符。"));
        CompoundTag tag = stack.getTagElement(SNAPSHOT);
        if (tag == null || !tag.hasUUID(RECORDER)) tooltip.add(Component.literal("右键记录修为；死亡后取回此石并右键恢复").withStyle(ChatFormatting.GRAY));
        else {
            String recorderName = tag.getString(RECORDER_NAME);
            if (recorderName.isBlank() && level != null) {
                var owner = level.getPlayerByUUID(tag.getUUID(RECORDER));
                if (owner != null) recorderName = owner.getGameProfile().getName();
            }
            if (recorderName.isBlank()) recorderName = "已绑定修士";
            tooltip.add(Component.literal("记录者：" + recorderName).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("境界：" + ReincarnationStoneRules.recordedRealmText(tag)).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("蹲下右键可更新至更高境界").withStyle(ChatFormatting.GRAY));
        }
    }

    public static void prepareDeath(PlayerCultivationData original, PlayerCultivationData restored, UUID owner) {
        CompoundTag records = original.reincarnationStoneRecords();
        if (records.isEmpty()) return;
        UUID deathId = UUID.randomUUID();
        ListTag pending = restored.pendingReincarnationStones();
        for (String key : records.getAllKeys()) {
            CompoundTag snapshot = records.getCompound(key);
            if (!snapshot.hasUUID(INSTANCE_ID) || !ReincarnationStoneRules.matchesRecord(snapshot, snapshot.getUUID(INSTANCE_ID), owner)) continue;
            if(findPending(pending,snapshot.getUUID(INSTANCE_ID),owner)>=0)continue;
            CompoundTag entry = new CompoundTag();
            entry.putUUID(INSTANCE_ID, snapshot.getUUID(INSTANCE_ID));
            entry.putUUID(RECORDER, owner);
            entry.putUUID(DEATH_ID, deathId);
            pending.add(entry);
        }
        restored.setPendingReincarnationStones(pending);
    }

    private static CompoundTag capture(PlayerCultivationData data, UUID owner) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(RECORDER, owner);
        tag.putInt(REALM, data.realm());
        tag.putDouble(PROGRESS, data.progress());
        tag.putDouble(POWER, data.power());
        int roots = 0;
        for (var type : com.longju.xiantu.cultivation.SpiritRootType.values()) if (data.hasSpiritRoot(type)) roots |= 1 << type.ordinal();
        tag.putInt(ROOT_MASK, roots);
        tag.putString(PHYSIQUE, data.physique().id);
        tag.putBoolean(UNLOCKED, data.cultivationUnlocked());
        tag.put(GONGFA, data.gongfa().save());
        return tag;
    }

    static ItemStack consumeOne(ItemStack stack) {
        stack.setCount(ReincarnationStoneRules.remainingAfterConsume(stack.getCount()));
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    private static int findPending(ListTag pending, UUID instance, UUID owner) {
        for (int i = pending.size() - 1; i >= 0; i--) {
            CompoundTag tag = pending.getCompound(i);
            if (tag.hasUUID(INSTANCE_ID) && tag.hasUUID(RECORDER)
                    && tag.getUUID(INSTANCE_ID).equals(instance) && tag.getUUID(RECORDER).equals(owner)) return i;
        }
        return -1;
    }

    private static void removeDeath(ListTag pending, UUID deathId) {
        for (int i = pending.size() - 1; i >= 0; i--) {
            CompoundTag entry = pending.getCompound(i);
            if (entry.hasUUID(DEATH_ID) && entry.getUUID(DEATH_ID).equals(deathId)) pending.remove(i);
        }
    }

    private static void restore(PlayerCultivationData data, CompoundTag snapshot, UUID deathId) {
        restore(data,snapshot,deathId,false);
    }
    private static void restore(PlayerCultivationData data, CompoundTag snapshot, UUID deathId,boolean talisman) {
        int index = Math.max(0, Math.min(RealmRegistry.REALMS.size() - 1, snapshot.getInt(REALM)));
        var realm = RealmRegistry.get(talisman?com.longju.xiantu.talisman.TalismanRules.rebirthIndex(index,RealmRegistry.get(index).majorIndex()):Math.max(0,index-1));
        data.setCultivationUnlocked(snapshot.getBoolean(UNLOCKED));
        data.setRealmStage(realm.majorIndex(), realm.stage());
        data.setProgress(0);
        data.setPowerMax(snapshot.getBoolean(UNLOCKED) ? realm.powerCap() : 0);
        data.setPower(Math.min(snapshot.getDouble(POWER), data.powerMax()));
        data.setPhysique(com.longju.xiantu.cultivation.PhysiqueType.byId(snapshot.getString(PHYSIQUE)));
        data.resetSpiritRoots();
        int roots = snapshot.getInt(ROOT_MASK);
        for (var type : com.longju.xiantu.cultivation.SpiritRootType.values()) if ((roots & (1 << type.ordinal())) != 0) data.addSpiritRoot(type);
        data.gongfa().load(snapshot.getCompound(GONGFA));
    }

    private static UUID ensureInstance(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.hasUUID(INSTANCE)) tag.putUUID(INSTANCE, UUID.randomUUID());
        return tag.getUUID(INSTANCE);
    }
}
