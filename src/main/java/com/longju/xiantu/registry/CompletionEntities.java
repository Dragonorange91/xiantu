package com.longju.xiantu.registry;
import com.longju.xiantu.entity.*;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

public final class CompletionEntities {
    @SuppressWarnings("unchecked")
    public static Map<String,RegistryObject<EntityType<Mob>>> registerTypes(){
        Map<String,RegistryObject<EntityType<Mob>>> map=new LinkedHashMap<>();
        for(var d:CompletionBestiary.ALL)map.put(d.id(),ModEntities.ENTITY_TYPES.register(d.id(),()->
                EntityType.Builder.<Mob>of((type,level)->switch(d.kind()){
                    case "FLY" -> new CompletionFlyingEntity((EntityType<CompletionFlyingEntity>)(EntityType<?>)type,level);
                    case "WATER" -> new CompletionRayEntity((EntityType<CompletionRayEntity>)(EntityType<?>)type,level);
                    default -> new CompletionLandEntity((EntityType<CompletionLandEntity>)(EntityType<?>)type,level);
                },d.kind().equals("WATER")?MobCategory.WATER_CREATURE:MobCategory.CREATURE)
                .sized(d.width(),d.height()).clientTrackingRange(10).build("xiantu:"+d.id())));
        return Collections.unmodifiableMap(map);
    }
    public static Map<String,RegistryObject<Item>> registerEggs(){
        Map<String,RegistryObject<Item>> map=new LinkedHashMap<>();
        for(var d:CompletionBestiary.ALL)map.put(d.id(),ModItems.ITEMS.register(d.id()+"_spawn_egg",()->
            new ForgeSpawnEggItem(ModEntities.COMPLETION.get(d.id()),d.eggBase(),d.eggSpot(),new Item.Properties())));
        return Collections.unmodifiableMap(map);
    }
    public static void attributes(EntityAttributeCreationEvent event){
        for(var d:CompletionBestiary.ALL)event.put(ModEntities.COMPLETION.get(d.id()).get(),Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.damage())
            .add(Attributes.FOLLOW_RANGE,16).add(Attributes.MOVEMENT_SPEED,d.boss()?.23:.25)
            .add(Attributes.KNOCKBACK_RESISTANCE,d.boss()?.6:0).build());
    }
    private CompletionEntities(){}
}
