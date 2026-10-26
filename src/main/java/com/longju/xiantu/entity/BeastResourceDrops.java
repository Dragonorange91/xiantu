package com.longju.xiantu.entity;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.config.XiantuConfig;
import com.longju.xiantu.item.*;
import com.longju.xiantu.registry.ModItems;
import com.longju.xiantu.combat.ElementalCombat;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Persistent server source data; unknown realms never manufacture a guessed grade. */
@Mod.EventBusSubscriber(modid=XiantuMod.MOD_ID)
public final class BeastResourceDrops {
    public static final String REALM="xiantu_beast_realm", NATURAL="xiantu_beast_natural";
    public static boolean eligibleSource(Entity entity) { var n=entity.getPersistentData(); return n.getBoolean(NATURAL)||n.hasUUID(com.longju.xiantu.worldgen.RuinEncounterService.SOURCE); }
    private static final String SETTLED="xiantu_beast_resources_settled";
    private static final String OWNER="xiantu_loot_owner", UNTIL="xiantu_loot_until";
    private static final String HIT_BY="xiantu_beast_hit_by", HIT_AT="xiantu_beast_hit_at";
    private static ServerPlayer creditedPlayer(Entity entity,LivingEntity beast) {
        if(entity instanceof ServerPlayer p) return p;
        if(entity instanceof TamableAnimal pet && pet.getOwner() instanceof ServerPlayer p
                &&p.isAlive()&&p.level()==beast.level()&&p.distanceToSqr(beast)<=4096) return p;
        return null;
    }
    @SubscribeEvent public static void damaged(LivingDamageEvent e) {
        var beast=e.getEntity();
        if(beast.level().isClientSide||e.getAmount()<=0||species(beast).isEmpty())return;
        var player=creditedPlayer(e.getSource().getEntity(),beast);
        if(player!=null){beast.getPersistentData().putUUID(HIT_BY,player.getUUID());
            beast.getPersistentData().putLong(HIT_AT,beast.level().getGameTime());}
    }
    private static String species(LivingEntity entity) {
        var id=ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id!=null&&id.getNamespace().equals(XiantuMod.MOD_ID)&&BeastResourceRules.SPECIES.contains(id.getPath())?id.getPath():"";
    }
    @SubscribeEvent public static void spawned(MobSpawnEvent.FinalizeSpawn e) {
        if(!species(e.getEntity()).isEmpty()&&!e.getEntity().getPersistentData().getBoolean(BeastRealmService.INITIALIZED)) e.getEntity().getPersistentData().putBoolean(NATURAL,
                e.getSpawnType()==MobSpawnType.NATURAL||e.getSpawnType()==MobSpawnType.CHUNK_GENERATION);
    }
    @SubscribeEvent public static void drops(LivingDropsEvent e) {
        var beast=e.getEntity();var data=beast.getPersistentData();String species=species(beast);
        if(beast.level().isClientSide||species.isEmpty()||!eligibleSource(beast)||data.getBoolean(SETTLED)
                ||!data.contains(REALM,Tag.TAG_INT)) return;
        int realm=data.getInt(REALM);
        if(realm<0||realm>8) return;
        // The effective final player hit includes a pet's credited owner, but not a later environmental death.
        ServerPlayer killer=creditedPlayer(e.getSource().getEntity(),beast);
        if(killer==null||!data.hasUUID(HIT_BY)||!killer.getUUID().equals(data.getUUID(HIT_BY))
                ||beast.level().getGameTime()-data.getLong(HIT_AT)>600) return;
        var affinities=ElementalCombat.affinities(beast);
        if(affinities.size()!=1) return;
        String element=affinities.iterator().next().id();
        if(!DemonCoreItem.ELEMENTS.contains(element)) return;
        data.putBoolean(SETTLED,true);
        // Fixed cost for killing an intrinsically peaceful species, unaffected by physique multipliers.
        if(BeastRealmService.peacefulSpecies(species))killer.getCapability(com.longju.xiantu.cultivation.CultivationCapability.DATA)
                .ifPresent(d->d.setInnerDemon(d.innerDemon()+2));
        int grade=realm+1;
        List<ItemStack> stacks=new ArrayList<>();
        var core=new ItemStack(ModItems.DEMON_CORE.get());DemonCoreItem.setData(core,BeastRealmRules.coreTier(realm),element);stacks.add(core);
        String main=BeastResourceRules.mainMaterial(species);
        if(main.equals("beast_hide")) stacks.add(BeastHideItem.fromRealm(realm,element));
        else stacks.add(BeastMaterialItem.setData(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(XiantuMod.MOD_ID,main))),grade,element));
        stacks.add(BeastMaterialItem.setData(new ItemStack(ModItems.BEAST_BLOOD_SAC.get()),grade,element));
        String exclusive=BeastResourceRules.EXCLUSIVE.get(species);
        if(exclusive!=null) stacks.add(BeastMaterialItem.setData(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(XiantuMod.MOD_ID,exclusive))),grade,element));
        var candidates=BeastResourceRules.candidates(species);
        String extra=BeastResourceRules.selectAuxiliary(species,XiantuConfig.BEAST_AUXILIARY_CHANCE.get(),
                beast.getRandom().nextDouble(),candidates.isEmpty()?0:beast.getRandom().nextInt(candidates.size()));
        if(!extra.isEmpty()) stacks.add(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(XiantuMod.MOD_ID,extra))));
        if(species.equals("azure_scale_python")&&realm>=5)
            stacks.add(new ItemStack(com.longju.xiantu.medicine.MedicineRegistry.herb("herb_0024")));
        for(var stack:stacks) if(!stack.isEmpty()) {
            var item=new ItemEntity(beast.level(),beast.getX(),beast.getY()+.25,beast.getZ(),stack);
            item.setDefaultPickUpDelay();
            item.getPersistentData().putUUID(OWNER,killer.getUUID());
            item.getPersistentData().putLong(UNTIL,beast.level().getGameTime()+200L);
            e.getDrops().add(item);
        }
    }
    @SubscribeEvent public static void pickup(EntityItemPickupEvent e) {
        var data=e.getItem().getPersistentData();
        if(data.hasUUID(OWNER)&&e.getItem().level().getGameTime()<data.getLong(UNTIL)
                &&!e.getEntity().getUUID().equals(data.getUUID(OWNER))) e.setCanceled(true);
    }
    private BeastResourceDrops() {}
}
