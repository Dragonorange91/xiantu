package com.longju.xiantu.talisman;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import java.util.*;
public final class TalismanBlueprints {
 public static String key(ItemStack s){if(!s.is(TalismanItems.BLUEPRINT.get())||!s.hasTag())return "";String id=s.getTag().getString("Talisman");return TalismanTomes.find(id)!=null?id:"";}
 public static String name(String id){var tome=TalismanTomes.find(id);return tome==null?"":tome.name();}
 public static List<TalismanCatalog.Ingredient> ingredients(String id){return List.of();}
 public static String learn(Player p,ItemStack s){
  String id=key(s);if(id.isEmpty())return "无效符箓典籍";
  if(TalismanProgress.knows(p,id))return "已经掌握，不消耗物品";
  TalismanProgress.learn(p,id);s.shrink(1);p.getInventory().setChanged();return "";
 }
 /** Copies are planned before any live inventory changes, including duplicate ingredients. */
 public static List<ItemStack> take(Player p,List<TalismanCatalog.Ingredient> ingredients){var out=new ArrayList<ItemStack>();for(int i=0;i<36;i++)out.add(p.getInventory().getItem(i).copy());for(var ingredient:ingredients){int remaining=ingredient.count();for(var s:out)if(ingredient.matches(s)){int n=Math.min(remaining,s.getCount());s.shrink(n);remaining-=n;if(remaining==0)break;}if(remaining>0)return null;}return out;}
 private TalismanBlueprints(){}
}
