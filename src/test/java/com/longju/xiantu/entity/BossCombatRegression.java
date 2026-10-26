package com.longju.xiantu.entity;

public final class BossCombatRegression {
    private static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
    public static void main(String[] args){
        int count=0;
        for(String boss:new String[]{"meteor_general","spirit_ape_king","five_element_warden"}){
            var list=BossCombatSkills.forBoss(boss);check(list.size()>=5,boss);
            check(list.stream().filter(BossCombatSkills.Skill::ultimate).count()==1,"one final stage");
            double maximum=list.stream().filter(s->!s.ultimate()).mapToDouble(BossCombatSkills.Skill::multiplier).max().orElseThrow();
            for(var s:list){
                check(s.recovery()>=40,"two second recovery");check(s.cooldown()>=160,"independent cooldown");
                check(!s.ultimate() || s.windup()==30 && s.cooldown()>=600 && s.multiplier()<=maximum,"ultimate timing/damage cap");count++;
            }
        }
        var activeIds=CompletionBestiary.ALL.stream().map(CompletionBestiary.Definition::id).collect(java.util.stream.Collectors.toSet());
        var archivedIds=CompletionBestiary.ARCHIVED_BOSSES.stream().map(CompletionBestiary.Definition::id).collect(java.util.stream.Collectors.toSet());
        check(archivedIds.equals(java.util.Set.of("battle_general_soul","xuantians_remnant","cloud_sea_lord")),"temporarily archived humanoid bosses");
        check(java.util.Collections.disjoint(activeIds,archivedIds),"archived bosses are not registered in the active roster");
        check(activeIds.contains("meteor_general"),"Meteor General remains active");
        check(!BossCombatSkills.shapeHits("gap_ring",6,0,6,6,1,0),"front safe gap");
        check(BossCombatSkills.shapeHits("gap_ring",-6,0,6,6,1,0),"rear danger ring");
        check(!BossCombatSkills.shapeHits("wave",5,0,5,10,.5,.9),"jump clears shockwave");
        check(BossCombatSkills.shapeHits("wave",5,0,5,10,.5,0),"feet meet shockwave");
        check(!BossCombatSkills.shapeHits("fall",0,0,0,3,.3,0),"fall must travel before impact");
        check(BossCombatSkills.shapeHits("fall",0,0,0,3,.85,0),"landed fall hits");
        check(!BossCombatSkills.shapeHits("lanes",7,1.3,7,14,.5,0),"sword tide side gap");
        check(!BossCombatSkills.shapeHits("illusion",0,0,0,5,1,0),"illusion never damages");
        check(BossCombatSkills.shapeHits("fist_combo",2,0,2,3.8,.15,0),"first fist contact");
        check(BossCombatSkills.shapeHits("fist_combo",2,0,2,3.8,.48,0),"second fist contact");
        check(BossCombatSkills.shapeHits("fist_combo",2,0,2,3.8,.82,0),"third fist contact");
        check(!BossCombatSkills.shapeHits("fist_combo",2,0,2,3.8,.24,0),"fists recover between hits");
        check(!BossCombatSkills.shapeHits("fist_combo",-2,0,2,3.8,.15,0),"fist cannot strike behind");
        check(!BossCombatSkills.shapeHits("fist_combo",5,0,5,3.8,.15,0),"fist cannot reach remote target");
        check(BossCombatSkills.forBoss("xuantians_remnant").stream().noneMatch(s->s.shape().equals("fall_sequence")||s.shape().equals("line")),"city lord is a close combat boss");
        System.out.println("PASS "+count+" Boss timelines, recovery, final damage cap, travelling impacts and safe gaps");
    }
}
