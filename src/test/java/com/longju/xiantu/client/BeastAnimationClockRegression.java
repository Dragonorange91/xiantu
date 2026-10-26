package com.longju.xiantu.client;

public final class BeastAnimationClockRegression {
    private static void near(double actual,double expected) {
        if(Math.abs(actual-expected)>1e-10)throw new AssertionError(actual+" != "+expected);
    }
    public static void main(String[] args) {
        near(BeastAnimationClock.deltaSeconds(0,1_000_000_000L,false,false),0);
        near(BeastAnimationClock.deltaSeconds(1_000_000_000L,1_016_666_667L,false,false),.016666667);
        near(BeastAnimationClock.deltaSeconds(1_000_000_000L,61_000_000_000L,true,false),0);
        near(BeastAnimationClock.deltaSeconds(1_000_000_000L,61_000_000_000L,false,true),0);
        near(BeastAnimationClock.deltaSeconds(1_000_000_000L,61_000_000_000L,false,false),.1);
        near(BeastAnimationClock.deltaSeconds(2_000_000_000L,1_000_000_000L,false,false),0);
        System.out.println("PASS beast animation clock: pause, resume, first frame and bounded elapsed time");
    }
}
