package com.longju.xiantu.alchemy;
import net.minecraft.nbt.CompoundTag;
import java.util.*;
/** Fixed-point toxin and absolute server-time effect/cooldown deadlines. */
public final class MedicineData {
    private int totalDoses, toxinMilli, soberTicks, metabolismTicks;
    private final CompoundTag state = new CompoundTag();
    private final Map<String,Integer> permanentDoses = new HashMap<>();
    public int totalDoses() { return totalDoses; }
    public double toxicity() { return toxinMilli/1000.0; }
    public int soberTicks() { return soberTicks; }
    public CompoundTag state() { return state; }
    public int permanentDoses(String id) { return permanentDoses.getOrDefault(id,0); }
    public void setSummary(int doses,double toxicity) {
        totalDoses=Math.max(0,doses);
        toxinMilli=Double.isFinite(toxicity)?(int)Math.round(Math.max(0,Math.min(100,toxicity))*1000):0;
    }
    public void recordDose(String id,boolean permanent,double added) {
        if(totalDoses<Integer.MAX_VALUE) totalDoses++;
        if(permanent) permanentDoses.merge(id,1,(a,b)->a==Integer.MAX_VALUE?a:a+1);
        if(Double.isFinite(added)&&added>0) setSummary(totalDoses,toxicity()+added);
        soberTicks=metabolismTicks=0;if(state.contains("seat_mode"))state.putString("seat_mode","idle");state.remove("detox_active");state.remove("detox_ticks");
    }
    public void decay(double amount) { if(Double.isFinite(amount)&&amount>0) setSummary(totalDoses,toxicity()-amount); }
    public void onlineTick(boolean metabolize,boolean detox) {
        if(!metabolize)return;
        if(soberTicks<2400){soberTicks++;return;}
        if(detox){metabolismTicks=0;int ticks=state.getInt("detox_ticks")+1;
            if(ticks>=400){decay(1);ticks=0;}state.putInt("detox_ticks",ticks);
        }else if(++metabolismTicks>=2400){decay(1);metabolismTicks=0;}
    }
    public void effect(String key,double value,long until) {
        CompoundTag e=new CompoundTag();e.putDouble("value",value);e.putLong("until",until);state.put("effect_"+key,e);
    }
    public double effect(String key,long now) {var e=state.getCompound("effect_"+key);return e.getLong("until")>now?e.getDouble("value"):0;}
    public void clearEffect(String key){state.remove("effect_"+key);}
    public long cooldown(String group,long now){return Math.max(0,state.getLong("cd_"+group)-now);}
    public void cooldown(String group,long now,int seconds){if(seconds>0)state.putLong("cd_"+group,now+seconds*20L);}
    public void clearTemporary(){for(String k:Set.copyOf(state.getAllKeys()))if(k.startsWith("effect_")||k.startsWith("injury_")||k.startsWith("detox_")||k.startsWith("teleport_")||k.equals("seat_mode"))state.remove(k);}
    public double cultivationMultiplier(){return toxicity()<20?1:toxicity()<40?.9:toxicity()<60?.75:toxicity()<80?.6:.4;}
    public CompoundTag save(){
        CompoundTag t=new CompoundTag();t.putInt("total_doses",totalDoses);t.putInt("toxin_milli",toxinMilli);t.putDouble("toxicity",toxicity());
        t.putInt("sober_ticks",soberTicks);t.putInt("metabolism_ticks",metabolismTicks);t.put("state",state.copy());
        CompoundTag counts=new CompoundTag();permanentDoses.forEach(counts::putInt);t.put("permanent_doses",counts);return t;
    }
    public void load(CompoundTag t){
        setSummary(t.getInt("total_doses"),t.contains("toxin_milli")?t.getInt("toxin_milli")/1000.0:t.getDouble("toxicity"));
        soberTicks=Math.max(0,Math.min(2400,t.getInt("sober_ticks")));metabolismTicks=Math.max(0,Math.min(2399,t.getInt("metabolism_ticks")));
        for(String k:Set.copyOf(state.getAllKeys()))state.remove(k);state.merge(t.getCompound("state").copy());permanentDoses.clear();
        var counts=t.getCompound("permanent_doses");for(String k:counts.getAllKeys())if(counts.getInt(k)>0)permanentDoses.put(k,counts.getInt(k));
    }
    public void copyFrom(MedicineData source){load(source.save());}
}
