package com.longju.xiantu.talisman;
import net.minecraft.nbt.CompoundTag;
import java.util.*;
/** Server validates ordered arc coverage and elapsed ticks, never client success flags. */
public final class TalismanDrawing {
 public record Point(double x,double y){}
 public static List<Point> template(String id,int stroke){
  return TalismanGlyphs.stroke(id,stroke);
 }
 public int stroke,progress,precise,mistakes;public boolean dirty;private int within,samples;private long started=-1,last=-1;private Point previous;
 public boolean started(){return started>=0||stroke>0;}
 public boolean sample(String id,double x,double y,long tick,boolean end,int minimumTicks){
  if(!Double.isFinite(x)||!Double.isFinite(y)||x<0||x>1||y<0||y>1)return false;
  var path=resample(template(id,stroke));var at=new Point(x,y);
  if(started<0){if(distance(at,path.get(0))>.055){mistake();return false;}started=tick;previous=at;}
  if(last>tick)return false;last=tick;
  double d=distance(previous,at);int steps=Math.max(1,(int)Math.ceil(d/.0125));if(steps>128){mistake();return false;}
  for(int j=1;j<=steps;j++){
   var p=new Point(previous.x+(x-previous.x)*j/steps,previous.y+(y-previous.y)*j/steps);int best=progress;double nearest=Double.MAX_VALUE;
   for(int k=Math.max(0,progress-3);k<=Math.min(path.size()-1,progress+5);k++){double n=distance(p,path.get(k));if(n<nearest){nearest=n;best=k;}}
   if(nearest>.05||best<progress-2){mistake();previous=at;return false;}
   progress=Math.max(progress,best);samples++;if(nearest<=.025)within++;
  }
  previous=at;
  if(end){boolean complete=progress>=path.size()*.90&&distance(at,path.get(path.size()-1))<=.055&&tick-started>=minimumTicks;
   if(complete){if(!dirty&&samples>0&&within>=samples*.95)precise++;stroke++;progress=0;within=samples=0;dirty=false;started=last=-1;previous=null;return true;}mistake();progress=0;started=-1;previous=null;
  }return false;
 }
 private void mistake(){if(!dirty){mistakes++;dirty=true;}}
 private static double distance(Point a,Point b){return Math.hypot(a.x-b.x,a.y-b.y);}
 public static List<Point> resample(List<Point> points){var out=new ArrayList<Point>();out.add(points.get(0));for(int i=1;i<points.size();i++){var a=points.get(i-1);var b=points.get(i);int n=Math.max(1,(int)Math.ceil(distance(a,b)/.01));for(int j=1;j<=n;j++)out.add(new Point(a.x+(b.x-a.x)*j/n,a.y+(b.y-a.y)*j/n));}return out;}
 public CompoundTag save(){var n=new CompoundTag();n.putInt("Stroke",stroke);n.putInt("Precise",precise);n.putInt("Mistakes",mistakes);return n;}
 public static TalismanDrawing load(CompoundTag n){var d=new TalismanDrawing();d.stroke=Math.max(0,Math.min(7,n.getInt("Stroke")));d.precise=Math.max(0,Math.min(d.stroke,n.getInt("Precise")));d.mistakes=Math.max(0,n.getInt("Mistakes"));return d;}
}
