package com.longju.xiantu.formation;

/** Motion boundaries: all streams converge, stop at the seat, and begin/end transparently. */
public final class FormationQiFlowRegression {
    public static void main(String[] args){
        int count=0;double[] point=new double[3];
        for(int tier=1;tier<=3;tier++)for(int stream=0;stream<FormationQiFlow.streams(tier);stream++){
            double phase=stream*2.3999632297,last=Double.POSITIVE_INFINITY;
            for(int i=0;i<=1000;i++){
                double t=i/1000.0,r=FormationQiFlow.radius(tier,t);
                if(r>last||r<0)throw new AssertionError("Outward radial motion");last=r;
                FormationQiFlow.point(tier,t,phase,point,0);
                for(double value:point)if(!Double.isFinite(value))throw new AssertionError("Nonfinite stream coordinate");
                if(point[1]<.3||point[1]>2.5)throw new AssertionError("Qi escaped seated gathering region");count++;
            }
            if(Math.abs(point[0])+Math.abs(point[2])>1e-7||Math.abs(point[1]-1.67)>1e-7)throw new AssertionError("Stream missed formation center");
        }
        if(FormationQiFlow.envelope(0)!=0||Math.abs(FormationQiFlow.envelope(1))>1e-7)throw new AssertionError("Visible loop seam");
        System.out.println("Formation inward qi flow PASS: "+count+" positions, three radii, seated convergence, transparent endpoints");
    }
}
