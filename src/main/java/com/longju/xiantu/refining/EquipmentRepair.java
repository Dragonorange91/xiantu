package com.longju.xiantu.refining;

import com.longju.xiantu.mining.MetalMaterialRegistry;
import com.longju.xiantu.mining.MiningPickaxeItem;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import java.util.*;

/** Pure preview on copied stacks. The menu alone commits input and power payment. */
public final class EquipmentRepair {
 public record Result(ItemStack stack,long cost,String error){}
 public static int tier(ItemStack stack){var r=RefinedEquipment.recipe(stack);if(r!=null)return r.tier();if(!(stack.getItem() instanceof MiningPickaxeItem))return 0;var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());return id==null?0:switch(id.getPath()){case "hanxing_pickaxe"->1;case "jiuyou_pickaxe"->4;case "hundun_pickaxe"->7;default->0;};}
 public static Result preview(ItemStack left,ItemStack right){
  int tier=tier(left);if(tier<1||tier>8||left.getCount()!=1||!left.isDamageableItem()||RefinedEquipment.infinite(left))return bad("左槽需要有耐久的正式装备或修仙镐");
  if(left.getDamageValue()<=0)return bad("装备耐久已满");if(right.isEmpty())return bad("右槽放同阶主材，或相同装备");
  var out=left.copy();int restored;
  if(right.is(MetalMaterialRegistry.INGOTS.get(MetalMaterialRegistry.ALL.get(tier-1).id()).get()))restored=Math.max(1,left.getMaxDamage()/4);
  else if(right.getItem()==left.getItem()&&tier(right)==tier&&right.isDamageableItem()&&!RefinedEquipment.infinite(right)){
   restored=right.getMaxDamage()-right.getDamageValue();if(restored<=0)return bad("右槽装备没有可合并耐久");
   var merged=new LinkedHashMap<>(EnchantmentHelper.getEnchantments(left));
   for(var entry:EnchantmentHelper.getEnchantments(right).entrySet()){
    var enchant=entry.getKey();for(var old:merged.keySet())if(old!=enchant&&!old.isCompatibleWith(enchant))return bad("两件装备的附魔互相冲突");
    int previous=merged.getOrDefault(enchant,0),next=entry.getValue();merged.put(enchant,Math.min(enchant.getMaxLevel(),previous==next?next+1:Math.max(previous,next)));
   }
   EnchantmentHelper.setEnchantments(merged,out);
  }else return bad("主材阶位或装备编号不匹配");
  out.setDamageValue(Math.max(0,left.getDamageValue()-restored));return new Result(out,RefiningRules.energy(tier)*100L,"");
 }
 private static Result bad(String reason){return new Result(ItemStack.EMPTY,0,reason);}
 private EquipmentRepair(){}
}
