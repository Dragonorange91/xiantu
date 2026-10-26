package com.longju.xiantu.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Shared locomotion and bounded server-side attacks. Boss encounter phases are a separate module. */
public final class CompletionLandEntity extends PathfinderMob implements CompletionAnimated {
    private static final EntityDataAccessor<String> ACTION=SynchedEntityData.defineId(CompletionLandEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<CompoundTag> BOSS_VISUAL=SynchedEntityData.defineId(CompletionLandEntity.class,EntityDataSerializers.COMPOUND_TAG);
    private BossCombatGoal bossCombat;
    private Combat ordinaryCombat;
    public boolean weaponInterruptible(){return boss()?bossCombat!=null&&bossCombat.weaponInterruptible():ordinaryCombat!=null&&ordinaryCombat.mode==1&&!ordinaryCombat.basicStrike;}
    public boolean talismanInterrupt(){return boss()?bossCombat!=null&&bossCombat.talismanInterrupt():weaponInterrupt();}
    public boolean weaponInterrupt(){if(boss())return bossCombat!=null&&bossCombat.weaponDelay();if(!weaponInterruptible())return false;ordinaryCombat.stop();return true;}
    private int bossSequence;
    private CompletionBestiary.Definition definition;
    private BlockPos home;
    private LivingEntity warned;
    private int warning, lostSight;
    private long neutralUntil;
    public CompletionLandEntity(EntityType<? extends CompletionLandEntity> type,Level level) {
        super(type,level);
        if(aquatic()){
            // Neutral buoyancy: the vanilla controller's optional +.005Y each tick
            // overpowers our gentle swimming acceleration and strands pursuit at the surface.
            moveControl=new net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl(this,85,10,.07F,.1F,false);
            lookControl=new net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl(this,10);
        }
    }
    private boolean aquatic(){return def().id().equals("azure_scale_python");}
    @Override protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level){
        return aquatic()?new net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation(this,level):new BeastGroundNavigation(this,level);
    }
    @Override public void move(MoverType type,Vec3 delta){
        if(level() instanceof ServerLevel l&&getPersistentData().getBoolean(com.longju.xiantu.worldgen.RuinAdventureService.BOSS)&&getPersistentData().hasUUID(com.longju.xiantu.worldgen.RuinEncounterService.SOURCE)){
            var i=com.longju.xiantu.worldgen.RuinAdventureData.get(l).find(getPersistentData().getUUID(com.longju.xiantu.worldgen.RuinEncounterService.SOURCE).toString());
            if(i!=null&&!i.defeated()){var arena=i.arena();var box=getBoundingBox();
                delta=new Vec3(net.minecraft.util.Mth.clamp(delta.x,Math.min(0,arena.minX-box.minX),Math.max(0,arena.maxX-box.maxX)),net.minecraft.util.Mth.clamp(delta.y,Math.min(0,arena.minY-box.minY),Math.max(0,arena.maxY-box.maxY)),net.minecraft.util.Mth.clamp(delta.z,Math.min(0,arena.minZ-box.minZ),Math.max(0,arena.maxZ-box.maxZ)));
            }
        }super.move(type,delta);
    }
    @Override public void travel(Vec3 input){
        if(aquatic()&&isEffectiveAi()&&isInWater()){
            moveRelative(.01F,input);move(MoverType.SELF,getDeltaMovement());setDeltaMovement(getDeltaMovement().scale(.9));
        }else super.travel(input);
    }
    private CompletionBestiary.Definition def() { if(definition==null) definition=CompletionBestiary.get(getType()); return definition; }
    private boolean peaceful() { return level().getDifficulty()==Difficulty.PEACEFUL; }
    private boolean valid(LivingEntity e) { return e!=null && e.isAlive() && (!(e instanceof Player p)||(!p.isCreative()&&!p.isSpectator())); }
    @Override protected void defineSynchedData() { super.defineSynchedData();entityData.define(ACTION,"");entityData.define(BOSS_VISUAL,new CompoundTag()); }
    public void resetRuinEncounter(){if(bossCombat!=null)bossCombat.resetEncounter();setTarget(null);}
    public boolean boss(){return def().boss();}
    public String definitionId(){return def().id();}
    public boolean warningActive(){return warning>0;}
    public boolean combatAllowed(){return !peaceful()&&!def().passive()&&valid(getTarget());}
    public void setCombatAction(String action){entityData.set(ACTION,action);}
    public boolean hasCombatAction(){return !entityData.get(ACTION).isEmpty();}
    public int bossSequence(){return bossSequence;}
    public CompoundTag bossVisual(){return entityData.get(BOSS_VISUAL);}
    public void beginBossVisual(String skill,Vec3 origin,Vec3 aim,Vec3 direction){
        CompoundTag n=new CompoundTag();n.putString("skill",skill);n.putInt("sequence",++bossSequence);n.putLong("start",level().getGameTime());
        vector(n,"origin",origin);vector(n,"aim",aim);vector(n,"direction",direction);entityData.set(BOSS_VISUAL,n);
    }
    public void updateBossAim(Vec3 aim,Vec3 direction){
        if(tickCount%2!=0)return;var n=bossVisual().copy();vector(n,"aim",aim);vector(n,"direction",direction);entityData.set(BOSS_VISUAL,n);
    }
    public void clearBossVisual(){entityData.set(BOSS_VISUAL,new CompoundTag());}
    public void cancelBossVisual(){var n=bossVisual().copy();n.putBoolean("cancelled",true);entityData.set(BOSS_VISUAL,n);}
    public void setBossBrokenLinks(int count){var n=bossVisual().copy();n.putInt("brokenLinks",count);entityData.set(BOSS_VISUAL,n);}
    public void setBossCounterTriggered(){var n=bossVisual().copy();n.putBoolean("counterTriggered",true);n.putLong("counterStart",level().getGameTime());entityData.set(BOSS_VISUAL,n);}
    private static void vector(CompoundTag n,String prefix,Vec3 v){n.putDouble(prefix+"X",v.x);n.putDouble(prefix+"Y",v.y);n.putDouble(prefix+"Z",v.z);}
    public boolean interruptBoss(Player player,Entity anchor){return bossCombat!=null&&bossCombat.interrupt(player,anchor);}
    public boolean strikeBossAnchor(Player player,Entity anchor){return bossCombat!=null&&bossCombat.strikeAnchor(player,anchor);}
    public boolean bossBannerActive(){return bossCombat!=null&&bossCombat.bannerActive();}
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean canBreatheUnderwater() { return def().element().equals("water"); }
    @Override protected void registerGoals() {
        if(!aquatic())goalSelector.addGoal(0,new FloatGoal(this));
        else goalSelector.addGoal(5,new RandomSwimmingGoal(this,1,20){
            @Override public boolean canUse(){return getTarget()==null&&super.canUse();}
            @Override public boolean canContinueToUse(){return getTarget()==null&&super.canContinueToUse();}
        });
        goalSelector.addGoal(1,new PanicGoal(this,1.5) {
            @Override public boolean canUse() { return def().passive() && super.canUse(); }
        });
        bossCombat=new BossCombatGoal(this);goalSelector.addGoal(2,bossCombat);
        ordinaryCombat=new Combat();goalSelector.addGoal(3,ordinaryCombat);
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,1.0) {
            @Override public boolean canUse() { return !aquatic()&&getTarget()==null && super.canUse(); }
            @Override public boolean canContinueToUse() { return !aquatic()&&getTarget()==null && super.canContinueToUse(); }
        });
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(0,new HurtByTargetGoal(this) {
            @Override public boolean canUse() { return !peaceful()&&!def().passive()&&super.canUse(); }
            @Override public boolean canContinueToUse() { return !peaceful()&&!def().passive()&&super.canContinueToUse(); }
        });
        targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,10,true,false,this::valid) {
            @Override public boolean canUse() { return !peaceful()&&def().hostile()&&super.canUse(); }
            @Override public boolean canContinueToUse() { return !peaceful()&&def().hostile()&&super.canContinueToUse(); }
        });
    }
    @Override public void tick() {
        super.tick();if(level().isClientSide||isNoAi())return;
        if(home==null) { home=blockPosition();restrictTo(home,24); }
        LivingEntity t=getTarget();
        if(peaceful()||def().passive()||!valid(t)) { setTarget(null);warned=null;warning=lostSight=0;return; }
        if(t!=warned) { warned=t;warning=def().hostile()?30:0; }
        if(getSensing().hasLineOfSight(t))lostSight=0;else lostSight++;
        if(t.distanceToSqr(Vec3.atCenterOf(home))>576 || distanceToSqr(Vec3.atCenterOf(home))>576 || lostSight>=200
                || (!def().hostile()&&level().getGameTime()>neutralUntil)) { setTarget(null);getNavigation().moveTo(home.getX(),home.getY(),home.getZ(),1); }
        if(warning>0) { warning--;getNavigation().stop(); }
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(boss()&&bossCombat!=null){
            if(bossCombat.blocksBodyDamage()&&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return false;
            if(source.getEntity() instanceof LivingEntity attacker)bossCombat.struck(attacker,source.getDirectEntity()==attacker);
            if(bossCombat.guarding())amount*=.7F;
        }
        boolean hit=super.hurt(source,amount);
        if(hit && !level().isClientSide && def().passive() && isAlive())
            BeastVfx.emit(this,"flee",position(),getLookAngle(),12,true);
        if(hit&&!level().isClientSide&&!peaceful()&&!def().passive()&&source.getEntity() instanceof LivingEntity e&&valid(e)&&e!=this) {
            setTarget(e);warned=e;warning=0;neutralUntil=level().getGameTime()+240;
        }
        return hit;
    }
    @Override public String animationState() {
        String action=entityData.get(ACTION);if(!action.isEmpty())return action;
        boolean moving=getDeltaMovement().horizontalDistanceSqr()>.0004;
        if(isInWater()&&def().model().endsWith("mang"))return "swim";
        return moving?(isSprinting()?"run":"walk"):"idle";
    }
    @Override public void addAdditionalSaveData(CompoundTag nbt) { super.addAdditionalSaveData(nbt);if(home!=null)nbt.putLong("XiantuHome",home.asLong());if(bossCombat!=null)bossCombat.save(nbt); }
    @Override public void readAdditionalSaveData(CompoundTag nbt) { super.readAdditionalSaveData(nbt);if(nbt.contains("XiantuHome")){home=BlockPos.of(nbt.getLong("XiantuHome"));restrictTo(home,24);}if(bossCombat!=null)bossCombat.load(nbt); }
    @Override public void die(DamageSource source){if(bossCombat!=null)bossCombat.stop();super.die(source);}

    private record Strike(String clip,int windup,int recovery,int cooldown,double reach,double angle,double multiplier,double travel) {}
    private Strike strike() {
        return switch(def().id()) {
            case "thunder_prison_lion","mushroom_toad","five_element_warden" -> new Strike("breath",24,20,140,4,60,.8,0);
            case "dark_shell_crab","azure_scale_python","ancient_branch_python" -> new Strike("sweep",18,16,100,3,100,1.1,0);
            case "electric_marten" -> new Strike("leap",16,20,120,7,0,1.2,6);
            case "flint_armored_lizard","jade_scale_crocodile" -> new Strike("leap",20,24,140,4,0,1.15,4);
            case "gold_back_beetle" -> new Strike("charge",24,30,160,10,0,1.35,8);
            case "jade_leaf_mantis" -> new Strike("attack",14,24,100,5,0,1.2,4);
            case "xuantians_remnant","cloud_sea_lord" -> new Strike("cast",24,26,100,4,60,1,0);
            default -> new Strike("attack",12,16,50,3,80,1,0);
        };
    }
    private void effect(boolean impact,Vec3 dir) { /* Presentation is exclusively Photon. */ }
    private final class Combat extends Goal {
        private Strike strike;
        private int elapsed,mode;
        private long next;
        private long nextMelee;
        private Vec3 direction,start;
        private boolean hit;
        private boolean basicStrike;
        Combat(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return !boss()&&!peaceful()&&!def().passive()&&valid(getTarget());}
        @Override public boolean canContinueToUse(){return canUse();}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void stop(){mode=elapsed=0;entityData.set(ACTION,"");setSprinting(false);getNavigation().stop();BeastVfx.cancel(CompletionLandEntity.this);}
        @Override public void tick(){
            LivingEntity target=getTarget();if(!valid(target)||peaceful()){stop();return;}
            if(target!=warned){warned=target;warning=def().hostile()?30:0;}
            if(mode==0){
                getLookControl().setLookAt(target,30,30);
                if(warning>0){getNavigation().stop();return;}
                strike=strike();
                long now=level().getGameTime();
                double meleeReach=Math.max(1.6,(getBbWidth()+target.getBbWidth())*.5+.9);
                boolean locked=com.longju.xiantu.talisman.TalismanEffects.locked(CompletionLandEntity.this);
                boolean basic=(now<next||locked) && now>=nextMelee && distanceToSqr(target)<=meleeReach*meleeReach;
                if(basic)strike=new Strike("attack",8,12,30,meleeReach,100,1,0);
                if((!locked&&now>=next||basic)&&distanceToSqr(target)<=strike.reach()*strike.reach()&&getSensing().hasLineOfSight(target)){
                    direction=target.position().subtract(position()).multiply(1,0,1).normalize();
                    if(direction.lengthSqr()<.01)direction=getLookAngle().multiply(1,0,1).normalize();
                    float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));setYRot(yaw);setYBodyRot(yaw);setYHeadRot(yaw);
                    mode=1;elapsed=0;hit=false;start=position();
                    basicStrike=basic;
                    if(basic)nextMelee=now+30;else next=now+strike.cooldown();
                    entityData.set(ACTION,strike.clip());getNavigation().stop();setSprinting(false);effect(false,direction);
                    BeastVfx.emit(CompletionLandEntity.this,basic?"melee":"warn",position().add(0,getBbHeight()*.55,0),direction,strike.windup(),true);
                }else { getNavigation().moveTo(target,1.2);setSprinting(true); }
                return;
            }
            elapsed++;getNavigation().stop();
            float attackYaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
            setYRot(attackYaw);setYBodyRot(attackYaw);setYHeadRot(attackYaw);
            if(mode==1&&elapsed<strike.windup())return;
            if(mode==1){
                if(strike.travel()>0){mode=2;elapsed=0;BeastVfx.emit(CompletionLandEntity.this,"trail",position(),direction,24,true);if(strike.clip().equals("leap"))setDeltaMovement(direction.scale(.4).add(0,.3,0));}
                else {
                    if(getSensing().hasLineOfSight(target)){
                        Vec3 offset=target.position().subtract(position());double distance=offset.horizontalDistance();
                        if(distance<=strike.reach()&&Math.abs(offset.y)<=2.5&&direction.dot(offset.multiply(1,0,1).normalize())>=Math.cos(Math.toRadians(strike.angle()/2)))
                            hit=target.hurt(damageSources().mobAttack(CompletionLandEntity.this),(float)(getAttributeValue(Attributes.ATTACK_DAMAGE)*strike.multiplier()));
                    }
                    effect(true,direction);
                    if(hit)BeastVfx.emit(CompletionLandEntity.this,basicStrike?"melee":"impact",target.position().add(0,target.getBbHeight()*.45,0),direction,12,false);
                    mode=3;elapsed=0;
                }
            }else if(mode==2){
                setDeltaMovement(direction.scale(.42).add(0,getDeltaMovement().y,0));hasImpulse=true;
                if(!hit&&getBoundingBox().inflate(.45).intersects(target.getBoundingBox())&&getSensing().hasLineOfSight(target)){
                    hit=target.hurt(damageSources().mobAttack(CompletionLandEntity.this),(float)(getAttributeValue(Attributes.ATTACK_DAMAGE)*strike.multiplier()));
                    if(hit){effect(true,direction);BeastVfx.emit(CompletionLandEntity.this,basicStrike?"melee":"impact",target.position().add(0,target.getBbHeight()*.45,0),direction,12,false);}
                }
                if(horizontalCollision||position().distanceToSqr(start)>=strike.travel()*strike.travel()||elapsed>=24){mode=3;elapsed=0;setDeltaMovement(getDeltaMovement().multiply(0,1,0));}
            }else if(elapsed>=strike.recovery()){mode=0;entityData.set(ACTION,"");}
        }
    }
}
