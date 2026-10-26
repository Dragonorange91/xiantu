package com.longju.xiantu.trade;

import com.longju.xiantu.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

public final class TownMerchantMenu extends AbstractContainerMenu {
    private final TownMerchantEntity merchant;
    private final Player owner;
    private final ContainerData state=new SimpleContainerData(TownTradeService.OFFERS.size()+3);
    private long lastPurchaseTick=Long.MIN_VALUE;
    public TownMerchantMenu(int id,Inventory inventory,FriendlyByteBuf buffer) {
        this(id,inventory,inventory.player.level().getEntity(buffer.readInt()) instanceof TownMerchantEntity m?m:null);
    }
    public TownMerchantMenu(int id,Inventory inventory,TownMerchantEntity merchant) {
        super(ModMenus.TOWN_MERCHANT.get(),id);this.merchant=merchant;owner=inventory.player;addDataSlots(state);
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,18+col*18,153+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,18+col*18,211));
        broadcastChanges();
    }
    public boolean wanbao() { return state.get(0)==1; }
    public boolean guide() { return state.get(0)==2; }
    public boolean claimed() { return state.get(TownTradeService.OFFERS.size()+1)==1; }
    public int purchases(){return state.get(TownTradeService.OFFERS.size()+2);}
    public java.util.List<TownTradeService.Offer> offers(){String shop=switch(state.get(0)){case 1->"wanbao_merchant";case 3->"smith_merchant";case 4->"alchemy_merchant";case 5->"talisman_merchant";default->"";};return TownTradeService.OFFERS.stream().filter(o->o.shop().equals(shop)).toList();}
    public boolean known(int offer) { return (state.get(offer+1)&1)!=0; }
    public int remaining(int offer){return (state.get(offer+1)>>1)-1;}
    @Override public void broadcastChanges() {
        if(owner instanceof ServerPlayer server && merchant!=null) {
            state.set(0,merchant.guide()?2:merchant.wanbao()?1:merchant.profession().equals("smith_merchant")?3:merchant.profession().equals("alchemy_merchant")?4:merchant.profession().equals("talisman_merchant")?5:0);
            state.set(TownTradeService.OFFERS.size()+2,TownTradeService.purchases(server,merchant));
            state.set(TownTradeService.OFFERS.size()+1,TownJourney.state(server).getBoolean("BookClaimed")?1:0);
            for(int i=0;i<TownTradeService.OFFERS.size();i++)state.set(i+1,(TownTradeService.known(server,i)?1:0)|((TownTradeService.remaining(server,TownTradeService.OFFERS.get(i))+1)<<1));
        }
        super.broadcastChanges();
    }
    public static void open(ServerPlayer player,TownMerchantEntity merchant) {
        NetworkHooks.openScreen(player,new SimpleMenuProvider((id,inventory,title)->new TownMerchantMenu(id,inventory,merchant),merchant.getDisplayName()),buf->buf.writeInt(merchant.getId()));
    }
    @Override public boolean stillValid(Player player) { return player==owner && merchant!=null && merchant.mayTrade(player); }
    @Override public boolean clickMenuButton(Player player,int button) {
        if(!(player instanceof ServerPlayer server)||player.containerMenu!=this||!stillValid(player)||button<0||button>=TownTradeService.OFFERS.size())return false;
        long now=player.level().getGameTime();if(lastPurchaseTick==now)return false;lastPurchaseTick=now;
        player.sendSystemMessage(Component.literal(TownTradeService.buy(server,merchant,button)));broadcastChanges();return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int slot) { return ItemStack.EMPTY; }
}
