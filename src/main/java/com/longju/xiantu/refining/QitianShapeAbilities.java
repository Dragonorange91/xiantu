package com.longju.xiantu.refining;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import java.util.*;

/** Temporary species traits; never copies hostile AI or permanent player attributes. */
public final class QitianShapeAbilities {
    private record Flight(boolean allowed,boolean flying,float speed){}
    private static final String FLIGHT_KEY="xiantu_shape_flight";
    private static final UUID SPEED=UUID.fromString("f03975f0-e741-451e-a9c1-45cc4fb6ae49");
    public static final Set<String> FLYING=Set.of("thunder_feather_raven","copper_beak_vulture","fire_beak_falcon","purple_lightning_bat");
    public static final Set<String> AQUATIC=Set.of("azure_scale_python","jade_scale_crocodile","dark_shell_crab","tide_pattern_ray");
    public static final Set<String> FIRE=Set.of("yanwei_fox","red_sun_lion","fire_beak_falcon","flint_armored_lizard","molten_turtle");
    public static String description(String species){
        if(FLYING.contains(species))return "可飞行，免疫摔落";
        if(AQUATIC.contains(species))return "水下呼吸，游泳加速";
        if(FIRE.contains(species))return "火焰环境伤害降低50%";
        return "陆行速度提高15%";
    }
    public static void tick(ServerPlayer p,String species){
        boolean bird=FLYING.contains(species)&&!p.isSpectator()&&p.isAlive();
        if(bird){
            if(!p.getPersistentData().contains(FLIGHT_KEY)){var a=p.getAbilities();var saved=new net.minecraft.nbt.CompoundTag();saved.putBoolean("allowed",a.mayfly);saved.putBoolean("flying",a.flying);saved.putFloat("speed",a.getFlyingSpeed());p.getPersistentData().put(FLIGHT_KEY,saved);a.mayfly=true;p.onUpdateAbilities();}
            p.fallDistance=0;
        }else revokeFlight(p);
        if(AQUATIC.contains(species)&&p.isInWaterOrBubble()){
            p.setAirSupply(p.getMaxAirSupply());
            if(p.isSwimming()){var v=p.getDeltaMovement();if(v.lengthSqr()<.16)p.setDeltaMovement(v.scale(1.025));}
        }
        var speed=p.getAttribute(Attributes.MOVEMENT_SPEED);
        if(speed!=null){boolean land=!species.isEmpty()&&!FLYING.contains(species)&&!AQUATIC.contains(species)&&!FIRE.contains(species);
            if(land&&speed.getModifier(SPEED)==null)speed.addTransientModifier(new AttributeModifier(SPEED,"七十二变陆行",.15,AttributeModifier.Operation.MULTIPLY_TOTAL));
            if(!land)speed.removeModifier(SPEED);
        }
    }
    private static void revokeFlight(ServerPlayer p){
        if(!p.getPersistentData().contains(FLIGHT_KEY))return;
        var saved=p.getPersistentData().getCompound(FLIGHT_KEY);var previous=new Flight(saved.getBoolean("allowed"),saved.getBoolean("flying"),saved.getFloat("speed"));p.getPersistentData().remove(FLIGHT_KEY);
        var a=p.getAbilities();
        // Creative/spectator owns flight independently. Restore the pre-transformation grant otherwise.
        if(!p.isCreative()&&!p.isSpectator()){a.mayfly=previous.allowed;a.flying=previous.allowed&&previous.flying;a.setFlyingSpeed(previous.speed);p.fallDistance=0;}
        p.onUpdateAbilities();
    }
    public static void clear(ServerPlayer p){revokeFlight(p);var a=p.getAttribute(Attributes.MOVEMENT_SPEED);if(a!=null)a.removeModifier(SPEED);}
    private QitianShapeAbilities(){}
}
