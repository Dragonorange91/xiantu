package com.longju.xiantu.alchemy;
import net.minecraft.nbt.CompoundTag;
public final class MedicineRegression {
    private static int checks;
    private static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;}
    public static void main(String[] args){
        var m=new MedicineData();m.recordDose("pill_04",false,3.75);
        check(m.toxicity()==3.75&&m.totalDoses()==1,"milli toxin exact dose");
        for(int i=0;i<4800;i++)m.onlineTick(false,false);
        check(m.soberTicks()==0&&m.toxicity()==3.75,"creative and offline do not metabolize");
        for(int i=0;i<2400;i++)m.onlineTick(true,false);
        check(m.soberTicks()==2400&&m.toxicity()==3.75,"120 second no-medication delay");
        for(int i=0;i<2399;i++)m.onlineTick(true,false);
        check(m.toxicity()==3.75,"natural metabolism not early");m.onlineTick(true,false);
        check(m.toxicity()==2.75,"one toxin per 120 online seconds");
        for(int i=0;i<399;i++)m.onlineTick(true,true);
        check(m.toxicity()==2.75,"detox not early");m.onlineTick(true,true);
        check(m.toxicity()==1.75,"seated detox once per 20 seconds");
        m.state().putString("seat_mode","detox");m.state().putBoolean("detox_active",true);
        m.recordDose("pill_35",true,.1);check(m.soberTicks()==0,"new dose cancels detox and resets delay");
        check(m.state().getString("seat_mode").equals("idle")&&!m.state().getBoolean("detox_active"),"new dose pauses meditation rather than spending fuel");
        m.effect("rage",72,1600);m.cooldown("power",1000,20);
        var copy=new MedicineData();copy.load(m.save());check(copy.effect("rage",1599)==72&&copy.effect("rage",1600)==0,"absolute effect expiry");
        check(copy.cooldown("power",1399)==1&&copy.cooldown("power",1400)==0,"shared cooldown expiry");
        check(copy.permanentDoses("pill_35")==1&&copy.toxicity()==1.85,"persistent dose and toxin");
        copy.clearTemporary();check(copy.effect("rage",1000)==0&&copy.cooldown("power",1000)==400&&copy.toxicity()==1.85,"death retains toxin and cooldown");
        for(double[] row:new double[][]{{0,1},{19.999,1},{20,.9},{39.999,.9},{40,.75},{60,.6},{80,.4},{100,.4}}){m.setSummary(0,row[0]);check(m.cultivationMultiplier()==row[1],"toxin band "+row[0]);}
        m.recordDose("x",false,1000);check(m.toxicity()==100,"toxin bounded");m.decay(1000);check(m.toxicity()==0,"no negative toxin");m.recordDose("x",false,Double.NaN);check(m.toxicity()==0,"NaN ignored");
        for(var d:PillCatalog.ALL){check(d.values().size()==5,"all five qualities");for(int q=1;q<5;q++)check(d.value(q)<=d.value(q+1),"quality effect monotonic");}
        check(PillCatalog.ALL.size()==36,"only retained pills registered");
        System.out.println("Current pill regression: "+checks+" PASS");
    }
}
