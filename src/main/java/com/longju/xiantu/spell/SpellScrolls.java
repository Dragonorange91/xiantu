package com.longju.xiantu.spell;
import com.longju.xiantu.registry.ModItems;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

public final class SpellScrolls {
 public static final Map<String,RegistryObject<Item>> ITEMS=register();
 private static Map<String,RegistryObject<Item>> register(){var m=new LinkedHashMap<String,RegistryObject<Item>>();for(var s:SpellCatalog.ALL)if(SpellCatalog.enabled(s))m.put(s.id(),ModItems.ITEMS.register("spell_scroll_"+s.id(),()->new Scroll(s)));return Collections.unmodifiableMap(m);}
 public static void bootstrap(){}
 public static ItemStack stack(String id){if(!ITEMS.containsKey(id))return ItemStack.EMPTY;var s=new ItemStack(ITEMS.get(id).get());s.getOrCreateTag().putInt("SpellVersion",1);s.getOrCreateTag().putString("BlueprintId","xiantu:spell/"+id);return s;}
 public static final class Scroll extends Item {
  private final SpellCatalog.Spell spell;
  Scroll(SpellCatalog.Spell spell){super(new Properties());this.spell=spell;}
  @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
   if(spell.id().equals("rejuvenation")){lines.add(Component.literal("练气可学；装备到被动槽后自动生效"));lines.add(Component.literal("血量不高于30%时恢复最大生命10%起"));lines.add(Component.literal("每级熟练多恢复2.5%，最高32.5%；冷却60秒"));lines.add(Component.literal("消耗8%灵力；不能抵挡致命伤害"));}
   if(spell.id().equals("great_fireball")){lines.add(Component.literal("金丹可学；长按法术键凝聚，松开发射"));lines.add(Component.literal("约5秒蓄满，直径最大7格；越大伤害越高"));lines.add(Component.literal("灵力消耗随蓄力增加；范围伤害，不破坏地形"));}
   if(spell.id().equals("sword_flight")){lines.add(Component.literal("筑基可学；在面前地面凝聚蓝色灵剑"));lines.add(Component.literal("右键登剑，自动第三人称站立飞行"));lines.add(Component.literal("无冷却，再次施法回收灵剑"));lines.add(Component.literal("空格上升，潜行键下降；双击潜行键下剑"));lines.add(Component.literal("移动键控制方向；双击前进或疾跑键加速"));lines.add(Component.literal("筑基至金丹约可常速飞行5分钟；加速耗灵翻倍"));lines.add(Component.literal("实际驾剑飞行每10秒积累熟练度"));}
  }
  @Override public InteractionResultHolder<ItemStack> use(Level l,Player player,InteractionHand hand){
   var item=player.getItemInHand(hand);
   if(player instanceof ServerPlayer p){String error=SpellService.denial(p,spell,false);
    if(item.getOrCreateTag().getInt("SpellVersion")!=1||!item.getOrCreateTag().getString("BlueprintId").equals("xiantu:spell/"+spell.id()))error="卷轴版本或编号无效";
    if(error.isEmpty()&&SpellService.data(p).knows(spell.id()))error="已学会此法术，卷轴未消耗";
    if(error.isEmpty()){SpellService.data(p).learn(spell.id());item.shrink(1);error="已学会"+spell.name()+"，按K配置法术";SpellService.sync(p);}
    p.sendSystemMessage(Component.literal(error));
   }return InteractionResultHolder.sidedSuccess(item,l.isClientSide);
  }
 }
 private SpellScrolls(){}
}
