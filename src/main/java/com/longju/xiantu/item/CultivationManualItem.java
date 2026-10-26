package com.longju.xiantu.item;

import com.longju.xiantu.cultivation.CultivationCapability;
import com.longju.xiantu.cultivation.RealmRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CultivationManualItem extends Item {
    public CultivationManualItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if(player instanceof net.minecraft.server.level.ServerPlayer p&&hand==InteractionHand.MAIN_HAND){if(com.longju.xiantu.formation.FormationProjection.click(p))return InteractionResultHolder.success(stack);com.longju.xiantu.instance.InstanceMenu.openGuide(p);return InteractionResultHolder.success(stack);}
        if (!level.isClientSide) player.getCapability(CultivationCapability.DATA).ifPresent(data -> {
            var realm = RealmRegistry.get(data.majorRealm(), data.stage());
            player.sendSystemMessage(Component.literal("《仙途修真手册》"));
            player.sendSystemMessage(Component.literal("境界：" + realm.majorName() + "·" + realm.minorName()));
            player.sendSystemMessage(Component.literal("小阶段修为满额后自动突破；达到大境界瓶颈时，在探灵玉鉴界面确认突破。"));
            player.sendSystemMessage(Component.literal("灵石可用于吸收灵力、阵法核心供能和城镇商人交易。"));
            var method=com.longju.xiantu.gongfa.GongfaRegistry.get(data.gongfa().active());
            player.sendSystemMessage(Component.literal(data.cultivationUnlocked()?"修仙资格已开启（死亡保留）；主功法："+(method==null?"未装备":method.name()+"·"+data.gongfa().activeLevel()+"层"):"请先学习玄元养气决，开启修仙之路。"));
            player.sendSystemMessage(Component.literal("手持功法书右键确认后，盘坐蒲团长按右键10秒研习；书籍保留，探灵玉鉴显示当前功法、层数及加成。常用功法跨多境界，不用逐境界换书。"));
        });
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
