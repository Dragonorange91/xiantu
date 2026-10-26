package com.longju.xiantu.client.equipment;

import static com.longju.xiantu.client.equipment.WeaponAttackMotion.*;

/** Fixed-tick critically damped presentation. Retargeting never resets position or velocity. */
public final class WeaponAttackTimeline {
    private final Kind kind;
    private final float[] position=new float[12],velocity=new float[12],previous=new float[12],previousVelocity=new float[12];
    private float age=1000;
    public WeaponAttackTimeline(Kind kind){this.kind=kind;}
    public void attack(){age=0;}
    public void tick(){
        System.arraycopy(position,0,previous,0,12);System.arraycopy(velocity,0,previousVelocity,0,12);
        // Four deterministic integration steps avoid tying continuity to client FPS.
        for(int step=0;step<4;step++){
            age+=.25f;
            float[] target=sample(kind,age/duration(kind)).values();
            float omega=1.65f,dt=.25f,decay=(float)Math.exp(-omega*dt);
            for(int i=0;i<12;i++){
                float offset=position[i]-target[i],term=velocity[i]+omega*offset;
                position[i]=target[i]+(offset+term*dt)*decay;
                velocity[i]=(velocity[i]-omega*term*dt)*decay;
            }
        }
        if(age>duration(kind)+20){java.util.Arrays.fill(position,0);java.util.Arrays.fill(velocity,0);}
    }
    public Pose pose(float partial){
        float t=Math.max(0,Math.min(1,partial));float[] a=new float[12];
        for(int i=0;i<12;i++)a[i]=hermite(previous[i],position[i],previousVelocity[i],velocity[i],t);
        a[10]=Math.max(0,Math.min(1,a[10]));a[11]=Math.max(0,Math.min(1,a[11]));return Pose.of(a);
    }
}
