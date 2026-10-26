package com.longju.xiantu.instance;

import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.gongfa.GongfaService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

/** Creative/operator testing aid; deliberately has no survival acquisition route. */
public final class RealmTestItem extends Item {
    public RealmTestItem(){super(new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer p){
            if(!p.isCreative()&&!p.hasPermissions(2)){p.displayClientMessage(Component.literal("仅创造模式或管理员可使用测试道具"),true);return InteractionResultHolder.fail(stack);}
            if(p.getCooldowns().isOnCooldown(this))return InteractionResultHolder.fail(stack);
            var data=GongfaService.data(p);
            int next=data.cultivationUnlocked()?Math.min(RealmRegistry.MAJOR_REALM_NAMES.size()-1,data.majorRealm()+1):0;
            if(data.cultivationUnlocked()&&data.majorRealm()==next){p.displayClientMessage(Component.literal("已达到最高大境界"),true);return InteractionResultHolder.fail(stack);}
            data.setCultivationUnlocked(true);data.setRealmStage(next,0);data.setProgress(0);
            GongfaService.refresh(p);CultivationService.applyRealmAttributes(p,data);data.setPower(data.powerMax());p.setHealth(p.getMaxHealth());CultivationEvents.sync(p);
            p.getCooldowns().addCooldown(this,10);p.displayClientMessage(Component.literal("测试修为："+RealmRegistry.get(next,0).majorName()+RealmRegistry.get(next,0).minorName()),true);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
