package com.longju.xiantu.spell;
import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.cultivation.CultivationEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;
/** Server-only passive; checked after damage resolution, never resurrects a lethal hit. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class RejuvenationService {
 public static boolean attempt(ServerPlayer p){
  var s=SpellCatalog.find("rejuvenation");var d=SpellService.data(p);long now=SpellService.now(p);
  if(p.isCreative()||!SupportSpellRules.heals(p.getHealth(),p.getMaxHealth())||!SpellService.denial(p,s,true).isEmpty()||d.cooldown(s.id(),now)>0)return false;
  boolean equipped=false;for(int i=SpellData.ACTIVE_SLOTS;i<SpellData.TOTAL_SLOTS;i++)if(d.slot(i).equals(s.id()))equipped=true;
  if(!equipped)return false;var power=GongfaService.data(p);long cost=SpellService.cost(p,s);
  if(power.power()*1000+1e-6<cost)return false;
  // Commit cooldown and payment before callbacks, so healing events cannot re-enter.
  d.setCooldown(s.id(),now+SupportSpellRules.HEAL_COOLDOWN);power.setPower(Math.max(0,power.power()-cost/1000.0));
  float before=p.getHealth();p.heal((float)(p.getMaxHealth()*SupportSpellRules.healRatio(d.mastery(s.id()))));
  UUID tx=UUID.randomUUID();if(p.getHealth()>before)d.award(s.id(),(int)Math.round(1000*com.longju.xiantu.cultivation.PhysiqueService.effectiveSpellExperience(power,1)),now,tx,null);
  p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,p.getX(),p.getY()+1,p.getZ(),14,.45,.65,.45,.02);
  p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.PLAYERS,.6F,1.5F);
  CultivationEvents.sync(p);SpellService.sync(p);return true;
 }
 @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){if(e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayer p&&p.tickCount%5==0)attempt(p);}
 private RejuvenationService(){}
}
