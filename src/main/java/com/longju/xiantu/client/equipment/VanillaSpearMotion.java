package com.longju.xiantu.client.equipment;

/** 1.21.11 jab phase/easing, retargeted from the Mojang item axis to Xiantu's +Y spear. */
public final class VanillaSpearMotion {
    /** Applied only by the normal first-person spear adapter; model display stays authored. */
    public static float handPitch(WeaponAttackMotion.Pose pose){return -10+pose.pitch();}
    private static float progress(float t,float a,float b){return Math.max(0,Math.min(1,(t-a)/(b-a)));}
    private static float sine(float t){return (float)(.5-.5*Math.cos(Math.PI*t));}
    private static float back(float t){float x=t-1;return 1+2.70158f*x*x*x+1.70158f*x*x;}
    private static float expo(float t){return t<=0?0:t>=1?1:t<.5?(float)(Math.pow(2,20*t)-1)/2046:1-(float)(Math.pow(2,20*(1-t))-1)/2046;}
    public static WeaponAttackMotion.Pose sample(float t){
        if(!Float.isFinite(t)||t<=0||t>=1)return WeaponAttackMotion.REST;
        float raise=sine(progress(t,0,.05f)),jab=back(progress(t,.05f,.20f)),recover=expo(progress(t,.4f,1));
        float aligned=raise-recover;
        // The authored -55deg display plus the -10deg idle correction gives
        // -65deg at rest. A -7deg jab reaches -72deg: ten degrees above the
        // previous -82deg strike, independently of the lower idle direction.
        float pitch=-7*aligned;
        // Xiantu's long +Y mesh needs a raised grip when horizontal. The vanilla
        // .65-block pullback crosses our camera near plane and hides the shaft.
        float z=.12f*raise-.48f*jab+.36f*recover;
        return new WeaponAttackMotion.Pose(-.20f*aligned,.28f*aligned,z,pitch,0,0,
            -34*aligned,0,0,0,0,aligned);
    }
    private VanillaSpearMotion(){}
}
