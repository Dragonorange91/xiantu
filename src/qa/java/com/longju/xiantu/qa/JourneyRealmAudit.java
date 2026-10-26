package com.longju.xiantu.qa;

import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.registry.ModEntities;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class JourneyRealmAudit {
    private static void require(boolean ok,String text){if(!ok)throw new IllegalStateException(text);}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xianturealmjourneyaudit").requires(s->s.hasPermission(2)).executes(c->{
            var p=c.getSource().getPlayerOrException();
            if(!p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
            var data=(PlayerCultivationData)p.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new);
            CompoundTag saved=new CompoundTag();data.save(saved);var persistent=p.getPersistentData().copy();
            var advancementList=new ArrayList<net.minecraft.advancements.Advancement>();
            Map<ResourceLocation,List<String>> done=new HashMap<>();
            for(String id:RealmAdvancements.IDS){
                var a=p.server.getAdvancements().getAdvancement(new ResourceLocation("xiantu","cultivation/"+id));require(a!=null,"Missing realm "+id);
                advancementList.add(a);var criteria=new ArrayList<String>();p.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(criteria::add);done.put(a.getId(),criteria);
            }
            var guide=ModEntities.MERCHANTS.get("town_guide").get().create(p.serverLevel());
            var shop=ModEntities.MERCHANTS.get("wanbao_merchant").get().create(p.serverLevel());
            int checks=0;
            try{
                for(var a:advancementList)for(String criterion:done.get(a.getId()))p.getAdvancements().revoke(a,criterion);
                data.setCultivationUnlocked(false);data.setRealmStage(0,0);RealmAdvancements.check(p,data);
                require(!p.getAdvancements().getOrStartProgress(advancementList.get(0)).isDone(),"Mortal must not grant Qi");checks++;
                data.setCultivationUnlocked(true);
                for(int i=0;i<9;i++){
                    data.setRealmStage(i,0);RealmAdvancements.check(p,data);
                    for(int j=0;j<9;j++){require(p.getAdvancements().getOrStartProgress(advancementList.get(j)).isDone()==(j<=i),"Realm progression "+i+":"+j);checks++;}
                    data.setRealmStage(i,1);RealmAdvancements.check(p,data);require(p.getAdvancements().getOrStartProgress(advancementList.get(i)).getPercent()==1,"Minor stage no new criterion");checks++;
                }
                data.setRealmStage(0,0);data.setCultivationUnlocked(false);RealmAdvancements.check(p,data);
                for(var a:advancementList){require(p.getAdvancements().getOrStartProgress(a).isDone(),"Rollback preserves milestone");checks++;}
                LivingEntity target=guide;float hp=target.getHealth();
                var sources=List.of(p.damageSources().generic(),p.damageSources().playerAttack(p),p.damageSources().magic(),p.damageSources().lava(),p.damageSources().fall(),p.damageSources().fellOutOfWorld());
                for(var source:sources){require(!target.hurt(source,9999F)&&target.getHealth()==hp&&target.isAlive(),"Guide immunity "+source);checks++;}
                LivingEntity normal=shop;require(normal.hurt(p.damageSources().generic(),1)&&normal.getHealth()<normal.getMaxHealth(),"Normal merchant remains mortal");checks++;
                var file=net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("xiantu-qa/journey-realms.json");java.nio.file.Files.createDirectories(file.getParent());
                java.nio.file.Files.writeString(file,"{\"passed\":true,\"checks\":"+checks+",\"at\":\""+java.time.Instant.now()+"\"}");
                int count=checks;c.getSource().sendSuccess(()->Component.literal("Realm/guide PASS "+count),false);return count;
            }catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}finally{
                data.load(saved);p.getPersistentData().getAllKeys().stream().toList().forEach(p.getPersistentData()::remove);p.getPersistentData().merge(persistent);
                for(var a:advancementList){var now=new ArrayList<String>();p.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(now::add);for(String criterion:now)if(!done.get(a.getId()).contains(criterion))p.getAdvancements().revoke(a,criterion);for(String criterion:done.get(a.getId()))p.getAdvancements().award(a,criterion);}
                ((net.minecraft.world.entity.Entity)guide).discard();((net.minecraft.world.entity.Entity)shop).discard();
            }
        }));
    }
}
