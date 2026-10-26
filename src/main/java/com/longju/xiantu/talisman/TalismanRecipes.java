package com.longju.xiantu.talisman;
import com.longju.xiantu.material.MaterialQuality;
import com.longju.xiantu.wood.SpiritWoodType;
import net.minecraft.world.item.ItemStack;
import java.util.*;
public final class TalismanRecipes {
 public record Recipe(String id,String kind,int tier,int brush,List<TalismanCatalog.Ingredient> ingredients){
  public String knowledge(){return TalismanTomes.forTier(tier).id();}
  public int ticks(){return kind.equals("symbol")?TalismanRules.ticks(tier):kind.equals("paper")?200:60;}
  public int energy(){return kind.equals("symbol")?TalismanRules.energy(tier):kind.equals("paper")?TalismanRules.energy(tier)/2:0;}
  public int realm(){return kind.equals("brush")?new int[]{0,3,7}[brush-1]:TalismanRules.realm(tier);}
  public ItemStack output(int quality){return switch(kind){case "symbol"->TalismanItems.symbol(id,tier,quality);case "paper"->TalismanItems.paper(tier,quality,4);case "brush"->new ItemStack(TalismanItems.BRUSHES.get(brush-1).get());default->ItemStack.EMPTY;};}
 }
 public static final List<Recipe> ALL=build();
 private static List<Recipe> build(){var out=new ArrayList<Recipe>();
  for(var d:TalismanCatalog.ALL)for(int f=d.minimum();f<=(d.id().equals("rebirth")?3:8);f++)out.add(new Recipe(d.id(),"symbol",f,0,d.ingredients(f)));
  for(int f=1;f<=8;f++){
   String wood=SpiritWoodType.values()[f-1].id;
   out.add(new Recipe("paper_"+f,"paper",f,0,List.of(TalismanCatalog.ingredient(wood+"_fiber",2),TalismanCatalog.ingredient("minecraft:paper",2))));
  }
  out.add(new Recipe("brush_1","brush",1,1,List.of(TalismanCatalog.ingredient("qing_wen_elm_stick",1),TalismanCatalog.ingredient("minecraft:feather",2),TalismanCatalog.ingredient("beast_silk",1))));
  out.add(new Recipe("brush_2","brush",4,2,List.of(TalismanCatalog.ingredient("xuan_tie_pine_stick",1),new TalismanCatalog.Ingredient("xiantu:vulture_feather",2,4,""),TalismanCatalog.ingredient("beast_sinew",1))));
  out.add(new Recipe("brush_3","brush",7,3,List.of(TalismanCatalog.ingredient("xing_wen_wutong_stick",1),new TalismanCatalog.Ingredient("xiantu:falcon_feather",2,7,""),TalismanCatalog.ingredient("beast_sinew",1))));
  return List.copyOf(out);
 }
 public static String name(Recipe r){return r.output(1).getHoverName().getString();}
 private TalismanRecipes(){}
}
