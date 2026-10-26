package com.longju.xiantu.spell;

/** Shared deterministic flight math. Neither inputs nor prediction authorize server positions. */
public final class SwordFlightMotion {
 public static final double SPEED=.8, BOOST_SPEED=1.4;
 public record Input(int sequence,float forward,float strafe,float vertical,float yaw,float pitch,boolean boost){}
 public record State(double x,double y,double z,double vx,double vy,double vz){}
 public static State step(State s,Input i){
  double yaw=Math.toRadians(i.yaw),pitch=Math.toRadians(Math.max(-60,Math.min(60,i.pitch)));
  double x=-Math.sin(yaw)*Math.cos(pitch)*i.forward+Math.cos(yaw)*i.strafe;
  double y=-Math.sin(pitch)*i.forward+i.vertical;
  double z=Math.cos(yaw)*Math.cos(pitch)*i.forward+Math.sin(yaw)*i.strafe;
  double length=Math.sqrt(x*x+y*y+z*z);if(length>1){x/=length;y/=length;z/=length;}
  double speed=(i.boost?BOOST_SPEED:SPEED)*.45;
  double vx=s.vx*.55+x*speed,vy=s.vy*.55+y*speed,vz=s.vz*.55+z*speed;
  if(length==0&&vx*vx+vy*vy+vz*vz<.0001)vx=vy=vz=0;
  return new State(s.x+vx,s.y+vy,s.z+vz,vx,vy,vz);
 }
 private SwordFlightMotion(){}
}
