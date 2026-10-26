package com.longju.xiantu.wood;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import com.longju.xiantu.item.SpiritStoneItem;

/** Every button is a server-side transaction. Preview binds to the exact held stack and growth. */
public final class SaplingGrowthMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    public final boolean batch;
    public final SpiritWoodType type;
    private final Player player;
    private final SpiritWoodBlockEntity be;
    private final int[] values=new int[9];
    private ItemStack prepared=ItemStack.EMPTY;
    private long preparedGrowth=-1;
    public SaplingGrowthMenu(int id,Inventory inv,FriendlyByteBuf buf){this(id,inv,buf.readBlockPos(),null,buf.readBoolean(),SpiritWoodType.values()[buf.readInt()]);}
    public SaplingGrowthMenu(int id,Inventory inv,BlockPos pos,SpiritWoodBlockEntity be,boolean batch){this(id,inv,pos,be,batch,be.type());}
    private SaplingGrowthMenu(int id,Inventory inv,BlockPos pos,SpiritWoodBlockEntity be,boolean batch,SpiritWoodType type){
        super(SpiritWoodRegistry.MENU.get(),id);this.pos=pos;this.be=be;this.batch=batch;this.type=type;player=inv.player;
        for(int i=0;i<values.length;i++){final int index=i;addDataSlot(new DataSlot(){public int get(){return values[index];}public void set(int n){values[index]=n;}});}
        if(be!=null){if(!batch)prepare();refresh();}
    }
    private void refresh(){
        long remain=be.remainingUnits();for(int i=0;i<4;i++)values[i]=(int)(remain>>>(16*i))&65535;
        var stone=SpiritStoneItem.tierOf(player.getMainHandItem());values[4]=stone==null?0:stone.rank();values[5]=player.getMainHandItem().getCount();
        values[6]=prepared.isEmpty()?0:1;
        values[8]=failureReason();values[7]=values[8]==0?1:0;
    }
    public long remaining(){long r=0;for(int i=0;i<4;i++)r|=(values[i]&65535L)<<(i*16);return r;}
    public long total(){return type.growthTicks()*type.energyCost();}
    public int stoneRank(){return values[4];}
    public int count(){return values[5];}
    public boolean validPreview(){return values[6]!=0;}
    public boolean allowed(){return values[7]!=0;}
    public int reason(){return values[8];}
    public String reasonText(){return switch(reason()){
        case 1 -> "树苗已移除，请重新打开";
        case 2 -> "已长满，成熟树形空间受阻";
        case 3 -> "境界不足，需"+com.longju.xiantu.cultivation.RealmRegistry.MAJOR_REALM_NAMES.get(type.realm);
        case 4 -> "主手需要拿着灵石";
        case 5 -> "灵石阶数不足";
        case 6 -> "灵石已被吸收，需完整灵石";
        case 7 -> "树苗上方需要露天";
        case 8 -> "光照不足，需要亮度≥9";
        case 9 -> "树苗下方需要泥土类方块";
        case 10 -> "成熟树形范围有方块或未加载区块";
        default -> "可催熟";
    };}
    public long perStone(){return stoneRank()==0?0:type.growthTicks()*com.longju.xiantu.item.SpiritStoneTier.values()[stoneRank()-1].spiritualPower();}
    public long wastedPower(){return Math.max(0,perStone()-remaining())/type.growthTicks();}
    private boolean valid(){
        return failureReason()==0;
    }
    private int failureReason(){
        var held=player.getMainHandItem();var tier=SpiritStoneItem.tierOf(held);
        if(be==null||be.isRemoved()||be.getLevel()==null||be.getLevel().getBlockEntity(pos)!=be)return 1;
        if(be.remainingUnits()<=0)return 2;
        if(!SpiritWoodRules.realm(player,type))return 3;
        if(tier==null||held.isEmpty())return 4;
        if(tier.rank()<type.rank())return 5;
        if(!SpiritStoneItem.isFullyCharged(held))return 6;
        if(!player.level().canSeeSky(pos))return 7;
        if(player.level().getMaxLocalRawBrightness(pos)<9)return 8;
        if(!player.level().getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.DIRT))return 9;
        if(!SpiritTreeGrowth.space(player.level(),pos,type))return 10;
        return 0;
    }
    private void prepare(){prepared=player.getMainHandItem().copy();preparedGrowth=be.growthUnits;}
    @Override public boolean clickMenuButton(Player p,int button){
        if(p!=player||!stillValid(p)||!valid()){refresh();p.displayClientMessage(Component.literal("无法催熟："+reasonText()),true);return false;}
        if(button==3){prepare();refresh();return true;}
        ItemStack held=p.getMainHandItem();long step=type.growthTicks()*SpiritStoneItem.tierOf(held).spiritualPower();
        int n;
        if(button==2&&batch)n=(int)Math.min(held.getCount(),be.remainingUnits()/step);
        else if(button==1&&!batch||button==4&&batch){
            if(prepared.isEmpty()||be.growthUnits<preparedGrowth||!ItemStack.matches(prepared,held)){prepare();refresh();p.displayClientMessage(Component.literal("进度或灵石已变化，请查看新预览后再次确认"),true);return false;}
            // Single-button label includes overflow; explicit overflow button is reserved for batch mode.
            n=1;
        }else return false;
        if(n<=0)return false;
        held.shrink(n);be.growthUnits=Math.min(be.totalUnits(),be.growthUnits+step*n);be.update();prepared=ItemStack.EMPTY;
        if(be.remainingUnits()==0&&SpiritTreeGrowth.grow(p.level(),pos,type))p.closeContainer();
        else{prepare();refresh();}
        return true;
    }
    @Override public void broadcastChanges(){if(be!=null)refresh();super.broadcastChanges();}
    @Override public boolean stillValid(Player p){return p.level().getBlockState(pos).getBlock() instanceof SpiritSaplingBlock&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;}
    @Override public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
}
