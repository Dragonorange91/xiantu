package com.longju.xiantu.cultivation;

import com.google.common.collect.Multimap;
import com.longju.xiantu.entity.InnerDemonEntity;
import com.longju.xiantu.network.InnerDemonStatePacket;
import com.longju.xiantu.network.XiantuNetwork;
import com.longju.xiantu.registry.ModEntities;
import com.longju.xiantu.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = com.longju.xiantu.XiantuMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InnerDemonService {
    private static final String TX_KEY = "xiantu_inner_demon_transaction";
    private static final String ENTITY_KEY = "xiantu_inner_demon_entity";
    private static final String EXPIRES_KEY = "xiantu_inner_demon_expires";
    private enum FailureReason { TIMEOUT, LOW_HEALTH, INTERRUPTED }
    private InnerDemonService() {}

    public static boolean active(ServerPlayer player) { return player.getPersistentData().hasUUID(TX_KEY); }

    public static boolean begin(ServerPlayer player, PlayerCultivationData data) {
        if (active(player)) return false;
        long now = player.serverLevel().getGameTime();
        UUID transaction = UUID.randomUUID();
        InnerDemonEntity phantom = ModEntities.INNER_DEMON.get().create(player.serverLevel());
        if (phantom == null) return false;
        phantom.moveTo(player.getX() + 2.5D, player.getY(), player.getZ(), player.getYRot(), 0);
        phantom.bind(player.getUUID(), transaction, now + InnerDemonRules.DURATION_TICKS);
        phantom.setCustomName(Component.literal("心魔幻影"));
        phantom.setCustomNameVisible(true);
        mirrorEquipment(player, phantom);
        stripMirroredEquipmentAttributes(phantom);
        snapshotAttributes(player, phantom);
        phantom.setHealth(Math.max(1.0F, Math.min(phantom.getMaxHealth(), player.getHealth() * .8F)));
        if (!player.serverLevel().addFreshEntity(phantom)) return false;

        player.getPersistentData().putUUID(TX_KEY, transaction);
        player.getPersistentData().putUUID(ENTITY_KEY, phantom.getUUID());
        player.getPersistentData().putLong(EXPIRES_KEY, now + InnerDemonRules.DURATION_TICKS);
        syncLock(player, true);
        player.closeContainer();
        player.sendSystemMessage(Component.literal("心魔幻影现身，三分钟内击败它以继续突破"));
        CultivationEvents.sync(player);
        return true;
    }

    private static void mirrorEquipment(ServerPlayer player, InnerDemonEntity phantom) {
        phantom.setItemSlot(EquipmentSlot.MAINHAND, player.getMainHandItem().copy());
        phantom.setItemSlot(EquipmentSlot.OFFHAND, player.getOffhandItem().copy());
        phantom.setItemSlot(EquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.HEAD).copy());
        phantom.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST).copy());
        phantom.setItemSlot(EquipmentSlot.LEGS, player.getItemBySlot(EquipmentSlot.LEGS).copy());
        phantom.setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.FEET).copy());
        for (EquipmentSlot slot : EquipmentSlot.values()) phantom.setDropChance(slot, 0.0F);
    }

    private static void snapshotAttributes(ServerPlayer player, InnerDemonEntity phantom) {
        copyEffective(player, phantom, Attributes.MAX_HEALTH);
        copyEffective(player, phantom, Attributes.ATTACK_DAMAGE);
        copyEffective(player, phantom, Attributes.ARMOR);
        copyEffective(player, phantom, Attributes.ARMOR_TOUGHNESS);
        copyEffective(player, phantom, Attributes.MOVEMENT_SPEED);
        phantom.setHealth((float) phantom.getAttributeValue(Attributes.MAX_HEALTH));
    }

    private static void stripMirroredEquipmentAttributes(InnerDemonEntity phantom) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = phantom.getItemBySlot(slot);
            Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(slot);
            modifiers.asMap().forEach((attribute, values) -> {
                AttributeInstance instance = phantom.getAttribute(attribute);
                if (instance != null) for (AttributeModifier modifier : values) instance.removeModifier(modifier);
            });
        }
    }

    private static void copyEffective(ServerPlayer player, InnerDemonEntity phantom, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        AttributeInstance instance = phantom.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(Math.max(0.1D, player.getAttributeValue(attribute) * .8D));
    }

    private static void syncLock(ServerPlayer player, boolean locked) {
        XiantuNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new InnerDemonStatePacket(locked));
    }

    public static void syncCurrentLock(ServerPlayer player) { syncLock(player, active(player)); }

    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.level().isClientSide || !active(player)) return;
        if (player.containerMenu != player.inventoryMenu) player.closeContainer();
        long expires = player.getPersistentData().getLong(EXPIRES_KEY);
        if (player.serverLevel().getGameTime() >= expires) finish(player, player.getPersistentData().getUUID(TX_KEY), false, FailureReason.TIMEOUT);
        else if (InnerDemonRules.belowFailureHealth(player.getHealth(), player.getMaxHealth()))
            finish(player, player.getPersistentData().getUUID(TX_KEY), false, FailureReason.LOW_HEALTH);
        else {
            UUID entityId = player.getPersistentData().getUUID(ENTITY_KEY);
            if (!(player.serverLevel().getEntity(entityId) instanceof InnerDemonEntity))
                finish(player, player.getPersistentData().getUUID(TX_KEY), false);
        }
    }

    public static void timeout(InnerDemonEntity phantom) {
        if (!(phantom.level() instanceof net.minecraft.server.level.ServerLevel level) || phantom.ownerId() == null || phantom.transactionId() == null) {
            net.minecraft.world.entity.Entity entity = phantom;
            entity.discard();
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(phantom.ownerId());
        if (player != null && active(player) && player.getPersistentData().getUUID(TX_KEY).equals(phantom.transactionId()))
            finish(player, phantom.transactionId(), false, FailureReason.TIMEOUT);
        else {
            net.minecraft.world.entity.Entity entity = phantom;
            entity.discard();
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void heartDemonDamageFailure(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !active(player)
                || !(event.getSource().getEntity() instanceof InnerDemonEntity)) return;
        if (InnerDemonRules.wouldFallBelowFailureHealth(player.getHealth(), player.getMaxHealth(), event.getAmount())) {
            event.setAmount(InnerDemonRules.damageToFailureBoundary(player.getHealth(), player.getMaxHealth()));
            finish(player, player.getPersistentData().getUUID(TX_KEY), false, FailureReason.LOW_HEALTH);
        }
    }

    @SubscribeEvent public static void entityDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof InnerDemonEntity phantom && phantom.level() instanceof net.minecraft.server.level.ServerLevel level
                && phantom.ownerId() != null && phantom.transactionId() != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(phantom.ownerId());
            if (player != null) finish(player, phantom.transactionId(), true);
        }
        if (event.getEntity() instanceof ServerPlayer player && active(player))
            finish(player, player.getPersistentData().getUUID(TX_KEY), false);
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && active(player))
            finish(player, player.getPersistentData().getUUID(TX_KEY), false);
    }

    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && active(player))
            finish(player, player.getPersistentData().getUUID(TX_KEY), false);
    }

    @SubscribeEvent public static void blockArmorHotbarEquip(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && active(player)
                && event.getItemStack().getItem() instanceof net.minecraft.world.item.ArmorItem)
            event.setCanceled(true);
    }

    private static void finish(ServerPlayer player, UUID transaction, boolean victory) {
        finish(player, transaction, victory, FailureReason.INTERRUPTED);
    }

    private static void finish(ServerPlayer player, UUID transaction, boolean victory, FailureReason failureReason) {
        if (!active(player) || !player.getPersistentData().getUUID(TX_KEY).equals(transaction)) return;
        UUID entityId = player.getPersistentData().getUUID(ENTITY_KEY);
        player.getPersistentData().remove(TX_KEY);
        player.getPersistentData().remove(ENTITY_KEY);
        player.getPersistentData().remove(EXPIRES_KEY);
        for (net.minecraft.server.level.ServerLevel level : player.getServer().getAllLevels())
            if (level.getEntity(entityId) instanceof InnerDemonEntity phantom) phantom.discard();
        syncLock(player, false);
        player.getCapability(CultivationCapability.DATA).ifPresent(data -> {
            if (!(data instanceof PlayerCultivationData mutable)) return;
            if (victory) {
                mutable.setInnerDemon(InnerDemonRules.victoryInnerDemon(mutable.innerDemon()));
                if (player.getRandom().nextFloat() < InnerDemonRules.CRYSTAL_DROP_CHANCE) player.addItem(new ItemStack(ModItems.INNER_DEMON_CRYSTAL.get()));
                BreakthroughService.resolveAfterInnerDemon(player, mutable);
            } else {
                mutable.setProgress(BreakthroughRules.failureProgress((long) mutable.progressMax()));
                PhysiqueService.addInnerDemon(mutable, InnerDemonRules.FAILURE_INCREASE);
                String reason = switch (failureReason) {
                    case TIMEOUT -> "三分钟限时结束";
                    case LOW_HEALTH -> "生命降至两成警戒线";
                    case INTERRUPTED -> "挑战未完成";
                };
                player.sendSystemMessage(Component.literal("心魔战失败（" + reason + "），心魔幻影已消散；修为回退至阶段上限七成，心魔加深10"));
            }
            CultivationEvents.sync(player);
        });
    }
}
