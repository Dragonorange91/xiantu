package com.longju.xiantu.trade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** A player's daily stock is shared across towns and survives death/logout. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class TradeDailyLedger {
    private static final String KEY="xiantu_town_daily_purchases";
    public static CompoundTag read(CompoundTag root,long gameTime){
        var row=root.getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY).copy();long day=TradeEconomyRules.day(gameTime);
        if(!row.contains("Day")||row.getLong("Day")!=day){row=new CompoundTag();row.putLong("Day",day);}return row;
    }
    public static void write(CompoundTag root,CompoundTag row){
        if(!root.contains(Player.PERSISTED_NBT_TAG))root.put(Player.PERSISTED_NBT_TAG,new CompoundTag());
        root.getCompound(Player.PERSISTED_NBT_TAG).put(KEY,row.copy());
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){
        var old=event.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if(old.contains(KEY))write(event.getEntity().getPersistentData(),old.getCompound(KEY));
    }
    private TradeDailyLedger(){}
}
