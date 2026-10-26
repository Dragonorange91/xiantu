package com.longju.xiantu.talisman;
import com.longju.xiantu.registry.ModItems;
import com.longju.xiantu.material.MaterialQuality;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;
public final class TalismanItems {
 public static final RegistryObject<Item> CINNABAR=com.longju.xiantu.mining.SpecialMineralRegistry.MATERIALS.get("cinnabar");
 public static final RegistryObject<Item> SPACE_SAND=ModItems.ITEMS.register("space_sand",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> RESIDUE=ModItems.ITEMS.register("talisman_residue",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> BROKEN_BRUSH=ModItems.ITEMS.register("broken_talisman_brush",()->new Item(new Item.Properties().stacksTo(1)));
 public static final List<RegistryObject<Item>> BRUSHES=new ArrayList<>();
 public static final RegistryObject<Item> PAPER=ModItems.ITEMS.register("talisman_paper",()->new Item(new Item.Properties()){
  @Override public Component getName(ItemStack s){return Component.literal(MaterialQuality.RANKS[tier(s)]+"符纸"+(MaterialQuality.get(s)>0?"·"+MaterialQuality.NAMES[MaterialQuality.get(s)]:""));}
 });
 public static final RegistryObject<Item> BLUEPRINT=ModItems.ITEMS.register("talisman_blueprint",()->new Item(new Item.Properties()){
  @Override public Component getName(ItemStack s){String id=TalismanBlueprints.key(s);return id.isEmpty()?super.getName(s):Component.literal(TalismanBlueprints.name(id));}
  @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){var s=p.getItemInHand(h);if(p instanceof net.minecraft.server.level.ServerPlayer sp)com.longju.xiantu.refining.BlueprintLearningMenu.open(sp,h);return InteractionResultHolder.sidedSuccess(s,l.isClientSide);}
  @Override public void appendHoverText(ItemStack s,Level l,List<Component> text,TooltipFlag f){String id=TalismanBlueprints.key(s);var tome=TalismanTomes.find(id);if(tome!=null){text.add(Component.literal(tome.description()));text.add(Component.literal("制作仍需满足符阶境界、符纸、妖丹及触媒要求"));}text.add(Component.literal("右键查看并确认学习；重复学习不消耗"));}
 });
 public static final Map<String,RegistryObject<Item>> SYMBOLS=new LinkedHashMap<>();
 static {
  for(int i=0;i<3;i++){final int b=i+1;BRUSHES.add(ModItems.ITEMS.register("talisman_brush_"+b,()->new Item(new Item.Properties().durability(new int[]{128,1024,8192}[b-1])){
   @Override public boolean isRepairable(ItemStack s){return false;}
   @Override public void appendHoverText(ItemStack s,Level l,List<Component> text,TooltipFlag f){text.add(Component.literal("可绘制一至八阶符箓；加成依符阶变化"));}
  }));}
  for(var d:TalismanCatalog.ALL)SYMBOLS.put(d.id(),ModItems.ITEMS.register("talisman_"+d.id(),()->d.id().equals("rebirth")?new com.longju.xiantu.item.ReincarnationStoneItem(new Item.Properties(),true):new Symbol(d)));
 }
 public static int tier(ItemStack s){if(s.is(SYMBOLS.get("rebirth").get()))return 3;return s.hasTag()?Math.max(1,Math.min(8,s.getTag().getInt("TalismanTier"))):1;}
 public static ItemStack paper(int tier,int quality,int count){if(tier<1||tier>8)throw new IllegalArgumentException("paper tier");var s=new ItemStack(PAPER.get(),count);s.getOrCreateTag().putInt("TalismanTier",tier);MaterialQuality.set(s,quality);return s;}
 public static ItemStack symbol(String id,int tier,int quality){var d=TalismanCatalog.find(id);if(d==null||tier<d.minimum()||tier>8||id.equals("rebirth")&&tier!=3)throw new IllegalArgumentException("talisman");var s=new ItemStack(SYMBOLS.get(id).get());s.getOrCreateTag().putInt("TalismanTier",tier);s.getTag().putInt("TalismanQuality",Math.max(1,Math.min(5,quality)));return s;}
 public static ItemStack blueprint(String id){if(TalismanTomes.find(id)==null)throw new IllegalArgumentException(id);var s=new ItemStack(BLUEPRINT.get());s.getOrCreateTag().putString("Talisman",id);s.getTag().putInt("CustomModelData",TalismanTomes.ALL.indexOf(TalismanTomes.find(id))+1);return s;}
 public static int brush(ItemStack s){for(int i=0;i<3;i++)if(s.is(BRUSHES.get(i).get()))return i+1;return 0;}
 public static final class Symbol extends Item {
  public final TalismanCatalog.Definition definition;
  Symbol(TalismanCatalog.Definition d){super(new Item.Properties().stacksTo(16));definition=d;}
  @Override public Component getName(ItemStack s){return Component.literal(MaterialQuality.RANKS[tier(s)]+definition.name()+"·"+MaterialQuality.NAMES[quality(s)]);}
  @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){var s=p.getItemInHand(h);if(p instanceof net.minecraft.server.level.ServerPlayer sp){String error=TalismanEffects.begin(sp,h,definition);if(!error.isEmpty()){p.displayClientMessage(Component.literal(error),true);return InteractionResultHolder.fail(s);}}return InteractionResultHolder.sidedSuccess(s,l.isClientSide);}
  @Override public void appendHoverText(ItemStack s,Level l,List<Component> text,TooltipFlag f){text.add(Component.translatable("tooltip.xiantu.talisman."+definition.id()));text.add(Component.literal("品质倍率 ×"+TalismanRules.QUALITY[quality(s)]+"；冷却 "+definition.cooldown()+"秒"));}
 }
 public static int quality(ItemStack s){return s.hasTag()?Math.max(1,Math.min(5,s.getTag().getInt("TalismanQuality"))):1;}
 public static void bootstrap(){}
 private TalismanItems(){}
}
