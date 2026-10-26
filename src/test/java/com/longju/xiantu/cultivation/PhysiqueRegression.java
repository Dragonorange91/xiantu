package com.longju.xiantu.cultivation;

import com.longju.xiantu.formation.FormationRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import java.util.UUID;

public final class PhysiqueRegression {
    private static int checks;
    private static void check(boolean value,String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }
    private static void near(double actual,double expected,String label) { check(Math.abs(actual-expected)<1e-7,label+": "+actual); }
    public static void main(String[] args) {
        double[] weights=PhysiqueType.defaultWeights();
        check(weights.length==12,"12 independent specializations");
        near(java.util.Arrays.stream(weights).sum(),100,"Default weights total 100");
        int[] bins=new int[12];
        for (int i=0;i<10000;i++) bins[PhysiqueType.roll((i+.5)/10000,weights).ordinal()]++;
        for (PhysiqueType body:PhysiqueType.values()) {
            check(bins[body.ordinal()]==body.defaultWeight*100,"Exact weighted interval "+body.id);
            check(PhysiqueType.byId(body.id)==body,"Stable ID "+body.id);
            check(!body.description().isEmpty(),"Readable effect description "+body.id);
            var data=new PlayerCultivationData();data.setPhysique(body);data.setCultivationUnlocked(true);
            data.setPowerMax(100);data.addSpiritRoot(SpiritRootType.values()[0]);
            near(PhysiqueService.meditationRecovery(data),.1*body.meditationRecovery,"Only declared meditation recovery "+body.id);
            double ordinary=FormationRules.progressPerSecond(0,1,0,1,1,1,1,0);
            near(FormationRules.progressPerSecond(0,1,0,1,1,body.cultivation,1,0),ordinary*body.cultivation,"Separate speed factor "+body.id);
            near(PhysiqueService.effectiveProfessionExperience(data,.25),.25*body.professionExperience,"Reserved profession fractional gain "+body.id);
            near(PhysiqueService.effectiveSpellExperience(data,.25),.25*body.spellExperience,"Reserved spell fractional gain "+body.id);
            data.gongfa().learn("jin_guang_jue");data.gongfa().commit("jin_guang_jue",UUID.randomUUID());
            for (int i=0;i<40;i++) data.gongfa().activeProgress().add(PhysiqueService.effectiveMethodExperience(data,.025));
            near(data.gongfa().activeProgress().experience(),body.methodExperience==2?2:1,"XP does not round every tick "+body.id);
            var nbt=new CompoundTag();data.save(nbt);var loaded=new PlayerCultivationData();loaded.load(nbt);
            check(loaded.physique()==body && loaded.physiqueInitialized(),"NBT round trip "+body.id);
            check(((PlayerCultivationData)data.copy()).physique()==body,"Clone/snapshot retains body "+body.id);
            loaded.setRealmStage(8,3);loaded.setRealmStage(0,0);check(loaded.physique()==body,"Realm changes never alter body "+body.id);
        }
        check(PhysiqueType.roll(.5,new double[12])==PhysiqueType.ORDINARY,"Zero weights ordinary fallback");
        check(PhysiqueType.byId("unknown")==PhysiqueType.ORDINARY,"Unknown ID cannot create a boosted body");
        for (int i=0;i<12;i++) {
            double[] only=new double[12];only[i]=7;
            check(PhysiqueType.roll(0,only)==PhysiqueType.values()[i] && PhysiqueType.roll(1,only)==PhysiqueType.values()[i],"Zero/end-boundary roll "+i);
        }
        var source=new PlayerCultivationData();source.setPhysique(PhysiqueType.HUNYUAN);
        var random=RandomSource.create(114514);
        var death=source.deathPhysique(random,weights);
        double[] fixed=new double[12];fixed[(death.ordinal()+1)%12]=1;
        check(source.deathPhysique(random,fixed)==death,"Repeated death clone never rerolls original transaction");
        var nbt=new CompoundTag();source.save(nbt);var loaded=new PlayerCultivationData();loaded.load(nbt);
        check(loaded.deathPhysique(random,fixed)==death,"Pending death result survives save/reload");
        loaded.clearDeathPhysique();check(loaded.deathPhysique(random,fixed)!=death,"Next life gets a new transaction");
        var demon=new PlayerCultivationData();demon.setPhysique(PhysiqueType.JINGLIAN);
        for (int i=0;i<5;i++) PhysiqueService.addInnerDemon(demon,.00025);
        near(demon.innerDemon(),.001,"Tiny heart-demon events accumulate instead of disappearing");
        for(int i=0;i<5;i++) PhysiqueService.addInnerDemon(demon,1);
        near(demon.innerDemon(),4.001,"Reduction applies once to positive growth");
        demon.setInnerDemon(90);near(demon.innerDemon(),90,"Absolute load/admin setter is not discounted");
        PhysiqueService.addInnerDemon(demon,-20);near(demon.innerDemon(),90,"Negative delta ignored by positive-event API");
        PhysiqueService.addInnerDemon(demon,100);near(demon.innerDemon(),100,"Threshold remains capped at 100");
        var star=new PlayerCultivationData();star.setCultivationUnlocked(true);
        double base=BreakthroughService.successChance(star);star.setPhysique(PhysiqueType.XINGCHEN);
        near(BreakthroughService.successChance(star),base+.03,"Star body adds percentage points not a multiplier");
        star.setComprehension(20);star.setInnerDemon(100);check(BreakthroughService.successChance(star)>=.05,"Chance floor preserved");
        star.setInnerDemon(0);check(BreakthroughService.successChance(star)<=1,"Chance ceiling preserved");
        System.out.println("Physique regression: "+checks+" passed");
    }
}
