package com.longju.xiantu.gongfa;

import com.longju.xiantu.cultivation.PlayerCultivationData;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

public final class GongfaRegression {
    private static int checks;
    private static void check(boolean value,String label) { if(!value)throw new AssertionError(label);checks++; }
    public static void main(String[] args) {
        for(var method:GongfaRegistry.ALL) {
            if(method.basicElemental()) {
                check(!method.learnableAt(0,7) && method.learnableAt(0,8),"Qi nine foundation bridge: "+method.id());
                check(method.learnableAt(1,0),"Foundation basic method: "+method.id());
            } else if(method.tierIndex()>0 && !method.special()) check(!method.learnableAt(0,8),"Ordinary method minimum Foundation: "+method.id());
        }
        for(var method:GongfaRegistry.ALL)if(method.tierIndex()>0){
            check(method.learnableAt(8,0)&&method.usableAt(8),"high cultivation may study/use lower methods: "+method.id());
            if(method.minRealm()>0)check(!method.learnableAt(method.minRealm()-1,0),"minimum realm still enforced: "+method.id());
        }
        check(GongfaRegistry.get("xuan_shui_jue").maxRealm()==2,"low method breakthrough ceiling retained");
        check(GongfaRegistry.get("tai_chu_dao_jing").learnableAt(0,0),"Special method early study");
        GongfaData data=new GongfaData();
        check(!data.learn("xuan_yuan_yang_qi_jue"),"Entrance decision cannot occupy a method slot");
        check(data.learn("xuan_shui_jue") && !data.learn("xuan_shui_jue"),"Study is single-use per method");
        UUID first=UUID.randomUUID();check(data.commit("xuan_shui_jue",first),"First equip");
        check(!data.commit("xuan_shui_jue",UUID.randomUUID()),"Repeated equip cannot clear experience");
        for(int i=0;i<1199;i++)data.activeProgress().add(.05);
        check(data.activeLevel()==0 && data.activeProgress().experience()==59,"Tick experience preserves fractional points");
        data.activeProgress().add(.05);check(data.activeLevel()==1 && data.activeProgress().experience()==0,"Exactly sixty effective seconds");
        data.activeProgress().add(2610);check(data.activeLevel()==9,"Two sources combine to total 2670");
        data.activeProgress().add(999999);check(data.activeProgress().experience()==0 && data.activeProgress().milliRemainder()==0,"Layer nine does not retain overflow");
        data.learn("you_shui_zhen_jing");UUID replacement=UUID.randomUUID();
        check(data.commit("you_shui_zhen_jing",replacement),"Replacement equips new method");
        check(data.progress("xuan_shui_jue").level()==0 && data.activeLevel()==0,"Both methods clear on replacement");
        check(!data.commit("xuan_shui_jue",replacement),"Completed transaction cannot replay on another method");
        data.activeProgress().add(.025);var restored=new GongfaData();restored.load(data.save());
        restored.activeProgress().add(.975);check(restored.activeProgress().experience()==1,"Subpoint save reload retains experience");
        restored.commit("xuan_shui_jue",UUID.randomUUID());check(restored.activeLevel()==0,"Returning to old method starts at zero");
        for(int stage=0;stage<4;stage++)check(GongfaRules.replacementStage(1,stage)==new int[]{0,2,5,8}[stage],"Foundation rollback mapping "+stage);
        for(int realm=2;realm<9;realm++)for(int stage=0;stage<4;stage++)
            check(GongfaRules.replacementRealm(realm)==realm-1 && GongfaRules.replacementStage(realm,stage)==stage,"One-major rollback "+realm+":"+stage);
        check(GongfaRules.killExperience(1,1,false,false)==20,"Ordinary hostile kill");
        check(GongfaRules.killExperience(3,1,false,false)==5,"Two-major penalty");
        check(GongfaRules.killExperience(1,4,false,true)==400,"Boss multiplier");
        for(var method:GongfaRegistry.ALL) {
            if(method.tierIndex()==0)continue;
            check(method.cultivationMultiplier(0)==1 && Math.abs(method.cultivationMultiplier(9)-method.maximumMultiplier())<1e-9,"Layer curve "+method.id());
            check(method.powerBonus(0)==0 && method.recoveryBonus(0)==0,"No free full-layer bonus "+method.id());
        }
        check(GongfaRegistry.get("xuan_shui_jue").usableAt(2),"Entry method spans through Golden Core");
        check(GongfaRegistry.get("tai_qing_hun_yuan_jing").usableAt(1) && GongfaRegistry.get("tai_qing_hun_yuan_jing").usableAt(6),"Intermediate method supports rollback and Body Combining");
        check(GongfaRegistry.get("tai_shang_yan_dao_jing").usableAt(5) && GongfaRegistry.get("tai_shang_yan_dao_jing").usableAt(8),"High method supports rollback and Tribulation");
        for (var method:GongfaRegistry.ALL) {
            if (method.tierIndex()==0) continue;
            check(method.attackBonus(0)==0 && method.healthBonus(0)==0 && method.armorBonus(0)==0,
                    "No combat attribute before gaining method layers "+method.id());
            check(Math.abs(method.attackBonus(4)*2-method.attackBonus(8))<1e-9
                    && Math.abs(method.healthBonus(4)*2-method.healthBonus(8))<1e-9
                    && Math.abs(method.armorBonus(4)*2-method.armorBonus(8))<1e-9,
                    "Combat attributes scale linearly with layers "+method.id());
            check(method.buff() != null && method.buffBonus(0) == 0 && method.buffBonus(9) > 0,
                    "Each main method has a positive passive buff that starts at layer one "+method.id());
            check(Math.abs(method.buffBonus(4)*2-method.buffBonus(8))<1e-9,
                    "Passive buff scales linearly with layers "+method.id());
        }
        check(GongfaRegistry.get("xuan_yuan_yang_qi_jue").attackBonus(9)==0
                && GongfaRegistry.get("xuan_yuan_yang_qi_jue").healthBonus(9)==0
                && GongfaRegistry.get("xuan_yuan_yang_qi_jue").armorBonus(9)==0
                && GongfaRegistry.get("xuan_yuan_yang_qi_jue").buff()==null,"Entrance manual has no combat bonuses");
        check(Math.abs(GongfaRules.combatMultiplier(.08,.15)-1.23)<1e-9,"Physique and technique bonuses add in one multiplier zone");
        check(GongfaRules.combatMultiplier(.4,2.0)==2.2,"Common attack multiplier cap applies after adding bonuses");
        var player=new PlayerCultivationData();player.setCultivationUnlocked(true);player.gongfa().copyFrom(data);
        CompoundTag tag=new CompoundTag();player.save(tag);var clone=new PlayerCultivationData();clone.load(tag);
        check(clone.cultivationUnlocked() && clone.gongfa().active().equals(player.gongfa().active()),"Full capability persistence");
        clone.gongfa().clear();check(clone.cultivationUnlocked(),"Clearing death-sensitive methods preserves entry qualification");
        System.out.println("Gongfa regression: "+checks+" passed");
    }
}
