package com.longju.xiantu.spell;

import java.util.ArrayDeque;
import java.util.function.BiFunction;
import static com.longju.xiantu.spell.SwordFlightMotion.*;

/** Bounded input replay. A stalled connection cannot keep flying into unseen terrain. */
public final class SwordFlightPrediction {
 public static final int MAX_PENDING=6;
 private final ArrayDeque<Input> pending=new ArrayDeque<>();
 private State state;
 private int acknowledged=-1;
 public SwordFlightPrediction(State state){this.state=state;}
 public State state(){return state;}
 public int pending(){return pending.size();}
 public void advance(Input input,BiFunction<State,Input,State> step){
  if(pending.size()>=MAX_PENDING)return;
  pending.addLast(input);state=step.apply(state,input);
 }
 public boolean reconcile(int ack,State authoritative,BiFunction<State,Input,State> step){
  if(ack<acknowledged)return false;
  acknowledged=ack;while(!pending.isEmpty()&&pending.getFirst().sequence()<=ack)pending.removeFirst();
  state=authoritative;for(var input:pending)state=step.apply(state,input);return true;
 }
}
