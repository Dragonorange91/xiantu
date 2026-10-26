package com.longju.xiantu.equipment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-owned three-slot equipment, persisted on every change, not only menu close. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class AccessoryInventory extends SimpleContainer {
    public static final String KEY="xiantu_accessories";
    private final Player owner;
    private boolean loading=true;
    public AccessoryInventory(Player owner) {
        super(3); this.owner=owner;
        CompoundTag data=owner.getPersistentData().getCompound(KEY);
        for(int i=0;i<3;i++) setItem(i,ItemStack.of(data.getCompound("slot"+i)));
        loading=false;
    }
    @Override public int getMaxStackSize(){return 1;}
    @Override public void setChanged(){
        super.setChanged();
        if(loading || owner.level().isClientSide) return;
        CompoundTag tag=new CompoundTag();
        for(int i=0;i<3;i++) tag.put("slot"+i,getItem(i).save(new CompoundTag()));
        owner.getPersistentData().put(KEY,tag);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){
        if(e.getOriginal().getPersistentData().contains(KEY))
            e.getEntity().getPersistentData().put(KEY,e.getOriginal().getPersistentData().getCompound(KEY).copy());
    }
    @SubscribeEvent public static void drops(LivingDropsEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p) || p.isSpectator() || com.longju.xiantu.instance.InstanceService.temporary(p.level())
            || p.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        // Close first so no menu can later persist an old copy over the cleared inventory.
        p.closeContainer();
        var inv=new AccessoryInventory(p);
        for(int i=0;i<3;i++){
            ItemStack stack=inv.removeItemNoUpdate(i);
            if(!stack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(stack)){
                ItemEntity drop=new ItemEntity(p.level(),p.getX(),p.getY()+.5,p.getZ(),stack);
                drop.setDefaultPickUpDelay();e.getDrops().add(drop);
            }
        }
        inv.setChanged();
    }
}
