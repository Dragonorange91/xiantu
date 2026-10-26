package com.longju.xiantu.refining;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Presentation catalogue only. Existing server combat code remains the authority for actual effects. */
public final class WeaponSkillSettings {
    public record Setting(String theme,String name,String base,String condition,String activation,int minTier,int maxTier){}
    private static final Map<String,Setting> ALL=load();
    public static Setting get(String theme){return ALL.get(theme);}
    private static Map<String,Setting> load(){
        try(var in=WeaponSkillSettings.class.getResourceAsStream("/data/xiantu/refining/weapon_skill_settings.json")){
            if(in==null)throw new IllegalStateException("Missing weapon skill settings");var root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();if(root.get("schema_version").getAsInt()!=1)throw new IllegalStateException("Weapon skill schema");var map=new LinkedHashMap<String,Setting>();
            for(var item:root.getAsJsonArray("ordinary")){var n=item.getAsJsonObject();var s=new Setting(n.get("theme").getAsString(),n.get("name").getAsString(),n.get("base").getAsString(),n.get("condition").getAsString(),n.get("activation").getAsString(),n.get("min_tier").getAsInt(),n.get("max_tier").getAsInt());if(map.put(s.theme,s)!=null)throw new IllegalStateException("Duplicate weapon skill");}
            if(map.size()!=30)throw new IllegalStateException("Incomplete weapon skills");return Map.copyOf(map);
        }catch(IOException failure){throw new UncheckedIOException(failure);}
    }
    private WeaponSkillSettings(){}
}
