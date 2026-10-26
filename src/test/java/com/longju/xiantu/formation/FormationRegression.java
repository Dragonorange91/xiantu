package com.longju.xiantu.formation;

public final class FormationRegression {
    private static int passed;

    public static void main(String[] args) {
        check(FormationLayout.count(true) == 20, "Tier-one outer ring has exactly 20 blocks");
        check(FormationLayout.count(false) == 8, "Tier-one inner ring has exactly 8 blocks");
        check(!FormationLayout.isBaseRing(0, 0) && !FormationLayout.isRuneRing(0, 0), "Center remains open for the array eye");
        check(FormationLayout.isBaseRing(4, 0), "Outer radius edge is included");
        check(FormationLayout.isRuneRing(2, 0), "Inner radius edge is included");
        check(FormationLayout.count(2, true) == 56, "Tier two retains tier one and adds its outer base ring");
        check(FormationLayout.count(2, false) == 40, "Tier two retains tier one and adds a spaced rune ring");
        check(FormationLayout.count(3, true) == 120, "Tier three retains lower base rings and adds its outer ring");
        check(FormationLayout.count(3, false) == 88, "Tier three retains lower rune rings and adds a spaced rune ring");
        check(FormationLayout.innerRadius(2)==5 && FormationLayout.innerRadius(3)==8,
                "Added rune rings stay separated from preserved lower-tier base rings");
        check(FormationLayout.baseRingTierAt(3,4,0)==1 && FormationLayout.baseRingTierAt(3,7,0)==2
                        && FormationLayout.baseRingTierAt(3,10,0)==3,
                "Tier-three structure contains each base ring at its original tier");
        check(!FormationLayout.isBaseRing(2, 0, 0) && !FormationLayout.isRuneRing(3, 0, 0),
                "Higher-tier rings preserve a clear central meditation space");
        verifyBalance();
        verifyTraces();
        System.out.println("Formation regression: " + passed + " passed");
    }

    private static void verifyTraces() {
        for (int tier=1;tier<=3;tier++) for(int ringTier=1;ringTier<=tier;ringTier++) for(boolean outer:new boolean[]{true,false}) {
            var ring=FormationTrace.ringStage(ringTier,outer);
            var positions=new java.util.HashMap<String,Integer>();
            for(var t:ring) positions.put(t.x()+","+t.z(),t.trace());
            check(ring.size()==FormationLayout.countStage(ringTier,outer),"Trace footprint preserves layer count "+tier+"/"+ringTier+"/"+outer);
            int links=0;
            var diagonalQuadrants=new java.util.HashSet<String>();
            for(var t:ring) for(int p:FormationTrace.ports(t.trace())) {
                int x=t.x()+FormationTrace.DX[p],z=t.z()+FormationTrace.DZ[p];
                if(FormationTrace.DX[p]!=0&&FormationTrace.DZ[p]!=0)
                    diagonalQuadrants.add((FormationTrace.DX[p]>0?"E":"W")+(FormationTrace.DZ[p]>0?"S":"N"));
                Integer target=positions.get(x+","+z);
                check(target!=null&&java.util.Arrays.stream(FormationTrace.ports(target)).anyMatch(n->n==(p+4)%8),
                        "Reciprocal ring port "+tier+"/"+ringTier+"/"+outer+"/"+t.x()+","+t.z()+"/"+p);
                links++;
            }
            check(links==ring.size()*2,"Exactly two ports per closed ring tile "+tier+"/"+ringTier+"/"+outer);
            check(diagonalQuadrants.size()==4,"Four diagonal corner joins stay connected "+tier+"/"+ringTier+"/"+outer);
            var visited=new java.util.HashSet<String>();
            var queue=new java.util.ArrayDeque<String>();queue.add(ring.get(0).x()+","+ring.get(0).z());
            while(!queue.isEmpty()) {
                String key=queue.remove();if(!visited.add(key))continue;
                String[] xy=key.split(",");int x=Integer.parseInt(xy[0]),z=Integer.parseInt(xy[1]);
                for(int p:FormationTrace.ports(positions.get(key)))queue.add((x+FormationTrace.DX[p])+","+(z+FormationTrace.DZ[p]));
            }
            check(visited.size()==ring.size(),"Single connected cycle, no isolated components "+tier+"/"+ringTier+"/"+outer);
        }
        for(int trace=1;trace<=28;trace++) {
            check(FormationTrace.transform(trace,4,false,false)==trace,"Full rotation preserves trace "+trace);
            for(boolean mx:new boolean[]{false,true})for(boolean mz:new boolean[]{false,true})
                check(FormationTrace.transform(FormationTrace.transform(trace,0,mx,mz),0,mx,mz)==trace,"Mirror involution "+trace+"/"+mx+"/"+mz);
        }
    }

    private static void verifyBalance() {
        check(FormationRules.ticksForBudget(100_000L * 1200, 50) == 2400, "One full basic stone runs tier one for two minutes");
        check(FormationRules.ticksForBudget(310_000L * 1200, 200) == 1860, "Tier two blue stone runs for 93 seconds");
        check(FormationRules.ticksForBudget(2_790_000L * 1200, 600) == 5580, "Tier three fourth-tier stone runs for 279 seconds");
        check(FormationRules.ticksForBudget(299_000_000L * 1200, 600) == 598000, "Dao energy provides 8h18m20s");
        for (long n : new long[]{0,32767,32768,65536,29900,358800,1_000_000_000_000L}) {
            check(FormationRules.join(FormationRules.limb(n,0),FormationRules.limb(n,1),FormationRules.limb(n,2),FormationRules.limb(n,3)) == n,
                    "Long menu synchronization " + n);
        }
        double ordinaryHours = 0, fastestHours = 0;
        double[] techniques = {1.3,1.3,1.3,2.8,2.8,2.8,2.8,3,3};
        for (int realm = 0; realm < 9; realm++) {
            int rank = com.longju.xiantu.item.SpiritStoneAbsorptionRules.stoneRankForRealm(realm);
            int tier = realm == 0 ? 1 : realm <= 2 ? 2 : 3;
            check(!FormationRules.unsafe(realm,rank), "Corresponding stone is safe " + realm);
            if (rank < 8) check(FormationRules.unsafe(realm,rank+1), "Even one-tier bonus excess is unsafe " + realm);
            double total = com.longju.xiantu.cultivation.RealmRegistry.get(realm,0).progressCap()
                    * com.longju.xiantu.cultivation.RealmRegistry.STAGE_COUNTS[realm];
            ordinaryHours += total / FormationRules.progressPerSecond(realm,3,0,tier,rank,1,techniques[realm],0) / 3600;
            fastestHours += total / FormationRules.progressPerSecond(realm,1,0,tier,rank,1.5,3.2,.35) / 3600;
        }
        check(ordinaryHours > fastestHours && fastestHours > 0, "Bonuses speed cultivation without a zero duration");
        for(int realm=0;realm<9;realm++) {
            double minutes=com.longju.xiantu.cultivation.RealmRegistry.get(realm,0).progressCap()/FormationRules.baseRate(realm)/60;
            check(minutes>=4 && minutes<=20,"Unboosted natural stage duration stays bounded");
            if(realm>0)check(minutes>FormationRules.naturalStageMinutes(realm-1),"Higher realms take gradually longer");
        }
        check(FormationRules.bonusMultiplier(1,8)==1.3 && FormationRules.bonusMultiplier(2,8)==1.5,
                "Low arrays cap strength without modifying safety");
        var data = new com.longju.xiantu.cultivation.PlayerCultivationData();
        data.setRealmStage(0,0);
        for(int i=0;i<1200;i++) data.accumulateMeditation(.00001);
        check(Math.abs(data.progress()-.012)<1e-9, "Sub-milli gains are not lost by per-tick truncation");
        data.accumulateMeditation(.00025);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag(); data.save(tag);
        var restored = new com.longju.xiantu.cultivation.PlayerCultivationData(); restored.load(tag);
        restored.accumulateMeditation(.00075);
        check(Math.abs(restored.progress()-.013)<1e-9, "Save reload retains sub-milli remainder");
        data.setProgress(data.progressMax()-.001); data.accumulateMeditation(99);
        check(data.progress()==data.progressMax(), "Stage overflow is discarded");
        data.setRealmStage(1,0); data.setProgress(0); data.accumulateMeditation(.00075);
        check(data.progress()==0, "Reset cannot carry overflow to next stage");
    }

    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        passed++;
        System.out.println("[Formation PASS] " + name);
    }
}
