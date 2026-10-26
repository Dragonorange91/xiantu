package com.longju.xiantu.item;

import com.longju.xiantu.trade.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class GuidingStoneItem extends Item {
    public GuidingStoneItem(){super(new Properties().stacksTo(1));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stone=player.getItemInHand(hand);
        if(player instanceof ServerPlayer p){
            if(TownJourney.arrived(p)){TownJourney.removeStones(p);return InteractionResultHolder.success(ItemStack.EMPTY);}
            TownJourney.prepare(p);var data=TownJourney.state(p);
            if(data.contains("Target")){
                BlockPos target=BlockPos.of(data.getLong("Target"));
                p.sendSystemMessage(Component.literal("引路石所指城镇：主世界 X "+target.getX()+"，Z "+target.getZ()+"。到达后，与广场引路人交谈。"));
            }else p.sendSystemMessage(Component.literal(data.getString("SearchState").equals("unavailable")?"此地暂未寻得修仙城镇；请确认世界开启了结构生成。":"引路石正在感应城镇，请稍候。"));
        }
        return InteractionResultHolder.sidedSuccess(stone,level.isClientSide);
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity owner,int slot,boolean selected){
        if(owner instanceof ServerPlayer player&&player.tickCount%20==0){
            if(TownJourney.arrived(player)){stack.setCount(0);return;}
            TownJourney.syncStone(player);
        }
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal("手持引路，前往最近的修仙城镇"));
        lines.add(Component.literal("到达城镇完成「初入仙途」后消失"));
        if(stack.hasTag()&&stack.getTag().contains("TownTarget")){
            BlockPos p=BlockPos.of(stack.getTag().getLong("TownTarget"));lines.add(Component.literal("主世界 · X "+p.getX()+" / Z "+p.getZ()));
        }
    }
}
