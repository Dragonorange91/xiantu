package com.longju.xiantu.wood;
import com.longju.xiantu.cultivation.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.*;
public final class SpiritWoodRules {
    private SpiritWoodRules(){}
    public static boolean realm(Player p,SpiritWoodType t){return p.getAbilities().instabuild||p.getCapability(CultivationCapability.DATA).map(d->d.majorRealm()>=t.realm).orElse(false);}
    public static boolean canHarvest(Player p,SpiritWoodType t,ItemStack tool){
        if(p.getAbilities().instabuild)return true;
        return realm(p,t)&&tool.getItem() instanceof AxeItem axe&&axe.getTier().getLevel()>=t.axeLevel();
    }
    public static void harvestHint(Player p,SpiritWoodType t){p.displayClientMessage(Component.literal(t.treeName+"需要"+RealmRegistry.MAJOR_REALM_NAMES.get(t.realm)+"境界及"+(t.axeLevel()==1?"石":t.axeLevel()==2?"铁":"钻石")+"斧或更高等级斧"),true);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void harvest(BlockEvent.BreakEvent e){
        if(!e.isCanceled()&&e.getState().getBlock() instanceof SpiritLogBlock log&&!canHarvest(e.getPlayer(),log.type,e.getPlayer().getMainHandItem())){e.setCanceled(true);harvestHint(e.getPlayer(),log.type);}
    }
}
