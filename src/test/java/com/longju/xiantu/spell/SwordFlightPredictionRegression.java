package com.longju.xiantu.spell;

import static com.longju.xiantu.spell.SwordFlightMotion.*;
import java.util.ArrayDeque;

/** Real movement/replay algorithm under delayed, jittered and stalled acknowledgements. */
public final class SwordFlightPredictionRegression {
 private static int checks;
 private static final State ORIGIN=new State(0,100,0,0,0,0);
 private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 private record Snapshot(int due,int sequence,State state){}
 public static void main(String[] args){
  for(int delay=0;delay<=4;delay++){
   var prediction=new SwordFlightPrediction(ORIGIN);var snapshots=new ArrayDeque<Snapshot>();var authoritative=ORIGIN;
   for(int tick=0;tick<240;tick++){
    // Include release, turns, diagonal ascent and a boost transition.
    var input=new Input(tick,tick<150?1:0,tick>70&&tick<100?.5F:0,tick>90&&tick<110?.5F:0,tick<100?0:70,tick<80?0:-10,tick>35&&tick<125);
    authoritative=step(authoritative,input);prediction.advance(input,SwordFlightMotion::step);
    snapshots.add(new Snapshot(tick+delay,tick,authoritative));
    while(!snapshots.isEmpty()&&snapshots.getFirst().due<=tick){var s=snapshots.removeFirst();prediction.reconcile(s.sequence,s.state,SwordFlightMotion::step);}
    check(distance(prediction.state(),authoritative)<1e-9,"input replay equals authoritative flight across delayed turns/stop; delay="+delay);
    check(prediction.pending()<=delay,"acknowledged inputs do not accumulate");
   }
  }
  var prediction=new SwordFlightPrediction(ORIGIN);
  for(int tick=0;tick<6;tick++)prediction.advance(new Input(tick,1,0,0,0,0,true),SwordFlightMotion::step);
  var last=prediction.state();
  for(int tick=6;tick<200;tick++)prediction.advance(new Input(tick,1,0,0,0,0,true),SwordFlightMotion::step);
  check(prediction.state().equals(last)&&prediction.pending()==6,"server stall freezes prediction within six steps, never unbounded flight");
  prediction.reconcile(199,ORIGIN,SwordFlightMotion::step);
  check(prediction.pending()==0&&prediction.state().equals(ORIGIN),"server collision/stop overrides all pending guesses");
  check(!prediction.reconcile(198,new State(999,100,0,0,0,0),SwordFlightMotion::step)&&prediction.state().equals(ORIGIN),"stale acknowledgement cannot move player");
  prediction.advance(new Input(200,1,0,0,0,0,false),SwordFlightMotion::step);
  check(prediction.state().z()>0,"flight resumes after stalled server recovers");
  State normal=ORIGIN,boost=ORIGIN;for(int i=0;i<50;i++){normal=step(normal,new Input(i,1,0,0,0,0,false));boost=step(boost,new Input(i,1,0,0,0,0,true));}
  check(Math.abs(normal.vz()-.8)<1e-9&&Math.abs(boost.vz()-1.4)<1e-9,"accepted normal and boost speeds retained");
  State diagonal=ORIGIN;for(int i=0;i<80;i++)diagonal=step(diagonal,new Input(i,1,1,1,45,0,true));
  check(Math.sqrt(diagonal.vx()*diagonal.vx()+diagonal.vy()*diagonal.vy()+diagonal.vz()*diagonal.vz())<=1.40000001,"diagonal input cannot exceed speed cap");
  System.out.println("Sword flight prediction regression PASS "+checks+" assertions");
 }
 private static double distance(State a,State b){return Math.abs(a.x()-b.x())+Math.abs(a.y()-b.y())+Math.abs(a.z()-b.z())+Math.abs(a.vx()-b.vx())+Math.abs(a.vy()-b.vy())+Math.abs(a.vz()-b.vz());}
}
