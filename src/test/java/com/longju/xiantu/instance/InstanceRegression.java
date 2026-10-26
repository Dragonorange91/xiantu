package com.longju.xiantu.instance;

import java.util.*;

public final class InstanceRegression {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static int resourceCap(InstanceRules.Abundance a){return Math.min(8,a.maxRealm+1);}
    public static void main(String[] args){
        int[] abundance=new int[5],size=new int[3];
        for(int i=0;i<100;i++){abundance[InstanceRules.abundance(i).ordinal()]++;size[InstanceRules.size(i).ordinal()]++;}
        check(Arrays.equals(abundance,new int[]{20,30,25,18,7}),"exact abundance weights");
        check(Arrays.equals(size,new int[]{40,40,20}),"independent size weights");
        var themes=new HashSet<Integer>();var modes=new HashSet<Boolean>();
        for(int seed=0;seed<120;seed++){
            var t=new InstanceTerrain(seed,16);themes.add(t.theme());modes.add(t.natural());
            check(t.generationRadius()==160,"exact two chunk generation buffer");
            check(!t.generates(161,0)&&!t.generates(-161,0)&&!t.generates(0,161),"outside session bounds");
            check(new InstanceTerrain(seed,192).generationRadius()==1568,"large generation bounds");
            check(t.sanctuary(0,0)&&!t.sanctuary(40,40),"safe arrival is local");
            boolean differs=false;var other=new InstanceTerrain(seed+1,16);
            for(int x=-130;x<=130;x+=13)for(int z=-130;z<=130;z+=13){
                double h=t.noise(x,z,128,11);check(h>=-1&&h<=1,"bounded biome patch noise");check(h==new InstanceTerrain(seed,16).noise(x,z,128,11),"biome patch reload reproducibility");
                differs|=h!=other.noise(x,z,128,11);
            }
            check(differs,"new seed changes biome patch input, native terrain tested in game");
        }
        check(themes.size()==6&&modes.size()==2,"six themes and natural/ruins modes reachable");
        var random=new Random(20261023);
        for(int i=0;i<10000;i++){
            var p=InstanceRules.draw(random);var a=p.abundance();var s=p.size();
            check(p.diameterChunks()>=s.minChunks&&p.diameterChunks()<=s.maxChunks,"diameter bounds");
            check(p.minutes()>=a.minMinutes&&p.minutes()<=a.maxMinutes,"duration bounds");
            if(a.minRealm==0)check(a.maxRealm==1&&resourceCap(a)==2,"Qi-entry realms cap guardians and resources at Foundation");
            check(!p.soulCrystal()||a.maxRealm>=3,"low realm never grants soul crystal");
            int normal=InstanceRules.guardianRealm(a,false,new Random(i)),high=InstanceRules.guardianRealm(a,true,new Random(i));
            check(normal>=a.minRealm&&high>=normal&&high<=a.maxRealm,"high risk biases realm without overrank");
            int resource=InstanceRules.resourceTier(a,true,random);check(resource>=a.minRealm+1&&resource<=Math.min(8,a.maxRealm+1),"legal resource tier");
            check(!InstanceRules.eligible(p,false,a.minRealm)&&InstanceRules.eligible(p,true,a.minRealm),"cultivation gate");
            check(!InstanceRules.eligible(p,true,a.maxRealm+1),"upper realm gate");
            long closedAt=1790000000000L+i;check(InstanceRules.nextRealmAt(closedAt)-closedAt==7200000L,"exact two real hours after previous close");
        }

        var ledger=new InstanceLedger();check(ledger.slots().size()==272,"separate 16/256 slot pools");
        var realm=ledger.reserve(InstanceLedger.Kind.REALM,null).orElseThrow();
        check(ledger.reserve(InstanceLedger.Kind.REALM,null).isEmpty(),"one global realm lock");
        var owner=UUID.randomUUID();var cave=ledger.reserve(InstanceLedger.Kind.CAVE,owner).orElseThrow();
        check(ledger.reserve(InstanceLedger.Kind.CAVE,owner).isEmpty(),"one cave per owner");
        check(ledger.activate(cave)&&!ledger.beginClose(cave),"permanent caves cannot be expired");
        check(ledger.activate(realm)&&!ledger.activate(realm),"idempotent activation");
        var stale=new InstanceLedger.Slot(realm.kind(),realm.index(),realm.generation()+1,realm.instance(),null,realm.state());
        check(!ledger.beginClose(stale),"stale generation rejected");
        check(ledger.beginClose(realm)&&ledger.quarantine(realm),"closing and quarantine");
        check(ledger.reserve(InstanceLedger.Kind.REALM,null).isEmpty(),"quarantine alone cannot release global lock");
        var loaded=InstanceLedger.load(ledger.save());check(loaded.save().equals(ledger.save()),"restart preserves identities, ownership and state");
        check(loaded.reserve(InstanceLedger.Kind.CAVE,owner).isEmpty(),"restart never creates a second cave");
        String corrupt=ledger.save().replace(realm.instance().toString(),cave.instance().toString());
        boolean rejected=false;try{InstanceLedger.load(corrupt);}catch(RuntimeException expected){rejected=true;}check(rejected,"duplicate identity fails closed");
        check(!loaded.cleanedAfterArchive(cave)&&!loaded.cleanedAfterArchive(stale),"cleanup refuses permanent caves and stale generations");
        check(loaded.cleanedAfterArchive(realm),"closed realm can only be reclaimed after verified archive confirmation");
        check(!loaded.cleanedAfterArchive(realm),"duplicate cleanup cannot release a later allocation");
        var recycled=InstanceLedger.load(loaded.save()).reserve(InstanceLedger.Kind.REALM,null).orElseThrow();
        check(recycled.generation()==realm.generation()+1&&!recycled.instance().equals(realm.instance()),"recycling preserves epoch and creates new identity");
        check(!loaded.activate(realm),"old realm identity cannot be reactivated after cleanup");
        String secret="ac".repeat(32),dimension="xiantu:dongfu_000",instance=UUID.randomUUID().toString(),creator=owner.toString();
        String seal=TokenSeal.sign(secret,dimension,instance,1,creator,"master",1);
        check(TokenSeal.matches(seal,seal)&&!TokenSeal.matches(seal,"forged"),"constant-time well formed token verification");
        check(!TokenSeal.matches(seal,TokenSeal.sign(secret,dimension,instance,1,creator,"master",2)),"master version is signed");
        check(!TokenSeal.matches(seal,TokenSeal.sign(secret,dimension,instance,1,UUID.randomUUID().toString(),"master",1)),"owner is signed");
        check(!TokenSeal.matches(seal,TokenSeal.sign(secret,"xiantu:dongfu_001",instance,1,creator,"master",1)),"dimension is signed");
        check(!TokenSeal.matches(seal,TokenSeal.sign(secret,dimension,instance,2,creator,"master",1)),"generation is signed");
        check(!TokenSeal.matches(seal,TokenSeal.sign(secret,dimension,instance,1,creator,UUID.randomUUID().toString(),1)),"child authorization cannot become master");
        System.out.println("Instance foundation regression PASS: "+checks);
    }
}
