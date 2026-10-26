package com.longju.xiantu.gongfa;

import com.longju.xiantu.cultivation.CultivationCapability;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.UseAnim;

public class GongfaItem extends Item {
    private final String definitionId;
    public GongfaItem(String definitionId, Properties properties) { super(properties.stacksTo(1)); this.definitionId = definitionId; }
    public String definitionId() { return definitionId; }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        com.longju.xiantu.client.GongfaStudyRenderer.install(consumer);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            String denial = GongfaService.denial(serverPlayer,definitionId);
            if (!denial.isEmpty()) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(denial),true);
                return InteractionResultHolder.fail(stack);
            }
            if (!GongfaService.approved(serverPlayer,definitionId,hand)) {
                GongfaMenu.open(serverPlayer,hand);
                return InteractionResultHolder.consume(stack);
            }
            if (!GongfaService.beginUse(serverPlayer,definitionId,hand)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("请盘坐在蒲团上，再长按右键研习10秒"),true);
                return InteractionResultHolder.fail(stack);
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
    @Override public int getUseDuration(ItemStack stack) { return GongfaRules.PREPARATION_TICKS; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }
    @Override public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!level.isClientSide && user instanceof ServerPlayer player && !GongfaService.useTick(player)) player.stopUsingItem();
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide && user instanceof ServerPlayer player) GongfaService.finishUse(player);
        return stack;
    }
    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!level.isClientSide && user instanceof ServerPlayer player) GongfaService.cancel(player,"松开右键");
    }
    @Override public net.minecraft.network.chat.Component getName(ItemStack stack) {
        GongfaDefinition definition=GongfaRegistry.get(definitionId);
        return net.minecraft.network.chat.Component.literal(definition.tierIndex()==0 ? definition.name() : definition.tier()+"·"+definition.name());
    }
    @Override public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable Level level,
            java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        GongfaDefinition method=GongfaRegistry.get(definitionId);
        lines.add(net.minecraft.network.chat.Component.literal("右键确认，盘坐蒲团长按右键10秒；功法书可重复使用，不消耗"));
        if(method.tierIndex()==0) { lines.add(net.minecraft.network.chat.Component.literal("开启修仙资格，允许无主功法修炼至练气九层；死亡保留资格")); return; }
        lines.add(net.minecraft.network.chat.Component.literal(method.learningRequirement()));
        lines.add(net.minecraft.network.chat.Component.literal("学成即修习；首次免回退，更换会散功重修"));
        lines.add(net.minecraft.network.chat.Component.literal("学习要求："+method.learningRequirement()));
        lines.add(net.minecraft.network.chat.Component.literal("修炼上限："+com.longju.xiantu.cultivation.RealmRegistry.MAJOR_REALM_NAMES.get(method.maxRealm())+"；高境界仍可学习使用"));
        lines.add(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT,"九层修炼×%.1f，灵力容量+%.0f%%，基础回复+%.0f%%",method.maximumMultiplier(),method.powerMaxBonus()*100,method.recoveryBonus()*100)));
        lines.add(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT,"九层战斗：攻击+%.0f%%，生命+%.0f%%，护甲+%.0f",method.attackBonus(9)*100,method.healthBonus(9)*100,method.armorBonus(9))));
        lines.add(net.minecraft.network.chat.Component.literal("九层被动：" + method.buff().nameText() + "（" + method.buff().format(method.buffBonus(9)) + "）"));
    }
}
