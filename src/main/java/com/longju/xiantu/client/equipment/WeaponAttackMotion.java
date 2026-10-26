package com.longju.xiantu.client.equipment;

/** Authored visual clips in degrees; no gameplay timing or attack damage is changed. */
public final class WeaponAttackMotion {
    public enum Kind { NONE, SWORD, AXE, SPEAR, STAFF }
    public record Pose(float x,float y,float z,float pitch,float yaw,float roll,
                       float armPitch,float armYaw,float armRoll,float bodyYaw,float support,float weight) {
        public Pose mirrored(){return new Pose(-x,y,z,pitch,-yaw,-roll,armPitch,-armYaw,-armRoll,-bodyYaw,support,weight);}
        public float[] values(){return new float[]{x,y,z,pitch,yaw,roll,armPitch,armYaw,armRoll,bodyYaw,support,weight};}
        public static Pose of(float[] a){return new Pose(a[0],a[1],a[2],a[3],a[4],a[5],a[6],a[7],a[8],a[9],a[10],a[11]);}
    }
    public static final Pose REST=new Pose(0,0,0,0,0,0,0,0,0,0,0,0);
    private static Pose p(float x,float y,float z,float pitch,float yaw,float roll,
                          float armPitch,float armYaw,float armRoll,float body,float support){
        return new Pose(x,y,z,pitch,yaw,roll,armPitch,armYaw,armRoll,body,support,1);
    }
    // Short preparation, a single decisive stroke, then a longer relaxed recovery.
    private static final Pose[] SWORD={REST,
        p(.04f,.035f,.025f,-14,-15,16,-65,-18,12,-5,0),
        p(-.12f,-.055f,-.16f,15,22,-32,-72,23,-22,7,0),
        p(-.15f,-.09f,-.08f,24,27,-38,-44,28,-25,8,0),REST};
    private static final Pose[] AXE={REST,
        p(.025f,.10f,.035f,-28,-5,7,-100,-9,7,-5,0),
        p(-.025f,-.10f,-.20f,24,5,-10,-57,7,-8,7,0),
        p(-.04f,-.15f,-.10f,35,7,-14,-30,10,-10,8,0),REST};
    private static final Pose[] SPEAR={REST,
        p(.015f,-.01f,.06f,-8,-4,3,-42,-9,-3,-4,0),
        p(-.01f,.015f,-.45f,-20,2,2,-78,-3,-2,5,0),
        p(-.01f,.01f,-.34f,-17,2,2,-68,-3,-2,4,0),REST};
    private static final Pose[] STAFF={REST,
        p(.09f,.025f,.04f,-8,-23,15,-60,-26,16,-8,0),
        p(-.15f,-.04f,-.18f,8,28,-29,-73,29,-22,9,0),
        p(-.18f,-.08f,-.08f,14,35,-35,-42,36,-27,10,0),REST};
    private static final float[] TIMES={0,.16f,.36f,.53f,1};
    private static final float[][][] CURVES={new float[0][],data(SWORD),data(AXE),data(SPEAR),data(STAFF)};
    private static float[][] data(Pose[] keys){float[][] a=new float[keys.length][];for(int i=0;i<a.length;i++)a[i]=keys[i].values();return a;}
    public static boolean custom(Kind kind){return kind==Kind.SPEAR;}
    public static float duration(Kind kind){return switch(kind){case AXE,STAFF->12;case SWORD,SPEAR->10;default->1;};}
    public static Kind kind(String kind){return switch(kind){case "sword"->Kind.SWORD;case "axe"->Kind.AXE;case "spear"->Kind.SPEAR;case "staff"->Kind.STAFF;default->Kind.NONE;};}
    public static Pose sample(Kind kind,float progress){
        if(!Float.isFinite(progress)||progress<=0||progress>=1||kind==Kind.NONE)return REST;
        if(kind==Kind.SPEAR)return VanillaSpearMotion.sample(progress);
        float[][] keys=CURVES[kind.ordinal()];
        int i=0;while(progress>TIMES[i+1])i++;
        float h=TIMES[i+1]-TIMES[i],t=(progress-TIMES[i])/h;
        float[] a=keys[i],b=keys[i+1],out=new float[12];
        for(int j=0;j<12;j++){
            float m0=tangent(keys,i,j),m1=tangent(keys,i+1,j);
            out[j]=hermite(a[j],b[j],m0*h,m1*h,t);
        }
        return Pose.of(out);
    }
    // Shape-preserving tangents: pass through the strike without stopping; never overshoot a joint limit.
    private static float tangent(float[][] keys,int i,int j){
        if(i==0||i==keys.length-1)return 0;
        float a=(keys[i][j]-keys[i-1][j])/(TIMES[i]-TIMES[i-1]);
        float b=(keys[i+1][j]-keys[i][j])/(TIMES[i+1]-TIMES[i]);
        return a*b<=0?0:2*a*b/(a+b);
    }
    public static float hermite(float a,float b,float va,float vb,float t){
        float t2=t*t,t3=t2*t;
        return (2*t3-3*t2+1)*a+(t3-2*t2+t)*va+(-2*t3+3*t2)*b+(t3-t2)*vb;
    }
    private WeaponAttackMotion(){}
}
