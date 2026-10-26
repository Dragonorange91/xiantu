from pathlib import Path
import shutil
R=Path(__file__).resolve().parents[1]
J=R/'src/main/java/com/longju/xiantu'
B=R/'asset_sources/presentation-20261024/before'
def write(path,text):
 p=R/path
 if p.exists():
  b=B/path
  if not b.exists():b.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,b)
 p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf8')
def java(path,text):write(Path('src/main/java/com/longju/xiantu')/path,text)
def edit(path,a,b):
 p=J/path;s=p.read_text(encoding='utf8')
 if b in s:return
 assert a in s,(path,a[:70]);java(path,s.replace(a,b))
java('network/SkillVfxPacket.java','''package com.longju.xiantu.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;
/** Presentation only. No client packet can authorize a hit. */
public record SkillVfxPacket(UUID transaction,UUID owner,int entityId,ResourceLocation dimension,
 String skill,String phase,Vec3 position,Vec3 direction) {
 public static void encode(SkillVfxPacket p,FriendlyByteBuf b){b.writeUUID(p.transaction);b.writeUUID(p.owner);b.writeVarInt(p.entityId);b.writeResourceLocation(p.dimension);b.writeUtf(p.skill,64);b.writeUtf(p.phase,16);b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);b.writeDouble(p.direction.x);b.writeDouble(p.direction.y);b.writeDouble(p.direction.z);}
 public static SkillVfxPacket decode(FriendlyByteBuf b){return new SkillVfxPacket(b.readUUID(),b.readUUID(),b.readVarInt(),b.readResourceLocation(),b.readUtf(64),b.readUtf(16),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()));}
 public static void handle(SkillVfxPacket p,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.longju.xiantu.client.SkillPresentationClient.receive(p)));c.get().setPacketHandled(true);}
}
''')
java('spell/SkillPresentation.java','''package com.longju.xiantu.spell;
import com.longju.xiantu.network.*;
import com.longju.xiantu.refining.EquipmentSkillEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import java.util.UUID;
/** Read-only bridge: the authoritative services remain the sole damage owners. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class SkillPresentation {
 public static void emit(ServerPlayer p,UUID tx,String skill,String phase,Vec3 at,Vec3 direction){
  if(p==null||!Double.isFinite(at.lengthSqr())||!Double.isFinite(direction.lengthSqr()))return;
  var packet=new SkillVfxPacket(tx,p.getUUID(),p.getId(),p.level().dimension().location(),skill,phase,at,direction);
  XiantuNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p),packet);
 }
 @SubscribeEvent public static void spell(SpellLifecycleEvent e){
  Vec3 at=e.position;
  if(e.phase==SpellLifecycleEvent.Phase.RELEASE){
   if(e.spellId.equals("fire_bolt"))at=e.caster.getEyePosition();
   else if(e.target!=null&&e.caster.serverLevel().getEntity(e.target) instanceof net.minecraft.world.entity.LivingEntity t)at=t.position();
  }
  emit(e.caster,e.transaction,e.spellId,e.phase.name(),at,e.direction);
 }
 @SubscribeEvent public static void equipment(EquipmentSkillEvent e){emit(e.owner,e.transaction,e.skill,e.phase.name(),e.position,e.direction);}
 private SkillPresentation(){}
}
''')
edit('network/XiantuNetwork.java','PROTOCOL = "21"','PROTOCOL = "22"')
edit('network/XiantuNetwork.java','public static void register() {','''public static void register() {
        CHANNEL.messageBuilder(SkillVfxPacket.class,nextId++,net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT).encoder(SkillVfxPacket::encode).decoder(SkillVfxPacket::decode).consumerMainThread(SkillVfxPacket::handle).add();''')
edit('spell/SpellLifecycleEvent.java','WINDUP,RELEASE,HIT,CANCEL','WINDUP,RELEASE,FLIGHT,HIT,END,CANCEL')
edit('spell/SpellService.java','if(wall||(f.age+1)*1.25>=f.spell.range())FLIGHTS.remove(i);else FLIGHTS.set',
'''if(f.age%2==0)notify(p,f.spell,f.transaction,null,SpellLifecycleEvent.Phase.FLIGHT,end,f.direction);
   if(wall||(f.age+1)*1.25>=f.spell.range()){notify(p,f.spell,f.transaction,null,SpellLifecycleEvent.Phase.END,end,f.direction);FLIGHTS.remove(i);}else FLIGHTS.set''')
edit('client/BeastPhotonRuntime.java','static void install(', 'static FX preloaded(net.minecraft.resources.ResourceLocation id){return PRELOADED.get(id);}\n    static void install(')
java('client/SkillPresentationClient.java','''package com.longju.xiantu.client;
import com.longju.xiantu.network.SkillVfxPacket;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fml.ModList;
public final class SkillPresentationClient {
 public static void receive(SkillVfxPacket p){StaffSkillRenderer.receive(p);if(ModList.get().isLoaded("photon"))SkillPhotonRuntime.receive(p);}
 public static void formation(BlockPos p,int tier){if(ModList.get().isLoaded("photon"))SkillPhotonRuntime.formation(p,tier);}
 public static void clear(){StaffSkillRenderer.clear();SkillCastAnimations.clear();if(ModList.get().isLoaded("photon"))SkillPhotonRuntime.clear();}
 private SkillPresentationClient(){}
}
''')
java('client/SkillPhotonRuntime.java','''package com.longju.xiantu.client;
import com.lowdragmc.photon.client.fx.*;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import com.longju.xiantu.network.SkillVfxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
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
  var mc=Minecraft.getInstance();if(!world()||mc.player==null||!level.dimension().location().equals(p.dimension())||!SKILLS.contains(p.skill())||!Double.isFinite(p.position().lengthSqr())||!Double.isFinite(p.direction().lengthSqr())||mc.player.distanceToSqr(p.position())>96*96)return;
  Entity owner=level.getEntity(p.entityId());if(owner==null||!owner.getUUID().equals(p.owner()))return;
  String key=p.transaction().toString();String phase=p.phase();
  if(phase.equals("FLIGHT")){var a=ACTIVE.get(key+":flight");if(a!=null)a.effect.move(p.position());return;}
  if(phase.equals("CANCEL")||phase.equals("END")){remove(key+":charge");remove(key+":flight");SkillCastAnimations.end(owner.getUUID());return;}
  boolean start=phase.equals("WINDUP")||phase.equals("START"),release=phase.equals("RELEASE"),hit=phase.equals("HIT");
  if(!start&&!release&&!hit)return;
  if(release){remove(key+":charge");SkillCastAnimations.release(owner.getUUID());}
  if(start)SkillCastAnimations.begin(owner.getUUID(),p.skill());
  String skill=p.skill().equals("staff_charge")?"weapon_charge":p.skill();
  String stage=start?"charge":hit?"impact":skill.equals("fire_bolt")?"flight":"release";
  if(hit&&skill.startsWith("staff_"))skill="weapon_metal";
  if(hit&&!Set.of("fire_bolt","thunder_strike","weapon_fire","weapon_earth","weapon_metal","staff_extend","staff_quake").contains(skill))return;
  if(start&&!Set.of("fire_bolt","thunder_strike").contains(skill))stage="release";
  ResourceLocation id=new ResourceLocation("xiantu","skills/"+skill+"/"+stage);
  Vec3 at=start?owner.position():p.position();if(skill.equals("staff_quake")&&!hit)at=owner.position();Entity follow=start?owner:null;
  add(key+":"+(start?"charge":stage),id,at,p.direction(),follow,owner,start?60:stage.equals("flight")?40:40,1,stage);
 }
 private static Active add(String key,ResourceLocation id,Vec3 at,Vec3 dir,Entity follow,Entity owner,int ticks,float size,String stage){
  remove(key);if(ACTIVE.size()+FORMATIONS.size()>=64)return null;
  FX fx=BeastPhotonRuntime.preloaded(id);if(fx==null){if(WARNED.add(id.toString()))com.mojang.logging.LogUtils.getLogger().error("[Xiantu Skill FX] Missing preloaded {}",id);return null;}
  try{var effect=new Mobile(fx,level,at,dir,follow,size);effect.start();var active=new Active(effect,owner,level.getGameTime()+ticks,stage);ACTIVE.put(key,active);return active;}
  catch(RuntimeException ex){if(WARNED.add(id.toString()))com.mojang.logging.LogUtils.getLogger().error("[Xiantu Skill FX] {}",id,ex);return null;}
 }
 static void formation(BlockPos pos,int tier){
  var mc=Minecraft.getInstance();if(!world()||mc.player==null||mc.player.distanceToSqr(Vec3.atCenterOf(pos))>48*48)return;
  var current=FORMATIONS.get(pos);if(current!=null){current.last=level.getGameTime();return;}if(FORMATIONS.size()>=8)return;
  String key="formation:"+pos.asLong();
  var a=add(key,new ResourceLocation("xiantu","formation/gather"),Vec3.atBottomCenterOf(pos).add(0,1,0),new Vec3(0,0,1),null,null,1100,(float)(com.longju.xiantu.formation.FormationLayout.outerRadius(tier)/2.5),"formation");
  if(a!=null){ACTIVE.remove(key);a.last=level.getGameTime();FORMATIONS.put(pos.immutable(),a);}
 }
 private static void destroy(Active a){if(a.effect.getRuntime()!=null)a.effect.getRuntime().destroy(true);}
 private static void remove(String key){var a=ACTIVE.remove(key);if(a!=null)destroy(a);}
 static void tick(){if(!world())return;long t=level.getGameTime();ACTIVE.values().removeIf(a->{if(t>=a.expires||a.owner!=null&&(a.owner.isRemoved()||!a.owner.isAlive())||a.effect.getRuntime()==null||!a.effect.getRuntime().isAlive()){destroy(a);return true;}return false;});
  FORMATIONS.entrySet().removeIf(e->{var p=e.getKey();var a=e.getValue();var mc=Minecraft.getInstance();if(t-a.last>3||t>=a.expires||!level.hasChunkAt(p)||mc.player==null||mc.player.distanceToSqr(Vec3.atCenterOf(p))>48*48){destroy(a);return true;}return false;});SkillCastAnimations.tick();}
 static void clear(){ACTIVE.values().forEach(SkillPhotonRuntime::destroy);FORMATIONS.values().forEach(SkillPhotonRuntime::destroy);ACTIVE.clear();FORMATIONS.clear();WARNED.clear();level=null;SkillCastAnimations.clear();}
 private SkillPhotonRuntime(){}
}
''')
edit('client/BeastPhotonClient.java','BeastPhotonRuntime.tick(); BossPhotonTimeline.tick();','BeastPhotonRuntime.tick(); BossPhotonTimeline.tick(); SkillPhotonRuntime.tick();')
edit('client/BeastPhotonClient.java','public static void clear() {','public static void clear() { SkillPresentationClient.clear();')
edit('client/BeastPhotonPreload.java','BeastPhotonRuntime.install(loaded);BossPhotonTimeline.clear();','SkillPresentationClient.clear();BeastPhotonRuntime.install(loaded);BossPhotonTimeline.clear();')
p=J/'formation/FormationCoreBlockEntity.java';s=p.read_text(encoding='utf8');a=s.index('        double cx =',s.index('public static void clientTick'));b=s.index('\n    public static void serverTick',a)
java('formation/FormationCoreBlockEntity.java',s[:a]+'''        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
            () -> () -> com.longju.xiantu.client.SkillPresentationClient.formation(pos,core.tier()));
    }
'''+s[b:])
edit('refining/WeaponConditions.java','t.hurt(new ElementalDamageSource(p.damageSources().mobAttack(p),proc.element),(float)damage);',
'''float before=t.getHealth()+t.getAbsorptionAmount();boolean hit=t.hurt(new ElementalDamageSource(p.damageSources().mobAttack(p),proc.element),(float)damage);
                if(hit&&t.getHealth()+t.getAbsorptionAmount()<before)com.longju.xiantu.spell.SkillPresentation.emit(p,UUID.randomUUID(),proc.element==Element.FIRE?"weapon_fire":proc.element==Element.PHYSICAL||proc.element==Element.EARTH?"weapon_earth":"weapon_metal","HIT",t.position().add(0,t.getBbHeight()*.5,0),p.getLookAngle());''')
edit('refining/WeaponConditions.java','if(r.theme().equals("开天斧")){h.empowered=true;h.empoweredUntil=now()+120;return;}',
'''com.longju.xiantu.spell.SkillPresentation.emit(p,UUID.randomUUID(),"weapon_charge","RELEASE",p.position(),p.getLookAngle());
        if(r.theme().equals("开天斧")){h.empowered=true;h.empoweredUntil=now()+120;return;}''')
java('client/SkillCastAnimations.java','''package com.longju.xiantu.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import java.util.*;
/** Smooth cast-only additive pose. Normal sword/axe/staff melee stays vanilla. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="xiantu",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class SkillCastAnimations {
 private record Pose(long start,long released,String skill){}
 private static final Map<UUID,Pose> POSES=new HashMap<>();
 private static long now(){var l=Minecraft.getInstance().level;return l==null?0:l.getGameTime();}
 static void begin(UUID id,String skill){if(POSES.size()<32)POSES.put(id,new Pose(now(),0,skill));}
 static void release(UUID id){var p=POSES.get(id);if(p!=null)POSES.put(id,new Pose(p.start,now(),p.skill));}
 static void end(UUID id){var p=POSES.get(id);if(p!=null&&p.released==0)POSES.put(id,new Pose(p.start,now(),p.skill));}
 static void tick(){POSES.values().removeIf(p->now()-p.start>65||p.released>0&&now()-p.released>10);}
 static void clear(){POSES.clear();}
 private static float weight(Pose p){double t=now()+Minecraft.getInstance().getFrameTime();double u=Math.max(0,Math.min(1,(t-p.start)/5));double v=p.released==0?1:Math.max(0,Math.min(1,1-(t-p.released)/10));return (float)(u*u*(3-2*u)*v*v*(3-2*v));}
 public static boolean thirdPerson(HumanoidModel<?> m,LivingEntity e){var p=POSES.get(e.getUUID());if(p==null)return false;float w=weight(p);boolean right=e.getMainArm()==HumanoidArm.RIGHT;var arm=right?m.rightArm:m.leftArm;var other=right?m.leftArm:m.rightArm;
  float pitch=p.skill.equals("staff_extend")?-1.57f:p.skill.equals("staff_quake")?-2.2f:p.skill.equals("thunder_strike")?-2.1f:p.skill.equals("staff_charge")?-.95f:-1.25f;
  arm.xRot=arm.xRot*(1-w)+pitch*w;arm.yRot=arm.yRot*(1-w)+(right?-.18f:.18f)*w;
  other.xRot=other.xRot*(1-w)-.65f*w;other.zRot=other.zRot*(1-w)+(right?-.2f:.2f)*w;return true;}
 @net.minecraftforge.eventbus.api.SubscribeEvent public static void hand(net.minecraftforge.client.event.RenderHandEvent e){var mc=Minecraft.getInstance();if(mc.player==null||e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND)return;var p=POSES.get(mc.player.getUUID());if(p==null)return;float w=weight(p);e.getPoseStack().translate(-.06*w,.12*w,-.12*w);e.getPoseStack().mulPose(com.mojang.math.Axis.XP.rotationDegrees(-16*w));}
 private SkillCastAnimations(){}
}
''')
edit('client/equipment/WeaponAttackAnimations.java','if(!(entity instanceof Player player)||!allowed(player))return false;',
'''if(com.longju.xiantu.client.SkillCastAnimations.thirdPerson(model,entity))return true;
        if(!(entity instanceof Player player)||!allowed(player))return false;''')
edit('worldgen/RuinPlacementQueue.java','public static boolean enqueue(','public static boolean busy(){return !JOBS.isEmpty();}\n    public static boolean enqueue(')
print('Integrated v11 server/client lifecycle and shared preloaded effects')
