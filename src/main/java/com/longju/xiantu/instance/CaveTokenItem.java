package com.longju.xiantu.instance;

import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class CaveTokenItem extends Item {
    public final boolean child;
    public CaveTokenItem(boolean child){super(new Properties().stacksTo(1));this.child=child;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){
        if(!level.isClientSide&&p instanceof ServerPlayer sp){
            String result=hand!=InteractionHand.MAIN_HAND?"请主手持令牌使用":InstanceService.useToken(sp,p.getItemInHand(hand),child);
            if(!result.isEmpty())p.displayClientMessage(Component.literal(result),true);
        }return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand),level.isClientSide);
    }
    @Override public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext c){
        // Let ordinary containers handle their own right-click, including while crouching.
        if(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof net.minecraft.world.Container)return InteractionResult.PASS;
        var p=c.getPlayer();if(p!=null)use(c.getLevel(),p,c.getHand());return InteractionResult.sidedSuccess(c.getLevel().isClientSide);
    }
    @Override public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal(child?"洞府访客令牌：可交易，不能签发其他令牌":"首次使用创建永久洞府；绑定后仅创建者可用"));
        lines.add(Component.literal("主手右键进出；附近有敌对生物时无法使用"));
        if(s.hasTag()&&s.getTag().contains("Cave"))lines.add(Component.literal("已绑定洞府"));
    }
}
