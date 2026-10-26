package com.longju.xiantu.gongfa;

import com.longju.xiantu.cultivation.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.longju.xiantu.formation.FormationSeatEntity;
import com.longju.xiantu.registry.ModBlocks;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GongfaService {
    private record Approval(String method, InteractionHand hand, ItemStack book, String oldMethod,
                            int realm, int stage, String dimension) {}
    private static final class Pending {
        final Approval approval;
        final UUID token = UUID.randomUUID(), seat;
        final Vec3 position;
        int ticks, lastTick = Integer.MIN_VALUE;
        Pending(Approval approval, FormationSeatEntity seat, Vec3 position) {
            this.approval = approval; this.seat = seat.getUUID(); this.position = position;
        }
    }
    private static final Map<UUID, Approval> APPROVALS = new HashMap<>();
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private GongfaService() {}
    public static CultivationData data(ServerPlayer player) {
        return player.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new);
    }
    public static GongfaDefinition active(CultivationData data) {
        if (!data.cultivationUnlocked()) return null;
        GongfaDefinition method = GongfaRegistry.get(data.gongfa().active());
        return method != null && method.usableAt(data.majorRealm()) ? method : null;
    }
    public static double cultivationMultiplier(CultivationData data) {
        GongfaDefinition method = active(data);
        return method == null ? 1 : method.cultivationMultiplier(data.gongfa().activeLevel());
    }
    public static double attackBonus(CultivationData data) {
        GongfaDefinition method = active(data);
        return method == null ? 0 : method.attackBonus(data.gongfa().activeLevel());
    }
    public static double healthBonus(CultivationData data) {
        GongfaDefinition method = active(data);
        return method == null ? 0 : method.healthBonus(data.gongfa().activeLevel());
    }
    public static double healthBonusPoints(CultivationData data) {
        double ratio = healthBonus(data);
        return Math.round(RealmRegistry.baseHealthExact(data.majorRealm(), data.stage()) * ratio);
    }
    public static double armorBonus(CultivationData data) {
        GongfaDefinition method = active(data);
        return method == null ? 0 : method.armorBonus(data.gongfa().activeLevel());
    }
    public static double buffBonus(CultivationData data) {
        GongfaDefinition method = active(data);
        return method == null ? 0 : method.buffBonus(data.gongfa().activeLevel());
    }
    public static boolean canMeditate(CultivationData data) {
        if (!data.cultivationUnlocked()) return false;
        return data.gongfa().active().isEmpty() ? data.majorRealm() == 0 : active(data) != null;
    }
    public static String breakthroughDenial(CultivationData data) {
        if (!data.cultivationUnlocked()) return "请先学习玄元养气决，开启修仙之路";
        int next = data.majorRealm() + (data.stage() == RealmRegistry.STAGE_COUNTS[data.majorRealm()]-1 ? 1 : 0);
        if (data.gongfa().active().isEmpty()) return next == 0 ? "" : "养气诀止步练气九层，请先研习五行基础功法再突破筑基";
        GongfaDefinition method = active(data);
        return method != null && next <= method.maxRealm() ? "" : "当前主功法修炼上限不足或效果已暂停";
    }
    public static void refresh(ServerPlayer player) {
        CultivationData data = data(player);
        CultivationService.applyRealmAttributes(player, data);
        addMethodModifier(player, Attributes.MAX_HEALTH, UUID.fromString("cc03473a-885b-49ef-9d51-20d70e991801"),
                "xiantu_gongfa_health", healthBonusPoints(data), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION);
        addMethodModifier(player, Attributes.ARMOR, UUID.fromString("cc03473a-885b-49ef-9d51-20d70e991802"),
                "xiantu_gongfa_armor", armorBonus(data), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION);
        GongfaDefinition method = active(data);
        GongfaBuff buff = method == null ? null : method.buff();
        double buffAmount = buffBonus(data);
        addMethodModifier(player, Attributes.MOVEMENT_SPEED, UUID.fromString("cc03473a-885b-49ef-9d51-20d70e991803"),
                "xiantu_gongfa_movement", buff == GongfaBuff.AGILITY ? buffAmount : 0,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE);
        addMethodModifier(player, Attributes.ATTACK_SPEED, UUID.fromString("cc03473a-885b-49ef-9d51-20d70e991804"),
                "xiantu_gongfa_attack_speed", buff == GongfaBuff.SWIFTNESS ? buffAmount : 0,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE);
        addMethodModifier(player, Attributes.KNOCKBACK_RESISTANCE, UUID.fromString("cc03473a-885b-49ef-9d51-20d70e991805"),
                "xiantu_gongfa_fortitude", buff == GongfaBuff.FORTITUDE ? buffAmount : 0,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION);
        // Remove obsolete prototype attributes even in existing disposable QA worlds.
        for (var entry : new Object[][]{
                {Attributes.MAX_HEALTH, "8b7bca34-f7b9-4a68-9c6c-7f2016b9b101"},
                {Attributes.ATTACK_DAMAGE, "8b7bca34-f7b9-4a68-9c6c-7f2016b9b102"},
                {Attributes.ARMOR, "8b7bca34-f7b9-4a68-9c6c-7f2016b9b103"}}) {
            var attribute = player.getAttribute((net.minecraft.world.entity.ai.attributes.Attribute)entry[0]);
            if (attribute != null) attribute.removeModifier(UUID.fromString((String)entry[1]));
        }
        var tag = player.getPersistentData();
        if (!data.cultivationUnlocked()) {
            data.setPowerMax(0); data.setPower(0);
            tag.putDouble("xiantu_gongfa_power_max_bonus",0);
            tag.putDouble("xiantu_physique_power_max_bonus",0);
            tag.putDouble("xiantu_equipment_power_bonus",0);
            tag.putDouble("xiantu_pill_power_bonus",0);
            tag.putDouble("xiantu_gongfa_power_speed_bonus",0);
            tag.putString("xiantu_gongfa_effect_state",effectState(data));
            return;
        }
        double oldCapBonus = tag.getDouble("xiantu_gongfa_power_max_bonus");
        double oldSpeedBonus = tag.getDouble("xiantu_gongfa_power_speed_bonus");
        double baseCap = Math.max(1, data.powerMax() - oldCapBonus - tag.getDouble("xiantu_physique_power_max_bonus") - tag.getDouble("xiantu_pill_power_bonus") - tag.getDouble("xiantu_equipment_power_bonus"));
        double baseSpeed = Math.max(RealmRegistry.basePowerRecovery(data.majorRealm(), data.stage()), data.powerSpeed() - oldSpeedBonus);
        method = active(data);
        int layer = data.gongfa().activeLevel();
        double methodCapBonus=method==null?0:baseCap*method.powerBonus(layer);
        double nextCap = Math.floor(baseCap + methodCapBonus + baseCap*data.physique().powerBonus + 1e-8);
        double speedBonus = method == null ? 0 : RealmRegistry.basePowerRecovery(data.majorRealm(), data.stage()) * method.recoveryBonus(layer);
        double pillBonus=baseCap*com.longju.xiantu.alchemy.MedicineService.effect(player,"capacity")/100;
        double equipmentBonus=baseCap*com.longju.xiantu.refining.EquipmentEffects.powerBonus(player);
        data.setPowerMax(nextCap+pillBonus+equipmentBonus);tag.putDouble("xiantu_equipment_power_bonus",equipmentBonus);
        tag.putDouble("xiantu_pill_power_bonus",pillBonus); data.setPowerSpeed(baseSpeed + speedBonus);
        data.setPower(Math.min(data.power(), data.powerMax()));
        tag.putDouble("xiantu_gongfa_power_max_bonus", methodCapBonus);
        tag.putDouble("xiantu_physique_power_max_bonus", nextCap-baseCap-methodCapBonus);
        tag.putDouble("xiantu_gongfa_power_speed_bonus", speedBonus);
        tag.remove("xiantu_active_gongfa"); tag.remove("xiantu_gongfa_signature"); tag.remove("xiantu_applied_gongfa");
        tag.putString("xiantu_gongfa_effect_state",effectState(data));
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }
    private static void addMethodModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                          UUID id, String name, double amount,
                                          net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation) {
        var instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (amount != 0) instance.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id, name, amount, operation));
    }
    private static String effectState(CultivationData data) {
        return data.cultivationUnlocked()+":"+data.majorRealm()+":"+data.stage()+":"+data.gongfa().active()+":"+data.gongfa().activeLevel()+":"+data.physique().id;
    }
    private static boolean learnEntrance(ServerPlayer player, String id) {
        GongfaDefinition method = GongfaRegistry.get(id);
        CultivationData data = data(player);
        if (method != null && method.tierIndex() == 0) {
            if (data.cultivationUnlocked()) return false;
            data.setCultivationUnlocked(true); data.setRealmStage(0,0); data.setProgress(0);
            data.setPowerMax(100); data.setPower(0); data.setPowerSpeed(1);
            player.getPersistentData().putDouble("xiantu_pill_power_bonus",0);
        refresh(player); CultivationEvents.sync(player); return true;
        }
        return false;
    }
    /** Experience is submitted only alongside an actual uncapped progression transaction. */
    public static void meditationExperience(ServerPlayer player) {
        CultivationData data = data(player);
        if (active(data) == null) return;
        addExperience(player, 1.0 / 20);
    }
    public static void addExperience(ServerPlayer player, double points) {
        CultivationData data = data(player);
        if (active(data) == null || data.gongfa().activeProgress() == null) return;
        if (data.gongfa().activeProgress().add(PhysiqueService.effectiveMethodExperience(data,points))) { refresh(player); CultivationEvents.sync(player); }
    }
    public static int previewRealm(CultivationData data) {
        return data.gongfa().active().isEmpty() ? data.majorRealm() : GongfaRules.replacementRealm(data.majorRealm());
    }
    public static int previewStage(CultivationData data) {
        return data.gongfa().active().isEmpty() ? data.stage() : GongfaRules.replacementStage(data.majorRealm(), data.stage());
    }
    public static boolean busy(ServerPlayer player) {
        var tag = player.getPersistentData();
        return tag.getInt("xiantu_thunder_ticks") > 0 || tag.getBoolean("xiantu_breakthrough_active")
                || tag.getBoolean("xiantu_inner_demon_battle_active");
    }
    public static void markCombat(ServerPlayer player) {
        player.getPersistentData().putLong("xiantu_method_combat_until",player.getServer().overworld().getGameTime()
                +net.minecraft.world.damagesource.CombatTracker.RESET_COMBAT_STATUS_TIME);
    }
    public static boolean inCombat(ServerPlayer player) {
        return player.getPersistentData().getLong("xiantu_method_combat_until") > player.getServer().overworld().getGameTime();
    }
    public static String denial(ServerPlayer player, String id) {
        CultivationData data = data(player); GongfaDefinition method = GongfaRegistry.get(id);
        if (method == null) return "功法不存在";
        if (method.tierIndex() == 0) {
            if (data.cultivationUnlocked()) return "已经开启修仙资格，无需重复学习入门决";
        } else {
            if (!data.cultivationUnlocked()) return "请先学习玄元养气决";
            if (id.equals(data.gongfa().active())) return "正在修炼此功法，不能重复学习或清空层数";
            if (!method.learnableAt(previewRealm(data), previewStage(data))) return "散功后的境界不满足学习条件：" + method.learningRequirement();
        }
        if (busy(player)) return "突破、天劫或心魔战中不能更换";
        if (inCombat(player)) return "战斗中不能更换";
        if (!player.isAlive()) return "当前无法学习";
        return "";
    }
    public static boolean heldBook(ServerPlayer player, InteractionHand hand, ItemStack snapshot, String id) {
        ItemStack held = player.getItemInHand(hand);
        return !held.isEmpty() && held.getItem() instanceof GongfaItem book && book.definitionId().equals(id)
                && ItemStack.matches(held, snapshot);
    }
    public static boolean confirm(ServerPlayer player, String id, InteractionHand hand, ItemStack snapshot) {
        if (PENDING.containsKey(player.getUUID()) || !heldBook(player,hand,snapshot,id) || !denial(player,id).isEmpty()) return false;
        CultivationData data = data(player);
        APPROVALS.put(player.getUUID(),new Approval(id,hand,snapshot.copy(),data.gongfa().active(),data.majorRealm(),data.stage(),
                player.level().dimension().location().toString()));
        return true;
    }
    private static boolean validApproval(ServerPlayer player, Approval approval) {
        CultivationData data = data(player);
        return heldBook(player,approval.hand,approval.book,approval.method) && approval.oldMethod.equals(data.gongfa().active())
                && approval.realm == data.majorRealm() && approval.stage == data.stage()
                && approval.dimension.equals(player.level().dimension().location().toString()) && denial(player,approval.method).isEmpty();
    }
    public static boolean approved(ServerPlayer player, String id, InteractionHand hand) {
        Approval approval = APPROVALS.get(player.getUUID());
        return approval != null && approval.method.equals(id) && approval.hand == hand && validApproval(player,approval);
    }
    public static boolean seated(ServerPlayer player) {
        return player.getVehicle() instanceof FormationSeatEntity seat && seat.isAlive()
                && player.level().hasChunkAt(seat.cushionPos())
                && player.level().getBlockState(seat.cushionPos()).is(ModBlocks.MEDITATION_CUSHION.get()) && !player.isShiftKeyDown();
    }
    public static boolean beginUse(ServerPlayer player, String id, InteractionHand hand) {
        if (!approved(player,id,hand) || !seated(player) || PENDING.containsKey(player.getUUID())) return false;
        Approval approval = APPROVALS.remove(player.getUUID());
        PENDING.put(player.getUUID(),new Pending(approval,(FormationSeatEntity)player.getVehicle(),player.position()));
        studyVisual(player, id, 0);
        return true;
    }
    private static boolean validUse(ServerPlayer player, Pending pending) {
        return validApproval(player,pending.approval) && seated(player)
                && player.getVehicle().getUUID().equals(pending.seat)
                && player.position().distanceToSqr(pending.position) <= .01
                && player.isUsingItem() && player.getUsedItemHand() == pending.approval.hand;
    }
    public static boolean useTick(ServerPlayer player) {
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null) return false;
        if (!validUse(player,pending) || pending.lastTick != Integer.MIN_VALUE && player.tickCount-pending.lastTick > 1) {
            cancel(player,"学习中断，请重新确认"); return false;
        }
        if (pending.lastTick != player.tickCount) { pending.ticks++; pending.lastTick = player.tickCount; }
        if (pending.ticks % 20 == 0) {
            studyVisual(player, pending.approval.method, pending.ticks);
            player.displayClientMessage(Component.literal("研习功法："+Math.min(100,pending.ticks*100/GongfaRules.PREPARATION_TICKS)+"%"),true);
        }
        return true;
    }
    public static boolean finishUse(ServerPlayer player) {
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || pending.ticks < GongfaRules.PREPARATION_TICKS || !validUse(player,pending)) {
            cancel(player,""); return false;
        }
        PENDING.remove(player.getUUID()); APPROVALS.remove(player.getUUID());
        studyVisual(player, "", 0);
        boolean success = GongfaRegistry.get(pending.approval.method).tierIndex() == 0
                ? learnEntrance(player,pending.approval.method) : commit(player,pending.approval.method,pending.token);
        if (success) player.sendSystemMessage(Component.literal("已修习："+GongfaRegistry.get(pending.approval.method).name()+"（功法书保留）"));
        return success;
    }
    public static int remainingTicks(ServerPlayer player) {
        Pending pending = PENDING.get(player.getUUID());
        return pending == null ? 0 : Math.max(1, GongfaRules.PREPARATION_TICKS-pending.ticks);
    }
    public static void cancel(ServerPlayer player, String reason) {
        boolean pending = PENDING.remove(player.getUUID()) != null;
        APPROVALS.remove(player.getUUID());
        if (pending) {
            studyVisual(player, "", 0);
            player.stopUsingItem();
            if (!reason.isEmpty()) player.sendSystemMessage(Component.literal("功法学习已取消："+reason));
        }
    }
    private static void studyVisual(ServerPlayer player, String method, int ticks) {
        com.longju.xiantu.network.XiantuNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new com.longju.xiantu.network.GongfaStudyPacket(player.getUUID(), method, ticks));
    }
    public static void tick(ServerPlayer player) {
        // Other modules may change realm directly; update eligibility without reapplying attributes every tick.
        if (player.tickCount % 20 == 0 && !effectState(data(player)).equals(player.getPersistentData().getString("xiantu_gongfa_effect_state"))) {
            refresh(player); CultivationEvents.sync(player);
        }
        Approval approval = APPROVALS.get(player.getUUID());
        if (approval != null && !validApproval(player,approval)) APPROVALS.remove(player.getUUID());
        Pending pending = PENDING.get(player.getUUID());
        if (pending != null && !validUse(player,pending)) cancel(player,"离座、松开右键或使用资格发生变化");
    }
    /** Atomic completion, reachable in gameplay only after confirmed continuous seated book use. */
    private static boolean commit(ServerPlayer player, String id, UUID token) {
        if (!denial(player, id).isEmpty()) return false;
        CultivationData data = data(player);
        boolean replacement = !data.gongfa().active().isEmpty();
        int realm = previewRealm(data), stage = previewStage(data);
        long oldBase = RealmRegistry.get(data.majorRealm(), data.stage()).powerCap();
        double base = Math.max(1, data.powerMax()-player.getPersistentData().getDouble("xiantu_gongfa_power_max_bonus")
                -player.getPersistentData().getDouble("xiantu_physique_power_max_bonus")-player.getPersistentData().getDouble("xiantu_pill_power_bonus")
                -player.getPersistentData().getDouble("xiantu_equipment_power_bonus"));
        double extraRecovery = Math.max(0, data.powerSpeed()-RealmRegistry.basePowerRecovery(data.majorRealm(), data.stage())
                -player.getPersistentData().getDouble("xiantu_gongfa_power_speed_bonus"));
        if (!data.gongfa().knows(id)) data.gongfa().learn(id);
        if (!data.gongfa().commit(id, token)) return false;
        if (replacement) {
            data.setRealmStage(realm, stage); data.setProgress(0);
            base += RealmRegistry.get(realm, stage).powerCap()-oldBase;
        }
        data.setPowerMax(Math.max(1,base));
        data.setPowerSpeed(RealmRegistry.basePowerRecovery(realm,stage)+extraRecovery);
        player.getPersistentData().putDouble("xiantu_gongfa_power_max_bonus",0);
        player.getPersistentData().putDouble("xiantu_physique_power_max_bonus",0);
        player.getPersistentData().putDouble("xiantu_gongfa_power_speed_bonus",0);
        player.getPersistentData().putDouble("xiantu_pill_power_bonus",0);
        player.getPersistentData().putDouble("xiantu_equipment_power_bonus",0);
        refresh(player); CultivationEvents.sync(player); return true;
    }
}
