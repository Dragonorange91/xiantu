package com.longju.xiantu.client;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.*;

/** Small immutable tracks exported from the editable Blockbench choreography project. */
public final class AuthoredWeaponClips {
    private record Clip(float length,Map<String,float[][]> tracks){}
    private static Map<String,Clip> clips=Map.of();
    public static void reload(ResourceManager manager){
        Map<String,Clip> result=new HashMap<>();
        try(var reader=manager.openAsReader(new ResourceLocation("xiantu","animations/weapon_choreography.json"))){
            for(var entry:JsonParser.parseReader(reader).getAsJsonObject().entrySet()){
                var a=entry.getValue().getAsJsonObject();Map<String,float[][]> tracks=new HashMap<>();
                for(var bone:a.getAsJsonObject("tracks").entrySet())for(var channel:bone.getValue().getAsJsonObject().entrySet()){
                    var rows=channel.getValue().getAsJsonArray();float[][] values=new float[rows.size()][4];
                    for(int i=0;i<rows.size();i++)for(int j=0;j<4;j++)values[i][j]=rows.get(i).getAsJsonArray().get(j).getAsFloat();
                    tracks.put(bone.getKey()+"/"+channel.getKey(),values);
                }
                result.put(entry.getKey(),new Clip(a.get("length").getAsFloat(),Map.copyOf(tracks)));
            }
        }catch(Exception e){throw new IllegalStateException("Cannot load authored weapon clips",e);}
        clips=Map.copyOf(result);SkillCastAnimations.clear();
    }
    static String family(String skill){return switch(skill){
        case "shadow_sever"->"sword_cut";
        case "dragon_pierce"->"sword_thrust";
        case "godslayer_pierce","dao_corrosion","divine_reveal","sun_pierce","staff_extend"->"spear_thrust";
        case "star_shatter","heaven_split"->"axe_slam";
        case "chaos_suppress","spirit_sweep"->"staff_sweep";
        case "great_dao_quake","staff_quake"->"staff_slam";
        case "radiant_return","forged_guard","celestial_guard"->"guard";
        default->null;
    };}
    static float duration(String skill){String family=family(skill);var c=family==null?null:clips.get(family);return c==null?.7f:c.length;}
    static float[] sample(String skill,String bone,String channel,double seconds){
        String family=family(skill);var c=family==null?null:clips.get(family);if(c==null)return new float[3];
        var keys=c.tracks.get(bone+"/"+channel);if(keys==null||keys.length==0)return new float[3];
        if(seconds<=keys[0][0])return Arrays.copyOfRange(keys[0],1,4);
        if(seconds>=keys[keys.length-1][0])return Arrays.copyOfRange(keys[keys.length-1],1,4);
        int i=0;while(seconds>keys[i+1][0])i++;float h=keys[i+1][0]-keys[i][0],t=(float)(seconds-keys[i][0])/h;
        float[] out=new float[3];
        for(int j=1;j<=3;j++){
            float a=keys[i][j],b=keys[i+1][j];
            // Monotone Hermite avoids wrist overshoot between authored keys.
            out[j-1]=com.longju.xiantu.client.equipment.WeaponAttackMotion.hermite(a,b,tangent(keys,i,j)*h,tangent(keys,i+1,j)*h,t);
        }return out;
    }
    private static float tangent(float[][] k,int i,int j){if(i==0||i==k.length-1)return 0;float a=(k[i][j]-k[i-1][j])/(k[i][0]-k[i-1][0]),b=(k[i+1][j]-k[i][j])/(k[i+1][0]-k[i][0]);return a*b<=0?0:2*a*b/(a+b);}
    private AuthoredWeaponClips(){}
}
