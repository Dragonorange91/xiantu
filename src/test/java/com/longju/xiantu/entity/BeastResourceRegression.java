package com.longju.xiantu.entity;
import java.util.*;
public final class BeastResourceRegression {
    public static void main(String[] args) {
        require(BeastResourceRules.SPECIES.size()==30,"thirty species retained");
        require(BeastResourceRules.candidates("crag_rock_spider").equals(List.of("beast_silk","beast_venom_sac")),"spider anatomy");
        require(BeastResourceRules.mainMaterial("crag_rock_spider").equals("beast_carapace"),"spider carapace");
        require(BeastResourceRules.mainMaterial("purple_lightning_bat").equals("beast_hide"),"bat membrane is hide");
        require(BeastResourceRules.mainMaterial("thunder_feather_raven").equals("beast_hide"),"raven exclusive feather does not change main material");
        require(BeastResourceRules.candidates("cracked_blade_wolf").size()==4,"wolf four candidates");
        require(BeastResourceRules.candidates("iron_tooth_badger").size()==2,"registered badger id");
        Random r=new Random(20261020);int passed=0;Map<String,Integer> frequencies=new TreeMap<>();
        for(int i=0;i<100000;i++){
            var choices=BeastResourceRules.candidates("cracked_blade_wolf");
            String result=BeastResourceRules.selectAuxiliary("cracked_blade_wolf",.5,r.nextDouble(),r.nextInt(choices.size()));
            if(!result.isEmpty()){passed++;frequencies.merge(result,1,Integer::sum);}
        }
        require(passed>49000&&passed<51000,"single fifty percent roll");
        for(var e:frequencies.entrySet())require(e.getValue()>11800&&e.getValue()<13200,"uniform candidates "+e.getKey());
        require(BeastResourceRules.selectAuxiliary("crag_rock_spider",0,0,0).isEmpty(),"disabled config");
        require(BeastResourceRules.selectAuxiliary("crag_rock_spider",.5,.5,0).isEmpty(),"exclusive probability boundary");
        require(BeastResourceRules.selectAuxiliary("electric_marten",1,0,0).isEmpty(),"no invented anatomy");
        System.out.println("Beast resources: 100000 trials, "+passed+" extra drops, "+frequencies);
    }
    private static void require(boolean v,String message){if(!v)throw new AssertionError(message);}
}
