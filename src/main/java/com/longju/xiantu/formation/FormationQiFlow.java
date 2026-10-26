package com.longju.xiantu.formation;

/** Deterministic world-space streamlines; progress always travels from the rim to the seated cultivator. */
public final class FormationQiFlow {
    public static final double PERIOD=138, TRAIL=.38;
    public static int streams(int tier){return 4+Math.max(1,Math.min(3,tier));}
    public static double radius(int tier,double progress){return (FormationLayout.outerRadius(tier)+.15)*Math.pow(Math.max(0,1-progress),1.14);}
    public static double angle(double progress,double phase){return phase+progress*Math.PI*2.15;}
    public static double height(double progress,double phase){return .55+1.12*progress+Math.sin(progress*Math.PI)*(.38+.22*Math.sin(phase));}
    public static double envelope(double progress){return Math.sin(Math.PI*Math.max(0,Math.min(1,progress)));}
    /** Results are written into reused primitive arrays, avoiding per-frame vector allocations. */
    public static void point(int tier,double progress,double phase,double[] out,int index){
        double r=radius(tier,progress),a=angle(progress,phase);
        double curl=Math.sin(progress*19+phase*2)*.07*envelope(progress);
        out[index]=Math.cos(a)*(r+curl);out[index+1]=height(progress,phase)+curl*.7;out[index+2]=Math.sin(a)*(r+curl);
    }
    private FormationQiFlow(){}
}
