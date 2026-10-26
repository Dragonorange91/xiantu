package com.longju.xiantu.cultivation;
import com.longju.xiantu.formation.*;

public final class CultivationPacingRegression {
    private static int checks;
    private static void check(boolean condition,String label){checks++;if(!condition)throw new AssertionError(label);}
    private static int seconds(int roots, int stage) {
        double progress=0,stock=3600;int second=0;
        while(progress<RealmRegistry.get(0,stage).progressCap()&&second<3600){
            if(stock==0)stock=3600; // Move to a fresh ordinary chunk. Travel/wait time is excluded.
            var a=ChunkAuraRules.allocate(stock,3600,100,4);stock=a.remaining();
            progress+=FormationRules.baseRate(0)*FormationRules.rootMultiplier(roots)*a.concentration()/100*a.supply();second++;
        }
        return second;
    }
    public static void main(String[] args) {
        check(seconds(5,0)>=420&&seconds(5,0)<=540,"five-root Qi initial natural stage takes 7-9 active minutes");
        check(seconds(1,0)>=120&&seconds(1,0)<=180,"one-root Qi natural stage takes 2-3 active minutes");
        for(int stage=1;stage<9;stage++)check(seconds(5,stage)>seconds(5,stage-1),"minor stage grows in time as well as displayed requirement");
        check(seconds(5,8)<1200,"Qi final stage stays below twenty active minutes before bonuses");
        double[] roots={2,1.5,1,.8,.6};
        for(int i=0;i<5;i++)check(FormationRules.rootMultiplier(i+1)==roots[i],"preserve root differences");
        for(int r=0;r<9;r++)for(int t=0;t<9;t++)for(boolean boss:new boolean[]{false,true})for(boolean elite:new boolean[]{false,true}) {
            double cap=RealmRegistry.get(r,0).progressCap();double reward=KillCultivationRules.reward(cap,0,r,t,elite,boss);
            check(reward>=0&&reward<=cap*.05,"single kill bounded to 5 percent");
            check(KillCultivationRules.reward(cap,cap,r,t,elite,boss)==0,"full stage no reward");
            check(KillCultivationRules.reward(cap,cap-.001,r,t,elite,boss)<=.00100001,"discard overflow");
            if(t-r<=-3)check(reward==0,"outleveled target no reward");
        }
        check(Math.abs(KillCultivationRules.reward(330,0,0,0,false,false)-1.65)<1e-9,"ordinary same-realm reward");
        check(Math.abs(KillCultivationRules.reward(330,0,0,0,true,false)-4.95)<1e-9,"elite same-realm reward");
        check(Math.abs(KillCultivationRules.reward(330,0,0,0,true,true)-16.5)<1e-9,"boss does not stack elite multiplier");
        System.out.println("Cultivation pacing/kill rules PASS: "+checks+"; Qi first single/five-root active seconds="+seconds(1,0)+"/"+seconds(5,0)+"; final five-root="+seconds(5,8));
    }
}
