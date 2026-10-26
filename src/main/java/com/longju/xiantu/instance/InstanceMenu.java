package com.longju.xiantu.instance;

import com.longju.xiantu.registry.ModMenus;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** One committed action per menu; currency and all output space are checked on copies first. */
public final class InstanceMenu extends AbstractContainerMenu {
    public final CompoundTag view;
    private final Player owner;private final UUID cave;private final int page;private final List<String> grants;
    private boolean committed;
    public InstanceMenu(int id,Inventory inv,FriendlyByteBuf b){super(ModMenus.INSTANCES.get(),id);owner=inv.player;view=b.readNbt();cave=null;page=0;grants=List.of();}
    private InstanceMenu(int id,Inventory inv,CompoundTag view,UUID cave,int page,List<String> grants){super(ModMenus.INSTANCES.get(),id);owner=inv.player;this.view=view;this.cave=cave;this.page=page;this.grants=grants;}
    public static void open(ServerPlayer p){open(p,0);}
    public static void openGuide(ServerPlayer p){
        if(!p.isAlive()||p.isSpectator()||!p.containerMenu.getCarried().isEmpty())return;
        var view=new CompoundTag();view.putBoolean("Guide",true);view.putBoolean("Owned",false);view.putBoolean("Inside",false);
        NetworkHooks.openScreen(p,new SimpleMenuProvider((id,inv,title)->new InstanceMenu(id,inv,view,null,0,List.of()),Component.literal("仙途修真手册")),b->b.writeNbt(view));
    }
    private static void open(ServerPlayer p,int page){
        if(!p.isAlive()||p.isSpectator()||!p.containerMenu.getCarried().isEmpty())return;
        var d=InstanceService.data(p.server);var s=d.blocked()?null:d.ledger().cave(p.getUUID()).filter(v->v.state()==InstanceLedger.State.ACTIVE).orElse(null);var view=new CompoundTag();
        view.putBoolean("Owned",s!=null);boolean inside=s!=null&&p.level().dimension().location().toString().equals(s.dimension());view.putBoolean("Inside",inside);
        List<String> ids=s==null?List.of():d.session(s).getCompound("Grants").getAllKeys().stream().sorted().toList();int last=Math.max(0,(ids.size()-1)/8);int current=Math.max(0,Math.min(last,page));
        var shown=ids.subList(Math.min(ids.size(),current*8),Math.min(ids.size(),current*8+8));var flags=new ListTag();
        for(String grant:shown){var tag=new CompoundTag();tag.putBoolean("Active",d.session(s).getCompound("Grants").getInt(grant)>0);flags.add(tag);}view.put("Grants",flags);view.putInt("Page",current);view.putInt("LastPage",last);
        NetworkHooks.openScreen(p,new SimpleMenuProvider((id,inv,title)->new InstanceMenu(id,inv,view,s==null?null:s.instance(),current,List.copyOf(shown)),Component.literal("修仙手册 · 洞府")),b->b.writeNbt(view));
    }
    @Override public boolean stillValid(Player p){return p==owner&&p.isAlive()&&!p.isSpectator()&&!committed;}
    @Override public boolean clickMenuButton(Player who,int action){
        if(!(who instanceof ServerPlayer p)||p.containerMenu!=this||!stillValid(p))return false;
        if(view.getBoolean("Guide")&&action>=2001&&action<=2003){com.longju.xiantu.formation.FormationProjection.begin(p,action-2000);return true;}
        var d=InstanceService.data(p.server);if(d.blocked()||cave==null)return false;var s=d.ledger().cave(p.getUUID()).filter(v->v.instance().equals(cave)&&v.state()==InstanceLedger.State.ACTIVE).orElse(null);if(s==null)return false;
        if(action==1000||action==1001){open(p,page+(action==1000?1:-1));return true;}
        if(action!=0&&action!=1&&(action<10||action>=10+grants.size()))return false;
        var n=d.session(s);String result;
        if(action==0){
            int version=Math.addExact(n.getInt("MasterVersion"),1);var output=InstanceService.token(d,s,"master",version);
            if(!pay(p,3,16,output))result="补发需要16枚完整三阶灵石及背包空位";
            else{n.putInt("MasterVersion",version);d.setDirty();result="已补发主令牌，旧主令牌失效；子令牌不受影响";}
        }else{
            boolean atHome=p.level().dimension().location().toString().equals(s.dimension());var main=p.getMainHandItem();var checked=InstanceService.validate(p,main);
            if(!atHome||checked==null||!checked.instance().equals(s.instance())||main.getItem()!=InstanceRegistry.TOKEN.get())result="须在自己的洞府内，主手持有效主令牌管理授权";
            else if(action==1){
                String grant=UUID.randomUUID().toString();var output=InstanceService.token(d,s,grant,1);
                if(!childPayment(p,output))result="签发需要云玉方块×4、完整三阶灵石×1、妖骨×1及背包空位";
                else{n.getCompound("Grants").putInt(grant,1);d.setDirty();result="已签发独立子令牌，可交易给其他玩家";}
            }else{String grant=grants.get(action-10);if(n.getCompound("Grants").getInt(grant)<=0)result="此授权已撤销";else{n.getCompound("Grants").putInt(grant,0);d.setDirty();result="授权已撤销，后续进入将被拒绝";}}
        }
        committed=true;p.closeContainer();p.sendSystemMessage(Component.literal(result));return true;
    }
    private static List<ItemStack> inventory(ServerPlayer p){var out=new ArrayList<ItemStack>();p.getInventory().items.forEach(s->out.add(s.copy()));out.add(p.getOffhandItem().copy());return out;}
    private static void commit(ServerPlayer p,List<ItemStack> next){for(int i=0;i<36;i++)p.getInventory().items.set(i,next.get(i));p.getInventory().offhand.set(0,next.get(36));p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();}
    private static boolean pay(ServerPlayer p,int rank,int count,ItemStack output){var next=com.longju.xiantu.trade.TownTradeService.plan(inventory(p),rank,count,output);if(next==null)return false;commit(p,next);return true;}
    private static boolean consume(List<ItemStack> inv,net.minecraft.world.item.Item item,int count){for(var s:inv)if(s.is(item)){int take=Math.min(s.getCount(),count);s.shrink(take);count-=take;if(count==0)return true;}return false;}
    private static boolean childPayment(ServerPlayer p,ItemStack output){
        var copy=inventory(p);
        if(!consume(copy,ForgeRegistries.ITEMS.getValue(new ResourceLocation("xiantu:cloud_jade")),4)||!consume(copy,com.longju.xiantu.registry.ModItems.BEAST_BONE.get(),1))return false;
        var next=com.longju.xiantu.trade.TownTradeService.plan(copy,3,1,output);if(next==null)return false;commit(p,next);return true;
    }
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
}
