package com.longju.xiantu.client;
import com.longju.xiantu.network.SkillVfxPacket;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fml.ModList;
public final class SkillPresentationClient {
 public static void receive(SkillVfxPacket p){
  var world=net.minecraft.client.Minecraft.getInstance().level;
  if(world==null||!world.dimension().location().equals(p.dimension()))return;
  var owner=world.getEntity(p.entityId());if(owner==null||!owner.getUUID().equals(p.owner()))return;
  // Caster motion must remain available without the optional particle library.
  if(java.util.Set.of("fire_bolt","thunder_strike","cloud_step","great_sage_form","seventy_two_forms").contains(p.skill())
      ||p.skill().startsWith("talisman_")){
   switch(p.phase()){
    case "WINDUP","START"->SkillCastAnimations.begin(p.owner(),p.skill());
    case "RELEASE"->SkillCastAnimations.pulse(p.owner(),p.skill());
    case "CANCEL","END"->SkillCastAnimations.end(p.owner());
   }
  }
  StaffSkillRenderer.receive(p);WeaponSkillRenderer.receive(p);
  // Authored weapon surfaces own both release and impact; avoid a second legacy burst.
  if(ModList.get().isLoaded("photon")&&!com.longju.xiantu.refining.WeaponActives.SKILLS.contains(p.skill()))SkillPhotonRuntime.receive(p);
 }
 public static void formation(BlockPos p,int tier){FormationGatheringRenderer.touch(p,tier);}
 public static void clear(){FormationGatheringRenderer.clear();WeaponSkillRenderer.clear();StaffSkillRenderer.clear();SkillCastAnimations.clear();if(ModList.get().isLoaded("photon"))SkillPhotonRuntime.clear();}
 private SkillPresentationClient(){}
}
