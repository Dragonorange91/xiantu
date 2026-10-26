package com.longju.xiantu.talisman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import com.longju.xiantu.item.*;
import java.util.*;
/** Recipes resolve registered survival materials; a missing ingredient never becomes air. */
public final class TalismanCatalog {
 public record Ingredient(String id,int count,int grade,String element){
  public boolean matches(ItemStack s){var key=ForgeRegistries.ITEMS.getKey(s.getItem());return key!=null&&key.toString().equals(id)&&(grade==0||BeastMaterialItem.grade(s)==grade||s.getItem() instanceof DemonCoreItem&&DemonCoreItem.grade(s)==grade)&&(element.isEmpty()||(s.getItem() instanceof DemonCoreItem?DemonCoreItem.element(s):BeastMaterialItem.element(s)).equals(element));}
  public ItemStack example(){var item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));if(item==null||item==net.minecraft.world.item.Items.AIR)throw new IllegalStateException("Missing talisman ingredient "+id);var s=new ItemStack(item,count);if(grade>0){if(item instanceof DemonCoreItem)DemonCoreItem.setData(s,grade,element);else BeastMaterialItem.setData(s,grade,element);}return s;}
 }
 public record Definition(String id,String name,String category,int minimum,int complexity,String element,String catalysts,int cooldown){
  public List<Ingredient> ingredients(int tier){var out=new ArrayList<Ingredient>();out.add(new Ingredient("xiantu:cinnabar",2+(tier-1)/3,0,""));out.add(new Ingredient("xiantu:demon_core",1,tier,element));for(String id:catalysts.split(",")){boolean ranked=id.startsWith("@");id=id.replace("@","");String element=id.endsWith(":wood")?"wood":"";id=id.replace(":wood","");out.add(new Ingredient((id.contains(":")?"":"xiantu:")+id,ranked?1:2+(tier-1)/4,ranked?tier:0,element));}return List.copyOf(out);}
  public int strokes(){return 3+complexity*2;}
  public String base(){return switch(category){case "attack"->"fireball";case "defense"->"diamond";case "support"->"lightbody";case "healing"->"healing";case "seal"->"soul_seal";default->"earth_escape";};}
 }
 public static final List<Definition> ALL=List.of(
  new Definition("fireball","火球符","attack",1,0,"fire","flame_heart_stone,beast_fang",3),
  new Definition("thunder","雷击符","attack",3,1,"thunder","thunder_stone,@thunder_crow_feather",5),
  new Definition("sword_qi","剑气符","attack",2,0,"metal","beast_fang,purple_pattern_crystal",4),
  new Definition("explosion","爆裂符","attack",3,1,"fire","cinnabar,minecraft:gunpowder",6),
  new Definition("diamond","金刚符","defense",1,0,"earth","@beast_carapace,beast_bone",20),
  new Definition("ward","护身符","defense",1,0,"earth","@beast_carapace",20),
  new Definition("reflection","反伤符","defense",4,1,"metal","beast_scale,@beast_carapace",30),
  new Definition("lightbody","轻身符","support",1,0,"wood","@beast_feather",30),
  new Definition("speed","神行符","support",2,0,"wood","beast_sinew,@beast_feather",30),
  new Definition("invisibility","隐身符","support",3,1,"water","moonlit_stone,beast_silk",60),
  new Definition("concealment","敛息符","support",2,0,"water","beast_silk",30),
  new Definition("healing","回春符","healing",1,0,"wood","@beast_blood_sac:wood,herb_0007",20),
  new Definition("detox","解毒符","healing",1,0,"wood","beast_venom_sac,herb_0029",15),
  new Definition("cleanse","净体符","healing",3,1,"water","beast_venom_sac,cold_soul_crystal",30),
  new Definition("soul_seal","镇魂符","seal",3,1,"earth","beast_horn,purple_pattern_crystal",15),
  new Definition("spirit_lock","锁灵符","seal",4,1,"metal","beast_silk,beast_horn",20),
  new Definition("demon_seal","封魔符","seal",5,2,"thunder","inner_demon_crystal,thunder_stone",25),
  new Definition("trap","困阵符","seal",3,1,"earth","beast_silk,lodestone_shard",20),
  new Definition("earth_escape","遁地符","teleport",2,1,"earth","lodestone_shard,beast_bone",30),
  new Definition("blink","瞬移符","teleport",3,1,"water","space_sand,moonlit_stone",30),
  new Definition("long_return","千里符","teleport",6,2,"water","void_crystal,beast_horn",300),
  new Definition("rebirth","转生符","healing",3,1,"wood","herb_0008,beast_silk",0));
 public static final Set<String> BASIC=Set.of("fireball","diamond","lightbody","healing","detox");
 public static Definition find(String id){return ALL.stream().filter(x->x.id.equals(id)).findFirst().orElse(null);}
 public static Ingredient ingredient(String id,int count){return new Ingredient(id.contains(":")?id:"xiantu:"+id,count,0,"");}
 private TalismanCatalog(){}
}
