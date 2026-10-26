package com.longju.xiantu.spell;

import com.longju.xiantu.combat.Element;
import com.longju.xiantu.worldgen.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Versioned chest rewards run inside the existing one-time chest claim transaction. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="xiantu")
public final class SpellSources {
 public static List<SpellCatalog.Spell> beastPool(int min,int max,String element,ServerPlayer p){
  String normalized=List.of("metal","wood","water","fire","earth","thunder").contains(element)?element:"physical";
  return SpellCatalog.ALL.stream().filter(SpellCatalog::enabled).filter(s->s.tier()>=min&&s.tier()<=max)
   .filter(s->s.element().id().equals(normalized)).filter(s->!SpellService.data(p).knows(s.id())).toList();
 }
 private static ItemStack reward(ServerPlayer p,List<SpellCatalog.Spell> candidates,String source,String sourceId){
  if(candidates.isEmpty())return ItemStack.EMPTY;
  var s=SpellScrolls.stack(candidates.get(p.getRandom().nextInt(candidates.size())).id());var tag=s.getOrCreateTag();
  tag.putString("SpellSource",source);tag.putString("SourceUUID",sourceId);tag.putUUID("SpellTransaction",UUID.randomUUID());return s;
 }
 @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
 public static void elite(net.minecraftforge.event.entity.living.LivingDeathEvent e){
  if(e.isCanceled()||!(e.getSource().getEntity() instanceof ServerPlayer p)||p.isCreative()||p.isSpectator()||p instanceof net.minecraftforge.common.util.FakePlayer)return;
  var target=e.getEntity();var n=target.getPersistentData();
  if(!com.longju.xiantu.entity.BeastRealmService.isBeast(target)||!com.longju.xiantu.entity.BeastRealmService.valid(target)||!com.longju.xiantu.entity.BeastRealmService.elite(target)
    ||n.getBoolean(com.longju.xiantu.worldgen.RuinAdventureService.BOSS)||n.getBoolean("xiantu_spell_elite_rolled"))return;
  n.putBoolean("xiantu_spell_elite_rolled",true);
  var ledger=SpellRewardData.get(p);if(!ledger.available(p))return;
  int ceiling=Math.min(5,com.longju.xiantu.entity.BeastRealmService.realm(target)+1);
  var candidates=beastPool(3,ceiling,n.getString(com.longju.xiantu.entity.BeastRealmService.ELEMENT),p);
  if(candidates.isEmpty()||p.getRandom().nextDouble()>=com.longju.xiantu.config.XiantuConfig.SPELL_ELITE_CHANCE.get())return;
  var stack=reward(p,candidates,"elite_beast_v1",target.getUUID().toString());if(!stack.isEmpty()&&ledger.claim(p))target.spawnAtLocation(stack);
 }
 /** Called only inside RuinAdventureService's validated and committed BossLoot transaction. */
 public static ItemStack boss(ServerPlayer p,RuinAdventureData.Instance instance,net.minecraft.world.entity.LivingEntity boss){
  if(p.isCreative()||p.isSpectator()||!instance.state.getBoolean("BossLoot")||!instance.state.hasUUID("BossUUID")
    ||!instance.state.getUUID("BossUUID").equals(boss.getUUID())||instance.state.getBoolean("BossSpellRolled"))return ItemStack.EMPTY;
  instance.state.putBoolean("BossSpellRolled",true);RuinAdventureData.get(p.serverLevel()).setDirty();
  var n=boss.getPersistentData();int max=com.longju.xiantu.gongfa.GongfaEvents.target(boss).realmRank();
  var candidates=beastPool(5,Math.min(8,max),n.getString(com.longju.xiantu.entity.BeastRealmService.ELEMENT),p);
  if(p.getRandom().nextDouble()>=com.longju.xiantu.config.XiantuConfig.SPELL_BOSS_CHANCE.get())return ItemStack.EMPTY;
  return reward(p,candidates,"ruin_boss_v1",instance.id);
 }
 public static List<SpellCatalog.Spell> pool(int min,int max,String theme,ServerPlayer player){
  return SpellCatalog.ALL.stream().filter(SpellCatalog::enabled).filter(s->s.tier()>=min&&s.tier()<=max)
   .filter(s->theme.equals("generic")||s.element()==Element.PHYSICAL||s.element().id().equals(theme))
   .filter(s->player==null||!SpellService.data(player).knows(s.id()))
   .toList();
 }
 public static ItemStack chest(ServerPlayer p,RuinAdventureData.Instance i,String chest,String grade){
  if(p.isCreative()||p.isSpectator())return ItemStack.EMPTY;
  var ledger=RuinAdventureData.get(p.serverLevel());var attempts=i.state.getCompound("SpellRolls");
  if(attempts.contains(chest))return ItemStack.EMPTY;
  var transaction=UUID.randomUUID();attempts.putUUID(chest,transaction);i.state.put("SpellRolls",attempts);ledger.setDirty();
  String loot="";for(var v:i.spec.getAsJsonArray("markers")){var m=v.getAsJsonObject();if(m.has("id")&&m.get("id").getAsString().equals(chest)){loot=m.get("loot").getAsString();break;}}
  boolean core=grade.equals("core");String theme="generic";
  for(String value:List.of("metal","wood","water","fire","earth"))if(loot.endsWith("_"+value))theme=value;
  int tier=1;var matcher=java.util.regex.Pattern.compile("tier_(\\d)").matcher(loot);if(matcher.find())tier=Integer.parseInt(matcher.group(1));
  if(i.blueprint.equals("water_curtain_caverns_grand")){tier=6;theme="earth";}
  boolean high=core&&tier>=4;int min=high?4:2,max=high?Math.min(6,tier):3;
  var candidates=pool(min,max,theme,p);if(candidates.isEmpty()||high&&i.state.getBoolean("HighSpellAwarded"))return ItemStack.EMPTY;
  var state=p.getPersistentData();int misses=state.getInt("xiantu_spell_chest_misses");double probability=high?.12:misses>=10?.16:.08;
  if(p.getRandom().nextDouble()>=probability){if(!high)state.putInt("xiantu_spell_chest_misses",Math.min(10,misses+1));return ItemStack.EMPTY;}
  var chosen=candidates.get(p.getRandom().nextInt(candidates.size()));if(chosen.tier()>=5)i.state.putBoolean("HighSpellAwarded",true);
  if(!high)state.putInt("xiantu_spell_chest_misses",0);ledger.setDirty();
  var stack=SpellScrolls.stack(chosen.id());var tag=stack.getOrCreateTag();tag.putString("SpellSource","ruin_chest_v1");tag.putString("SourceUUID",i.id);tag.putUUID("SpellTransaction",transaction);return stack;
 }
 private SpellSources(){}
}
