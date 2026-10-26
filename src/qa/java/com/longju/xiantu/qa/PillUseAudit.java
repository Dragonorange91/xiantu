package com.longju.xiantu.qa;

import com.longju.xiantu.alchemy.*;
import com.longju.xiantu.cultivation.*;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Temporary real-client use fixture; always restore before leaving the QA world. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class PillUseAudit {
    private record Saved(CompoundTag cultivation,CompoundTag persistent,List<ItemStack> inventory,
                         List<MobEffectInstance> effects,GameType mode,float health,int selected,int food,float saturation){}
    private static final Map<UUID,Saved> SAVED=new HashMap<>();
    @SubscribeEvent public static void register(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantupillfixture").requires(s->s.hasPermission(2))
            .then(Commands.literal("prepare").executes(c->prepare(c.getSource().getPlayerOrException())))
            .then(Commands.literal("verify").executes(c->verify(c.getSource().getPlayerOrException())))
            .then(Commands.literal("restore").executes(c->restore(c.getSource().getPlayerOrException()))));
    }
    private static int prepare(ServerPlayer p){
        if(!p.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        if(SAVED.containsKey(p.getUUID()))throw new IllegalStateException("Restore previous pill fixture first");
        p.closeContainer();var c=(PlayerCultivationData)MedicineService.data(p);var tag=new CompoundTag();c.save(tag);
        var inventory=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)inventory.add(p.getInventory().getItem(i).copy());
        SAVED.put(p.getUUID(),new Saved(tag,p.getPersistentData().copy(),inventory,
            p.getActiveEffects().stream().map(MobEffectInstance::new).toList(),p.gameMode.getGameModeForPlayer(),p.getHealth(),p.getInventory().selected,p.getFoodData().getFoodLevel(),p.getFoodData().getSaturationLevel()));
        c.medicine().load(new CompoundTag());p.removeAllEffects();MedicineService.refreshHealth(p);
        p.setGameMode(GameType.SURVIVAL);p.getFoodData().setFoodLevel(10);p.setHealth(p.getMaxHealth()/2);
        p.getInventory().selected=0;var pill=PillCatalog.stack("pill_05",5);pill.setCount(2);p.getInventory().setItem(0,pill);
        p.getInventory().setItem(1,PillCatalog.stack("pill_01",5));
        p.getInventory().setItem(2,PillCatalog.stack("pill_20",4));
        p.getInventory().setItem(3,PillCatalog.stack("pill_21",5));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
        p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();CultivationEvents.sync(p);
        return 1;
    }
    private static int verify(ServerPlayer p){
        try {
            var s=p.getMainHandItem();var m=MedicineService.data(p).medicine();
            if(!s.is(PillCatalog.ITEMS.get("pill_05").get())||s.getCount()!=1||m.totalDoses()!=1
                ||p.getHealth()<p.getMaxHealth()-.01||m.cooldown("heal",MedicineService.now(p))<=0)
                throw new IllegalStateException("Real 32-tick pill use failed: count="+s.getCount()+" doses="+m.totalDoses()+" health="+p.getHealth());
            org.slf4j.LoggerFactory.getLogger(PillUseAudit.class).info("[Pill interaction QA] PASS: native held-use, one dose, full healing, cooldown");
            p.sendSystemMessage(Component.literal("[Pill interaction QA] PASS"));return 1;
        } finally {restore(p);}
    }
    private static int restore(ServerPlayer p){
        var old=SAVED.remove(p.getUUID());if(old==null)return 0;
        p.stopUsingItem();p.closeContainer();((PlayerCultivationData)MedicineService.data(p)).load(old.cultivation());
        for(String k:Set.copyOf(p.getPersistentData().getAllKeys()))p.getPersistentData().remove(k);p.getPersistentData().merge(old.persistent());
        for(int i=0;i<old.inventory().size();i++)p.getInventory().setItem(i,old.inventory().get(i));p.getInventory().selected=old.selected();
        p.removeAllEffects();for(var effect:old.effects())p.addEffect(effect);p.setGameMode(old.mode());
        p.getFoodData().setFoodLevel(old.food());p.getFoodData().setSaturation(old.saturation());
        CultivationService.applyRealmAttributes(p,MedicineService.data(p));MedicineService.refreshHealth(p);MedicineService.refreshCapacity(p);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(old.selected()));
        p.setHealth(Math.min(old.health(),p.getMaxHealth()));p.inventoryMenu.broadcastChanges();CultivationEvents.sync(p);return 1;
    }
}
