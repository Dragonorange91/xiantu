package com.longju.xiantu.client;
import com.lowdragmc.photon.client.fx.*;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import com.longju.xiantu.network.SkillVfxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;
/** Shared preloaded graphs; bounded runtimes with no block/entity global cache. */
final class SkillPhotonRuntime {
 private static final Set<String> SKILLS=Set.of("fire_bolt","thunder_strike","staff_charge","staff_extend","staff_quake","cloud_step","great_sage_form","seventy_two_forms","weapon_fire","weapon_earth","weapon_metal","weapon_charge");
 private static final Map<String,Active> ACTIVE=new HashMap<>();
 private static final Map<BlockPos,Active> FORMATIONS=new HashMap<>();
 private static final Set<String> WARNED=new HashSet<>();
 private static ClientLevel level;
 private static final class Active {Mobile effect;long expires,last;Entity owner;String stage;Active(Mobile e,Entity o,long expiry,String s){effect=e;owner=o;expires=expiry;stage=s;}}
 private static final class Mobile extends FXEffect {
  Vec3 previous,position,local;Entity owner;long updated;final FX asset;
  Mobile(FX fx,ClientLevel l,Vec3 at,Vec3 dir,Entity follow,float size){super(fx,l);asset=fx;position=previous=at;owner=follow;local=follow==null?Vec3.ZERO:at.subtract(follow.position());updated=l.getGameTime();setScale(new Vector3f(size));setRotation(new Quaternionf().rotateY((float)Math.atan2(dir.x,dir.z)));}
  public void start(){runtime=asset.createRuntime();root(1);runtime.emmit(this);}
  void move(Vec3 at){previous=position;position=at;updated=getLevel().getGameTime();}
  private void root(float partial){if(runtime==null)return;Vec3 p=owner==null?previous.lerp(position,Math.min(1,Math.max(0,(getLevel().getGameTime()-updated+partial)/2.0))):owner.getPosition(partial).add(local);runtime.getRoot().updatePos(new Vector3f((float)p.x,(float)p.y,(float)p.z));runtime.getRoot().updateRotation(rotation);runtime.getRoot().updateScale(scale);}
  @Override public void updateFXObjectTick(IFXObject o){root(1);}
  @Override public void updateFXObjectFrame(IFXObject o,float partial){root(partial);}
 }
 private static boolean world(){var l=Minecraft.getInstance().level;if(l!=level){clear();level=l;}return level!=null;}
 static void receive(SkillVfxPacket p){
  var mc=Minecraft.getInstance();if(!world()||mc.player==null||!level.dimension().location().equals(p.dimension())||!(SKILLS.contains(p.skill())||com.longju.xiantu.refining.WeaponActives.SKILLS.contains(p.skill())||p.skill().startsWith("talisman_")&&com.longju.xiantu.talisman.TalismanCatalog.find(p.skill().substring(9))!=null)||!Double.isFinite(p.position().lengthSqr())||!Double.isFinite(p.direction().lengthSqr())||mc.player.distanceToSqr(p.position())>96*96)return;
  Entity owner=level.getEntity(p.entityId());if(owner==null||!owner.getUUID().equals(p.owner()))return;
  String key=p.transaction().toString();String phase=p.phase();
  if(phase.equals("FLIGHT")){var a=ACTIVE.get(key+":flight");if(a!=null)a.effect.move(p.position());return;}
  boolean weapon=com.longju.xiantu.refining.WeaponActives.SKILLS.contains(p.skill());
  if(phase.equals("CANCEL")||phase.equals("END")){remove(key+":charge");remove(key+":flight");return;}
  boolean start=phase.equals("WINDUP")||phase.equals("START"),release=phase.equals("RELEASE"),hit=phase.equals("HIT");
  if(!start&&!release&&!hit)return;
  if(release)remove(key+":charge");
  String skill=p.skill().startsWith("talisman_")?talismanVisual(p.skill().substring(9)):p.skill().equals("staff_charge")?"weapon_charge":p.skill();
  String stage=start?"charge":hit?"impact":skill.equals("fire_bolt")?"flight":"release";
  if(hit&&skill.startsWith("staff_"))skill="weapon_metal";
  if(hit&&!p.skill().startsWith("talisman_")&&!com.longju.xiantu.refining.WeaponActives.SKILLS.contains(skill)&&!Set.of("fire_bolt","thunder_strike","weapon_fire","weapon_earth","weapon_metal","staff_extend","staff_quake").contains(skill))return;
  // Non-spell windups animate the caster, but must not play their release blast twice.
  if(start&&!Set.of("fire_bolt","thunder_strike").contains(skill))return;
  // World-space weapon paths own release geometry; Photon adds only short contact bursts.
  if(weapon&&release)return;
  ResourceLocation id=new ResourceLocation("xiantu","skills/"+skill+"/"+stage);if(BeastPhotonRuntime.preloaded(id)==null&&p.skill().startsWith("talisman_")){stage="release";id=new ResourceLocation("xiantu","skills/"+skill+"/"+stage);}
  Vec3 at=start?owner.position():p.position();if(skill.equals("staff_quake")&&!hit)at=owner.position();Entity follow=start?owner:null;
  add(key+":"+(start?"charge":stage)+(hit?":"+BlockPos.containing(p.position()).asLong():""),id,at,p.direction(),follow,owner,start?60:stage.equals("flight")?40:40,1,stage);
 }
 private static String talismanVisual(String id){return switch(id){
  case "fireball"->"fire_bolt";case "thunder"->"thunder_strike";case "sword_qi"->"shadow_sever";case "explosion"->"staff_quake";
  case "diamond","ward"->"forged_guard";case "reflection"->"celestial_guard";
  case "healing","detox","cleanse"->"radiant_return";case "soul_seal","spirit_lock","demon_seal","trap"->"chaos_suppress";
  case "earth_escape","blink","long_return"->"cloud_step";default->"seventy_two_forms";};}
 private static Active add(String key,ResourceLocation id,Vec3 at,Vec3 dir,Entity follow,Entity owner,int ticks,float size,String stage){
  remove(key);if(ACTIVE.size()+FORMATIONS.size()>=64){var oldest=ACTIVE.entrySet().stream().min(java.util.Comparator.comparingLong(e->e.getValue().expires));if(oldest.isPresent())remove(oldest.get().getKey());else return null;}
  FX fx=BeastPhotonRuntime.preloaded(id);if(fx==null){if(WARNED.add(id.toString()))com.mojang.logging.LogUtils.getLogger().error("[Xiantu Skill FX] Missing preloaded {}",id);return null;}
  try{var effect=new Mobile(fx,level,at,dir,follow,size);effect.start();var active=new Active(effect,owner,level.getGameTime()+ticks,stage);ACTIVE.put(key,active);return active;}
  catch(RuntimeException ex){if(WARNED.add(id.toString()))com.mojang.logging.LogUtils.getLogger().error("[Xiantu Skill FX] {}",id,ex);return null;}
 }
 static void formation(BlockPos pos,int tier){
  var mc=Minecraft.getInstance();if(!world()||mc.player==null||mc.player.distanceToSqr(Vec3.atCenterOf(pos))>48*48)return;
  var current=FORMATIONS.get(pos);if(current!=null){if(level.getGameTime()<current.expires&&current.effect.getRuntime()!=null&&current.effect.getRuntime().isAlive()){current.last=level.getGameTime();return;}destroy(current);FORMATIONS.remove(pos);}if(FORMATIONS.size()>=8)return;
  String key="formation:"+pos.asLong();
  var a=add(key,new ResourceLocation("xiantu","formation/gather"),Vec3.atBottomCenterOf(pos).add(0,1,0),new Vec3(0,0,1),null,null,1100,(float)(com.longju.xiantu.formation.FormationLayout.outerRadius(tier)/2.5),"formation");
  if(a!=null){ACTIVE.remove(key);a.last=level.getGameTime();FORMATIONS.put(pos.immutable(),a);}
 }
 private static void destroy(Active a){if(a.effect.getRuntime()!=null)a.effect.getRuntime().destroy(true);}
 private static void remove(String key){var a=ACTIVE.remove(key);if(a!=null)destroy(a);}
 static void tick(){if(!world())return;long t=level.getGameTime();ACTIVE.values().removeIf(a->{if(t>=a.expires||a.owner!=null&&(a.owner.isRemoved()||!a.owner.isAlive())||a.effect.getRuntime()==null||!a.effect.getRuntime().isAlive()){destroy(a);return true;}return false;});
  FORMATIONS.entrySet().removeIf(e->{var p=e.getKey();var a=e.getValue();var mc=Minecraft.getInstance();if(t-a.last>3||t>=a.expires||!level.hasChunkAt(p)||mc.player==null||mc.player.distanceToSqr(Vec3.atCenterOf(p))>48*48){destroy(a);return true;}return false;});}
 static void clear(){ACTIVE.values().forEach(SkillPhotonRuntime::destroy);FORMATIONS.values().forEach(SkillPhotonRuntime::destroy);ACTIVE.clear();FORMATIONS.clear();WARNED.clear();level=null;}
 private SkillPhotonRuntime(){}
}
