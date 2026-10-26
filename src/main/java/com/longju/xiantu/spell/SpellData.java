package com.longju.xiantu.spell;
import net.minecraft.nbt.*;
import java.util.*;

/** Part of the cultivation capability; learning, loadout, mastery and cooldown survive death. */
public final class SpellData {
 public static final int ACTIVE_SLOTS=4,PASSIVE_SLOTS=2,TOTAL_SLOTS=ACTIVE_SLOTS+PASSIVE_SLOTS;
 private CompoundTag state=new CompoundTag();
 public int revision(){return state.getInt("Revision");}
 public boolean hudVisible(){return !state.getBoolean("HideHud");}
 public void toggleHud(){state.putBoolean("HideHud",hudVisible());}
 public boolean knows(String id){return SpellCatalog.find(id)!=null&&state.getCompound("Known").getBoolean(id);}
 public boolean learn(String id){if(SpellCatalog.find(id)==null||knows(id))return false;var k=state.getCompound("Known");k.putBoolean(id,true);state.put("Known",k);state.putInt("Revision",revision()+1);return true;}
 public String slot(int slot){String id=slot<0||slot>=TOTAL_SLOTS?"":state.getCompound("Slots").getString("s"+slot);return SpellCatalog.find(id)==null?"":id;}
 public boolean configure(int revision,List<String> ids){
  if(revision!=revision()||ids.size()!=TOTAL_SLOTS)return false;Set<String> unique=new HashSet<>();
  for(int i=0;i<TOTAL_SLOTS;i++){String id=ids.get(i);if(id.isEmpty())continue;var s=SpellCatalog.find(id);if(s==null||!knows(id)||s.active()!=(i<4)||!unique.add(id))return false;}
  var slots=new CompoundTag();for(int i=0;i<TOTAL_SLOTS;i++)slots.putString("s"+i,ids.get(i));state.put("Slots",slots);state.putInt("Revision",revision()+1);return true;
 }
 public int mastery(String id){return Math.min(9,Math.max(0,state.getCompound("XP").getInt(id+"Level")));}
 public int experience(String id){return state.getCompound("XP").getInt(id+"Exp")/1000;}
 public void award(String id,int milli,long now,UUID cast,UUID target){
  if(!knows(id)||mastery(id)>=9||milli<=0)return;var x=state.getCompound("XP");
  if(x.getLong(id+"Next")>now||x.hasUUID(id+"Cast")&&x.getUUID(id+"Cast").equals(cast))return;
  if(target!=null&&x.hasUUID(id+"Target")&&x.getUUID(id+"Target").equals(target)&&x.getLong(id+"TargetNext")>now)return;
  int level=mastery(id),exp=x.getInt(id+"Exp")+milli;
  while(level<9&&exp>=SpellRules.EXPERIENCE[level]*1000){exp-=SpellRules.EXPERIENCE[level]*1000;level++;}
  x.putInt(id+"Level",level);x.putInt(id+"Exp",level==9?0:exp);x.putLong(id+"Next",now+100);x.putUUID(id+"Cast",cast);
  if(target!=null){x.putUUID(id+"Target",target);x.putLong(id+"TargetNext",now+100);}state.put("XP",x);
 }
 public long cooldown(String id,long now){return Math.max(0,state.getCompound("Cooldowns").getLong(id)-now);}
 public void setCooldown(String id,long deadline){var c=state.getCompound("Cooldowns");c.putLong(id,deadline);state.put("Cooldowns",c);}
 public CompoundTag save(){return state.copy();}
 public void load(CompoundTag n){
  state=n.copy();
  for(String section:List.of("Known","Cooldowns","XP")){
   var tag=state.getCompound(section);
   for(String key:new ArrayList<>(tag.getAllKeys()))if(SpellCatalog.ALL.stream().noneMatch(s->section.equals("XP")?key.startsWith(s.id()):key.equals(s.id())))tag.remove(key);
  }
  var slots=state.getCompound("Slots");slots.remove("s6");slots.remove("s7");for(String key:new ArrayList<>(slots.getAllKeys()))if(!slots.getString(key).isEmpty()&&SpellCatalog.find(slots.getString(key))==null)slots.remove(key);
 }
 public void copyFrom(SpellData other){load(other.save());}
}
