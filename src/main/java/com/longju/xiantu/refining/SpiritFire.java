package com.longju.xiantu.refining;

import com.longju.xiantu.registry.ModItems;
import com.longju.xiantu.cultivation.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

/** Shared physical fire source for refining and alchemy; power remains in the device reservoir. */
public final class SpiritFire extends Item {
    public static final RegistryObject<Item> COMMON=ModItems.ITEMS.register("condensed_spirit_fire",()->new SpiritFire(0));
    public static final List<RegistryObject<Item>> WONDROUS=registerRare();
    private final int rank;
    private SpiritFire(int rank){super(new Item.Properties().durability(rank==0?600:1200).fireResistant());this.rank=rank;}
    private static List<RegistryObject<Item>> registerRare(){var list=new ArrayList<RegistryObject<Item>>();for(int i=1;i<=6;i++){final int rank=i;list.add(ModItems.ITEMS.register("wondrous_fire_"+i,()->new SpiritFire(rank)));}return List.copyOf(list);}
    public static void bootstrap(){}
    public static boolean isFire(ItemStack stack){return stack.getItem() instanceof SpiritFire;}
    public static int rank(ItemStack stack){return stack.getItem() instanceof SpiritFire fire?fire.rank:0;}
    private static long now(Level level){return level.getServer().overworld().getGameTime();}
    public static int remaining(ItemStack stack){return isFire(stack)?Math.max(0,stack.getMaxDamage()-stack.getDamageValue()):0;}
    public static void recover(ItemStack stack,Level level){
        if(level.isClientSide||rank(stack)==0)return;
        long now=now(level);var tag=stack.getOrCreateTag();
        if(!tag.contains("FireRecoveryAt")){tag.putLong("FireRecoveryAt",now);return;}
        long then=tag.getLong("FireRecoveryAt");if(then>now){tag.putLong("FireRecoveryAt",now);return;}
        long count=(now-then)/100;if(count<=0)return;
        stack.setDamageValue((int)Math.max(0,stack.getDamageValue()-count));
        tag.putLong("FireRecoveryAt",stack.getDamageValue()==0?now:then+count*100);
    }
    public static void holdActive(ItemStack stack,Level level){if(isFire(stack))stack.getOrCreateTag().putLong("FireRecoveryAt",now(level));}
    public static boolean burn(ItemStack stack,Level level){
        if(!isFire(stack))return false;holdActive(stack,level);
        int damage=stack.getDamageValue()+1;
        if(damage>=stack.getMaxDamage())stack.shrink(1);else stack.setDamageValue(damage);
        return true;
    }
    @Override public void inventoryTick(ItemStack stack,Level level,net.minecraft.world.entity.Entity entity,int slot,boolean selected){recover(stack,level);}
    /** Basis points shifted out of ordinary quality. Every excess target tier divides by four. */
    public static int qualityBonus(ItemStack stack,int targetTier){return qualityBonus(rank(stack),targetTier);}
    public static int qualityBonus(int rank,int targetTier){
        return RefiningRules.fireQualityBonus(rank,targetTier);
    }
    public static String condense(ServerPlayer player){
        if(player.isSpectator())return "旁观模式不能凝聚灵火";
        var d=player.getCapability(CultivationCapability.DATA).orElse(null);
        if(d==null||!d.cultivationUnlocked())return "先修习养气诀，踏入修行后再凝聚灵火";
        if(d.power()<100)return "凝聚一缕灵火需要100灵力";
        if(player.getInventory().getFreeSlot()<0)return "背包需要一个空位存放灵火";
        var fire=new ItemStack(COMMON.get());
        if(!player.getInventory().add(fire))return "背包无法容纳灵火";
        d.setPower(d.power()-100);CultivationEvents.sync(player);
        return "";
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> text,TooltipFlag flag){
        text.add(Component.literal(rank==0?"任意灵根可凝聚；不分品阶":"天地奇火 · 遗迹宝箱中寻获"));
        text.add(Component.literal("放入丹炉或炼器炉；每秒炼制消耗1耐久"));
        text.add(Component.literal("剩余火力："+remaining(stack)+" / "+stack.getMaxDamage()));
        text.add(Component.literal(rank==0?"耗尽后消失，不会恢复":"闲置每5秒恢复1耐久；烧尽永久消失"));
        text.add(Component.literal(rank==0?"提供火源，不增加品质概率":"品质转移加成最高 "+(rank*1.5)+" 个百分点"));
        if(rank>0)text.add(Component.literal("目标每高一阶，加成降为四分之一"));
        text.add(Component.literal("灵石独立补充炉内储能"));
    }
}
