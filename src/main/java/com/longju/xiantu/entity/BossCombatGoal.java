package com.longju.xiantu.entity;

import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Server-authoritative encounter timeline. Visual state carries an instance sequence;
 * no client event can deal damage. Temporary telegraph anchors never alter blocks. */
public final class BossCombatGoal extends Goal {
    private final CompletionLandEntity mob;
    private final Map<String,Long> cooldowns=new HashMap<>();
    private final Set<UUID> hit=new HashSet<>();
    private BossCombatSkills.Skill skill;
    private UUID targetId;
    private int elapsed,round;
    private int impactEffects;
    private long nextSpecial,ultimateReady;
    private boolean finalPhase,waitingFinal,counterTriggered;
    private Vec3 origin=Vec3.ZERO,aim=Vec3.ZERO,direction=Vec3.ZERO;
    private ArmorStand anchor;
    private final List<ArmorStand> mechanisms=new ArrayList<>();
    private int brokenLinks,brokenMask;
    private boolean coreDamage;
    public BossCombatGoal(CompletionLandEntity mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean canUse(){return mob.boss()&&mob.combatAllowed();}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    public boolean weaponInterruptible(){return skill!=null&&!skill.ultimate()&&elapsed<skill.windup();}
    public boolean talismanInterrupt(){if(!weaponInterruptible())return false;stop();nextSpecial=mob.level().getGameTime()+40;return true;}
    public boolean weaponDelay(){if(!weaponInterruptible())return false;elapsed=Math.max(0,elapsed-Math.max(1,(int)Math.ceil(skill.windup()*.1)));return true;}
    public boolean guarding(){return skill!=null && skill.shape().equals("guard") && elapsed>=skill.windup() && elapsed<skill.windup()+skill.active();}
    public boolean blocksBodyDamage(){return guarding() && skill.id().equals("meteor_guard") && !coreDamage;}
    public boolean bannerActive(){return skill!=null&&skill.shape().equals("banner")&&mechanisms.stream().anyMatch(Entity::isAlive);}
    public void struck(LivingEntity attacker,boolean melee){
        if(!counterTriggered&&skill!=null&&skill.shape().equals("counter")&&elapsed>=skill.windup()&&elapsed<skill.windup()+skill.active()
                && melee && attacker!=null && attacker.distanceToSqr(mob)<16 && attacker.position().subtract(mob.position()).normalize().dot(direction)>.4)
            {counterTriggered=true;mob.setBossCounterTriggered();if(mob.definitionId().equals("xuantians_remnant"))mob.setCombatAction("counter_hit");}
    }
    @Override public void stop(){
        clearAnchor();clearMechanisms();skill=null;hit.clear();mob.setCombatAction("");mob.clearBossVisual();
        mob.setSprinting(false);mob.getNavigation().stop();BeastVfx.cancel(mob);
    }
    public void resetEncounter(){stop();finalPhase=false;waitingFinal=false;ultimateReady=0;nextSpecial=0;cooldowns.clear();}
    public void save(CompoundTag n){n.putBoolean("BossFinalPhase",finalPhase);n.putLong("BossUltimateReady",ultimateReady);}
    public void load(CompoundTag n){finalPhase=n.getBoolean("BossFinalPhase");ultimateReady=n.getLong("BossUltimateReady");}
    @Override public void tick(){
        if(com.longju.xiantu.talisman.TalismanEffects.locked(mob)){stop();return;}
        LivingEntity target=mob.getTarget();if(target==null)return;
        long now=mob.level().getGameTime();
        if(!finalPhase && mob.getHealth()<=mob.getMaxHealth()*.30){finalPhase=true;waitingFinal=true;}
        if(skill==null){
            if(mob.warningActive())return;
            var list=BossCombatSkills.forBoss(mob.definitionId());
            BossCombatSkills.Skill chosen=null;
            if(finalPhase&&now>=ultimateReady&&(waitingFinal||now>=nextSpecial))chosen=list.stream().filter(BossCombatSkills.Skill::ultimate).findFirst().orElse(null);
            else if(now>=nextSpecial){
                for(int i=0;i<list.size();i++){
                    var candidate=list.get((round+i)%list.size());
                    if(!candidate.ultimate()&&now>=cooldowns.getOrDefault(candidate.id(),0L)){chosen=candidate;round=(round+i+1)%list.size();break;}
                }
            }
            var basic=BossCombatSkills.basic(mob.definitionId());
            if(chosen==null && mob.distanceToSqr(target)<basic.reach()*basic.reach())chosen=basic;
            double engage=18;
            if(chosen!=null && mob.definitionId().equals("xuantians_remnant"))
                engage=chosen.shape().equals("illusion")?6:chosen.shape().equals("charge")?9:chosen.reach()-.2;
            if(chosen!=null && mob.distanceToSqr(target)<engage*engage && mob.getSensing().hasLineOfSight(target)){begin(chosen,target);return;}
            mob.getLookControl().setLookAt(target,25,25);mob.getNavigation().moveTo(target,1.15);mob.setSprinting(true);return;
        }
        mob.getNavigation().stop();mob.setSprinting(false);
        // Changing the warned target restarts the FULL pre-warning, never a last-tick redirect.
        if(elapsed<skill.windup() && !target.getUUID().equals(targetId)){clearAnchor();begin(skill,target);return;}
        elapsed++;
        if(elapsed<=skill.windup()){
            if(!skill.motion().equals("spear_pierce")||elapsed<=skill.windup()-8){aim=target.position();direction=horizontal(aim.subtract(origin));}face(direction);
            mob.updateBossAim(aim,direction);
            if(elapsed==skill.windup())release();
        }else if(elapsed<=skill.windup()+skill.active())active(target);
        if(elapsed>=skill.duration()){
            if(skill.ultimate())ultimateReady=now+600;
            stop();
        }
    }
    private void begin(BossCombatSkills.Skill s,LivingEntity target){
        skill=s;elapsed=0;impactEffects=0;brokenLinks=brokenMask=0;counterTriggered=false;hit.clear();targetId=target.getUUID();origin=mob.position();aim=target.position();direction=horizontal(aim.subtract(origin));
        long now=mob.level().getGameTime();
        if(!s.id().equals("attack")){nextSpecial=now+160;cooldowns.put(s.id(),now+s.cooldown());}
        if(s.ultimate())waitingFinal=false;
        mob.setCombatAction(s.id());mob.beginBossVisual(s.id(),origin,aim,direction);mob.getNavigation().stop();face(direction);
        mob.level().playSound(null,mob.blockPosition(),SoundEvents.ENCHANTMENT_TABLE_USE,SoundSource.HOSTILE,.65F,s.ultimate()?.55F:.9F);
        if(s.id().equals("final_star")||s.id().equals("final_formation"))createAnchor();
    }
    private void release(){
        clearAnchor();
        mob.level().playSound(null,mob.blockPosition(),skill.shape().equals("fall")?SoundEvents.TRIDENT_THUNDER:SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.HOSTILE,.7F,.8F);
        if(skill.shape().equals("leap"))mob.setDeltaMovement(direction.scale(.35).add(0,.5,0));
        if(skill.motion().equals("spear_pierce"))blinkToThrust();
        switch(skill.shape()){
            case "seal" -> mechanism(aim.add(0,0,0),"阵眼 · 攻击破除",Items.AMETHYST_SHARD,"seal");
            case "banner" -> mechanism(origin.add(direction.z*2,0,-direction.x*2),"战旗 · 攻击破除",Items.RED_BANNER,"banner");
            case "links" -> {for(int i=0;i<3;i++){double a=i*Math.PI*2/3;mechanism(aim.add(Math.sin(a)*3,0,Math.cos(a)*3),"云锁结点 · 切断两处",Items.PRISMARINE_CRYSTALS,"link");}}
            case "guard" -> {if(skill.id().equals("meteor_guard"))mechanism(origin.add(direction.z*1.8,1,-direction.x*1.8),"暴露星核",Items.HEART_OF_THE_SEA,"core");}
        }
    }
    private void active(LivingEntity target){
        int age=elapsed-skill.windup();double p=age/(double)skill.active();String shape=skill.shape();
        if(shape.equals("fist_combo") && age%10==1)hit.clear();
        if(shape.equals("illusion"))return; // Decoys are renderer-only and can NEVER attack.
        if(shape.equals("guard")||shape.equals("banner"))return;
        if(shape.equals("links")){
            if(brokenLinks<2 && mob.getSensing().hasLineOfSight(target) && target.position().distanceToSqr(aim)<36)
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,6,0));
            return;
        }
        Vec3 centre=shape.equals("fall")?aim:origin;
        if(shape.equals("blink_thrust"))centre=mob.position();
        if(shape.equals("seal")||shape.equals("pressure"))centre=aim;
        if(shape.equals("fall_sequence")){
            if(age%12==1)hit.clear();
            int wave=Math.min(4,age/12);double angle=wave*Math.PI*2/5;
            centre=aim.add(Math.sin(angle)*3,0,Math.cos(angle)*3);
            p=(age%12)/12.0;
        }
        if(shape.equals("charge")||shape.equals("leap")){
            mob.setDeltaMovement(direction.scale(shape.equals("charge")?.45:.32).add(0,mob.getDeltaMovement().y,0));mob.hasImpulse=true;
            if(mob.horizontalCollision){mob.setDeltaMovement(mob.getDeltaMovement().multiply(0,1,0));elapsed=skill.windup()+skill.active();return;}
            centre=mob.position();
        }
        var bounds=new AABB(centre,centre).inflate(skill.reach()+2,5,skill.reach()+2);
        for(var victim:mob.level().getEntitiesOfClass(LivingEntity.class,bounds,e->e!=mob && e.isAlive() && !(e instanceof CompletionLandEntity)
                && (!(e instanceof Player player)||(!player.isCreative()&&!player.isSpectator())))){
            if(hit.contains(victim.getUUID()))continue;
            Vec3 delta=victim.position().subtract(centre);double along=delta.dot(direction),side=delta.dot(new Vec3(direction.z,0,-direction.x));
            boolean strikes=shape.equals("charge")||shape.equals("leap")?mob.getBoundingBox().inflate(.4).intersects(victim.getBoundingBox()):
                BossCombatSkills.shapeHits(shape.equals("fall_sequence")||shape.equals("seal")?"fall":shape,along,side,delta.horizontalDistance(),skill.reach(),p,delta.y);
            if(shape.equals("seal") && mechanisms.stream().anyMatch(Entity::isAlive) && delta.horizontalDistance()<skill.reach())
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,6,0));
            if(shape.equals("counter"))strikes &=counterTriggered;
            if(!strikes || skill.multiplier()<=0 || !clearLine(centre.add(0,1,0),victim.getEyePosition()))continue;
            if(victim.hurt(mob.damageSources().mobAttack(mob),(float)(mob.getAttributeValue(Attributes.ATTACK_DAMAGE)*skill.multiplier()))){
                hit.add(victim.getUUID());
                if(impactEffects<4){
                    BeastVfx.emit(mob,(skill.id().equals("blink_pierce")?"star_javelin":skill.id())+"/impact",victim.position().add(0,victim.getBbHeight()*.45,0),direction,14,false);
                    impactEffects++;
                }
                if(skill.id().equals("king_roar"))victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,20,0));
            }
        }
        // Client Photon timeline owns the per-skill impact presentation. Damage
        // remains here; a second generic impact packet would duplicate the FX.
    }
    private boolean clearLine(Vec3 from,Vec3 to){return mob.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mob)).getType()==HitResult.Type.MISS;}
    private void blinkToThrust(){
        // Once at release; the warned line is locked, not a homing teleport.
        Vec3 landing=aim.subtract(direction.scale(3.2));
        var cell=net.minecraft.core.BlockPos.containing(landing);
        var box=mob.getBoundingBox().move(landing.subtract(mob.position()));
        if(origin.distanceToSqr(landing)>14*14||!mob.level().hasChunkAt(cell)
                ||!mob.level().getWorldBorder().isWithinBounds(box)
                ||!mob.level().getBlockState(cell.below()).isCollisionShapeFullBlock(mob.level(),cell.below())
                ||!mob.level().getFluidState(cell).isEmpty()||!mob.level().noCollision(mob,box)
                ||!clearLine(origin.add(0,1.3,0),landing.add(0,1.3,0)))return;
        var data=mob.getPersistentData();
        if(data.getBoolean(com.longju.xiantu.worldgen.RuinAdventureService.BOSS)&&data.hasUUID(com.longju.xiantu.worldgen.RuinEncounterService.SOURCE)){
            var encounter=com.longju.xiantu.worldgen.RuinAdventureData.get((ServerLevel)mob.level()).find(data.getUUID(com.longju.xiantu.worldgen.RuinEncounterService.SOURCE).toString());
            if(encounter!=null&&(!encounter.arena().contains(box.minX,box.minY,box.minZ)||!encounter.arena().contains(box.maxX,box.maxY,box.maxZ)))return;
        }
        mob.teleportTo(landing.x,landing.y,landing.z);mob.setDeltaMovement(Vec3.ZERO);mob.hasImpulse=true;
    }
    private static Vec3 horizontal(Vec3 v){v=v.multiply(1,0,1);return v.lengthSqr()<1e-5?new Vec3(0,0,1):v.normalize();}
    private void face(Vec3 d){float yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z));mob.setYRot(yaw);mob.setYBodyRot(yaw);mob.setYHeadRot(yaw);}
    private void createAnchor(){
        var level=(ServerLevel)mob.level();anchor=new ArmorStand(level,origin.x+direction.z*2,origin.y,origin.z-direction.x*2);
        anchor.setInvisible(true);anchor.setNoGravity(true);anchor.setInvulnerable(true);
        anchor.setItemSlot(EquipmentSlot.HEAD,skill.id().equals("final_star")?ItemStack.EMPTY:new ItemStack(Items.RED_BANNER));
        anchor.setCustomName(Component.literal(skill.id().equals("final_star")?"星核 · 右键打断":"战旗 · 右键打断"));anchor.setCustomNameVisible(true);
        anchor.addTag("xiantu_boss_telegraph");anchor.getPersistentData().putUUID("BossOwner",mob.getUUID());anchor.getPersistentData().putInt("BossSequence",mob.bossSequence());
        anchor.getPersistentData().putLong("BossExpires",level.getGameTime()+skill.windup()+5);level.addFreshEntity(anchor);
    }
    public boolean interrupt(Player player,Entity clicked){
        if(clicked!=anchor||skill==null||!skill.ultimate()||elapsed>=skill.windup()||player.distanceToSqr(clicked)>36)return false;
        clearAnchor();ultimateReady=mob.level().getGameTime()+600;elapsed=skill.windup()+skill.active();mob.cancelBossVisual();mob.setCombatAction("");
        return true;
    }
    private void clearAnchor(){if(anchor!=null){anchor.discard();anchor=null;}}
    private void mechanism(Vec3 at,String name,Item item,String role){
        var level=(ServerLevel)mob.level();var display=new ArmorStand(level,at.x,at.y,at.z);
        display.setInvisible(true);display.setNoGravity(true);display.setInvulnerable(true);display.setItemSlot(EquipmentSlot.HEAD,role.equals("core")?ItemStack.EMPTY:new ItemStack(item));
        display.setCustomName(Component.literal(name));display.setCustomNameVisible(true);display.addTag("xiantu_boss_telegraph");
        var n=display.getPersistentData();n.putUUID("BossOwner",mob.getUUID());n.putInt("BossSequence",mob.bossSequence());n.putString("BossAnchorRole",role);n.putInt("BossLinkIndex",mechanisms.size());n.putLong("BossExpires",level.getGameTime()+skill.active()+5);
        mechanisms.add(display);level.addFreshEntity(display);
    }
    public boolean strikeAnchor(Player player,Entity clicked){
        if(skill==null||!mechanisms.contains(clicked)||player.distanceToSqr(clicked)>36||elapsed>=skill.windup()+skill.active())return false;
        String role=clicked.getPersistentData().getString("BossAnchorRole");
        if(role.equals("core")){
            coreDamage=true;try{mob.hurt(mob.damageSources().playerAttack(player),(float)player.getAttributeValue(Attributes.ATTACK_DAMAGE));}finally{coreDamage=false;}
        }else{
            clicked.discard();mechanisms.remove(clicked);
            if(role.equals("link")){brokenLinks++;brokenMask|=1<<clicked.getPersistentData().getInt("BossLinkIndex");mob.setBossBrokenLinks(brokenMask);}
            if(role.equals("seal") || role.equals("link")&&brokenLinks>=2){clearMechanisms();elapsed=skill.windup()+skill.active();mob.cancelBossVisual();mob.setCombatAction("");}
        }
        return true;
    }
    private void clearMechanisms(){mechanisms.forEach(Entity::discard);mechanisms.clear();}
}
