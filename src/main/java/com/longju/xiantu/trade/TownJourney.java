package com.longju.xiantu.trade;

import com.longju.xiantu.registry.ModItems;
import com.longju.xiantu.worldgen.town.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Player-owned introduction progress, independent of cultivation resets and individual NPCs. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class TownJourney {
    public static final ResourceLocation FIRST_STEP=new ResourceLocation("xiantu","journey/first_step");
    public static final String KEY="XiantuTownJourney";
    private TownJourney() {}
    public static CompoundTag state(Player player) {
        CompoundTag root=player.getPersistentData();
        if(!root.contains(Player.PERSISTED_NBT_TAG))root.put(Player.PERSISTED_NBT_TAG,new CompoundTag());
        CompoundTag persisted=root.getCompound(Player.PERSISTED_NBT_TAG);
        if(!persisted.contains(KEY))persisted.put(KEY,new CompoundTag());
        return persisted.getCompound(KEY);
    }
    public static boolean arrived(ServerPlayer player) {
        var a=player.server.getAdvancements().getAdvancement(FIRST_STEP);
        return state(player).getBoolean("Arrived") || a!=null&&player.getAdvancements().getOrStartProgress(a).isDone();
    }
    public static void removeStones(Player player) {
        var inv=player.getInventory();boolean changed=false;
        for(int i=0;i<inv.getContainerSize();i++)if(inv.getItem(i).is(ModItems.GUIDING_STONE.get())){inv.setItem(i,ItemStack.EMPTY);changed=true;}
        if(player.containerMenu.getCarried().is(ModItems.GUIDING_STONE.get())){player.containerMenu.setCarried(ItemStack.EMPTY);changed=true;}
        if(changed){inv.setChanged();player.inventoryMenu.broadcastChanges();player.containerMenu.broadcastChanges();}
    }
    public static void finish(ServerPlayer player) {
        state(player).putBoolean("Arrived",true);
        var a=player.server.getAdvancements().getAdvancement(FIRST_STEP);
        if(a!=null)for(String criterion:player.getAdvancements().getOrStartProgress(a).getRemainingCriteria())player.getAdvancements().award(a,criterion);
        removeStones(player);TownLocator.cancel(player.getUUID());
    }
    public static void issueManual(ServerPlayer player){
        var state=state(player);if(state.getBoolean("ManualIssued"))return;
        for(var item:player.getInventory().items)if(item.is(ModItems.CULTIVATION_MANUAL.get())){state.putBoolean("ManualIssued",true);return;}
        if(player.getInventory().add(new ItemStack(ModItems.CULTIVATION_MANUAL.get()))){state.putBoolean("ManualIssued",true);player.inventoryMenu.broadcastChanges();}
    }
    public static void prepare(ServerPlayer player) {
        if(player.isSpectator())return;
        issueManual(player);
        if(arrived(player)){removeStones(player);return;}
        var s=state(player);
        if(!s.contains("Origin"))s.putLong("Origin",player.blockPosition().asLong());
        if(!s.getBoolean("StoneIssued")) {
            ItemStack stone=new ItemStack(ModItems.GUIDING_STONE.get());
            if(player.getInventory().add(stone)) {s.putBoolean("StoneIssued",true);player.inventoryMenu.broadcastChanges();}
        }
        if(!s.contains("Target"))TownLocator.request(player);
        syncStone(player);
    }
    public static void syncStone(ServerPlayer player) {
        CompoundTag s=state(player);
        for(int i=0;i<player.getInventory().getContainerSize();i++) {
            ItemStack stone=player.getInventory().getItem(i);
            if(!stone.is(ModItems.GUIDING_STONE.get()))continue;
            CompoundTag tag=stone.getOrCreateTag();
            tag.putUUID("JourneyOwner",player.getUUID());
            if(s.contains("Target"))tag.putLong("TownTarget",s.getLong("Target"));
            tag.putString("SearchState",s.getString("SearchState"));
        }
    }
    /** Exact town tiles + surface height, not a wide structure bounding box underground. */
    public static boolean insideTown(ServerPlayer player) {
        var level=player.serverLevel();if(level.dimension()!=Level.OVERWORLD)return false;
        var type=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("xiantu","cultivation_town"));
        if(type==null||!level.hasChunkAt(player.blockPosition()))return false;
        var start=level.structureManager().getStructureWithPieceAt(player.blockPosition(),type);
        if(!start.isValid())return false;
        for(var piece:start.getPieces())if(piece instanceof TownTerrainPiece tile) {
            int x=player.getBlockX()-tile.originX(),z=player.getBlockZ()-tile.originZ();
            if(x<3||z<3||x>=TownLayout.WIDTH-3||z>=TownLayout.DEPTH-3||!tile.covers(x,z))continue;
            double surface=tile.plannedHeight(x,z)+1;
            if(player.getY()>=surface-1&&player.getY()<=surface+12)return true;
        }
        return false;
    }
    public static boolean grantBook(ServerPlayer player,TownMerchantEntity guide) {
        if(!guide.guide()||!guide.mayTrade(player))return false;
        CompoundTag s=state(player);
        if(s.getBoolean("BookClaimed"))return false;
        ItemStack book=new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("xiantu","xuan_yuan_yang_qi_jue")));
        // A full inventory leaves the gift claim open; no book is silently lost or dropped.
        if(!player.getInventory().add(book)) {
            player.sendSystemMessage(Component.literal("请先腾出一个背包位置，再与引路人交谈领取养气诀。"));return false;
        }
        s.putBoolean("BookClaimed",true);player.inventoryMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal("引路人：仙途始于静心。赠你《玄元养气诀》，坐于蒲团，持书研习，便可迈入修行。"));
        return true;
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)prepare(p);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){
        var old=e.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        var root=e.getEntity().getPersistentData();
        if(!root.contains(Player.PERSISTED_NBT_TAG))root.put(Player.PERSISTED_NBT_TAG,new CompoundTag());
        root.getCompound(Player.PERSISTED_NBT_TAG).put(KEY,old.getCompound(KEY).copy());
        if(e.isWasDeath()&&!com.longju.xiantu.instance.InstanceService.temporary(e.getOriginal().level())
                &&!e.getEntity().level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY))state(e.getEntity()).putBoolean("StoneIssued",false);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)prepare(p);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){TownLocator.cancel(e.getEntity().getUUID());}
    @SubscribeEvent public static void advancement(AdvancementEvent.AdvancementEarnEvent e){
        if(e.getAdvancement().getId().equals(FIRST_STEP)&&e.getEntity() instanceof ServerPlayer p){state(p).putBoolean("Arrived",true);removeStones(p);}
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||p.tickCount%20!=0)return;
        if(!state(p).getBoolean("ManualIssued"))issueManual(p);
        if(arrived(p)){removeStones(p);return;}
        if(insideTown(p)){finish(p);return;}
        if(!state(p).getBoolean("StoneIssued"))prepare(p);
    }
}
